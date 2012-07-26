package edu.washington.cs.oneswarm.f2f.servicesharing;

import com.aelitis.azureus.core.networkmanager.NetworkConnection;

public class ExitNodeSharedService extends SharedService {

    ExitNodeSharedService(long searchKey) {
        super(searchKey);
    }

    @Override
    public NetworkConnection createConnection() {
        return new PolicyNetworkConnection(this.searchKey);
    }
}
