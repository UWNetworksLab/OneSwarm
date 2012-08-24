package edu.washington.cs.oneswarm.f2f.servicesharing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import edu.washington.cs.oneswarm.f2f.xml.XMLHelper;
import edu.washington.cs.oneswarm.test.util.OneSwarmTestBase;

/**
 * Tests ExitNodeInfo, verifying that it correctly represents the policy of the
 * server it represents.
 * 
 * @author Nick
 * 
 */
public class ExitNodePublishingTest extends OneSwarmTestBase {
    public long serviceId;
    public ExitNodeInfo node;

    @Test
    public void testExitNodeInfo() throws Exception {

        // Run fake Directory Server
        try {
            int port = 7888;
            Thread directoryServer = new Thread(new OSDirectoryServer(port));
            directoryServer.setName("Exit Node Directory Web Server");
            directoryServer.start(); // Wait for server to come online.
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set the ExitNodeList to use our local DirectoryServer
        DirectoryServerManager.getInstance().setDirectoryServerUrls(
                new String[] { "http://notARealServer/", "http://127.0.0.1:7888/" });

        try { // Make sure there are no errors in registering
            serviceId = ExitNodeList.getInstance().getLocalServiceKey();
            node = new ExitNodeInfo("Servo the Magnificent", serviceId, 250,
                    new String[] { "accept *.*" }, new Date(), "Version string 2.0");
            ExitNodeList.getInstance().setExitNodeSharedService(node);
            DirectoryServerManager.getInstance().registerExitNodes();
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // Verify that the client successfully registered by having it download
        // its registration and making sure the service key matches the one we
        // made. Note: <code>this.serviceId</code> points now to the new
        // serviceId we asked the client to generate, not the original one.
        DirectoryServerManager.getInstance().refreshFromDirectoryServer();
        ExitNodeInfo node = ExitNodeList.getInstance().pickServer("google.com", 80);
        assertEquals(serviceId, node.serviceId);
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

                try {

                    XMLHelper xmlOut = new XMLHelper(resp.getOutputStream());
                    // Respond differently as each step progresses
                    switch (count) {
                    // Tell the client that their registration has expired
                    case 0:
                        xmlOut.startElement(XMLHelper.EXIT_NODE);
                        xmlOut.writeTag(XMLHelper.SERVICE_ID, serviceId + "");
                        xmlOut.writeStatus(XMLHelper.ERROR_UNREGISTERED_SERVICE_ID,
                                "Unregistered ServiceId.");
                        xmlOut.endElement(XMLHelper.EXIT_NODE);
                        break;
                    // Tell the client that their serviceId is duplicate and
                    // must be regenerated
                    case 1:
                        xmlOut.startElement(XMLHelper.EXIT_NODE);
                        xmlOut.writeTag(XMLHelper.SERVICE_ID, serviceId + "");
                        xmlOut.writeStatus(XMLHelper.ERROR_DUPLICATE_SERVICE_ID,
                                "Duplicate ServiceId.");
                        xmlOut.endElement(XMLHelper.EXIT_NODE);
                        break;
                    // Check that they did in fact regen the serviceId and tell
                    // them they suceeded
                    case 2:
                        long newServiceId = ExitNodeList.getInstance().getLocalServiceKey();
                        assertFalse(newServiceId == ExitNodePublishingTest.this.serviceId);
                        ExitNodePublishingTest.this.serviceId = newServiceId;
                        xmlOut.startElement(XMLHelper.EXIT_NODE);
                        xmlOut.writeTag(XMLHelper.SERVICE_ID, newServiceId + "");
                        xmlOut.writeStatus(XMLHelper.STATUS_SUCCESS, "Success.");
                        xmlOut.endElement(XMLHelper.EXIT_NODE);
                        break;
                    case 3:
                        node.fullXML(xmlOut);
                        break;
                    default:
                        fail();
                        break;
                    }
                    xmlOut.close();
                } catch (SAXException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
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
