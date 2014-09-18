package com.threerings.getdown.util;

import com.samskivert.text.MessageUtil;
import com.samskivert.util.StringUtil;
import com.threerings.getdown.DigestsTestHelper;
import com.threerings.getdown.FileTestHelper;
import com.threerings.getdown.data.Digests;
import com.threerings.getdown.data.Resource;
import com.threerings.getdown.data.ResourceGroup;
import com.threerings.getdown.data.ResourceType;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.threerings.getdown.DigestsTestHelper.createResource;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static com.threerings.getdown.util.VersionUtil.NO_VERSION;

public class DigestsUtilTest {
    private SignatureTestHelper helper;

    byte[] dataA, dataC, dataD, dataE, dataJ, dataZ;
    Resource r1,r2, r3;


    @Before
    public void generateData() throws IOException {
        dataA = DigestsTestHelper.generateData(DigestsUtil.DIGEST_BUFFER_SIZE);
        dataC = DigestsTestHelper.generateData(3 * DigestsUtil.DIGEST_BUFFER_SIZE);
        dataD = DigestsTestHelper.generateData(4 * DigestsUtil.DIGEST_BUFFER_SIZE);
        dataE = DigestsTestHelper.generateData(5 * DigestsUtil.DIGEST_BUFFER_SIZE);
        dataJ = DigestsTestHelper.generateData(10 * DigestsUtil.DIGEST_BUFFER_SIZE);
        dataZ = DigestsTestHelper.generateData(26 * DigestsUtil.DIGEST_BUFFER_SIZE);

        r1 = createResource(1024);
        r2 = createResource(1024);
        r3 = createResource(1024);
    }

    @Before
    public void initSignatureTestHelper() throws GeneralSecurityException, IOException {
        helper = new SignatureTestHelper();
    }


    @Test
    public void testRead() throws IOException, GeneralSecurityException {
        Digests digests = DigestsTestHelper.createDigests(r1, r2, r3);

        File tmpDir = DigestsTestHelper.createTempFile(new byte[0]).getParentFile();
        DigestsUtil.writeDigests(tmpDir, digests, null);

        assertEquals(digests, DigestsUtil.readDigests(tmpDir, NO_VERSION));
    }

    @Test
    public void testReadFailsForMissingDigest() throws IOException, GeneralSecurityException {
        Digests digests = DigestsTestHelper.createDigests(r1, r2, r3);

        File file = new File(DigestsTestHelper.createTempFile(new byte[0]).getParentFile(), DigestsUtil.DIGESTS_FILE_NAME);
        FileWriter out = new FileWriter(file);
        out.write(digests.toString());
        out.close();

        try {
            DigestsUtil.readDigests(file.getParentFile(), NO_VERSION);
        } catch (IOException e) {
            assertEquals(MessageUtil.tcompose("m.invalid_digest_file"), e.getMessage());
        }
    }


    @Test
    public void testReadFailsForWrongDigest() throws IOException, GeneralSecurityException {
        Digests digests = DigestsTestHelper.createDigests(r1, r2, r3);

        File file = new File(DigestsTestHelper.createTempFile(new byte[0]).getParentFile(), DigestsUtil.DIGESTS_FILE_NAME);
        FileWriter out = new FileWriter(file);
        out.write(digests.toString());
        out.write(DigestsUtil.DIGESTS_FILE_NAME + " = " + DigestsTestHelper.createDigests(r1, r2).getMetaDigest());
        out.close();

        try {
            DigestsUtil.readDigests(file.getParentFile(), NO_VERSION);
        } catch (IOException e) {
            assertEquals(MessageUtil.tcompose("m.invalid_digest_file"), e.getMessage());
        }
    }

    @Test
    public void testDownloadSuceedsForValidSignature() throws IOException, GeneralSecurityException {
        Digests digests = DigestsTestHelper.createDigests(r1, r2, r3);

        File tmpDir = DigestsTestHelper.createTempFile(new byte[0]).getParentFile();
        DigestsUtil.writeDigests(tmpDir, digests, helper.getPrivateKey());

        assertEquals(digests, DigestsUtil.downloadDigests(tmpDir, tmpDir.toURI().toURL(), NO_VERSION, Arrays.asList(helper.getCertificate())));
    }

