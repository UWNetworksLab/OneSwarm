package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.washington.cs.oneswarm.f2f.xml.XMLHelper;

public class DirectoryServerMsgHandler extends DefaultHandler {
    private DirectoryServerMsg tempMsg;
    private String tempVal;
    private long tempServiceId;
    private final List<DirectoryServerMsg> messages;

    public DirectoryServerMsgHandler(List<DirectoryServerMsg> list) {
        this.messages = list;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        if (qName.equals(XMLHelper.EXIT_NODE)) {
            tempMsg = new DirectoryServerMsg();
        }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase(XMLHelper.EXIT_NODE)) {
            // exitNodes.
        } else if (qName.equalsIgnoreCase(XMLHelper.SERVICE_ID)) {
            tempServiceId = Long.parseLong(tempVal);
        } else if (qName.equalsIgnoreCase(XMLHelper.STATUS_CODE)) {
            tempMsg.errorCodes.add(Integer.parseInt(tempVal));
        } else if (qName.equalsIgnoreCase(XMLHelper.STATUS_MESSAGE)) {
            tempMsg.errorStrings.add(tempVal);
        } else if (qName.equalsIgnoreCase(XMLHelper.STATUS)) {
            tempMsg.serviceId = tempServiceId;
            messages.add(tempMsg);
            tempServiceId = 0;
        }
    }

    @Override
    public void endDocument() throws SAXException {
    }
}
