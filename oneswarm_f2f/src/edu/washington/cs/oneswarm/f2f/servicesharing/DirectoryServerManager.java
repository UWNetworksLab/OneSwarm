package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.xml.sax.SAXException;

import edu.washington.cs.oneswarm.f2f.xml.DirectoryServerMsgHandler;
import edu.washington.cs.oneswarm.f2f.xml.ExitNodeInfoHandler;
import edu.washington.cs.oneswarm.f2f.xml.XMLHelper;

public class DirectoryServerManager {
    private static final String REGISTER = "register";
    private static final String CHECKIN = "checkin";
    private static Logger log = Logger.getLogger(DirectoryServerManager.class.getName());
    private static final DirectoryServerManager instance = new DirectoryServerManager();

    private static final String DIRECTORY_SERVER_URL_CONFIG_KEY = "DIRECTORY_SERVER_URL_CONFIG_KEY";
    private static final long KEEPALIVE_INTERVAL = 55 * 60 * 1000;
    private static final long DIRECTORY_SERVER_REFRESH_INTERVAL = 55 * 60 * 1000;

    private static final String ACTION_PARAM = "?action=";

    public void setDirectoryServerUrls(String[] urls) {
        String urlsString = "";
        for (String url : urls) {
            urlsString += url + ",";
        }
        COConfigurationManager.setParameter(DIRECTORY_SERVER_URL_CONFIG_KEY, urlsString);
    }

    public String[] getDirectoryServerUrls() {
        String urlsString = COConfigurationManager.getStringParameter(
                DIRECTORY_SERVER_URL_CONFIG_KEY, "");
        return urlsString.split(",");
    }

    public DirectoryServerManager() {
        Timer keepAliveRegistrations = new Timer();
        keepAliveRegistrations.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    DirectoryServerManager.this.registerExitNodes();
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
        List<ExitNodeInfo> exitNodes = new LinkedList<ExitNodeInfo>();
        XMLHelper.parse(conn.getInputStream(), new ExitNodeInfoHandler(exitNodes));
        conn.disconnect();

        // TODO (nick) remove when partial update is ready
        ExitNodeList.getInstance().exitNodeList.clear();

        ExitNodeList.getInstance().addNodes(exitNodes);
    }

    public void registerExitNodes() throws IOException, SAXException {
        HttpURLConnection conn = createConnectionTo(CHECKIN);
        conn.setRequestProperty("Content-Type", "text/xml");
        XMLHelper xmlOut = new XMLHelper(conn.getOutputStream());
        // Write check-in request to the connection
        for (ExitNodeInfo node : ExitNodeList.getInstance().localSharedExitServices.values()) {
            if (node.getEnabled()) {
                node.shortXML(xmlOut);
            }
        }
        xmlOut.close();

        // Retry registrations that are fixable until no fixable errors remain.
        while (true) {
            // Parse reply for error messages
            List<DirectoryServerMsg> msgs = new LinkedList<DirectoryServerMsg>();
            XMLHelper.parse(conn.getInputStream(), new DirectoryServerMsgHandler(msgs));
            conn.disconnect();
            conn = null;

            List<ExitNodeInfo> toReRegister = decideWhatNeedsReregistering(msgs);

            // If there are no fixable errors, stop trying
            if (toReRegister.size() == 0) {
                break;
            }

            // Otherwise, retry the nodes that need ro be registered
            conn = createConnectionTo(REGISTER);
            conn.setRequestProperty("Content-Type", "text/xml");
            xmlOut = new XMLHelper(conn.getOutputStream());
            // Write register request to the connection
            for (ExitNodeInfo node : toReRegister) {
                node.fullXML(xmlOut);
            }
            xmlOut.close();
        }
    }

    private List<ExitNodeInfo> decideWhatNeedsReregistering(List<DirectoryServerMsg> msgs) {
        List<ExitNodeInfo> toReregister = new LinkedList<ExitNodeInfo>();
        for (DirectoryServerMsg msg : msgs) {
            // If serviceId is duplicate, pull the node out and give
            // it a new serviceId.

            if (msg.errorCodes.contains(XMLHelper.STATUS_SUCCESS)) {
                msg.removeErrorCode(XMLHelper.STATUS_SUCCESS);
            } else if (msg.errorCodes.contains(XMLHelper.ERROR_UNREGISTERED_SERVICE_ID)) {
                ExitNodeInfo temp = ExitNodeList.getInstance().localSharedExitServices
                        .get(msg.serviceId);
                toReregister.add(temp);
                msg.removeErrorCode(XMLHelper.ERROR_UNREGISTERED_SERVICE_ID);
            } else if (msg.errorCodes.contains(XMLHelper.ERROR_DUPLICATE_SERVICE_ID)) {

                ExitNodeInfo temp = ExitNodeList.getInstance().getExitNodeSharedService(
                        msg.serviceId);

                // This assumes a single shared service model.
                ExitNodeList.getInstance().resetLocalServiceKey();

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

    private HttpURLConnection createConnectionTo(String action) throws IOException {
        HttpURLConnection conn = null;
        for (String url : getDirectoryServerUrls()) {
            try {
                URL server = new URL(url + ACTION_PARAM + action);
                conn = (HttpURLConnection) server.openConnection();
            } catch (Exception e) {
                log.info(e.toString());
            }
        }
        if (conn == null) {
            log.warning("No directory servers avaliable.");
        }
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        return conn;
    }
}
