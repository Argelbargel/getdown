package com.threerings.getdown.util;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;


public class SignatureUtil {
    public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
    private static Signature sig = null;

    public static String calculateSignature(InputStream in, PrivateKey key) throws GeneralSecurityException, IOException {
        Signature sig = getSignature();
        sig.initSign(key);
        updateSignature(sig, in);
        return new String(Base64.encodeBase64(sig.sign()));
    }

    public static boolean verifySignature(InputStream in, String signature, Certificate cert) throws IOException, GeneralSecurityException {
        Signature sig = getSignature();

        try {
            sig.initVerify(cert);
            updateSignature(sig, in);
            return sig.verify(Base64.decodeBase64(signature));
        } catch (GeneralSecurityException gse) {
            return false;
        }
    }

    private static void updateSignature(Signature sig, InputStream in) throws IOException, SignatureException {
        try {
            int length;
            byte[] buffer = new byte[8192];
            while ((length = in.read(buffer)) != -1) {
                sig.update(buffer, 0, length);
            }
        } finally {
            in.close();
        }
    }

    /* for internal use & tests only */
    static Signature getSignature() throws NoSuchAlgorithmException {
        if (sig == null) {
            sig = Signature.getInstance(SIGNATURE_ALGORITHM);
        }

        return sig;
    }
}
