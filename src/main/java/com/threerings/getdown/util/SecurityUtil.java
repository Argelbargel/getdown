package com.threerings.getdown.util;

import com.samskivert.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SecurityUtil {
    public static PrivateKey loadPrivateKey(File keystore, String storepass, String alias) throws IOException, GeneralSecurityException {
        return loadPrivateKey(keystore, storepass, storepass, alias);
    }

    public static PrivateKey loadPrivateKey(File keystore, String storepass, String keypass, String alias) throws IOException, GeneralSecurityException {
        if (keystore == null) {
            throw new IllegalArgumentException("keystore must not be null");
        }

        KeyStore store = KeyStore.getInstance("JKS");
        FileInputStream fis = new FileInputStream(keystore);
        try {
            store.load(fis, (!StringUtil.isBlank(storepass)) ? storepass.toCharArray() : new char[0]);
            return (PrivateKey) store.getKey(alias, (!StringUtil.isBlank(keypass)) ? keypass.toCharArray() : new char[0]);
        } finally {
            fis.close();
        }
    }


    public static List<Certificate> loadCertificates(Class<?> clazz) {
        return loadCertificates(clazz, null);
    }

    public static List<Certificate> loadCertificates(Class<?> clazz, String path) {
        if (!StringUtil.isBlank(path)) {
            Certificate cert = loadCertificate(clazz.getResource(path));
            if (cert == null) {
                throw new RuntimeException("certificate is missing!");
            }

            return Arrays.asList(cert);
        }

        List<Certificate> certificates = new ArrayList<Certificate>();
        Object[] signers = clazz.getSigners();
        if (signers != null) {
            for (Object signer : signers) {
                if (signer instanceof Certificate) {
                    Certificate c = ((Certificate) signer);
                    certificates.add(c);
                }
            }
        }

        return certificates;
    }

    private static Certificate loadCertificate(URL url) {
        if (url == null) {
            return null;
        }

        try {
            InputStream is = url.openStream();
            try {
                return CertificateFactory.getInstance("X.509").generateCertificate(is);
            } finally {
                is.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("error reading certificate from " + url, e);
        }
    }
}
