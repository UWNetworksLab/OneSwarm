package edu.washington.cs.oneswarm.f2f.servicesharing;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

import org.xml.sax.SAXException;

import sun.security.x509.CertAndKeyGen;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import edu.washington.cs.oneswarm.f2f.xml.XMLHelper;

public abstract class PublishableService {
    public String nickname;
    public long serviceId;
    public boolean published;

    public static final int KEY_SIZE_BITS = 1024;
    private PublicKey publicKey;
    private PrivateKey privateKey;

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
                + Base64.encode(this.publicKey.getEncoded());
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
                return Base64.decode(parts[2]);
            }
        };
        privateKey = null;
    }

    public String signature() {
        try {
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initSign(this.privateKey);
            sig.update(hashBase());
            return Base64.encode(sig.sign());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void generateNewKeys() {
        try {
            CertAndKeyGen keyPair = new CertAndKeyGen("RSA", "SHA1withRSA", null);
            keyPair.generate(KEY_SIZE_BITS);

            publicKey = keyPair.getPublicKey();
            privateKey = keyPair.getPrivateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

}
