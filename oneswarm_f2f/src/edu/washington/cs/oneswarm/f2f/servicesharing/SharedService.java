package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.xml.sax.SAXException;

import com.aelitis.azureus.core.networkmanager.ConnectionEndpoint;
import com.aelitis.azureus.core.networkmanager.NetworkConnection;
import com.aelitis.azureus.core.networkmanager.NetworkConnection.ConnectionListener;
import com.aelitis.azureus.core.networkmanager.NetworkManager;
import com.aelitis.azureus.core.networkmanager.impl.tcp.ProtocolEndpointTCP;

import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageDecoder;
import edu.washington.cs.oneswarm.f2f.servicesharing.DataMessage.RawMessageEncoder;
import edu.washington.cs.oneswarm.f2f.xml.XMLHelper;
import edu.washington.cs.oneswarm.ui.gwt.rpc.SharedServiceDTO;

public class SharedService extends PublishableService implements Comparable<SharedService>, Serializable {
    private static final long serialVersionUID = 5829873036975990477L;

    // Time the service is disabled after a failed connect attempt;
    public static final long FAILURE_BACKOFF = 60 * 1000;
    public static final String CONFIGURATION_PREFIX = "SHARED_SERVICE_";

    private long lastFailedConnect;
    private int activeConnections = 0;

    public SharedService(Long serviceId) {
        this.serviceId = serviceId;
    }

    public String getNickname() {
        return COConfigurationManager.getStringParameter(getNameKey());
    }

    private String getNameKey() {
        return CONFIGURATION_PREFIX + serviceId + "_name";
    }

    InetSocketAddress getAddress() {
        try {
            int port = COConfigurationManager.getIntParameter(getPortKey(), -1);
            String ip = COConfigurationManager.getStringParameter(getIpKey());
            return new InetSocketAddress(InetAddress.getByName(ip), port);
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    private String getPortKey() {
        return CONFIGURATION_PREFIX + serviceId + "_port";
    }

    private String getIpKey() {
        return CONFIGURATION_PREFIX + serviceId + "_ip";
    }

    public void setNickname(String name) {
        COConfigurationManager.setParameter(getNameKey(), name);
    }

    public void setAddress(InetSocketAddress address) {
        COConfigurationManager.setParameter(getPortKey(), address.getPort());
        COConfigurationManager.setParameter(getIpKey(), address.getAddress().getHostAddress());
    }

    @Override
    public int compareTo(SharedService that) {
        return this.getNickname().compareTo(that.getNickname());
    }

    protected ConnectionListener getMonitoringListener(final NetworkConnection conn) {
        final SharedService self = this;
        return new ConnectionListener() {
            @Override
            public void connectFailure(Throwable failure_msg) {
                self.activeConnections--;
                self.lastFailedConnect = System.currentTimeMillis();
            }

            @Override
            public void connectStarted() {
                self.activeConnections++;
            }

            @Override
            public void connectSuccess(ByteBuffer remaining_initial_data) {
            }

            @Override
            public void exceptionThrown(Throwable error) {
                self.activeConnections--;
            }

            @Override
            public String getDescription() {
                return "Shared Service Listener";
            }
        };
    }

    public NetworkConnection createConnection() {
        InetSocketAddress address = getAddress();
        ConnectionEndpoint target = new ConnectionEndpoint(address);
        target.addProtocol(new ProtocolEndpointTCP(address));
        NetworkConnection conn = NetworkManager.getSingleton().createConnection(target,
                new RawMessageEncoder(), new RawMessageDecoder(), false, false, new byte[0][0]);
        return new ListenedNetworkConnection(conn, this.getMonitoringListener(conn));
    }

    public boolean isEnabled() {
        long lastFailedAge = System.currentTimeMillis() - lastFailedConnect;
        if (activeConnections > 0) {
            return true;
        }
        boolean enabled = lastFailedAge > FAILURE_BACKOFF;
        if (!enabled) {
            ServiceSharingManager.logger.finer(String.format(
                    "Service %s is disabled, last failure: %d seconds ago", getNickname(),
                    lastFailedAge));
        }
        return enabled && this.enabled;
    }

    public SharedServiceDTO toDTO() {
        InetSocketAddress address = getAddress();
        return new SharedServiceDTO(getNickname(), Long.toHexString(serviceId), address.getAddress()
                .getHostAddress(), address.getPort());
    }

    @Override
    public String toString() {
        InetSocketAddress address = getAddress();
        return "key=" + serviceId + " name=" + getNickname() + " address=" + address + " enabled="
                + isEnabled();
    }

    public void clean() {
        COConfigurationManager.removeParameter(getPortKey());
        COConfigurationManager.removeParameter(getIpKey());
        COConfigurationManager.removeParameter(getNameKey());
    }

    @Override
    public String type() {
        return XMLHelper.SERVICE;
    }

    @Override
    public byte[] hashBase() {
        try {
            return (serviceId + getPublicKeyString() + getNickname()).getBytes(XMLHelper.ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void fullXML(XMLHelper xmlOut) throws SAXException {
        xmlOut.startElement(XMLHelper.SERVICE);
        xmlOut.writeTag(XMLHelper.SERVICE_ID, Long.toString(serviceId));
        xmlOut.writeTag(XMLHelper.PUBLIC_KEY, getPublicKeyString());
        xmlOut.writeTag(XMLHelper.NICKNAME, getNickname());
        xmlOut.writeTag(XMLHelper.SIGNATURE, signature());
        xmlOut.endElement(XMLHelper.SERVICE);
    }

    @Override
    public void shortXML(XMLHelper xmlOut) throws SAXException {
        fullXML(xmlOut);
    }
}