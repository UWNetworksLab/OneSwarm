package edu.washington.cs.oneswarm.f2f.xml;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.washington.cs.oneswarm.f2f.servicesharing.ExitNodeInfo;
import edu.washington.cs.oneswarm.f2f.servicesharing.PublishableService;
import edu.washington.cs.oneswarm.f2f.servicesharing.SharedService;

public class DirectoryInfoHandler extends DefaultHandler {
    private PublishableService tempNode;
    private String tempVal;
    private final List<PublishableService> nodes;

    public DirectoryInfoHandler(List<PublishableService> list) {
        this.nodes = list;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        if (qName.equalsIgnoreCase(XMLHelper.EXIT_NODE)) {
            tempNode = new ExitNodeInfo();
        } else if (qName.equalsIgnoreCase(XMLHelper.SERVICE)) {
            tempNode = new SharedService(0l);
        }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase(XMLHelper.EXIT_NODE)
                || qName.equalsIgnoreCase(XMLHelper.SERVICE)) {
            nodes.add(tempNode);
        } else if (qName.equalsIgnoreCase(XMLHelper.SERVICE_ID)) {
            tempNode.serviceId = Long.parseLong(tempVal);
        } else if (qName.equalsIgnoreCase(XMLHelper.PUBLIC_KEY)) {
            tempNode.setPublicKeyString(tempVal);
        } else if (qName.equalsIgnoreCase(XMLHelper.NICKNAME)) {
            if (tempNode instanceof ExitNodeInfo) {
                ((ExitNodeInfo) tempNode).setNickname(tempVal);
            }
        } else if (qName.equalsIgnoreCase(XMLHelper.BANDWIDTH)) {
            if (tempNode instanceof ExitNodeInfo) {
                ((ExitNodeInfo) tempNode).setAdvertizedBandwidth(Integer.parseInt(tempVal));
            }
        } else if (qName.equalsIgnoreCase(XMLHelper.EXIT_POLICY)) {
            if (tempNode instanceof ExitNodeInfo) {
                ((ExitNodeInfo) tempNode).setExitPolicy(tempVal.split(","));
            }
        } else if (qName.equalsIgnoreCase(XMLHelper.VERSION)) {
            if (tempNode instanceof ExitNodeInfo) {
                ((ExitNodeInfo) tempNode).setVersion(tempVal);
            }
        } else if (qName.equalsIgnoreCase(XMLHelper.SIGNATURE)) {
            // Ignored
        }
    }
}
