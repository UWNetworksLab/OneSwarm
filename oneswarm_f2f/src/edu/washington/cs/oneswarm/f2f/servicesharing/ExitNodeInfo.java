package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class ExitNodeInfo implements Comparable<ExitNodeInfo> {
    // Publicly available info
    private String nickname;
    private long id;
    private int advertizedBandwidth;
    private PolicyTree exitPolicy;
    private Date onlineSince;
    private String version;

    // Private data stored about this exit node
    private static final int HISTORY_LENGTH = 10; // Must be >= 3
    private Queue<Integer> bandwidthHistory; 
    private int avgBandwidth; // Stored avg of history (KB/s)
    private Queue<Integer> latencyHistory; 
    private int avgLatency; // Stored avg of history (ms)

    public ExitNodeInfo(String nickname, long id, int advertBandwidth, String[] exitPolicy,
            Date lastOutage, String version) {
        this.nickname = nickname;
        this.id = id;
        this.advertizedBandwidth = advertBandwidth;
        this.exitPolicy = new PolicyTree(exitPolicy);
        this.onlineSince = lastOutage;
        this.version = version;
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

    public boolean allowsConnectionTo(String url, int port) {
        return exitPolicy.getPolicy(url, port);
    }

    /**
     * Compares as per compareTo()'s contract using bandwidth. Attempts to use
     * privately collected data about each node if it is sufficiently available.
     */
    public int compareTo(ExitNodeInfo other) {
        int thisBandwidth = this.advertizedBandwidth;
        int otherBandwidth = other.advertizedBandwidth;
        if (this.bandwidthHistory.size() >= 3)
            thisBandwidth = this.avgBandwidth;
        if (other.bandwidthHistory.size() >= 3)
            otherBandwidth = other.avgBandwidth;
        return thisBandwidth - otherBandwidth;
    }

    public String getNickname() {
        return nickname;
    }

    public long getId() {
        return id;
    }

    public int getAdvertizedBandwith() {
        return advertizedBandwidth;
    }

    public Date getOnlineSinceDate() {
        return onlineSince;
    }

    public String getVersion() {
        return version;
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
        while (q.size() > HISTORY_LENGTH)
            q.remove();
        int sum = 0;
        for (int i = 0; i < q.size(); i++)
            sum += q.remove();
        return sum / q.size();
    }

    
    private static class PolicyTree {
        private PolicyNode root;

        public PolicyTree(String[] policy) {
            root = new PolicyNode("");
            addPolicies(policy);
        }

        public void addPolicies(String[] policyStrings) {
            for (int i = 0; i < policyStrings.length; i++)
                addPolicy(policyStrings[i]);
        }

        public void addPolicy(String policyString) {
            policyString = policyString.toLowerCase();
            PolicyValue policy;
            int port;

            String[] policyParts = policyString.split("[ :]");

            switch (policyParts.length) {
            case 2:
                port = -1;
                break;
            case 3:
                port = policyParts[2].equals("*") ? -1 : Integer.parseInt(policyParts[2]);
                if (port < -1 || port > 65535)
                    throw new IllegalArgumentException("Improper Format - Port out of range.");
                break;
            default:
                throw new IllegalArgumentException(
                        "Improper Format - Should be (reject|accept) (domain|ip)[:port]");
            }

            if (policyParts[0].equalsIgnoreCase("accept"))
                policy = PolicyValue.ACCEPT;
            else if (policyParts[0].equalsIgnoreCase("reject"))
                policy = PolicyValue.REJECT;
            else
                throw new IllegalArgumentException(
                        "Improper Format - First word is not (accept|reject)");

            String[] urlParts = policyParts[1].split("\\.");
            root = addPolicy(urlParts, urlParts.length - 1, port, policy, root);
        }

        private PolicyNode addPolicy(String[] url, int index, int port, PolicyValue policy,
                PolicyNode root) {
            if (index < 0) {
                root.children.add(new PolicyNode(port, policy));
            } else {
                PolicyNode child = root.lastInstanceOfUrlPart(url[index]);
                if (child == null)
                    child = root.add(new PolicyNode(url[index]));
                child = addPolicy(url, index - 1, port, policy, child);
            }
            return root;
        }

        // Must be a specific url or ip, and a specific port
        public boolean getPolicy(String url, int port) {
            url = url.toLowerCase();
            String[] urlParts = url.split("\\.");
            PolicyValue policy = getPolicy(urlParts, urlParts.length - 1, port, root);
            if (PolicyValue.ACCEPT == policy)
                return true;
            return false;
        }

        private PolicyValue getPolicy(String[] domain, int index, int port, PolicyNode root) {
            for (PolicyNode child : root.children) {
                if (index >= 0 && (child.domain.equals("*") || domain[index].equals(child.domain))) {
                    PolicyValue temp = getPolicy(domain, index - 1, port, child);
                    if (temp != null)
                        return temp;
                } else if (port == child.port || child.port == -1)
                    return child.policy;
            }
            return null;
        }
        
        private enum PolicyValue {
            ACCEPT, REJECT
        }
        
        class PolicyNode {
        /*
         * Either domainPart or port will be filled for each node. "*" is wild
         * card for domainPart, "" is unused field -1 means wild card for port,
         * -2 is unused field
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
            if (!children.isEmpty() && children.get(children.size() - 1).domain.equals(urlPart))
                return children.get(children.size() - 1);
            return null;
        }

        public PolicyNode add(PolicyNode node) {
            children.add(node);
            return node;
        }

        public String toString() {
            String temp = "";
            temp += domain.isEmpty() ? "--" : domain;
            temp += ":";
            temp += port == -1 ? "*" : port == -2 ? "--" : port;
            return temp;
        }
    }
    }
}
