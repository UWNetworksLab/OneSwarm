package edu.washington.cs.oneswarm.f2f.socks;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import org.apache.log4j.Logger;

class OSSocksServerThread implements Runnable {
    Logger log;
    private SocketChannel client;
    private SocketChannel remote;

    private int bufferSize;
    private ByteBuffer buf;

    // Stores the remote host request in the format presented by the client.
    // This must be appended to all messages sent to the client after the
    // initial connection request. ie. for a url-based request, the format would
    // be
    // [ address type (0x03) | address length | each | character | of | the |
    // address | port (high bytes) | port (low bytes) ]
    private byte[] remoteID;

    private InetSocketAddress remoteHost;

    public OSSocksServerThread(SocketChannel socket, int bufferSize) {
        log = Logger.getLogger(OSSocksServer.class.getName());
        client = socket;
        this.bufferSize = bufferSize;
        remoteID = new byte[0];
    }

    public void run() {

        buf = ByteBuffer.allocate(bufferSize);
        try {
            InetAddress addr = ((InetSocketAddress) client.socket().getRemoteSocketAddress()).getAddress();
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
            remote = null;
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

        try{
        switch (addressType) {
        case SocksConstants.AddressType.IPv4:
        case SocksConstants.AddressType.IPv6:
            length *= addressType;
            remoteID = new byte[length + 3];
            remoteID[0] = addressType;
            readNBytes(buf, length + 2);
            buf.get(remoteID, 2, length + 2);
            remoteHost = new InetSocketAddress(
                    InetAddress.getByAddress(Arrays.copyOfRange(remoteID, 1, length + 1)),
                    (remoteID[remoteID.length - 2] & 0xff) << 2 + (remoteID[remoteID.length - 1] & 0xff));
            break;

        case SocksConstants.AddressType.DOMAIN_NAME:
            readNBytes(buf, 1);
            length = buf.get() & 0xff;
            remoteID = new byte[length + 4];
            remoteID[0] = addressType;
            remoteID[1] = (byte) length;
            readNBytes(buf, length + 2);
            buf.get(remoteID, 2, length + 2);
            int port = (remoteID[remoteID.length - 2] & 0xff) << 8
                    | (remoteID[remoteID.length - 1] & 0xff);
            remoteHost = new InetSocketAddress(InetAddress.getByName(new String(Arrays.copyOfRange(
                    remoteID, 2, length + 2))), port);
            break;

        default:
            throw new SocksException(SocksConstants.Status.ADDRESS_TYPE_NOT_SUPPORTED);
        }

        checkIfConnectionIsPossible(remoteHost);

        switch (command) {
        case SocksConstants.Command.ESTABLISH_TCP_STREAM_CONNECTION:
            createPipe(client, remoteHost);
            sendSocks5Message(SocksConstants.Status.REQUEST_GRANTED);
            break;
        default:
            throw new SocksException(SocksConstants.Status.COMMAND_NOT_SUPPORTED);
        }
        } catch (SocksException e) {
            sendSocks5Message((byte) e.getErrorCode());
        }
    }

    private void socks4DoCommand() throws IOException {
        int port = 0;
        byte[] ip = new byte[4];
        try {
            readNBytes(buf, 7);
            byte command = buf.get();
            port = (buf.get() & 0xff) << 8 | (buf.get() & 0xff);
            ip = new byte[] { buf.get(), buf.get(), buf.get(), buf.get() };

            // Throw away user name string
            byte character;
            do {
                readNBytes(buf, 1);
                character = buf.get();
            } while (character != 0x00);

            // SOCKS 4a
            if (ip[0] == 0 && ip[1] == 0 && ip[2] == 0 & ip[3] != 0) {
                String hostname = "";
                readNBytes(buf, 1);
                character = buf.get();
                while (character != 0x00) {
                    hostname += (char) character;
                    readNBytes(buf, 1);
                    character = buf.get();
                }
                remoteHost = new InetSocketAddress(InetAddress.getByName(hostname), port);
                ip = remoteHost.getAddress().getAddress();
            } else {
                remoteHost = new InetSocketAddress(InetAddress.getByAddress(ip), port);
            }

            switch (command) {
            case SocksConstants.Command.ESTABLISH_TCP_STREAM_CONNECTION:
                createPipe(client, remoteHost);
                break;
            default:
                throw new SocksException(SocksConstants.StatusSocks4.REQUEST_REJECTED_OR_FAILED);
            }
            
            checkIfConnectionIsPossible(remoteHost);
            sendSocks4Message(SocksConstants.StatusSocks4.REQUEST_GRANTED, port, ip);

        } catch (SocksException e) {
            sendSocks4Message((byte) e.getErrorCode(), port, ip);
        }
    }

    private void sendSocks5Message(byte statusByte) throws IOException {
        client.write(new ByteBuffer[] {
                ByteBuffer.wrap(new byte[] { SocksConstants.Version.SOCKS_5, statusByte,
                        0x00 }), ByteBuffer.wrap(remoteID) });
    }

    private void sendSocks4Message(byte statusByte, int port, byte[] ip) throws IOException {
        client.write(new ByteBuffer[] { ByteBuffer.wrap(new byte[] { 0x00, statusByte }),
                ByteBuffer.wrap(new byte[] { (byte) (port >> 8), (byte) (port & 0xff) }),
                ByteBuffer.wrap(ip) });
    }

    private void createPipe(SocketChannel client, InetSocketAddress remoteHost) throws IOException {
        // Set up connection - This is where the hand-off to
        // OneSwarm will go
        log.info("Creating bi-directional pipe between " + client.socket().getRemoteSocketAddress() + " and " + remoteHost.getHostString());
        remote = SocketChannel.open(remoteHost);
        new Thread(new TcpPipe(client, remote)).start();
        new Thread(new TcpPipe(remote, client)).start();
        // End Set up
    }

    private void checkIfConnectionIsPossible(InetSocketAddress addr) throws SocksException {
        // TODO check with UrlToServer class for a server that this connection
        // should go through

        // if (connectionShouldntBeAllowed)
        // throw new
        // SocksException(SocksConstants.Status.CONNECTION_NOT_ALLOWED_BY_RULESET);
    }

    private void readNBytes(ByteBuffer buf, int n) throws IOException {

        buf.limit(buf.position() + n);
        client.read(buf);
        buf.position(buf.limit() - n);
    }

    private class TcpPipe implements Runnable {
        private SocketChannel input;
        private SocketChannel output;

        public TcpPipe(SocketChannel in, SocketChannel out) {
            input = in;
            output = out;
        }

        public void run() {
            ByteBuffer buf = ByteBuffer.allocate(bufferSize);
            int len = 0;
            try {
                Thread.currentThread().setName(
                        "Pipe from " + input.socket().getRemoteSocketAddress() + " to "
                                + output.socket().getRemoteSocketAddress());
                try {
                    while (len >= 0) {
                        len = input.read(buf);
                        buf.flip();
                        output.write(buf);
                        buf.compact();
                    }
                } catch (AsynchronousCloseException e) {
                    // Release Sockets
                    input = null;
                    output = null;
                } finally {
                    // Interrupt other Pipe Thread
                    if (input != null)
                        input.close();
                    if (output != null)
                        output.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
