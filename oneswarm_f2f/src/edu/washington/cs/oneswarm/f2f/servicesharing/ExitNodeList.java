package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.util.SystemProperties;

public class ExitNodeList {
    // Singleton Pattern.
    private final static ExitNodeList instance = new ExitNodeList();
    private static final String LOCAL_SERVICE_KEY_CONFIG_KEY = "DISTINGUISHED_SHARED_SERVICE_KEY";
    public static File OSF2F_DIR;
    private static final String OSF2F_DIR_NAME = "osf2f";
    private static final String EXIT_NODE_FILE = "osf2f.exits";
    static {
        OSF2F_DIR = new File(SystemProperties.getUserPath() + File.separator + OSF2F_DIR_NAME
                + File.separator);
    }

    final List<ExitNodeInfo> exitNodeList;
    final Map<Long, ExitNodeInfo> localSharedExitServices;

    private ExitNodeList() {
        File dbFile = new File(OSF2F_DIR, EXIT_NODE_FILE);
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

        File dbFile = new File(OSF2F_DIR, EXIT_NODE_FILE);
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
        if (temp == null) {
            temp = new ExitNodeInfo();
        }
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
}
