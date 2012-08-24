package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.xml.sax.SAXException;

import edu.washington.cs.oneswarm.f2f.xml.XMLHelper;

public class ExitNodeInfo extends PublishableService implements Comparable<ExitNodeInfo>,
        Serializable {
    private static final long serialVersionUID = -1094323182607119597L;

    // Publicly available info

    private byte[] ipAddr;
    private int advertizedBandwidth;
    private PolicyTree exitPolicy;
    private Date onlineSince;
    private String version;
    private boolean enabled;
    public String type;

    // Private data stored about this exit node
    private static final int HISTORY_LENGTH = 10; // Must be >= 3
    private Queue<Integer> bandwidthHistory;
    private int avgBandwidth; // Stored avg of history (KB/s)
    private Queue<Integer> latencyHistory;
    private int avgLatency; // Stored avg of history (ms)

    public static final LinkedList<String> EVERYTHING = new LinkedList<String>(
            Arrays.asList("allow *:*"));
    public static final LinkedList<String> LOCAL = new LinkedList<String>(
            Arrays.asList("allow localhost:80"));
    // TODO(willscott): Expand this list.
    public static final LinkedList<String> SAFE = new LinkedList<String>(Arrays.asList(
            "allow facebook.com:*", "allow *.facebook.com:*", "allow google.com:*",
            "allow *.google.com:*", "allow youtube.com:*", "allow yahoo.com:*",
            "allow baidu.com:*", "allow wikipedia.org:*", "allow *.wikipedia.org:*",
            "allow live.com:*", "allow qq.com:*", "allow twitter.com:*", "allow amazon.com:*",
            "allow *.blogspot.com:*", "allow taobao.com:*", "allow linkedin.com:*",
            "allow yahoo.co.jp:*", "allow sina.com.cn:*", "allow msn.com:*", "allow yandex.ru:*",
            "allow babylon.com:*", "allow bing.com:*",
            "allow *.wordpress.com:*",
            "allow ebay.com:*", // top 25 alexa.
            "allow t.co:*", "allow bbc.co.uk:*", "allow mail.ru:*", "allow blogger.com:*",
            "allow gmail.com:*", "allow googleusercontent.com:*"));

    public static final char COMMENT_CHAR = '#';

    public ExitNodeInfo(String nickname, long id, int advertBandwidth, String[] exitPolicy,
            Date onlineSince, String version) {
        this.nickname = nickname;
        this.serviceId = id;
        this.advertizedBandwidth = advertBandwidth;
        this.exitPolicy = new PolicyTree(exitPolicy);
        this.onlineSince = onlineSince;
        this.version = version;

        generateNewKeys();
        setEnabled(true);
    }

    /**
     * For use by XMLParser. Allows creation of invalid ExitNode
     */
    public ExitNodeInfo() {
        this("INVALID", 0, 0, new String[] { "reject *:*" }, null, "INVALID");
    }

    /**
     * Sets the exit policy of the server using Tor's notation.
     * 
     * The format is: (reject|accept) (domain|ip)[:port] with one policy per
     * line of the string.
     * 
     * EX: reject 66.146.193.31:* accept *:80
     * 
     * @param policy
     *            Tor style exit policy array
     */
    public void setExitPolicy(String[] policy) {
        exitPolicy = new PolicyTree(policy);
    }

    public String getExitPolicy() {
        return exitPolicy.toString();
    }

    public boolean allowsConnectionTo(String url, int port) {
        return this.enabled ? exitPolicy.getPolicy(url, port) : false;
    }

    /**
     * Compares as per compareTo()'s contract using bandwidth. Attempts to use
     * privately collected data about each node if it is sufficiently available.
     */
    @Override
    public int compareTo(ExitNodeInfo other) {
        int thisBandwidth = this.advertizedBandwidth;
        int otherBandwidth = other.advertizedBandwidth;
        if (this.bandwidthHistory.size() >= 3) {
            thisBandwidth = this.avgBandwidth;
        }
        if (other.bandwidthHistory.size() >= 3) {
            otherBandwidth = other.avgBandwidth;
        }
        return thisBandwidth - otherBandwidth;
    }

    public List<String> getPolicyStrings() {
        return exitPolicy.policyStringsAsEntered;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public int getAdvertizedBandwith() {
        return advertizedBandwidth;
    }

    public void setAdvertizedBandwidth(int advertizedBandwidth) {
        this.advertizedBandwidth = advertizedBandwidth;
    }

    public Date getOnlineSinceDate() {
        return onlineSince;
    }

    public void setOnlineSinceDate(Date date) {
        this.onlineSince = date;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void recordBandwidth(int kbps) {
        bandwidthHistory.add(kbps);
        avgBandwidth = averageIntQueue(bandwidthHistory);
    }

    public void recordLatency(int ms) {
        latencyHistory.add(ms);
        avgLatency = averageIntQueue(latencyHistory);
    }

    public int getAvgBandwidth() {
        return avgBandwidth;
    }

    public int getAvgLatency() {
        return avgLatency;
    }

    private int averageIntQueue(Queue<Integer> q) {
        while (q.size() > HISTORY_LENGTH) {
            q.remove();
        }
        int sum = 0;
        for (int i = 0; i < q.size(); i++) {
            sum += q.remove();
        }
        return sum / q.size();
    }

    public byte[] getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(byte[] ipAddr) {
        if (ipAddr.length == 4 || ipAddr.length == 16) {
            this.ipAddr = ipAddr;
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public byte[] hashBase() {
        try {
            return (serviceId + getPublicKeyString() + nickname + advertizedBandwidth
                    + exitPolicy.toString() + version).getBytes(XMLHelper.ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void fullXML(XMLHelper xmlOut) throws SAXException {
        xmlOut.startElement(XMLHelper.EXIT_NODE);
        xmlOut.writeTag(XMLHelper.SERVICE_ID, Long.toString(serviceId));
        xmlOut.writeTag(XMLHelper.PUBLIC_KEY, getPublicKeyString());
        xmlOut.writeTag(XMLHelper.NICKNAME, nickname);
        xmlOut.writeTag(XMLHelper.BANDWIDTH, "" + advertizedBandwidth);
        xmlOut.writeTag(XMLHelper.EXIT_POLICY, exitPolicy.toString());
        xmlOut.writeTag(XMLHelper.VERSION, version);
        xmlOut.writeTag(XMLHelper.SIGNATURE, signature());
        xmlOut.endElement(XMLHelper.EXIT_NODE);
    }

    @Override
    public void shortXML(XMLHelper xmlOut) throws SAXException {
        xmlOut.startElement(XMLHelper.EXIT_NODE);
        xmlOut.writeTag(XMLHelper.SERVICE_ID, Long.toString(serviceId));
        xmlOut.writeTag(XMLHelper.PUBLIC_KEY, getPublicKeyString());
        xmlOut.writeTag(XMLHelper.SIGNATURE, signature());
        xmlOut.endElement(XMLHelper.EXIT_NODE);
    }

    private static class PolicyTree implements Serializable {
        private static final long serialVersionUID = 6781046686283739766L;
        private PolicyNode root;
        private final StringBuilder policyString;
        private final List<String> policyStringsAsEntered;

        public PolicyTree(String[] policy) {
            policyString = new StringBuilder();
            policyStringsAsEntered = new LinkedList<String>();
            root = new PolicyNode("");
            addPolicies(policy);
        }

        public void addPolicies(String[] policyStrings) {
            for (int i = 0; i < policyStrings.length; i++) {
                addPolicy(policyStrings[i]);
            }
        }

        public void addPolicy(String policy) {
            policyStringsAsEntered.add(policy);
            policy = policy.toLowerCase();

            // Remove comments from published version and before adding into
            // data structure
            int commentIndex = policy.lastIndexOf(COMMENT_CHAR);
            if (commentIndex >= 0) {
                policy = policy.substring(0, commentIndex);
            }
            if (policy.trim().equals("")) {
                return;
            }

            policyString.append(policy + ",");
            PolicyValue policyVal;
            int port;

            String[] policyParts = policy.split("[ :]");

            switch (policyParts.length) {
            case 2:
                port = -1;
                break;
            case 3:
                port = policyParts[2].equals("*") ? -1 : Integer.parseInt(policyParts[2]);
                if (port < -1 || port > 65535) {
                    throw new IllegalArgumentException("Improper Format - Port out of range.");
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "Improper Format - Should be (reject|accept) (domain|ip)[:port]");
            }

            if (policyParts[0].equalsIgnoreCase("accept")) {
                policyVal = PolicyValue.ACCEPT;
            } else if (policyParts[0].equalsIgnoreCase("reject")) {
                policyVal = PolicyValue.REJECT;
            } else {
                throw new IllegalArgumentException(
                        "Improper Format - First word is not (accept|reject)");
            }

            String[] urlParts = policyParts[1].split("\\.");
            root = addPolicy(urlParts, urlParts.length - 1, port, policyVal, root);
        }

        private PolicyNode addPolicy(String[] url, int index, int port, PolicyValue policy,
                PolicyNode root) {
            if (index < 0) {
                root.children.add(new PolicyNode(port, policy));
            } else {
                PolicyNode child = root.lastInstanceOfUrlPart(url[index]);
                if (child == null) {
                    child = root.add(new PolicyNode(url[index]));
                }
                child = addPolicy(url, index - 1, port, policy, child);
            }
            return root;
        }

        // Must be a specific url or ip, and a specific port
        public boolean getPolicy(String url, int port) {
            url = url.toLowerCase();
            String[] urlParts = url.split("\\.");
            PolicyValue policy = getPolicy(urlParts, urlParts.length - 1, port, root);
            if (PolicyValue.ACCEPT == policy) {
                return true;
            }
            return false;
        }

        private PolicyValue getPolicy(String[] domain, int index, int port, PolicyNode root) {
            for (PolicyNode child : root.children) {
                if (index >= 0 && (child.domain.equals("*") || domain[index].equals(child.domain))) {
                    PolicyValue temp = getPolicy(domain, index - 1, port, child);
                    if (temp != null) {
                        return temp;
                    }
                } else if (port == child.port || child.port == -1) {
                    return child.policy;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return policyString.toString();
        }

        private enum PolicyValue {
            ACCEPT, REJECT
        }

        class PolicyNode implements Serializable {
            private static final long serialVersionUID = -8242520861243603331L;
            /*
             * Either domainPart or port will be filled for each node. "*" is
             * wild card for domainPart, "" is unused field -1 means wild card
             * for port, -2 is unused field
             */
            String domain;
            int port;
            List<PolicyNode> children;
            PolicyValue policy;

            // Constructs a domainPart node
            public PolicyNode(String domainPart) {
                this(domainPart, -2, null);
            }

            // Constructs a port node
            public PolicyNode(int port, PolicyValue policy) {
                this("", port, policy);
            }

            private PolicyNode(String domainPart, int port, PolicyValue policy) {
                this.domain = domainPart;
                this.port = port;
                this.policy = policy;
                this.children = new LinkedList<PolicyNode>();
            }

            public PolicyNode lastInstanceOfUrlPart(String urlPart) {
                if (!children.isEmpty() && children.get(children.size() - 1).domain.equals(urlPart)) {
                    return children.get(children.size() - 1);
                }
                return null;
            }

            public PolicyNode add(PolicyNode node) {
                children.add(node);
                return node;
            }
        }
    }

    @Override
    public String type() {
        return XMLHelper.EXIT_NODE;
    }
}
