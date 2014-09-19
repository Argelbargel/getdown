package com.threerings.getdown.util;

import com.threerings.getdown.FileTestHelper;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static com.threerings.getdown.util.VersionUtil.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VersionUtilTest {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Before
    public void deleteConfigurationAndVersionFiles() throws IOException {
        File tmpDir = FileTestHelper.getTempDirectory();
        new File(tmpDir, ConfigUtil.CONFIG_FILE).delete();
        new File(tmpDir, VERSION_FILE_NAME).delete();
        new File(tmpDir, LATEST_VERSION + File.separator + VERSION_FILE_NAME).delete();
        new File(tmpDir, LATEST_VERSION).delete();
    }

    @Test
    public void testIsValidVersion() {
        assertTrue(VersionUtil.isValidVersion("1"));
        assertTrue(VersionUtil.isValidVersion("1.6"));
        assertTrue(VersionUtil.isValidVersion("alpha"));
        assertTrue(VersionUtil.isValidVersion(".alpha"));
        assertTrue(VersionUtil.isValidVersion("alpha."));
        assertTrue(VersionUtil.isValidVersion(".alpha."));
        assertTrue(VersionUtil.isValidVersion(LATEST_VERSION));

        assertFalse(VersionUtil.isValidVersion(null));
        assertFalse(VersionUtil.isValidVersion("    "));
        assertFalse(VersionUtil.isValidVersion(".~\n \t# "));
        assertFalse(VersionUtil.isValidVersion(NO_VERSION));
    }

    @Test
    public void testCompareVersions() throws Exception {
        assertEquals(0, VersionUtil.compareVersions("1.6", "1-6"));
        assertEquals(0, VersionUtil.compareVersions("1.6-0", "1.6#0"));
        assertEquals(0, VersionUtil.compareVersions("1.6-0", "1.6_0"));
        assertEquals(0, VersionUtil.compareVersions("1/6/0", "1.6_0"));
// disabled until NO_VERSION is empty string again
//        assertEquals(0, VersionUtil.compareVersions("-1", "1"));
        assertEquals(-1, VersionUtil.compareVersions("version#1", "version#2"));

    }

    @Test
    public void testCreateVersionedUrl() throws Exception {
        URL base = new URL("http://example.com/" + VERSION_URL_PLACEHOLDER + "/");
        URL expected = new URL("http://example.com/version/");
        assertEquals(expected, VersionUtil.createVersionedUrl(base, "version"));
    }

    @Test
    public void testCreateVersionedUrlWithPlaceHolderForNoVersion() throws Exception {
        URL base = new URL("http://example.com/" + VERSION_URL_PLACEHOLDER + "/path");
        URL expected = new URL("http://example.com//path");
        assertEquals(expected, VersionUtil.createVersionedUrl(base, null));
        assertEquals(expected, VersionUtil.createVersionedUrl(base, " -#."));
    }

    @Test
    public void testCreateVersionedUrlWithMultiplePlaceHolders() throws Exception {
        URL base = new URL("http://example.com/" + VERSION_URL_PLACEHOLDER + "/path/" + VERSION_URL_PLACEHOLDER + ".data");
        URL expected = new URL("http://example.com/1/6/0/path/1/6/0.data");
        assertEquals(expected, VersionUtil.createVersionedUrl(base, "1/6/0"));
    }

    @Test
    public void testReadLatestVersionFromLatestUrl() throws IOException {
        String version = "1.6.2";
        File tmpDir = FileTestHelper.getTempDirectory();
        createLatestVersionFile(tmpDir, version);
        URL appbase = new File(tmpDir, VERSION_URL_PLACEHOLDER).toURI().toURL();

        assertEquals(version, VersionUtil.getLatestVersion(tmpDir, appbase));
        assertEquals(version, VersionUtil.getLocalVersion(tmpDir));
    }

    @Test
    public void testReadVersionFromLocalVersionFileIfLatestDoesNotExist() throws IOException {
        String version = "1.6.1";
        File tmpDir = FileTestHelper.getTempDirectory();
        VersionUtil.setLocalVersion(tmpDir, version);
        URL appbase = new File(tmpDir, VERSION_URL_PLACEHOLDER).toURI().toURL();

        assertEquals(version, VersionUtil.getLatestVersion(tmpDir, appbase));
        assertEquals(version, VersionUtil.getLocalVersion(tmpDir));
    }

    @Test
    public void testReadVersionFromLocalVersionFileLatestSpecifiesLowerVersion() throws IOException {
        String localVersion = "1.6.1";
        String remoteVersion = "1.6.0";
        File tmpDir = FileTestHelper.getTempDirectory();
        createLatestVersionFile(tmpDir, remoteVersion);
        VersionUtil.setLocalVersion(tmpDir, localVersion);
        URL appbase = new File(tmpDir, VERSION_URL_PLACEHOLDER).toURI().toURL();

        assertEquals(localVersion, VersionUtil.getLatestVersion(tmpDir, appbase));
        assertEquals(localVersion, VersionUtil.getLocalVersion(tmpDir));
    }



    @Test
    public void testReadVersion() throws IOException {
        String version = "1.6";
        File tmpDir = FileTestHelper.getTempDirectory();
        VersionUtil.setLocalVersion(tmpDir, version);

        assertEquals(version, VersionUtil.getLocalVersion(tmpDir));
    }

    @Test
    public void testWriteVersion() throws IOException {
        String version = "1.7";
        File tmpDir = FileTestHelper.getTempDirectory();
        VersionUtil.setLocalVersion(tmpDir, version);

        assertTrue(new File(tmpDir, VERSION_FILE_NAME).exists());
        assertEquals(version, VersionUtil.getLocalVersion(tmpDir));
    }

    @Test
    public void testWriteVersionForNoVersion() throws IOException {
        File tmpDir = FileTestHelper.getTempDirectory();
        VersionUtil.setLocalVersion(tmpDir, NO_VERSION);

        assertTrue(new File(tmpDir, VERSION_FILE_NAME).exists());
        assertEquals(NO_VERSION, VersionUtil.getLocalVersion(tmpDir));
    }

    @Test
    public void testWriteVersionForInvalidVersion() throws IOException {
        File tmpDir = FileTestHelper.getTempDirectory();
        VersionUtil.setLocalVersion(tmpDir, ".- \n");

        assertTrue(new File(tmpDir, VERSION_FILE_NAME).exists());
        assertEquals(NO_VERSION, VersionUtil.getLocalVersion(tmpDir));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void createLatestVersionFile(File dir, String version) throws IOException {
        File latestDir = new File(dir, LATEST_VERSION);
        latestDir.mkdir();
        latestDir.deleteOnExit();
        VersionUtil.setLocalVersion(latestDir, version);
    }
}