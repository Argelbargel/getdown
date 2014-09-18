package com.threerings.getdown.util;

import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SignatureTestHelper {
    private final PrivateKey key;
    private final Certificate cert;

    public SignatureTestHelper() throws GeneralSecurityException, IOException {
        String alias = "test";
        char[] keyPass = "test".toCharArray();

        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, null);
        CertAndKeyGen keypair = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
        X500Name x500Name = new X500Name("test.com", "T", "test", "test", "test", "DE");
        keypair.generate(1024);
        key = keypair.getPrivateKey();

        X509Certificate[] chain = new X509Certificate[1];
        chain[0] = keypair.getSelfCertificate(x500Name, new Date(), (long) 24 * 60 * 60);
        keyStore.setKeyEntry(alias, key, keyPass, chain);
        cert = keyStore.getCertificate(alias);
    }

    public PrivateKey getPrivateKey() {
        return key;
    }

    public Certificate getCertificate() {
        return cert;
    }
}
