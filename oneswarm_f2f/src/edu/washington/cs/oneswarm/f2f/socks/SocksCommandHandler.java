package edu.washington.cs.oneswarm.f2f.socks;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.util.logging.Logger;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.Transport;
import com.aelitis.azureus.core.networkmanager.impl.NetworkConnectionImpl;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilter;
import com.aelitis.azureus.core.networkmanager.impl.TransportHelperFilterTransparent;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportHelper;
import com.aelitis.azureus.core.networkmanager.impl.tcp.TCPTransportImpl;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamDecoder;
import com.aelitis.azureus.core.peermanager.messaging.MessageStreamEncoder;

import edu.washington.cs.oneswarm.f2f.servicesharing.ExitNodeInfo;
import edu.washington.cs.oneswarm.f2f.servicesharing.ExitNodeList;
import edu.washington.cs.oneswarm.f2f.servicesharing.RawMessageFactory;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceConnectionManager;

public class SocksCommandHandler {
    public final static Logger logger = Logger.getLogger(SocksCommandHandler.class.getName());

    public static interface Interface {
        /**
         * The method should make the connection in the manner defined by the
         * implementing class, or throw a SocksException if that is not
         * possible.
         * 
         * @param command
         * @param client
         * @param remoteHost
         * @throws SocksException
         *             Throws SocksException if the command is not supported by
         *             this handler or if the remoteHost is not allowed by the
         *             ruleset
         */
        void doCommand(byte command, SocketChannel client, String address, int port)
                throws SocksException;
    }

    public static class HandoffToOneSwarm implements SocksCommandHandler.Interface {
        public void doCommand(byte command, SocketChannel client, String address, int port)
                throws SocksException {
            switch (command) {
            case SocksConstants.Command.ESTABLISH_TCP_STREAM_CONNECTION:

                logger.info("Handing off connection between "
                        + client.socket().getRemoteSocketAddress() + " and " + address + ":" + port
                        + " to OneSwarm");
                ExitNodeInfo server = ExitNodeList.getInstance().pickServer(address, port);
                if (server == null)
                    throw new SocksException(
                            SocksConstants.Status.CONNECTION_NOT_ALLOWED_BY_RULESET);

                TransportHelper helper = new TCPTransportHelper(client);
                ProtocolEndpointTCP endpoint = new ProtocolEndpointTCP(helper.getAddress());
                TransportHelperFilter filter = new TransportHelperFilterTransparent(helper, true);
                Transport transport = new TCPTransportImpl(endpoint, filter);
                MessageStreamEncoder encoder = new RawMessageFactory().createEncoder();
                MessageStreamDecoder decoder = new RawMessageFactory().createDecoder();

                // Prepend header in "OneSwarm Proxy Wire Format"
                // ------------------------------------------------------------------
                // | port high bits | port low | address length | address bytes
                // |
                // ------------------------------------------------------------------
                ByteBuffer header = ByteBuffer.allocate(3 + address.length());
                header.put((byte) (port & 0xff00 >> 8));
                header.put((byte) (port & 0xff));
                header.put((byte) (address.length() & 0xff));
                header.put(address.getBytes());
                transport.setAlreadyRead(header);

                NetworkConnection nc = new NetworkConnectionImpl(transport, encoder, decoder);
                ServiceConnectionManager.getInstance().requestService(nc, server.getId());
                break;
            default:
                throw new SocksException(SocksConstants.Status.COMMAND_NOT_SUPPORTED);
            }
        }
    }

    public static class BidirectionalPipe implements SocksCommandHandler.Interface {
        private static final int BUFFER_SIZE = 1024;

        public void doCommand(byte command, SocketChannel client, String address, int port)
                throws SocksException {

            switch (command) {
            case SocksConstants.Command.ESTABLISH_TCP_STREAM_CONNECTION:
                InetSocketAddress remoteHost;
                try {
                    remoteHost = new InetSocketAddress(InetAddress.getByName(address), port);
                } catch (UnknownHostException e) {
                    throw new SocksException(e);
                }

                logger.info("Creating bi-directional pipe between "
                        + client.socket().getRemoteSocketAddress() + " and "
                        + remoteHost.toString());

                try {
                    SocketChannel remote = SocketChannel.open(remoteHost);
                    new Thread(new TcpPipe(client, remote)).start();
                    new Thread(new TcpPipe(remote, client)).start();
                } catch (IOException e) {
                    throw new SocksException(e);
                }
                break;
            default:
                throw new SocksException(SocksConstants.Status.COMMAND_NOT_SUPPORTED);
            }
        }

        private class TcpPipe implements Runnable {
            private SocketChannel input;
            private SocketChannel output;

            public TcpPipe(SocketChannel in, SocketChannel out) {
                input = in;
                output = out;
            }

            @Override
            public void run() {
                ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE);
                int len = 0;
                try {
                    Thread.currentThread().setName(
                            "Pipe from " + input.socket().getRemoteSocketAddress() + " to "
                                    + output.socket().getRemoteSocketAddress());

                    while (len >= 0) {
                        len = input.read(buf);
                        buf.flip();
                        output.write(buf);
                        buf.compact();
                    }
                } catch (AsynchronousCloseException e) {
                } catch (IOException e) {
                } finally {
                    if (input != null)
                        try {
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    if (output != null)
                        try {
                            output.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                }
            }
        }

    }
}
