package edu.washington.cs.oneswarm.f2f.servicesharing;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;

public class ExitNodeSharedService extends SharedService {

    public ExitNodeSharedService(Long searchKey) {
        super(searchKey);
    }

    @Override
    public NetworkConnection createConnection() {
        PolicyNetworkConnection pnc = new PolicyNetworkConnection(this.serviceId);
        return new ListenedNetworkConnection(pnc, this.getMonitoringListener(pnc));
    }
}
