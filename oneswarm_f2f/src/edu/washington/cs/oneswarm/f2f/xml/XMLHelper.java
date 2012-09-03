package edu.washington.cs.oneswarm.f2f.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.security.Signature;
import java.util.Arrays;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.IOUtils;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.common.annotations.VisibleForTesting;

@SuppressWarnings("deprecation")
public class XMLHelper {

    // General XML attributes and tag names
    public static final String ENCODING = "UTF-8";
    public static final String ROOT = "Root";
    public static final String STATUS = "Status";
    public static final String STATUS_CODE = "StatusCode";
    public static final String STATUS_MESSAGE = "StatusMessage";

    public static final String DIGEST = "Digest";
    public static final String DIGEST_PLACEHOLDER = "OneSwarmDigest";
    // ExitNode tags (must be used inside an EXIT_NODE block
    public static final String EXIT_NODE = "ExitNode"; // For ExitNodes using OS
                                                       // Wire format
    public static final String SERVICE = "Service"; // For OS Websites
    public static final String SERVICE_ID = "ServiceId";
    public static final String PUBLIC_KEY = "PublicKey";
    public static final String NICKNAME = "Nickname";
    public static final String BANDWIDTH = "Bandwidth";
    public static final String EXIT_POLICY = "ExitPolicy";
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

    XMLSerializer serializer;
    ContentHandler handler;

    public XMLHelper(OutputStream out) throws IOException, SAXException {
        OutputFormat format = new OutputFormat("XML", ENCODING, true);
        format.setIndent(1);
        format.setIndenting(true);
        serializer = new XMLSerializer(out, format);
        handler = serializer.asContentHandler();
        handler.startDocument();
        startElement(ROOT);
    }

    public XMLHelper(Writer out) throws IOException {
        OutputFormat format = new OutputFormat("XML", ENCODING, true);
        format.setIndent(1);
        format.setIndenting(true);
        serializer = new XMLSerializer(out, format);
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
    
    public void writeDigest() throws SAXException {
    	startElement(DIGEST);
    	serializer.startCDATA();
    	char[] ph = DIGEST_PLACEHOLDER.toCharArray();
    	serializer.characters(ph, 0, ph.length);
    	serializer.endCDATA();
    	endElement(DIGEST);
    }

    public void startElement(String qName) throws SAXException {
        handler.startElement("", "", qName, null);
    }

    public void endElement(String qName) throws SAXException {
        handler.endElement("", "", qName);
    }

    public void close() throws SAXException {
        endElement(ROOT);
        handler.endDocument();
    }

    @VisibleForTesting
    public static boolean validateDigest = true;
    
    public static void parse(InputStream in, DefaultHandler handler, Signature signature) throws SAXException,
            IOException {
        byte[] inputData = IOUtils.toByteArray(in);
        if (validateDigest) {
            byte[] cDataBytes = "<![CDATA[".getBytes();
            // Validate Digest.
            int digestEnd = Utils.lastIndexOf(inputData, DIGEST.getBytes());
            int cDataStart = Utils.lastIndexOf(inputData, cDataBytes);
            if (digestEnd < cDataStart || cDataStart < 0) {
                throw new SecurityException("Invalid Directory Response");
            }
            byte[] digest = Arrays.copyOfRange(inputData, cDataStart + cDataBytes.length, digestEnd - 5);
            int preDigestSize = inputData.length + DIGEST_PLACEHOLDER.getBytes().length - digest.length;
            ByteBuffer preDigest = ByteBuffer.allocate(preDigestSize);
            preDigest.put(inputData, 0, cDataStart + cDataBytes.length);
            preDigest.put(DIGEST_PLACEHOLDER.getBytes());
            preDigest.put(inputData, digestEnd - 5, inputData.length);

            try {
                synchronized(signature) {
                    signature.update(preDigest);
                    if (!signature.verify(digest)) {
                        throw new SecurityException("Invalid Directory Signature");
                    }
                }
            } catch(Exception e) {
                throw new SecurityException("Invalid Directory Signature");
            }
        }
        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(new ByteArrayInputStream(inputData), handler);
        } catch (ParserConfigurationException e) {
            // Fatal and shouldnt happen
            throw new RuntimeException(e);
        }
    }
}
