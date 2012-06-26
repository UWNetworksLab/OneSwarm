package edu.washington.cs.oneswarm.f2f.socks;

/**
 * Exception thrown by various socks classes to indicate errors with protocol or
 * unsuccessful server responses.
 */
public class SocksException extends java.io.IOException {
    private static final long serialVersionUID = 3261582171893982551L;
    private String errorString;
    private int errorByte;

    /**
     * Construct a SocksException with given error code.
     * <p>
     * Tries to look up message which corresponds to this error code.
     * 
     * @param errorByte
     *            Error code for this exception.
     */
    public SocksException(byte errorByte) {
        this.errorByte = errorByte;
        errorString = errorByte <= serverReplyMessage.length ? serverReplyMessage[errorByte]
                : "Unknown error message: " + errorByte;
    }

    /**
     * Get the error code associated with this exception.
     * 
     * @return Error code associated with this exception.
     */
    public int getErrorCode() {
        return errorByte <= serverReplyMessage.length ? errorByte
                : SocksConstants.Status.GENERAL_FAILURE;
    }

    /**
     * Get human readable representation of this exception.
     * 
     * @return String represntation of this exception.
     */
    public String toString() {
        return "Socks Exception: " + errorString;
    }

    public String getMessage() {
        return errorString;
    }

    static final String serverReplyMessage[] = { "Request Granted", "General SOCKS server failure",
            "Connection not allowed by ruleset", "Network unreachable", "Host unreachable",
            "Connection refused by destination host", "TTL expired",
            "Command not supported / Protocol error", "Address type not supported" };
}

