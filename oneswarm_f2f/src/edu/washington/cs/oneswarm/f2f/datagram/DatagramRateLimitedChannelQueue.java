package edu.washington.cs.oneswarm.f2f.datagram;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gudy.azureus2.core3.util.Average;
import org.gudy.azureus2.core3.util.Debug;

import edu.washington.cs.oneswarm.f2f.datagram.DatagramConnection.DatagramSendThread;
import edu.washington.cs.oneswarm.f2f.messaging.OSF2FMessage;
import edu.washington.cs.oneswarm.f2f.network.FriendConnection;

public class DatagramRateLimitedChannelQueue extends DatagramRateLimiter {
    public final static Logger logger = Logger.getLogger(DatagramRateLimitedChannelQueue.class
            .getName());

    // Just set to 1s for now, will be changed with pack pressure cc.
    private static final long MAX_DYN_QUEUE_LENGTH_MS = 1000;

    // average over 10s, update every 1s.
    private final Average uploadRate = Average.getInstance(1000, 10);

    private final DatagramSendThread sendThread;
    private final LinkedList<OSF2FMessage> messageQueue = new LinkedList<OSF2FMessage>();

    private int queueLength = 0;
    private final static int BASE_MAX_QUEUE_LENGTH = 2 * DatagramConnection.MAX_DATAGRAM_SIZE;

    // Reciprocal of how full bucket must be to assume backpressure limitation. (10 = 90% full, 100 = 99%)
    private final static int BACKPRESSURE_LIMITATION_THRESHOLD = 10;

    // Visible for testing
    int maxQueueLength = BASE_MAX_QUEUE_LENGTH;

    private long lastPacketSentAt = System.currentTimeMillis();

    private final int channelId;

    public DatagramRateLimitedChannelQueue(int channelId, DatagramSendThread sendThread) {
        this.sendThread = sendThread;
        this.channelId = channelId;
    }

    @Override
    public void allocateTokens() {
    }

    /**
     * Fill the token bucket for this queue with up to the given number of tokens.
     * @param tokens the number of tokens available to be added to the bucket.
     * @return The number of tokens added to the bucket.
     */
    @Override
    public synchronized int refillBucket(int tokens) {
        int originalTokens = availableTokens;
        availableTokens += tokens;
        int maxTokens = backpressureLimitation >= 0 ? backpressureLimitation : maxAvailableTokens;

        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(toString() + ": refilling " + tokens + " tokens, available="
                    + availableTokens);
        }        
        
        // Overflows.
        if (availableTokens < originalTokens && tokens > 0) {
            logger.finest(toString() + ": bucket overflow.");
            int used = maxTokens - originalTokens;
            availableTokens = maxTokens;
            tokens = used;
        }
        // Send
        send();

        // Return leftovers.
        if (availableTokens > maxTokens) {
            int overflow = availableTokens - maxTokens;
            logger.finest(toString() + ": overflow by " + overflow + "tokens, before: "
                    + availableTokens + "/" + maxTokens);
            availableTokens = maxTokens;
            return tokens - overflow;
        }
        return tokens;
    }

    public synchronized void queuePacket(OSF2FMessage message) {
        int messageSize = getWireSize(message);
        if (messageSize + queueLength > maxQueueLength) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(toString() + ": dropping packet, packetSize=" + messageSize
                        + " queueLength=" + queueLength + " max=" + maxQueueLength + ", packet:"
                        + message.getDescription());
            }
            message.destroy();
            return;
        }
        queueLength += messageSize;
        messageQueue.add(message);
        send();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - lastPacketSentAt > FriendConnection.OVERLAY_FORWARD_TIMEOUT;
    }

    public boolean isEmpty() {
        return queueLength == 0;
    }

    private int getWireSize(OSF2FMessage message) {
        int serializedByteNum = OSF2FMessage.MESSAGE_HEADER_LEN + message.getMessageSize();
        int padding = DatagramEncrypter.calcPaddingLength(serializedByteNum);
        int wireSize = DatagramEncrypter.SEQUENCE_NUMBER_BYTES + serializedByteNum + padding
                + DatagramEncrypter.HMAC_KEY_LENGTH;
        return wireSize;
    }

    private void send() {
        // Only initiate if we have enough tokens to send a full packet
        if (availableTokens < DatagramConnection.MAX_DATAGRAM_SIZE) {
            logger.finest(toString() + ": not enough tokens available to send a packet");
            return;
        }
        try {
            int bytes = 0;
            int packets = 0;
            while (sendOk()) {
                OSF2FMessage msg = messageQueue.removeFirst();
                int messageSize = getWireSize(msg);
                packets++;
                availableTokens -= messageSize;
                queueLength -= messageSize;
                bytes += messageSize;
                sendThread.queueMessage(msg, false);
            }
            if (packets > 0) {
                sendThread.notifyQueue();
            }
            uploadRate.addValue(bytes);
            updateMaxQueueLength();
            lastPacketSentAt = System.currentTimeMillis();
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(toString() + ": sent: " + packets + " packets (" + bytes
                        + " bytes), queue=" + queueLength + " rate=" + uploadRate.getAverage()
                        + " max_queue=" + maxQueueLength + " tokens=" + availableTokens);
                if (messageQueue.size() > 0) {
                    OSF2FMessage m = messageQueue.getFirst();
                    logger.finest(toString() + ": message queue: " + messageQueue.size()
                            + "packets, space needed to send next: "
                            + (getWireSize(m) - availableTokens));
                }
            }

        } catch (InterruptedException e) {
            Debug.out(e);
        }
    }

    private boolean sendOk() {
        if (messageQueue.size() == 0) {
            return false;
        }
        return availableTokens >= getWireSize(messageQueue.getFirst());
    }

    private void updateMaxQueueLength() {
        maxQueueLength = (int) Math.round(BASE_MAX_QUEUE_LENGTH + MAX_DYN_QUEUE_LENGTH_MS
                * uploadRate.getAverage() / 1000.0);
    }

    public synchronized void clear() {
        while (messageQueue.size() > 0) {
            OSF2FMessage message = messageQueue.removeLast();
            message.destroy();
        }
    }

    @Override
    public String toString() {
        return super.toString() + " Channel=" + getChannelId();
    }

    public int getChannelId() {
        return channelId;
    }

    public boolean isBackpressureLimited() {
        return this.backpressureLimitation >= 0 && this.backpressureLimitation < this.maxAvailableTokens &&
                (this.backpressureLimitation - this.availableTokens) * BACKPRESSURE_LIMITATION_THRESHOLD < this.backpressureLimitation;
    }
}
