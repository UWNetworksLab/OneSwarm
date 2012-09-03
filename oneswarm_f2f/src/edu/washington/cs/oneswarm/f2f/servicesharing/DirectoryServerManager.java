package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.xml.sax.SAXException;

import com.google.common.annotations.VisibleForTesting;

import edu.washington.cs.oneswarm.f2f.xml.DirectoryInfoHandler;
import edu.washington.cs.oneswarm.f2f.xml.DirectoryServerMsgHandler;
import edu.washington.cs.oneswarm.f2f.xml.XMLHelper;

public class DirectoryServerManager {
    private static final String REGISTER = "register";
    private static final String CHECKIN = "checkin";
    private static Logger log = Logger.getLogger(DirectoryServerManager.class.getName());
    private static final DirectoryServerManager instance = new DirectoryServerManager();

    private static final String DIRECTORY_SERVER_URL_CONFIG_KEY = "DIRECTORY_SERVER_URL_CONFIG_KEY";
    private static final String DIRECTORY_SERVER_CERT_CONFIG_KEY = "DIRECTORY_SERVER_CERT_CONFIG_KEY";
    private static final long KEEPALIVE_INTERVAL = 55 * 60 * 1000;
    private static final long DIRECTORY_SERVER_REFRESH_INTERVAL = 55 * 60 * 1000;

    private static final String ACTION_PARAM = "?action=";
    private Signature serverSignature = null;

    public void setDirectoryServer(String[] urls, File certificate) throws IOException {
        String urlsString = "";
        for (String url : urls) {
            urlsString += url + ",";
        }
        COConfigurationManager.setParameter(DIRECTORY_SERVER_URL_CONFIG_KEY, urlsString);
        COConfigurationManager.setParameter(DIRECTORY_SERVER_CERT_CONFIG_KEY, certificate.getCanonicalPath());
    }

    public String[] getDirectoryServerUrls() {
        String urlsString = COConfigurationManager.getStringParameter(
                DIRECTORY_SERVER_URL_CONFIG_KEY, "");
        return urlsString.split(",");
    }
    
    public Signature getDirectoryServerSignature() {
        if (this.serverSignature != null) {
            return this.serverSignature;
        } else {
            try {
                String filePath = COConfigurationManager.getStringParameter(DIRECTORY_SERVER_CERT_CONFIG_KEY, "");
                File certFile = new File(filePath);
                InputStream inStream = new FileInputStream(certFile);
                Certificate cert = CertificateFactory.getInstance("X.509").generateCertificate(inStream);
                this.serverSignature = Signature.getInstance("SHA1withRSA");
                this.serverSignature.initVerify(cert);
                return this.serverSignature;
            } catch(Exception e) {
                this.serverSignature = null;
                log.warning("Couldn't load directory server certificate " + e.getMessage());
                return null;
            }
        }
    }

