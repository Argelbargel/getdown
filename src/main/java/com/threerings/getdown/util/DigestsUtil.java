package com.threerings.getdown.util;

import com.samskivert.io.StreamUtil;
import com.samskivert.util.StringUtil;
import com.threerings.getdown.data.Digests;
import com.threerings.getdown.data.Resource;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.threerings.getdown.Log.log;
import static com.threerings.getdown.util.VersionUtil.createVersionedUrl;

/**
 * {@link DigestsUtil} contains helper methods concerning the calculation of message-digests stored in
 * {@link DigestsUtil#DIGESTS_FILE_NAME} used to validate the resources referenced in our
 * {@link com.threerings.getdown.util.ConfigUtil#CONFIG_FILE}.
 */
public final class DigestsUtil {
    /* for internal use & tests only */
    static final String DIGESTS_FILE_NAME = "digests.txt";
    static final String DIGESTS_SIGNATURE_SUFFIX = ".sig";
    static final String MESSAGEDIGEST_ALGORITHM = "MD5";
    static final int DIGEST_BUFFER_SIZE = 5 * 1025;

    private static final Pattern ZIPFILE_METADATA_ENTRY_PATTERN = Pattern.compile("^/?META-INF/.*");
    private static final Comparator<? super ZipEntry> ZIPFILE_ENTRY_COMPARATOR =
            new Comparator<ZipEntry>() {
                public int compare (ZipEntry e1, ZipEntry e2) {
                    return e1.getName().compareTo(e2.getName());
                }
            };

    private static MessageDigest md = null;


    public static Digests readDigests(File appdir, String version) throws IOException {
        File digestFile = new File(appdir, DIGESTS_FILE_NAME);
        if (!digestFile.exists()) {
            throw new IOException("missing digests-file " + digestFile);
        }

        Properties contents = new Properties();
        contents.load(new FileInputStream(digestFile));
        return Digests.create(contents, version);
    }

    /**
     * @deprecated appbase should already be an URL!
     */
    public static Digests downloadDigests(File appdir, String appbase, String version, Collection<Certificate> certificates) throws IOException {
        return downloadDigests(appdir, new URL(appbase), version, certificates);
    }

    public static Digests downloadDigests(File appdir, URL appbase, String version, Collection<Certificate> certificates) throws IOException {
        URL digestsURL = new URL(createVersionedUrl(appbase, version), DIGESTS_FILE_NAME);
        File tmpDigests = ConnectionUtil.download(digestsURL, FileUtil.createTempFile(DIGESTS_FILE_NAME, ".new", true));

        if (!certificates.isEmpty()) {
            if (!validateDigestsSignature(tmpDigests, new URL(digestsURL.toString() + DIGESTS_SIGNATURE_SUFFIX), certificates)) {
                throw new IOException("m.corrupt_digest_signature_error");
            }
        }

        FileUtil.renameTo(tmpDigests, new File(appdir, DIGESTS_FILE_NAME));
        return readDigests(appdir, version);
    }


    public static void writeDigests(File appdir, Digests digests, PrivateKey key) throws IOException, GeneralSecurityException {
        File digestFile = new File(appdir, DIGESTS_FILE_NAME);

        writeFile(digestFile, digests.toString());

        if (key != null) {
            File signatureFile = new File(appdir, DIGESTS_FILE_NAME + DIGESTS_SIGNATURE_SUFFIX);
            writeFile(signatureFile, SignatureUtil.calculateSignature(new FileInputStream(digestFile), key));
        }
    }

    private static  void writeFile(File file, String data) throws IOException {
        FileWriter writer = new FileWriter(file);
        try {
            writer.write(data);
        } finally {
            writer.close();
        }
    }


    private static boolean validateDigestsSignature(File digestsFile, URL signatureURL, Collection<Certificate> certificates) throws IOException {
        URLConnection conn = ConnectionUtil.open(signatureURL);
        if (conn == null) {
            throw new IOException("could not connect to signature-url " + signatureURL);
        }

        InputStream in = conn.getInputStream();
        try {
            return validateDigestsSignature(digestsFile, StreamUtil.toString(in), certificates);
        } finally {
            in.close();
        }
    }

