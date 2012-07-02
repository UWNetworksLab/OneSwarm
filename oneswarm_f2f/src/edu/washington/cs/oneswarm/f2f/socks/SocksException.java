package edu.washington.cs.oneswarm.f2f.socks;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;

/**
 * Exception thrown by various socks classes to indicate errors with protocol or
 * unsuccessful server responses.
 */
class SocksException extends java.io.IOException {
    private static final long serialVersionUID = 3261582171893982551L;
    private String errorString;
    private byte errorByte;

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
        setErrorString(errorByte);
    }
    
    public SocksException(IOException e){
        errorByte = SocksConstants.Status.GENERAL_FAILURE;            
        if(e instanceof NoRouteToHostException)
           errorByte = SocksConstants.Status.HOST_UNREACHABLE;
        else if(e instanceof ConnectException)
            errorByte = SocksConstants.Status.CONNECTION_REFUSED_BY_DESTINATION_HOST;
        else if(e instanceof InterruptedIOException)
            errorByte = SocksConstants.Status.TLL_EXPIRED;
        setErrorString(errorByte);
     }

    /**
     * Get the error code associated with this exception.
     * 
     * @return Error code associated with this exception.
     */
    public int getErrorCode() {
        int errorCode = errorByte & 0xff;
        return errorCode <= serverReplyMessage.length ? errorCode
                : SocksConstants.Status.GENERAL_FAILURE & 0xff;
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

    private static final String serverReplyMessage[] = { "Request Granted", "General SOCKS server failure",
            "Connection not allowed by ruleset", "Network unreachable", "Host unreachable",
            "Connection refused by destination host", "TTL expired",
            "Command not supported / Protocol error", "Address type not supported" };
    
    private void setErrorString(byte errorByte){
        errorString = errorByte <= serverReplyMessage.length ? serverReplyMessage[errorByte]
                : "Unknown error message: " + errorByte;
    }
}

