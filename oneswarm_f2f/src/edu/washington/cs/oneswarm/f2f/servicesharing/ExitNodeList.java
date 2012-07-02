package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ExitNodeList implements Serializable{
    // Singleton Pattern.
    private final static ExitNodeList instance = new ExitNodeList();
    private static final long serialVersionUID = -7232513344463947878L;
    
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
}
