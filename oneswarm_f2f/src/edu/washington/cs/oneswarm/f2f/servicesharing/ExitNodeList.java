package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.gudy.azureus2.core3.config.COConfigurationManager;

public class ExitNodeList implements Serializable{
    // Singleton Pattern.
    private final static ExitNodeList instance = new ExitNodeList();
    private static final long serialVersionUID = -7232513344463947878L;
    private static final String LOCAL_SERVICE_KEY_CONFIG_KEY = "DISTINGUISHED_SHARED_SERVICE_KEY";
    
    public static ExitNodeList getInstance(){
        return instance;
    }
    
    List<ExitNodeInfo> exitNodeList;

    private ExitNodeList(){
        this.exitNodeList = new LinkedList<ExitNodeInfo>();
    }
    
    public void addNodes(ExitNodeInfo[] exitNodes) {
        for (ExitNodeInfo server : exitNodes)
            this.exitNodeList.add(server);
        sortAndSave();
    }

    public void addNode(ExitNodeInfo exitNode) {
        exitNodeList.add(exitNode);
        sortAndSave();
    }
    
    private void sortAndSave(){
        Collections.sort(exitNodeList);
        //TODO serialize and save list to disk
    }
    
    public ExitNodeInfo pickServer(String url, int port) {
        for (ExitNodeInfo server : exitNodeList)
            if (server.allowsConnectionTo(url, port))
                return server;
        return null;
    }
    
    /**
     * Get (and generate if it does not yet exist) a distinguished key for this
     * machine's locally shared service.
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
     * Reset the locally shared service key, in case the node wishes to change identity.
     */
    public void resetLocalServiceKey() {
        COConfigurationManager.setParameter(LOCAL_SERVICE_KEY_CONFIG_KEY, 0L);
    }
}
