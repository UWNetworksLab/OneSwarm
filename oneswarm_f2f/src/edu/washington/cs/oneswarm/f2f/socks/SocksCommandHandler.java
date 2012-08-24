package edu.washington.cs.oneswarm.f2f.socks;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;
import java.util.Timer;
import java.util.TimerTask;
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

import edu.washington.cs.oneswarm.f2f.servicesharing.ClientService;
import edu.washington.cs.oneswarm.f2f.servicesharing.ExitNodeInfo;
import edu.washington.cs.oneswarm.f2f.servicesharing.ExitNodeList;
import edu.washington.cs.oneswarm.f2f.servicesharing.RawMessageFactory;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceConnection;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceConnectionDelegate;
import edu.washington.cs.oneswarm.f2f.servicesharing.ServiceSharingManager;

public class SocksCommandHandler {
    public final static Logger logger = Logger.getLogger(SocksCommandHandler.class.getName());

    public static interface Interface {
        /**
         * The method should make the connection in the manner defined by the
         * implementing class, or throw a SocksException if that is not
         * possible. Returns a byte[(6|18)] indicating the IP address of the
         * exitNode as seen by the remote destination followed by the port that
         * is exposed.
         * 
         * @param command
         * @param client
         * @param remoteHost
         * @return byte[] IP address of exitNode as seen by remote destination
         *         and port.
         * @throws SocksException
         *             Throws SocksException if the command is not supported by
         *             this handler or if the remoteHost is not allowed by the
         *             rule-set.
         */
        byte[] doCommand(byte command, SocketChannel client, String address, int port)
                throws SocksException;
    }

    private static byte[] concat(byte[] first, byte[] second) {
        int firstLength = first.length;
        int secondLength = second.length;
        byte[] result = new byte[firstLength + secondLength];
        for (int i = 0; i < firstLength; i++) {
            result[i] = first[i];
        }
        for (int j = 0; j < secondLength; j++) {
            result[firstLength + j] = second[j];
        }
        return result;
    };

    public static class HandoffToOneSwarm implements SocksCommandHandler.Interface {
        protected static final long BANDWIDTH_UPDATE_FREQUENCY = 10 * 1000;

        @Override
        public byte[] doCommand(byte command, SocketChannel client, String address, int port)
                throws SocksException {
            switch (command) {
            case SocksConstants.Command.ESTABLISH_TCP_STREAM_CONNECTION:

                logger.info("Handing off connection between "
                        + client.socket().getRemoteSocketAddress() + " and " + address + ":" + port
                        + " to OneSwarm");
                final ExitNodeInfo server = ExitNodeList.getInstance().pickServer(address, port);
                if (server == null) {
                    throw new SocksException(
                            SocksConstants.Status.CONNECTION_NOT_ALLOWED_BY_RULESET);
                }

                try {
                    client.configureBlocking(false);
                } catch (IOException e) {
                    logger.warning("Couldn't set incomming connection to non-blocking.");
                    e.printStackTrace();
                    throw new SocksException(SocksConstants.Status.GENERAL_FAILURE);
                }
                TransportHelper helper = new TCPTransportHelper(client);
                ProtocolEndpointTCP endpoint = new ProtocolEndpointTCP(helper.getAddress());
                TransportHelperFilter filter = new TransportHelperFilterTransparent(helper, true);
                Transport transport = new TCPTransportImpl(endpoint, filter);
                MessageStreamEncoder encoder = new RawMessageFactory().createEncoder();
                MessageStreamDecoder decoder = new RawMessageFactory().createDecoder();

                // Prepend header in "OneSwarm Proxy Wire Format"
                // -----------------------------------------------------------
                // | port high bits | port low | address length | address |
                // -----------------------------------------------------------
                ByteBuffer header = ByteBuffer.allocate(3 + address.length());
                header.putShort((short) port);
                header.put((byte) (address.length() & 0xff));
                header.put(address.getBytes());
                header.flip();
                transport.setAlreadyRead(header);

                NetworkConnection nc = new NetworkConnectionImpl(transport, encoder, decoder);

                ClientService service = ServiceSharingManager.getInstance().getClientService(
                        server.serviceId);
                if (service == null) {
                    service = new ClientService(server.serviceId);
                }
                final long connectionOpen = System.currentTimeMillis();
                service.connectionRouted(nc, null, new ServiceConnectionDelegate() {

                    @Override
                    public void connected(final ServiceConnection conn) {
                        new Timer().schedule(new TimerTask() {

                            @Override
                            public void run() {
                                server.recordBandwidth(conn.getDownloadRate() / 1000);
                            }

                        }, 0, BANDWIDTH_UPDATE_FREQUENCY);
                    }

                    @Override
                    public void closing(ServiceConnection conn) {
                        long lengthOfConnectionSeconds = (System.currentTimeMillis() - connectionOpen) / 1000;
                        long kbDownloaded = conn.getBytesIn() / 1024;
                        long kbps = (kbDownloaded / lengthOfConnectionSeconds);
                        if (kbps > Integer.MAX_VALUE) {
                            throw new RuntimeException(
                                    "Error: Uber fat pipe. Speed in kbps is larger than an int. "
                                            + (kbps / 1024 / 1024) + "GB/s");
                        }
                        server.recordBandwidth((int) kbps);
                    }
                });

                transport.connectedInbound();
                return concat(server.getIpAddr(), new byte[] { 0, 0 });
            default:
                throw new SocksException(SocksConstants.Status.COMMAND_NOT_SUPPORTED);
            }
        }
    }

    public static class BidirectionalPipe implements SocksCommandHandler.Interface {
        private static final int BUFFER_SIZE = 1024;

        @Override
        public byte[] doCommand(byte command, SocketChannel client, String address, int port)
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

                SocketChannel remote;
                try {
                    remote = SocketChannel.open(remoteHost);
                    new Thread(new TcpPipe(client, remote)).start();
                    new Thread(new TcpPipe(remote, client)).start();
                } catch (IOException e) {
                    throw new SocksException(e);
                }

                byte[] exposedIp = remote.socket().getLocalAddress().getAddress();
                int exposedPort = remote.socket().getLocalPort();

                return concat(exposedIp, new byte[] { (byte) (exposedPort & 0xff00 >> 8),
                        (byte) exposedPort });
            default:
                throw new SocksException(SocksConstants.Status.COMMAND_NOT_SUPPORTED);
            }
        }

        private class TcpPipe implements Runnable {
            private final SocketChannel input;
            private final SocketChannel output;

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
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (output != null) {
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
}
