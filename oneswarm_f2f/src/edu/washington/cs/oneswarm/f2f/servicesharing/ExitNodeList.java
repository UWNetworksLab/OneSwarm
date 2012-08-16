package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.xml.sax.SAXException;

import edu.washington.cs.oneswarm.f2f.xml.DirectoryServerMsgHandler;
import edu.washington.cs.oneswarm.f2f.xml.ExitNodeInfoHandler;
import edu.washington.cs.oneswarm.f2f.xml.XMLHelper;

public class ExitNodeList {
    private static Logger log = Logger.getLogger(ExitNodeList.class.getName());
    // Singleton Pattern.
    private final static ExitNodeList instance = new ExitNodeList();
    private static final String LOCAL_SERVICE_KEY_CONFIG_KEY = "DISTINGUISHED_SHARED_SERVICE_KEY";
    private static final String DIRECTORY_SERVER_URL_CONFIG_KEY = "DIRECTORY_SERVER_URL_CONFIG_KEY";
    private static final long KEEPALIVE_INTERVAL = 55 * 60 * 1000;
    private static final long DIRECTORY_SERVER_REFRESH_INTERVAL = 55 * 60 * 1000;

    private final SortedSet<ExitNodeInfo> exitNodeList;
    private final Map<Long, ExitNodeInfo> localSharedExitServices;

