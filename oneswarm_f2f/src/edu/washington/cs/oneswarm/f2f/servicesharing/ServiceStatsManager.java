package edu.washington.cs.oneswarm.f2f.servicesharing;

public class ServiceStatsManager {
    // Singleton.
    private static final ServiceStatsManager instance = new ServiceStatsManager();
    private ServiceStatsManager() {}
    public static ServiceStatsManager getInstance() {
        return ServiceStatsManager.instance;
    }
    
    private long retransmits = 0;

    public synchronized void onRetransmit() {
        this.retransmits += 1;
    }
    
    public synchronized long getRetransmits() {
        long retransmits = this.retransmits;
        this.retransmits = 0;
        return retransmits;
    }
}
