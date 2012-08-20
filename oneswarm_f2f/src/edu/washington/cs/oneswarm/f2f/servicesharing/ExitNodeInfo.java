package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

import org.xml.sax.SAXException;

import sun.security.x509.CertAndKeyGen;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import edu.washington.cs.oneswarm.f2f.xml.XMLHelper;

public class ExitNodeInfo implements Comparable<ExitNodeInfo> {
    // Publicly available info
    private String nickname;
    private long serviceId;
    private byte[] ipAddr;
    private int advertizedBandwidth;
    private PolicyTree exitPolicy;
    private Date onlineSince;
    private String version;
    private boolean enabled;

    public static final int KEY_SIZE_BITS = 1024;
    private PublicKey publicKey;
    private PrivateKey privateKey;

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
            "allow google.com:*", "allow *.google.com:*", "allow gmail.com:*",
            "allow googleusercontent.com:*", "allow wikipedia.org:*", "allow *.wikipedia.org:*"));

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

    public LinkedList<String> getPolicyStrings() {
        LinkedList<String> policies = new LinkedList<String>();
        ListIterator<edu.washington.cs.oneswarm.f2f.servicesharing.ExitNodeInfo.PolicyTree.PolicyNode> itr = exitPolicy.root.children
                .listIterator();
        while (itr.hasNext()) {
            policies.add(itr.next().toString());
        }
        return policies;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof ExitNodeInfo) {
            return this.serviceId == ((ExitNodeInfo) other).serviceId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) serviceId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean getEnabled() {
        return this.enabled;
    }

    public long getId() {
        return serviceId;
    }

    public void setId(long serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * Returns public key as String.
     * 
     * @return PublicKey in the following format ALGOYTHM:FORMAT:KEY (KEY in
     *         base 64)
     */
    public String getPublicKeyString() {
        return this.publicKey.getAlgorithm() + ":" + this.publicKey.getFormat() + ":"
                + Base64.encode(this.publicKey.getEncoded());
    }

    /**
     * See <code>getPublicKeyString()</code> for format.
     * 
     * @param key
     */
    public void setPublicKeyString(String key) {
        if (key == null || key.length() < 1) {
            return;
        }

        final String[] parts = key.split(":");
        publicKey = new PublicKey() {
            private static final long serialVersionUID = -7259961818850093509L;

            @Override
            public String getAlgorithm() {
                return parts[0];
            }

            @Override
            public String getFormat() {
                return parts[1];
            }

            @Override
            public byte[] getEncoded() {
                return Base64.decode(parts[2]);
            }
        };
        privateKey = null;
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

    public byte[] hashBase() {
        try {
            return (getPublicKeyString() + nickname + advertizedBandwidth + exitPolicy.toString() + version)
                    .getBytes(XMLHelper.ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String signature() {
        try {
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initSign(this.privateKey);
            sig.update(hashBase());
            return Base64.encode(sig.sign());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void generateNewKeys() {
        try {
            CertAndKeyGen keyPair = new CertAndKeyGen("RSA", "SHA1withRSA", null);
            keyPair.generate(KEY_SIZE_BITS);

            publicKey = keyPair.getPublicKey();
            privateKey = keyPair.getPrivateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

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

    public void shortXML(XMLHelper xmlOut) throws SAXException {
        xmlOut.startElement(XMLHelper.EXIT_NODE);
        xmlOut.writeTag(XMLHelper.SERVICE_ID, Long.toString(serviceId));
        xmlOut.writeTag(XMLHelper.PUBLIC_KEY, getPublicKeyString());
        xmlOut.writeTag(XMLHelper.SIGNATURE, signature());
        xmlOut.endElement(XMLHelper.EXIT_NODE);
    }

    private static class PolicyTree {
        private PolicyNode root;
        private final StringBuilder policyString;
        private final StringBuilder policyStringAsEntered;

        public PolicyTree(String[] policy) {
            policyString = new StringBuilder();
            policyStringAsEntered = new StringBuilder();
            root = new PolicyNode("");
            addPolicies(policy);
        }

        public void addPolicies(String[] policyStrings) {
            for (int i = 0; i < policyStrings.length; i++) {
                addPolicy(policyStrings[i]);
            }
        }

        public void addPolicy(String policy) {
            policyStringAsEntered.append(policy + "\n");
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

        public String toStringWithComments() {
            return policyStringAsEntered.toString();
        }

        private enum PolicyValue {
            ACCEPT, REJECT
        }

        class PolicyNode {
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
}