    private ExitNodeList() {
        Timer keepAliveRegistrations = new Timer();
        keepAliveRegistrations.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    ExitNodeList.this.registerExitNodes();
                } catch (IOException e) {
                    // Unexpected
                    e.printStackTrace();
                } catch (SAXException e) {
                    // Unexpected
                    e.printStackTrace();
                }
            }
        }, KEEPALIVE_INTERVAL / 2, KEEPALIVE_INTERVAL);

        keepAliveRegistrations.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    ExitNodeList.this.refreshFromDirectoryServer();
                } catch (IOException e) {
                    // Unexpected
                    e.printStackTrace();
                } catch (SAXException e) {
                    // Unexpected
                    e.printStackTrace();
                }
            }
        }, 5 * 1000, DIRECTORY_SERVER_REFRESH_INTERVAL);

        this.exitNodeList = new TreeSet<ExitNodeInfo>();
        this.localSharedExitServices = new HashMap<Long, ExitNodeInfo>();
    }

    public static ExitNodeList getInstance() {
        return instance;
    }

    public void addNodes(ExitNodeInfo[] exitNodes) {
        for (ExitNodeInfo server : exitNodes) {
            this.exitNodeList.add(server);
        }
        sortAndSave();
    }

    public void addNode(ExitNodeInfo exitNode) {
        exitNodeList.add(exitNode);
        sortAndSave();
    }

    private void sortAndSave() {
        // No need to sort a sorted set
        // TODO (nick) serialize and save list to disk
    }

    public ExitNodeInfo pickServer(String url, int port) {
        for (ExitNodeInfo server : exitNodeList) {
            if (server.allowsConnectionTo(url, port)) {
                return server;
            }
        }
        return null;
    }

    public void setDirectoryServer(String url) {
        COConfigurationManager.setParameter(DIRECTORY_SERVER_URL_CONFIG_KEY, url);
    }

    public String getDirectoryServerUrl() {
        return COConfigurationManager.getStringParameter(DIRECTORY_SERVER_URL_CONFIG_KEY, "");
    }

    /**
     * Get (and generate if it does not yet exist) a distinguished key for this
     * machine's locally shared service.
     * 
     * @return The local shared service key.
     */
    public long getLocalServiceKey() {
        long serviceKey = COConfigurationManager.getLongParameter(LOCAL_SERVICE_KEY_CONFIG_KEY, 0L);
        if (serviceKey == 0) {
            Random r = new Random();
            serviceKey = r.nextLong();
            serviceKey = Math.abs(serviceKey);
            COConfigurationManager.setParameter(LOCAL_SERVICE_KEY_CONFIG_KEY, serviceKey);
        }
        return serviceKey;
    }

    /**
     * Reset the locally shared service key, in case the node wishes to change
     * identity.
     */
    public void resetLocalServiceKey() {
        COConfigurationManager.setParameter(LOCAL_SERVICE_KEY_CONFIG_KEY, 0L);
    }

    public void setExitNodeSharedService(ExitNodeInfo exitNode) {
        localSharedExitServices.put(exitNode.getId(), exitNode);
    }

    public void removeExitNodeSharedService(long serviceId) {
        if (isExitNodeSharedService(serviceId)) {
            localSharedExitServices.remove(serviceId);
        }
    }

    public boolean isExitNodeSharedService(long serviceId) {
        return localSharedExitServices.containsKey(serviceId);
    }

    public boolean allowLocalExitConnection(long serviceId, String address, int port) {
        if (isExitNodeSharedService(serviceId)) {
            return localSharedExitServices.get(serviceId).allowsConnectionTo(address, port);
        } else {
            return false;
        }
    }

    protected void refreshFromDirectoryServer() throws IOException, SAXException {
        String exitNodeDirectoryUrl = getDirectoryServerUrl();
        if (exitNodeDirectoryUrl.equals("")) {
            log.warning("Could not retrive ExitNodes from directory server. No directory server URL set.");
            return;
        }
        HttpURLConnection conn = createConnectionTo(exitNodeDirectoryUrl + "?action=list");
        List<ExitNodeInfo> exitNodes = new LinkedList<ExitNodeInfo>();
        XMLHelper.parse(conn.getInputStream(), new ExitNodeInfoHandler(exitNodes));
        conn.disconnect();

        for (ExitNodeInfo node : exitNodes) {
            // TODO (nick) implement partial update functionality and allow
            // nodes to be removed if they have a flag
            if (exitNodeList.contains(node)) {
                exitNodeList.remove(node);
            }
            exitNodeList.add(node);
        }
    }

    public void registerExitNodes() throws IOException, SAXException {
        String exitNodeDirectoryUrl = getDirectoryServerUrl();
        if (exitNodeDirectoryUrl.equals("")) {
            new IllegalArgumentException("No DirectoryServer Specified.").printStackTrace();
            return;
        }
        HttpURLConnection conn = createConnectionTo(exitNodeDirectoryUrl + "?action=checkin");
        conn.setRequestProperty("Content-Type", "text/xml");
        XMLHelper xmlOut = new XMLHelper(conn.getOutputStream());
        // Write check-in request to the connection
        for (ExitNodeInfo node : localSharedExitServices.values()) {
            node.shortXML(xmlOut);
        }
        xmlOut.close();

        // Retry registrations that are fixable until no fixable errors remain.
        while (true) {
            // Parse reply for error messages
            List<DirectoryServerMsg> msgs = new LinkedList<DirectoryServerMsg>();
            XMLHelper.parse(conn.getInputStream(), new DirectoryServerMsgHandler(msgs));
            conn.disconnect();
            conn = null;

            List<ExitNodeInfo> toReRegister = decideWhatNeedsReregistering(msgs);

            // If there are no fixable errors, stop trying
            if (toReRegister.size() == 0) {
                break;
            }

            // Otherwise, retry the nodes that need ro be registered
            conn = createConnectionTo(exitNodeDirectoryUrl + "?action=register");
            conn.setRequestProperty("Content-Type", "text/xml");
            xmlOut = new XMLHelper(conn.getOutputStream());
            // Write register request to the connection
            for (ExitNodeInfo node : toReRegister) {
                node.fullXML(xmlOut);
            }
            xmlOut.close();
        }
    }

    private List<ExitNodeInfo> decideWhatNeedsReregistering(List<DirectoryServerMsg> msgs) {
        List<ExitNodeInfo> toReregister = new LinkedList<ExitNodeInfo>();
        for (DirectoryServerMsg msg : msgs) {
            // If serviceId is duplicate, pull the node out and give
            // it a new serviceId.

            if (msg.errorCodes.contains(XMLHelper.STATUS_SUCCESS)) {
                msg.removeErrorCode(XMLHelper.STATUS_SUCCESS);
            } else if (msg.errorCodes.contains(XMLHelper.ERROR_UNREGISTERED_SERVICE_ID)) {
                ExitNodeInfo temp = localSharedExitServices.get(msg.serviceId);
                toReregister.add(temp);
                msg.removeErrorCode(XMLHelper.ERROR_UNREGISTERED_SERVICE_ID);
            } else if (msg.errorCodes.contains(XMLHelper.ERROR_DUPLICATE_SERVICE_ID)) {
                ExitNodeInfo temp = localSharedExitServices.remove(msg.serviceId);

                // These two lines assume that there is one ExitService on
                // this computer.
                resetLocalServiceKey();
                temp.setId(getLocalServiceKey());

                setExitNodeSharedService(temp);
                toReregister.add(temp);
                msg.removeErrorCode(XMLHelper.ERROR_DUPLICATE_SERVICE_ID);
            }
            while (msg.errorCodes.size() > 0 && msg.errorStrings.size() > 0) {
                log.warning("ExitNode Registration Error: " + msg.errorCodes.remove(0) + " - "
                        + msg.errorStrings.remove(0));
            }
        }
        msgs.clear();
        return toReregister;
    }

    private HttpURLConnection createConnectionTo(String url) throws IOException {
        URL server = new URL(url);
        HttpURLConnection req = (HttpURLConnection) server.openConnection();
        req.setDoInput(true);
        req.setDoOutput(true);
        req.setUseCaches(false);
        // req.setRequestProperty("Content-Type", "text/xml");
        return req;
    }
}
