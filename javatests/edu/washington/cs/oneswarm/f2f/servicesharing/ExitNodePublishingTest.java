package edu.washington.cs.oneswarm.f2f.servicesharing;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.thread.QueuedThreadPool;
import org.xml.sax.SAXException;

import edu.washington.cs.oneswarm.test.util.OneSwarmTestBase;

/**
 * Tests ExitNodeInfo, verifying that it correctly represents the policy of the
 * server it represents.
 * 
 * @author Nick
 * 
 */
public class ExitNodePublishingTest extends OneSwarmTestBase {

    @Test
    public void testExitNodeInfo() throws Exception {
        // Run fake Directory Server
        try {
            int port = 7888;
            Thread directoryServer = new Thread(new OSDirectoryServer(port));
            directoryServer.setName("Exit Node Directory Web Server");
            directoryServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            // Make sure there are no errors in registering
            ExitNodeList.getInstance().setExitNodeSharedService(
                    new ExitNodeInfo("Servo the Magnificent", 123456, 250,
                            new String[] { "accept *.*" }, new Date(), "Version string 2.0"));
            ExitNodeList.getInstance().setDirectoryServer("http://127.0.0.1:7888/");
            ExitNodeList.getInstance().registerExitNodes();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public class OSDirectoryServer implements Runnable {
        final Server jettyServer = new Server();

        private OSDirectoryServer(int port) throws ParserConfigurationException, SAXException,
                IOException {

            /* Define thread pool for the web server. */
            QueuedThreadPool threadPool = new QueuedThreadPool();
            threadPool.setMinThreads(2);
            threadPool.setMaxThreads(250);
            threadPool.setName("Jetty thread pool");
            threadPool.setDaemon(true);
            jettyServer.setThreadPool(threadPool);

            /* Define connection statistics for the web server. */
            SelectChannelConnector connector = new SelectChannelConnector();
            connector.setMaxIdleTime(10000);
            connector.setLowResourceMaxIdleTime(5000);
            connector.setAcceptQueueSize(100);
            connector.setHost(null);
            connector.setPort(port);
            jettyServer.addConnector(connector);

            /* Define handlers for the web server. */
            jettyServer.addHandler(new DirectoryRequestHandler());
        }

        private class DirectoryRequestHandler extends AbstractHandler {
            int count = 0;

            @Override
            public void handle(String target, HttpServletRequest req, HttpServletResponse resp,
                    int dispatch) throws IOException, ServletException {

                Request request = (req instanceof Request) ? (Request) req : HttpConnection
                        .getCurrentConnection().getRequest();

                // Respond differently as each step progresses
                switch (count) {
                case 0:
                    resp.getOutputStream().write(
                            "<ExitNodeList><ExitNode><ServiceId>123456</ServiceId></ExitNode></ExitNodeList>"
                                    .getBytes(XMLConstants.ENCODING));
                    break;
                }
                count++;
                request.setHandled(true);

            }
        }

        @Override
        public void run() {
            try {
                jettyServer.start();
                jettyServer.join();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                jettyServer.destroy();
            }
        }
    }
}