    private static boolean validateDigestsSignature(File digestsFile, String signature, Collection<Certificate> certificates) {
        try {
            for (Certificate cert : certificates) {
                if (!SignatureUtil.verifySignature(new FileInputStream(digestsFile), signature, cert)) {
                    log.info("Signature does not match", "cert", cert.getPublicKey());
                } else {
                    log.info("Signature matches", "cert", cert.getPublicKey());
                    return true;
                }
            }
        } catch (Exception e) {
            log.warning("exception while validating digests signature: " + e);
        }

        return false;
    }

    public static boolean validateResourceDigest(Resource resource, Digests digests) {
        return validateResourceDigest(resource, digests, null);
    }

    public static boolean validateResourceDigest(Resource resource, Digests digests, ProgressObserver obs) {
        try {
            String digest = computeResourceDigest(resource, obs);
            String expected = digests.getResourceDigest(resource);
            if (digest.equals(expected)) {
                return true;
            }
        } catch (FileNotFoundException e) {
            log.info("Resource " + resource + " is missing!");
        } catch (Throwable t) {
            log.info("Exception during digest check", "rsrc", resource, "error", t);
        }

        return false;
    }


    public static String computeResourceDigest(Resource resource) throws IOException {
        return computeResourceDigest(resource, null);
    }

    public static String computeResourceDigest(Resource resource, ProgressObserver obs) throws IOException {
        MessageDigest md = getMessageDigest();

        File local = resource.getLocalFile();
        if (resource.isArchive()) {
            updateZipFileDigest(md, new ZipFile(local), obs);
        } else {
            updateMessageDigest(md, new FileInputStream(local), local.length(), obs);
        }

        return StringUtil.hexlate(md.digest());
    }

    public static String computeDigestsDigest(Digests digests, String version, ProgressObserver observer) throws IOException {
        MessageDigest md = getMessageDigest();
        byte[] contents = (version + digests.getContents()).getBytes();
        updateMessageDigest(md, new ByteArrayInputStream(contents), contents.length, observer);
        return StringUtil.hexlate(md.digest());
    }


    private static void updateZipFileDigest(MessageDigest md, ZipFile archive, ProgressObserver obs) throws IOException {
        List<? extends ZipEntry> entries = Collections.list(archive.entries());
        Collections.sort(entries, ZIPFILE_ENTRY_COMPARATOR);

        try {
            for (int position = 0; position < entries.size(); ++position) {
                ZipEntry entry = entries.get(position);
                // skip (jar) metadata; we just want the goods
                if (!(entry.isDirectory() || ZIPFILE_METADATA_ENTRY_PATTERN.matcher(entry.getName()).matches())) {
                    updateMessageDigest(md, archive.getInputStream(entry), 0L, null);
                }

                updateObserver(obs, position + 1, entries.size());
            }
        } finally {
            archive.close();
        }
    }

    /* for internal use & tests only */
    static void updateMessageDigest(MessageDigest md, InputStream in, long length, ProgressObserver obs) throws IOException {
        try {
            long position = 0L;
            byte[] buffer = new byte[DIGEST_BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
                position += read;
                updateObserver(obs, position, length);
            }
        } finally {
            in.close();
        }
    }

    private static void updateObserver(ProgressObserver obs, long position, long total) {
        if (obs != null && total > 0) {
            obs.progress((int) (100 * position / total));
        }
    }

     /* for internal use & tests only */
    static MessageDigest getMessageDigest () {
        if (md == null) {
            try {
                md = MessageDigest.getInstance(MESSAGEDIGEST_ALGORITHM);
            } catch (NoSuchAlgorithmException nsae) {
                throw new RuntimeException("JVM does not support MD5. Gurp!");
            }
        }

        md.reset();
        return md;
    }

    private DigestsUtil() { /* no instances allowed */ }
}
