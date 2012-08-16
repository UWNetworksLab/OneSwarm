package edu.washington.cs.oneswarm.f2f.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@SuppressWarnings("deprecation")
public class XMLHelper {

    // General XML attributes and tag names
    public static final String ENCODING = "UTF-8";
    public static final String EXIT_NODE_LIST = "ExitNodeList";
    public static final String STATUS = "Status";
    public static final String STATUS_CODE = "StatusCode";
    public static final String STATUS_MESSAGE = "StatusMessage";

    // ExitNode tags (must be used inside an EXIT_NODE block
    public static final String EXIT_NODE = "ExitNode";
    public static final String SERVICE_ID = "ServiceId";
    public static final String PUBLIC_KEY = "PublicKey";
    public static final String NICKNAME = "Nickname";
    public static final String BANDWIDTH = "Bandwidth";
    public static final String EXIT_POLICY = "ExitPolicy";
    public static final String ONLINE_SINCE = "OnlineSince";
    public static final String VERSION = "Version";
    public static final String SIGNATURE = "Signature";

    // Error codes (Based loosely on HTTP errors)
    // 3XX are actions to be taken by the client
    // 4XX are client errors that must be fixed by the user
    // 5XX are server side errors
    public static final int STATUS_SUCCESS = 200;
    public static final int ERROR_DUPLICATE_SERVICE_ID = 301;
    public static final int ERROR_UNREGISTERED_SERVICE_ID = 302;
    public static final int ERROR_BAD_REQUEST = 400;
    public static final int ERROR_INVALID_SIGNATURE = 403;

    public static final int ERROR_GENERAL_SERVER = 500;

    ContentHandler handler;

    public XMLHelper(OutputStream out) throws IOException, SAXException {
        OutputFormat format = new OutputFormat("XML", ENCODING, true);
        format.setIndent(1);
        format.setIndenting(true);
        XMLSerializer serializer = new XMLSerializer(out, format);
        handler = serializer.asContentHandler();
        handler.startDocument();
        startElement(EXIT_NODE_LIST);
    }

    public XMLHelper(Writer out) throws IOException {
        OutputFormat format = new OutputFormat("XML", ENCODING, true);
        format.setIndent(1);
        format.setIndenting(true);
        XMLSerializer serializer = new XMLSerializer(out, format);
        handler = serializer.asContentHandler();
    }

    public void writeStatus(int errorCode, String msg) throws SAXException {
        startElement(STATUS);
        writeTag(STATUS_CODE, "" + errorCode);
        writeTag(STATUS_MESSAGE, msg);
        endElement(STATUS);
    }

    public void writeTag(String tag, String content) throws SAXException {
        startElement(tag);
        handler.characters(content.toCharArray(), 0, content.length());
        endElement(tag);
    }

    public void startElement(String qName) throws SAXException {
        handler.startElement("", "", qName, null);
    }

    public void endElement(String qName) throws SAXException {
        handler.endElement("", "", qName);
    }

    public void close() throws SAXException {
        endElement(EXIT_NODE_LIST);
        handler.endDocument();
    }

    public static void parse(InputStream in, DefaultHandler handler) throws SAXException,
            IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(in, handler);
        } catch (ParserConfigurationException e) {
            // Fatal and shouldnt happen
            throw new RuntimeException(e);
        }
    }
}
