/**
 * SOCKS 4, 4a and 5 server implementation that accepts connections before handing the channel to OneSwarm.
 */

package edu.washington.cs.oneswarm.f2f.socks;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.logging.Logger;

public class OSSocksServer implements Runnable {
    public final static Logger logger = Logger.getLogger(OSSocksServer.class.getName());

    private static final int BUFFER_SIZE = 1024;
    private final int localPort;
    private ServerSocketChannel serverSocket;
    private SocksCommandHandler.Interface handler;

    public OSSocksServer(int localPort, SocksCommandHandler.Interface handler) {
        this.localPort = localPort;
        this.handler = handler;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("SOCKS Proxy Server");
        try {
            serverSocket = ServerSocketChannel.open();
            serverSocket.socket().bind(new InetSocketAddress("localhost", localPort));

            while (true) {
                try {
                    SocketChannel incomingConnection = serverSocket.accept();
                    logger.fine("Accepted connection via SOCKS from "
                            + incomingConnection.socket().getRemoteSocketAddress());
                    new Thread(new OSSocksServerThread(handler, incomingConnection, BUFFER_SIZE))
                            .start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class OSSocksServerThread implements Runnable {
        private SocketChannel client;
        private SocksCommandHandler.Interface handler;

        private final int bufferSize;
        private ByteBuffer buf;

        public OSSocksServerThread(SocksCommandHandler.Interface handler, SocketChannel socket,
                int bufferSize) {
            this.handler = handler;
            client = socket;
            this.bufferSize = bufferSize;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("SOCKS Proxy Server - Connection Handler Thread");
            buf = ByteBuffer.allocate(bufferSize);
            try {
                InetAddress addr = ((InetSocketAddress) client.socket().getRemoteSocketAddress())
                        .getAddress();
                if (!addr.isAnyLocalAddress() && !addr.isLoopbackAddress()) {
                    client.close();
                    return;
                }

                readNBytes(buf, 1);
                byte version = buf.get();
                if (version == SocksConstants.Version.SOCKS_5) {
                    socks5Handshake();
                    socks5DoCommand();
                } else if (version == SocksConstants.Version.SOCKS_4) {
                    socks4DoCommand();
                } else {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                buf = null;
                client = null;
            }
        }

        private void socks5Handshake() throws IOException {
            readNBytes(buf, 1);
            int numAuthMethods = buf.get() & 0xff;
            readNBytes(buf, numAuthMethods);
            byte authMethod = SocksConstants.Authentication.NO_ACCEPTABLE_METHOD;
            while (buf.hasRemaining())
                if (buf.get() == SocksConstants.Authentication.NO_AUTHENTICATION)
                    authMethod = SocksConstants.Authentication.NO_AUTHENTICATION;

            client.write(ByteBuffer.wrap(new byte[] { SocksConstants.Version.SOCKS_5, authMethod }));
        }

        private void socks5DoCommand() throws IOException {
            readNBytes(buf, 4);
            byte version = buf.get();

            if (version != SocksConstants.Version.SOCKS_5)
                throw new SocksException(SocksConstants.Status.GENERAL_FAILURE);

            byte command = buf.get();
            buf.get(); // Reserved Byte
            byte addressType = buf.get();
            int length = 4;
            int port = 0;
            String addr = "";

            byte[] remoteID = new byte[0];

            try {
                switch (addressType) {
                case SocksConstants.AddressType.IPv4:
                case SocksConstants.AddressType.IPv6:
                    length *= addressType;
                    remoteID = new byte[length + 3];
                    remoteID[0] = addressType;
                    readNBytes(buf, length + 2);
                    buf.get(remoteID, 1, length + 2);
                    port = (remoteID[remoteID.length - 2] & 0xff) << 8
                            | (remoteID[remoteID.length - 1] & 0xff);
                    addr = InetAddress.getByAddress(Arrays.copyOfRange(remoteID, 1, length + 1)).getHostAddress();
                    break;

                case SocksConstants.AddressType.DOMAIN_NAME:
                    readNBytes(buf, 1);
                    length = buf.get() & 0xff;
                    remoteID = new byte[length + 4];
                    remoteID[0] = addressType;
                    remoteID[1] = (byte) length;
                    readNBytes(buf, length + 2);
                    buf.get(remoteID, 2, length + 2);
                    port = (remoteID[remoteID.length - 2] & 0xff) << 8
                            | (remoteID[remoteID.length - 1] & 0xff);
                    addr = new String(Arrays.copyOfRange(remoteID, 2, length + 2));
                    break;

                default:
                    throw new SocksException(SocksConstants.Status.ADDRESS_TYPE_NOT_SUPPORTED);
                }

                handler.doCommand(command, client, addr, port);

                sendSocks5Message(SocksConstants.Status.REQUEST_GRANTED, remoteID);

            } catch (SocksException e) {
                sendSocks5Message((byte) e.getErrorCode(), remoteID);
            }
        }

        private void socks4DoCommand() throws IOException {
            int port = 0;
            String addr = "";

            try {
                readNBytes(buf, 7);
                byte command = buf.get();
                port = (buf.get() & 0xff) << 8 | (buf.get() & 0xff);
                addr = (buf.get() & 0xff) + "." + (buf.get() & 0xff) + "." + (buf.get() & 0xff) + "." + (buf.get() & 0xff);

                // Throw away user name string
                byte character;
                do {
                    readNBytes(buf, 1);
                    character = buf.get();
                } while (character != 0x00);

                // SOCKS 4a
                if (addr.startsWith("0.0.0.") && !addr.endsWith("0")) {
                    addr = "";
                    readNBytes(buf, 1);
                    character = buf.get();
                    while (character != 0x00) {
                        addr += (char) character;
                        readNBytes(buf, 1);
                        character = buf.get();
                    }
                }

                handler.doCommand(command, client, addr, port);

                sendSocks4Message(SocksConstants.Status.REQUEST_GRANTED, port, addr);

            } catch (SocksException e) {
                sendSocks4Message((byte) e.getErrorCode(), port, addr);
            }
        }

        private void sendSocks5Message(byte statusByte, byte[] remoteID) throws IOException {
            // remoteID must be the bytes sent by the client to indicate the
            // remote
            // host and must be in the same format as origonally sent..
            // This must be appended to all messages sent to the client after
            // the
            // initial connection request. ie. for a url-based request, the
            // format
            // would be [ address type (0x03) | address length | each |
            // character |
            // of | the | address | port (high bytes) | port (low bytes) ]
            client.write(new ByteBuffer[] {
                    ByteBuffer
                            .wrap(new byte[] { SocksConstants.Version.SOCKS_5, statusByte, 0x00 }),
                    ByteBuffer.wrap(remoteID) });
        }

        private void sendSocks4Message(byte statusByte, int port, String address)
                throws IOException {
            
            // Translate SOCKS5 status bytes
            if (statusByte == SocksConstants.Status.REQUEST_GRANTED)
                statusByte = 0x5a;
            if (SocksConstants.Status.GENERAL_FAILURE <= statusByte
                    && statusByte <= SocksConstants.Status.ADDRESS_TYPE_NOT_SUPPORTED) {
                statusByte = 0x5b;
            }

            byte[] ip;
            try{
                ip = InetAddress.getByName(address).getAddress();
            } catch (Exception e) {
                ip = new byte[]{0,0,0,1};
            }
            
            client.write(new ByteBuffer[] { ByteBuffer.wrap(new byte[] { 0x00, statusByte }),
                    ByteBuffer.wrap(new byte[] { (byte) (port & 0xff00 >> 8), (byte) (port & 0xff) }),
                    ByteBuffer.wrap(ip) });
        }

        private void readNBytes(ByteBuffer buf, int n) throws IOException {
            int position = buf.position();
            buf.limit(position + n);
            client.read(buf);
            buf.position(position);
        }
    }
}
