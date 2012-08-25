package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;

import org.apache.commons.codec.binary.Base64;
import org.xml.sax.SAXException;

import edu.washington.cs.oneswarm.f2f.xml.XMLHelper;

public abstract class PublishableService {
    public long serviceId = 0;
    public boolean published = true;

    public static final int KEY_SIZE_BITS = 1024;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public PublishableService() {
        generateNewKeys();
    }

    public abstract String type();

    public abstract byte[] hashBase();

    public abstract void fullXML(XMLHelper xmlOut) throws SAXException;

    public abstract void shortXML(XMLHelper xmlOut) throws SAXException;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof PublishableService) {
            return this.serviceId == ((PublishableService) other).serviceId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) serviceId;
    }

    /**
     * Returns public key as String.
     * 
     * @return PublicKey in the following format ALGOYTHM:FORMAT:KEY (KEY in
     *         base 64)
     */
    public String getPublicKeyString() {
        return this.publicKey.getAlgorithm() + ":" + this.publicKey.getFormat() + ":"
                + Base64.encodeBase64(this.publicKey.getEncoded());
    }

    /**
     * See <code>getPublicKeyString()</code> for format.
     * 
     * @param key
     */
    public void setPublicKeyString(String key) {
        if (key == null || key.length() < 1) {
            return;
        }

        final String[] parts = key.split(":");
        publicKey = new PublicKey() {
            private static final long serialVersionUID = -7259961818850093509L;

            @Override
            public String getAlgorithm() {
                return parts[0];
            }

            @Override
            public String getFormat() {
                return parts[1];
            }

            @Override
            public byte[] getEncoded() {
                return Base64.decodeBase64(parts[2].getBytes());
            }
        };
        privateKey = null;
    }

    public String signature() {
        try {
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initSign(this.privateKey);
            sig.update(hashBase());
            return Base64.encodeBase64(sig.sign()).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void generateNewKeys() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(KEY_SIZE_BITS, new SecureRandom());
            KeyPair keyPair = kpg.generateKeyPair();

            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

}
