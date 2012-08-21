package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
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
    private static final String DATABASE_FILE = "./db.obj";

    private final List<ExitNodeInfo> exitNodeList;
    private final Map<Long, ExitNodeInfo> localSharedExitServices;

    private ExitNodeList() {
        File dbFile = new File(DATABASE_FILE);
        if (dbFile.exists()) {
            try {
                ObjectInputStream obj = new ObjectInputStream(new FileInputStream(dbFile));
                exitNodeList = (List<ExitNodeInfo>) obj.readObject();
                localSharedExitServices = (Map<Long, ExitNodeInfo>) obj.readObject();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            exitNodeList = new LinkedList<ExitNodeInfo>();
            localSharedExitServices = new HashMap<Long, ExitNodeInfo>();
        }
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
    }

    public static ExitNodeList getInstance() {
        return instance;
    }

    public void addNodes(List<ExitNodeInfo> exitNodes) {
        exitNodeList.addAll(exitNodes);
        sortAndSave();
    }

    public void addNode(ExitNodeInfo exitNode) {
        exitNodeList.add(exitNode);
        sortAndSave();
    }

    private void sortAndSave() {
        Collections.sort(exitNodeList);

        File dbFile = new File(DATABASE_FILE);
        try {
            ObjectOutputStream obj = new ObjectOutputStream(new FileOutputStream(dbFile));
            obj.writeObject(exitNodeList);
            obj.writeObject(localSharedExitServices);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ExitNodeInfo getServerByKey(long serviceId) {
        for (ExitNodeInfo node : exitNodeList) {
            if (node.getId() == serviceId) {
                return node;
            }
        }
        return null;
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
     * 
     * Returns the new key exactly as getLocalServiceKey would
     */
    public long resetLocalServiceKey() {
        ExitNodeInfo temp = removeExitNodeSharedService(getLocalServiceKey());
        temp.generateNewKeys();
        COConfigurationManager.setParameter(LOCAL_SERVICE_KEY_CONFIG_KEY, 0L);
        long newKey = getLocalServiceKey();
        temp.setId(newKey);
        setExitNodeSharedService(temp);
        return newKey;
    }

    public void setExitNodeSharedService(ExitNodeInfo exitNode) {
        localSharedExitServices.put(exitNode.getId(), exitNode);
        sortAndSave();
    }

    public ExitNodeInfo removeExitNodeSharedService(long serviceId) {
        if (isExitNodeSharedService(serviceId)) {
            return localSharedExitServices.remove(serviceId);
        }
        return null;
    }

    public boolean isExitNodeSharedService(long serviceId) {
        return localSharedExitServices.containsKey(serviceId);
    }

    public ExitNodeInfo getExitNodeSharedService(long serviceId) {
        return localSharedExitServices.get(serviceId); // TODO(ben) does this
                                                       // have a default?
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

        // TODO (nick) remove when partial update is ready
        exitNodeList.clear();

        addNodes(exitNodes);
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
            if (node.getEnabled()) {
                node.shortXML(xmlOut);
            }
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

                ExitNodeInfo temp = getExitNodeSharedService(msg.serviceId);

                // This assumes a single shared service model.
                resetLocalServiceKey();

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
