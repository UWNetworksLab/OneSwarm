package edu.washington.cs.oneswarm.ui.gwt.rpc;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ClientServiceInfo implements IsSerializable {

    public String serviceName;
    public long serviceID;

    // Required by GWT for serialization
    // Deprecated to discourage creating an invalid object.  
    @Deprecated
    public ClientServiceInfo() {
        this.serviceID = 0l;
        this.serviceName = "-Error-";
    }

    public ClientServiceInfo(long id, String name) {
        this.serviceID = id;
        this.serviceName = name;
    }
}