    public DirectoryServerManager() {
        Timer keepAliveRegistrations = new Timer();
        keepAliveRegistrations.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    DirectoryServerManager.this.registerPublishableServices();
                } catch (IOException e) {
                    // Unexpected
                    e.printStackTrace();
                } catch (SAXException e) {
                    // Unexpected
                    e.printStackTrace();
                }
            }
        }, KEEPALIVE_INTERVAL / 2, KEEPALIVE_INTERVAL);

        keepAliveRegistrations.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    DirectoryServerManager.this.refreshFromDirectoryServer();
                } catch (IOException e) {
                    // Unexpected
                    e.printStackTrace();
                } catch (SAXException e) {
                    // Unexpected
                    e.printStackTrace();
                }
            }
        }, 5 * 1000, DIRECTORY_SERVER_REFRESH_INTERVAL);
    }

    public static DirectoryServerManager getInstance() {
        return instance;
    }

    protected void refreshFromDirectoryServer() throws IOException, SAXException {
        HttpURLConnection conn = createConnectionTo("list");
        List<PublishableService> services = new LinkedList<PublishableService>();
        XMLHelper.parse(conn.getInputStream(), new DirectoryInfoHandler(services), this.getDirectoryServerSignature());
        conn.disconnect();

        // Clear all non-manually added client services
        Iterator<ClientService> serviceItr = ServiceSharingManager.getInstance().clientServices
                .values().iterator();
        while (serviceItr.hasNext()) {
            if (!serviceItr.next().manuallyAdded) {
                serviceItr.remove();
            }
        }

        List<ExitNodeInfo> exitNodes = new LinkedList<ExitNodeInfo>();
        Iterator<PublishableService> itr = services.iterator();
        while (itr.hasNext()) {
            PublishableService service = itr.next();
            if (service instanceof SharedService) {
                itr.remove();
                ClientService newService = new ClientService(service.serviceId);
                newService.setName(((SharedService) service).getName());
                ServiceSharingManager.getInstance().clientServices.put(service.serviceId,
                        newService);
            } else {
                exitNodes.add((ExitNodeInfo) service);
            }
        }

        // TODO (nick) remove clear when partial update is implemented
        ExitNodeList.getInstance().exitNodeList.clear();

        // Add remaining exitNodes to the ExitNodeList
        ExitNodeList.getInstance().addNodes(exitNodes);
    }

    public void registerPublishableServices() throws IOException, SAXException {
        HttpURLConnection conn = createConnectionTo(CHECKIN);
        XMLHelper xmlOut = new XMLHelper(conn.getOutputStream());
        // Write check-in request to the connection
        for (ExitNodeInfo node : ExitNodeList.getInstance().localSharedExitServices.values()) {
            if (node.getEnabled()) {
                node.shortXML(xmlOut);
            }
        }

        for (SharedService service : ServiceSharingManager.getInstance().sharedServices.values()) {
            if (service.published) {
                service.shortXML(xmlOut);
            }
        }
        xmlOut.close();

        // Retry registrations that are fixable until no fixable errors remain.
        while (true) {
            // Parse reply for error messages
            List<DirectoryServerMsg> msgs = new LinkedList<DirectoryServerMsg>();
            XMLHelper.parse(conn.getInputStream(), new DirectoryServerMsgHandler(msgs), this.getDirectoryServerSignature());
            conn.disconnect();
            conn = null;

            List<PublishableService> toReRegister = decideWhatNeedsReregistering(msgs);

            // If there are no fixable errors, stop trying
            if (toReRegister.size() == 0) {
                break;
            }

            // Otherwise, retry the nodes that need ro be registered
            conn = createConnectionTo(REGISTER);
            xmlOut = new XMLHelper(conn.getOutputStream());
            // Write register request to the connection
            for (PublishableService node : toReRegister) {
                node.fullXML(xmlOut);
            }
            xmlOut.close();
        }
    }

    private List<PublishableService> decideWhatNeedsReregistering(List<DirectoryServerMsg> msgs) {
        List<PublishableService> toReregister = new LinkedList<PublishableService>();
        for (DirectoryServerMsg msg : msgs) {
            // If serviceId is duplicate, pull the node out and give
            // it a new serviceId.

            if (msg.errorCodes.contains(XMLHelper.STATUS_SUCCESS)) {
                msg.removeErrorCode(XMLHelper.STATUS_SUCCESS);
            } else if (msg.errorCodes.contains(XMLHelper.ERROR_UNREGISTERED_SERVICE_ID)) {
                PublishableService temp = getPublishableServiceById(msg.serviceId);
                toReregister.add(temp);
                msg.removeErrorCode(XMLHelper.ERROR_UNREGISTERED_SERVICE_ID);
            } else if (msg.errorCodes.contains(XMLHelper.ERROR_DUPLICATE_SERVICE_ID)) {

                PublishableService temp = getPublishableServiceById(msg.serviceId);

                if (temp instanceof ExitNodeInfo) {
                    // This assumes a single shared service model.
                    ExitNodeList.getInstance().resetLocalServiceKey();
                } else if (temp instanceof SharedService) {
                    InetSocketAddress address = ((SharedService) temp).getAddress();
                    String name = ((SharedService) temp).getName();
                    ServiceSharingManager.getInstance().deregisterServerService(msg.serviceId,
                            false);
                    long serviceId = new Random().nextLong();
                    ServiceSharingManager.getInstance().registerSharedService(serviceId, name,
                            address);
                    temp = ServiceSharingManager.getInstance().sharedServices.get(serviceId);
                }

                toReregister.add(temp);
                msg.removeErrorCode(XMLHelper.ERROR_DUPLICATE_SERVICE_ID);
            }
            while (msg.errorCodes.size() > 0 && msg.errorStrings.size() > 0) {
                log.warning("ExitNode Registration Error: " + msg.errorCodes.remove(0) + " - "
                        + msg.errorStrings.remove(0));
            }
        }
        msgs.clear();
        return toReregister;
    }

    private PublishableService getPublishableServiceById(long serviceId) {
        PublishableService service = ExitNodeList.getInstance().getExitNodeSharedService(serviceId);
        if (service != null) {
            return service;
        }
        service = ServiceSharingManager.getInstance().sharedServices.get(serviceId);
        if (service != null) {
            return service;
        }
        throw new IllegalArgumentException("Service ID " + serviceId
                + " is not a known PublishableService");
    }

    private HttpURLConnection createConnectionTo(String action) throws IOException {
        HttpURLConnection conn = null;
        for (String url : getDirectoryServerUrls()) {
            try {
                URL server = new URL(url + ACTION_PARAM + action);
                                
                conn = (HttpURLConnection) server.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.connect();
                break;
            } catch (Exception e) {
                log.info(e.toString());
            }
        }
        if (conn == null) {
            log.warning("No directory servers avaliable.");
        }
        return conn;
    }

    @VisibleForTesting
    public void allowInsecure() {
        XMLHelper.validateDigest = false;
        log.warning("Not validating directory server identity.");
    }
}
