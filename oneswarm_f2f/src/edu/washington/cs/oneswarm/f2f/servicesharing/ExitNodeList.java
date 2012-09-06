package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
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
    final Map<Long, PublishableService> localSharedExitServices;
    private ExitNodeInfo localInfo;

    private ExitNodeList() {
        File dbFile = new File(OSF2F_DIR, EXIT_NODE_FILE);
        if (dbFile.exists()) {
            try {
                ObjectInputStream obj = new ObjectInputStream(new FileInputStream(dbFile));
                exitNodeList = (List<ExitNodeInfo>) obj.readObject();
                localSharedExitServices = (Map<Long, PublishableService>) obj.readObject();
                localInfo = (ExitNodeInfo) obj.readObject();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            localInfo = new ExitNodeInfo();
            exitNodeList = new LinkedList<ExitNodeInfo>();
            localSharedExitServices = new HashMap<Long, PublishableService>();
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
            obj.writeObject(localInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ExitNodeInfo getServerByKey(long serviceId) {
        for (ExitNodeInfo node : exitNodeList) {
            if (node.serviceId == serviceId) {
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
        PublishableService temp = removeExitNodeSharedService(getLocalServiceKey());
        if (temp == null) {
            temp = new ExitNodeInfo();
        }
        temp.generateNewKeys();
        COConfigurationManager.setParameter(LOCAL_SERVICE_KEY_CONFIG_KEY, 0L);
        long newKey = getLocalServiceKey();
        temp.serviceId = newKey;
        setExitNodeSharedService(temp);
        return newKey;
    }

    public void setExitNodeSharedService(PublishableService exitNode) {
        localSharedExitServices.put(exitNode.serviceId, exitNode);
        sortAndSave();
    }

    public PublishableService removeExitNodeSharedService(long serviceId) {
        if (isExitNodeSharedService(serviceId)) {
            return localSharedExitServices.remove(serviceId);
        }
        return null;
    }

    public boolean isExitNodeSharedService(long serviceId) {
        return localSharedExitServices.containsKey(serviceId);
    }

    public PublishableService getExitNodeSharedService(long serviceId) {
        PublishableService service = localSharedExitServices.get(serviceId);
        if (service == null && serviceId == this.getLocalServiceKey()) {
            // First Time Shared service creation.
            service = new ExitNodeInfo();
            service.enabled = false;
            service.setNickname("My Exit Node");
            localSharedExitServices.put(serviceId, service);
        }
        return service;
    }

    public boolean allowLocalExitConnection(long serviceId, String address, int port) {
        if (isExitNodeSharedService(serviceId)) {
            PublishableService exitService = getExitNodeSharedService(serviceId);
            if (exitService instanceof ExitNodeInfo) {
                return ((ExitNodeInfo) exitService).allowsConnectionTo(address, port);
            }
            return false;
        } else {
            return false;
        }
    }

    public void setServiceIsProxy(boolean b) {
        long key = this.getLocalServiceKey();
        if (getServiceIsProxy() != b) {
            PublishableService current = this.getExitNodeSharedService(key);
            ServiceSharingManager.getInstance().deregisterServerService(key);                
            PublishableService other;
            if (current instanceof ExitNodeInfo) {
                other = new SharedService(key);
                ((SharedService)other).setAddress(new InetSocketAddress(Integer.parseInt(this.localInfo.getNickname())));
            } else {
                other = new ExitNodeInfo();
                ((ExitNodeInfo)other).setExitPolicy(this.localInfo.getExitPolicy().split("\n"));
            }
            this.removeExitNodeSharedService(key);
            other.serviceId = key;
            other.enabled = current.enabled;
            other.published = current.enabled;
            other.setNickname(current.getNickname());
            this.setExitNodeSharedService(other);
        }
    }
    
    /**
     * Return the exitnodeinfo associated with the locally shared service.  This is used to cache local
     * settings like policy & local port, even when there isn't a service being actively shared.  it's
     * just used as the model of preference data, and not instantiated as a service itself.
     * 
     */
    public ExitNodeInfo getLocalInfo() {
        return this.localInfo;
    }

    public boolean getServiceIsProxy() {
        long key = this.getLocalServiceKey();
        PublishableService current = this.getExitNodeSharedService(key);
        return current instanceof ExitNodeInfo;
    }
}
