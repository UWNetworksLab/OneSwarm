package edu.washington.cs.oneswarm.f2f.xml;

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import edu.washington.cs.oneswarm.f2f.servicesharing.ExitNodeInfo;

public class DirectoryInfoHandler extends DefaultHandler {
    private ExitNodeInfo tempNode;
    private String tempVal;
    private final List<ExitNodeInfo> nodes;

    public DirectoryInfoHandler(List<ExitNodeInfo> list) {
        this.nodes = list;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        if (qName.equalsIgnoreCase(XMLHelper.EXIT_NODE)) {
            tempNode = new ExitNodeInfo();
            tempNode.type = XMLHelper.EXIT_NODE;
        } else if (qName.equalsIgnoreCase(XMLHelper.SERVICE)) {
            tempNode = new ExitNodeInfo();
            tempNode.type = XMLHelper.SERVICE;
        }
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        tempVal = new String(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase(tempNode.type)) {
            nodes.add(tempNode);
        } else if (qName.equalsIgnoreCase(XMLHelper.SERVICE_ID)) {
            tempNode.serviceId = Long.parseLong(tempVal);
        } else if (qName.equalsIgnoreCase(XMLHelper.PUBLIC_KEY)) {
            tempNode.setPublicKeyString(tempVal);
        } else if (qName.equalsIgnoreCase(XMLHelper.NICKNAME)) {
            tempNode.nickname = tempVal;
        } else if (qName.equalsIgnoreCase(XMLHelper.BANDWIDTH)) {
            tempNode.setAdvertizedBandwidth(Integer.parseInt(tempVal));
        } else if (qName.equalsIgnoreCase(XMLHelper.EXIT_POLICY)) {
            tempNode.setExitPolicy(tempVal.split(","));
        } else if (qName.equalsIgnoreCase(XMLHelper.VERSION)) {
            tempNode.setVersion(tempVal);
        } else if (qName.equalsIgnoreCase(XMLHelper.SIGNATURE)) {
            // Ignored
        }
    }
}
