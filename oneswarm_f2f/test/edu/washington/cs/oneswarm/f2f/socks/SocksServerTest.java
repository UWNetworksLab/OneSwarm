/**
 * Test to demonstrate that the OSSocksServer accepts and connects a client and server usuing the SOCKS5 protocol.
 */

package edu.washington.cs.oneswarm.f2f.socks;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Random;

import org.junit.Test;

public class SocksServerTest {

    @Test
    public void test() {
        Thread server = new Thread(new OSSocksServer(1080));
        server.start();

        Random rnd = new Random();
        byte[] message = new byte[50];
        rnd.nextBytes(message);

        Thread socket = new Thread(new SocketThread(10001, message));
        socket.start();

        ByteBuffer readBuf = ByteBuffer.allocate(1024);

        try {
            // Connect to proxy and perform SOCKS5 handshake
            SocketChannel connection = SocketChannel.open(new InetSocketAddress("localhost", 1080));

            // Request access to the proxy with no authentication
            connection.write(ByteBuffer.wrap(new byte[] { 0x05, 0x01, 0x00 }));

            // Check response to handshake
            connection.read(readBuf);
            readBuf.flip();
            assertEquals(SocksConstants.Version.SOCKS_5, readBuf.get() & 0xff);
            assertEquals(SocksConstants.Status.REQUEST_GRANTED, readBuf.get() & 0xff);
            readBuf.clear();

            // Request connection to local test port
            connection.write(new ByteBuffer[] {
                    ByteBuffer.wrap(new byte[] { 0x05, 0x01, 0x00, 0x03,
                            (byte) "localhost".length() }),
                    ByteBuffer.wrap("localhost".getBytes()),
                    ByteBuffer.wrap(new byte[] { 10001 >> 8, (byte) 10001 }) });

            // Check response to request
            connection.read(readBuf);
            readBuf.flip();
            assertEquals(SocksConstants.Version.SOCKS_5, readBuf.get() & 0xff);
            assertEquals(SocksConstants.Status.REQUEST_GRANTED, readBuf.get() & 0xff);
            assertEquals(0x00, readBuf.get() & 0xff);
            assertEquals(SocksConstants.AddressType.DOMAIN_NAME, readBuf.get() & 0xff);
            assertEquals("localhost".length(), readBuf.get() & 0xff);
            readBuf.clear();
            
            // Send message
            connection.write(ByteBuffer.wrap(message));
            
            // Get reply (The string "PASS" encoded in ASCII)
            connection.read(readBuf);
            readBuf.flip();
            byte[] replyBytes = new byte[readBuf.remaining()];
            readBuf.get(replyBytes);
            assertEquals("PASS", new String(replyBytes));

        } catch (IOException e) {
            fail(e.toString());
        }
    }

    class SocketThread implements Runnable {
        private int port;
        private byte[] expectedMessage;

        public SocketThread(int port, byte[] expectedMessage) {
            this.port = port;
            this.expectedMessage = expectedMessage;
        }

        public void run() {
            try {
                // Set up a testing serverSocket to receive our proxied
                // connection
                ServerSocketChannel serverSocket = ServerSocketChannel.open();
                serverSocket.bind(new InetSocketAddress("localhost", port));
                SocketChannel socket = serverSocket.accept();
                assertTrue(socket != null);

                // Read message from the socket
                ByteBuffer buf = ByteBuffer.allocate(1024);
                socket.read(buf);
                buf.flip();
                byte[] actualMessage = new byte[buf.remaining()];
                buf.get(actualMessage);

                // Assert that the message is what we originally sent
                assertArrayEquals(expectedMessage, actualMessage);
                
                // Reply with the text "PASS"
                socket.write(ByteBuffer.wrap("PASS".getBytes()));
            } catch (IOException e) {
                fail(e.toString());
            }

        }
    }
}