    @Test
    public void testDownloadFailsForInvalidSignature() throws IOException, GeneralSecurityException {
        Digests digests = DigestsTestHelper.createDigests(r1, r2, r3);

        File tmpDir = DigestsTestHelper.createTempFile(new byte[0]).getParentFile();
        DigestsUtil.writeDigests(tmpDir, digests, helper.getPrivateKey());
        FileOutputStream sigOut = new FileOutputStream(new File(tmpDir, DigestsUtil.DIGESTS_FILE_NAME + DigestsUtil.DIGESTS_SIGNATURE_SUFFIX));
        sigOut.write(DigestsTestHelper.generateData(1024));
        sigOut.close();

        try {
            DigestsUtil.downloadDigests(tmpDir, tmpDir.toURI().toURL(), NO_VERSION, Arrays.asList(helper.getCertificate()));
        } catch (IOException e) {
            assertEquals("m.corrupt_digest_signature_error", e.getMessage());
        }
    }

    @Test
    public void testValidateResourceDigestSucceeds() throws IOException {
        File file = DigestsTestHelper.createTempFile(dataE);

        Resource resource = FileTestHelper.createLocalResource(ResourceType.RESOURCE_FILE, file);
        ResourceGroup group = new ResourceGroup();
        group.addResources(resource);
        Digests digests = Digests.create(group, NO_VERSION);

        assertTrue(DigestsUtil.validateResourceDigest(resource, digests, null));
    }

    @Test
    public void testValidateResourceDigestFailsDifferentContent() throws IOException {
        File file = DigestsTestHelper.createTempFile(dataE);

        Resource resource = FileTestHelper.createLocalResource(ResourceType.RESOURCE_FILE, file);
        ResourceGroup group = new ResourceGroup();
        group.addResources(resource);
        Digests digests = Digests.create(group, NO_VERSION);

        FileOutputStream out = new FileOutputStream(file);
        out.write(dataA);
        out.close();

        assertFalse(DigestsUtil.validateResourceDigest(resource, digests, null));
    }

