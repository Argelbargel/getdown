package com.threerings.getdown.util;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.RSAPrivateKeySpec;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

public class SignatureUtilTest {
    private SignatureTestHelper helper;

    @Before
    public void initSignatureTestHelper() throws GeneralSecurityException, IOException {
        helper = new SignatureTestHelper();
    }


    @Test
    public void calculateSignature() throws GeneralSecurityException, IOException {
        byte[] data = generateData(4096);

        Signature sig = SignatureUtil.getSignature();
        sig.initSign(helper.getPrivateKey());
        sig.update(data);

        String expected = new String(Base64.encodeBase64(sig.sign()));
        assertEquals(expected, SignatureUtil.calculateSignature(new ByteArrayInputStream(data), helper.getPrivateKey()));
    }

    @Test
    public void calculateSignatureGeneratesDifferentSignatures() throws GeneralSecurityException, IOException {
        byte[] data1 = generateData(4096);
        byte[] data2 = generateData(4096);

        Signature sig = SignatureUtil.getSignature();
        sig.initSign(helper.getPrivateKey());
        sig.update(data1);

        String expected = new String(Base64.encodeBase64(sig.sign()));
        assertThat(expected, not(equalTo(SignatureUtil.calculateSignature(new ByteArrayInputStream(data2), helper.getPrivateKey()))));
    }

    @Test
    public void verifySignatureSucceeds() throws GeneralSecurityException, IOException {
        byte[] data = generateData(4096);

        String signature = SignatureUtil.calculateSignature(new ByteArrayInputStream(data), helper.getPrivateKey());
        assertTrue(SignatureUtil.verifySignature(new ByteArrayInputStream(data), signature, helper.getCertificate()));
    }


    @Test
    public void verifySignatureFailsForDifferentContent() throws GeneralSecurityException, IOException {
        byte[] data1 = generateData(4096);
        byte[] data2 = generateData(4096);

        String signature = SignatureUtil.calculateSignature(new ByteArrayInputStream(data1), helper.getPrivateKey());
        assertFalse(SignatureUtil.verifySignature(new ByteArrayInputStream(data2), signature, helper.getCertificate()));
    }



    private byte[] generateData(long length) {
        String charset = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZüöä?!%&+~\n\t\r";
        Random rnd = new Random();

        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < length; i++ ) {
            sb.append(charset.charAt(rnd.nextInt(charset.length())));
        }

        return sb.toString().getBytes();
    }
}