package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.ArrayList;
import java.util.List;

public class DirectoryServerMsg {
    public long serviceId; // The service that this message is associated with.
    public List<Integer> errorCodes;
    public List<String> errorStrings;

    public DirectoryServerMsg() {
        errorCodes = new ArrayList<Integer>();
        errorStrings = new ArrayList<String>();
    }

    public void removeErrorCode(int errorCode) {
        int index = errorCodes.indexOf(errorCode);
        errorCodes.remove(index);
        errorStrings.remove(index);
    }
}