    @Test
    public void testValidateResourceDigestFailsUnknownResource() throws IOException {
        File file = DigestsTestHelper.createTempFile(dataE);

        Resource resource = FileTestHelper.createLocalResource(ResourceType.RESOURCE_FILE, file);
        ResourceGroup group = new ResourceGroup();
        group.addResources(resource);
        Digests digests = Digests.create(group, NO_VERSION);

        Resource other = FileTestHelper.createLocalResource(ResourceType.RESOURCE_FILE, FileTestHelper.createTempFile());

        assertFalse(DigestsUtil.validateResourceDigest(other, digests, null));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void testValidateResourceDigestFailsMissingResource() throws IOException {
        File file = DigestsTestHelper.createTempFile(dataE);

        Resource resource = FileTestHelper.createLocalResource(ResourceType.RESOURCE_FILE, file);
        ResourceGroup group = new ResourceGroup();
        group.addResources(resource);
        Digests digests = Digests.create(group, NO_VERSION);

        file.delete();

        assertFalse(DigestsUtil.validateResourceDigest(resource, digests, null));
    }


    @Test
    public void testComputeResourceDigestSimpleFileSucceeds() throws IOException {
        File tmpFile = DigestsTestHelper.createTempFile(dataE);

        MessageDigest md = DigestsUtil.getMessageDigest();
        byte[] digest = md.digest(dataE);

        String expected = StringUtil.hexlate(digest);
        Resource resource = FileTestHelper.createLocalResource(ResourceType.RESOURCE_FILE, tmpFile);
        DigestsTestHelper.ProgressObserverStub obs = new DigestsTestHelper.ProgressObserverStub();

        assertEquals(expected, DigestsUtil.computeResourceDigest(resource, obs));
        assertTrue(obs.getCalls().size() > 2);
    }

    @Test
    public void testComputeResourceDigestSimpleFileFails() throws IOException {
        File tmpFile = DigestsTestHelper.createTempFile(dataE);

        MessageDigest md = DigestsUtil.getMessageDigest();
        byte[] digest = md.digest(dataD);

        String expected = StringUtil.hexlate(digest);
        Resource resource = FileTestHelper.createLocalResource(ResourceType.RESOURCE_FILE, tmpFile);
        DigestsTestHelper.ProgressObserverStub obs = new DigestsTestHelper.ProgressObserverStub();

        assertThat(expected, not(equalTo(DigestsUtil.computeResourceDigest(resource, obs))));
        assertTrue(obs.getCalls().size() > 2);
    }


    @Test
    public void testComputeResourceDigestArchive() throws IOException {
        File zipFile = FileTestHelper.createTempFile(".zip");
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(zipFile));

        zout.putNextEntry(new ZipEntry("z.txt"));
        zout.write(dataZ);
        zout.closeEntry();
        zout.putNextEntry(new ZipEntry("a.txt"));
        zout.write(dataA);
        zout.closeEntry();
        zout.putNextEntry(new ZipEntry("META-INF/c.txt"));
        zout.write(dataC);
        zout.closeEntry();
        zout.putNextEntry(new ZipEntry("/META-INF/d.txt"));
        zout.write(dataD);
        zout.close();

        MessageDigest md = DigestsUtil.getMessageDigest();
        md.update(dataA);
        md.update(dataZ);
        byte[] digest = md.digest();

        String expected = StringUtil.hexlate(digest);
        Resource resource = FileTestHelper.createLocalResource(ResourceType.RESOURCE_ARCHIVE, zipFile);
        DigestsTestHelper.ProgressObserverStub obs = new DigestsTestHelper.ProgressObserverStub();

        assertEquals(expected, DigestsUtil.computeResourceDigest(resource, obs));
        assertEquals(Arrays.asList(25, 50, 75, 100), obs.getCalls());
    }


    @Test
    public void testComputeDigestsDigestSucceeds() throws IOException {
        Digests digests = DigestsTestHelper.createDigests(dataA, dataC, dataD);

        MessageDigest md = DigestsUtil.getMessageDigest();
        md.update((NO_VERSION + digests.getContents()).getBytes());

        DigestsTestHelper.ProgressObserverStub obs = new DigestsTestHelper.ProgressObserverStub();
        String expected = StringUtil.hexlate(md.digest());

        assertEquals(expected, DigestsUtil.computeDigestsDigest(digests, VersionUtil.NO_VERSION, obs));
        assertEquals(Arrays.asList(100), obs.getCalls());
    }

    @Test
    public void testComputeDigestsDigestFails() throws IOException {
        Digests digests = DigestsTestHelper.createDigests(dataA, dataC, dataD);
        // creates different filenames
        Digests other = DigestsTestHelper.createDigests(dataA, dataC, dataD);

        MessageDigest md = DigestsUtil.getMessageDigest();
        md.update(other.toString().getBytes());

        DigestsTestHelper.ProgressObserverStub obs = new DigestsTestHelper.ProgressObserverStub();
        String expected = StringUtil.hexlate(md.digest());

        assertThat(expected, not(equalTo(DigestsUtil.computeDigestsDigest(digests, VersionUtil.NO_VERSION, obs))));
        assertEquals(Arrays.asList(100), obs.getCalls());
    }

    @Test
    public void testUpdateMessageDigest() throws Exception {

        MessageDigest md = DigestsUtil.getMessageDigest();
        byte[] expected = md.digest(dataJ);
        md.reset();


        DigestsTestHelper.ProgressObserverStub obs = new DigestsTestHelper.ProgressObserverStub();
        ByteArrayInputStream in = new ByteArrayInputStream(dataJ);
        DigestsUtil.updateMessageDigest(md, in, dataJ.length, obs);

        assertTrue(Arrays.equals(expected, md.digest()));
        assertTrue(obs.getCalls().size() > 2);
    }

}