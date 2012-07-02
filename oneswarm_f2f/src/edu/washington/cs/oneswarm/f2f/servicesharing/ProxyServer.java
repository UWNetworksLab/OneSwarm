package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import edu.washington.cs.oneswarm.f2f.socks.OSSocksServer;
import edu.washington.cs.oneswarm.f2f.socks.SocksCommandHandler;

public class ProxyServer implements Runnable {
    public static Logger logger = Logger.getLogger(ProxyServer.class.getName());

    /**
     * Starts a SOCKS 4, 4a and 5 compliant proxy server at localhost:1080
     * For a test of the OSProxyServer see OneSwarm/javatests/edu.washington.cs.oneswarm.f2f.socks.SocksServerTest.java
     */
    public static void main(String[] args) throws UnknownHostException, IOException {        
        new OSSocksServer(1080, new SocksCommandHandler.BidirectionalPipe()).run();
    }

    private final int port;

    public ProxyServer(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        new OSSocksServer(port, new SocksCommandHandler.BidirectionalPipe()).run();
    }

    public Thread startDeamonThread(boolean blockUntilStarted) throws InterruptedException {
        Thread t = new Thread(new OSSocksServer(port, new SocksCommandHandler.BidirectionalPipe()));
        t.setDaemon(true);
        t.start();
        if(blockUntilStarted){
            Thread.sleep(50);
        }
        return t;
    }
}
