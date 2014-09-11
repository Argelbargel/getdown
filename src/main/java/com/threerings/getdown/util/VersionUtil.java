//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2014 Three Rings Design, Inc.
// https://raw.github.com/threerings/getdown/master/LICENSE

package com.threerings.getdown.util;

import com.samskivert.util.StringUtil;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Comparator;

import static com.threerings.getdown.Log.log;

/**
 * Version related utilities.
 */
public final class VersionUtil {
    public static final String NO_VERSION = "-1";
    public static final String LATEST_VERSION = "latest";


    /* for internal use & tests only */
    static final String VERSION_FILE_NAME = "version.txt";
    static final String VERSION_URL_PLACEHOLDER = "__VERSION__";

    private static final String VERSION_TOKEN_SEPARATOR = "([^\\p{L}\\d]|#)+";
    private static final Comparator<String> VERSION_COMPARATOR = new VersionComparator(VERSION_TOKEN_SEPARATOR);


    /**
     * determines wether the given String describes a valid version
     *
     * @param version the String to check
     * @return {code true} if the String describes a version, {code false} otherwise
     */
    public static boolean isValidVersion(String version) {
        return  (version != null && !NO_VERSION.equals(version) && !StringUtil.isBlank(sanitize(version, VERSION_TOKEN_SEPARATOR)));
    }


    /**
     * compares two version Strings
     *
     * comparison-rules used:
     * <li>version string are split by non-word/non-digit chars and compared</li>
     * <li>{@link #LATEST_VERSION} is always greater than any given other version</li>
     * <li>{@link #NO_VERSION} is always less than any other version</li>
     *
     * @param v1 the first version-string to compare
     * @param v2 the second version-string to compare
     * @return {code 1} if {code v1} describes a greater version than {code v2}, {code -1} if {code v1} is smaller than
     *         {code v2} and {code 0} if the versions are equal
     * @see com.threerings.getdown.util.VersionComparator
     */
    public static int compareVersions(String v1, String v2) {
        return VERSION_COMPARATOR.compare(v1, v2);
    }


    /**
     * creates versioned url for the given appbase
     *
     * @param base the application-base
     * @param version the version
     * @return a URL to the versioned appbase or an unversioned appbase if the given version is invalid
     */
    public static URL createVersionedUrl(URL base, String version)  {
        return createVersionedUrl(base.toString(), version);
    }

    /**
     * creates versioned url for the given appbase
     *
     * @param base the application-base
     * @param version the version
     * @return a URL to the versioned appbase or an unversioned appbase if the given version is invalid
     * @deprecated {@link com.threerings.getdown.data.Application} or {@link com.threerings.getdown.data.Configuration}
     *             should convert the appbase-String into an URL (and report invalid appbases), thus all methods
     *             accepting the appbase as a String will be removed from the public API
     */
    public static URL createVersionedUrl(String base, String version)  {
        try {
            return new URL(base.replaceAll(VERSION_URL_PLACEHOLDER, (isValidVersion(version) ? version : "")));
        } catch (MalformedURLException e) {
            throw new RuntimeException("error creating versioned url of base-url " + base + ": " + e.getMessage(), e);
        }
    }


    /**
     * reads the latest version for the application defined by the given application-directory and base-url.
     *
     * This implementation first checks the configuration for the versioned-url for {@link #LATEST_VERSION}; if this
     * configuration does not specify a version, it tries to read it from {@link #VERSION_FILE_NAME} in the given
     * application-directory.
     *
     * @param appdir the application-directory
     * @param appbase the application's base-url
     * @return the latest version or {@link #NO_VERSION}, if the application is not versioned
     * @throws IOException
     * @deprecated {@link com.threerings.getdown.data.Application} or {@link com.threerings.getdown.data.Configuration}
     *             should convert the appbase-String into an URL (and report invalid appbases), thus all methods
     *             accepting the appbase as a String will be removed from the public API
     */
    public static String readLatestVersion(File appdir, String appbase) throws IOException {
        return readLatestVersion(appdir, new URL(appbase));
    }

    /**
     * reads the latest version for the application defined by the given application-directory and base-url.
     *
     * This implementation first checks the configuration for the versioned-url for {@link #LATEST_VERSION}; if this
     * configuration does not specify a version, it tries to read it from {@link #VERSION_FILE_NAME} in the given
     * application-directory.
     *
     * @param appdir the application-directory
     * @param appbase the application's base-url
     * @return the latest version or {@link #NO_VERSION}, if the application is not versioned
     * @throws IOException
     */
    public static String readLatestVersion(File appdir, URL appbase) throws IOException {
        return readVersion(appdir, createVersionedUrl(appbase, LATEST_VERSION));

    }


    /**
     * reads the  version for the application by the given application-directory and url.
     *
     * This implementation first checks the configuration for the given url; if this
     * configuration does not specify a version, it tries to read it from {@link #VERSION_FILE_NAME} in the given
     * application-directory.
     *
     * @param appdir the application-directory
     * @param baseUrl the url for which the configuration should be read
     * @return the latest version or {@link #NO_VERSION}, if the application is not versioned
     * @throws IOException
     * @deprecated later versions will enforce the convention that the latest version's url can be found by replacing
     *             {@link #VERSION_URL_PLACEHOLDER} in the application-base-url with {@link #LATEST_VERSION} and remove
     *             the "latest"-config-parameter. Likewise this method will be removed from the public API
     *             Additionally {@link com.threerings.getdown.data.Application} or {@link com.threerings.getdown.data.Configuration}
     *             should convert the appbase-String into an URL (and report invalid appbases), thus all methods
     *             accepting the appbase as a String will be removed from the public API
     *
     */
    public static String readVersion(File appdir, String baseUrl) throws IOException {
        return readVersion(appdir, new URL(baseUrl));
    }

    /**
     * reads the version for the application by the given application-directory and url.
     *
     * This implementation first checks the version-file of the latest-URL for the the given base-url; if this
     * url does not specify a version, it tries to read it from {@link #VERSION_FILE_NAME} in the given
     * application-directory.
     *
     * @param appdir the application-directory
     * @param baseUrl the url for which the configuration should be read
     * @return the latest version or {@link #NO_VERSION}, if the application is not versioned
     * @throws IOException
     * @deprecated later versions will enforce the convention that the latest version's url can be found by replacing
     *             {@link #VERSION_URL_PLACEHOLDER} in the application-base-url with {@link #LATEST_VERSION} and remove
     *             the "latest"-config-parameter. Likewise this method will be removed from the public API
     */
    public static String readVersion(File appdir, URL baseUrl) throws IOException {
        String localVersion = readLocalVersion(appdir);

        URL versionURL = new URL(baseUrl, baseUrl.getPath() + "/" + VERSION_FILE_NAME);

        try {
            String latestVersion = readVersion(versionURL);
            if (compareVersions(latestVersion, localVersion) > 0) {
                writeLocalVersion(appdir, latestVersion);
                return latestVersion;
            }
        } catch (Exception e) {
            log.info("Unable to retrieve version from " + versionURL, e);
        }

        return localVersion;
    }


    /**
     * Reads a version number from the version-file in the given application-directory
     */
    public static String readLocalVersion(File appdir) {
        File vfile = new File(appdir, VERSION_FILE_NAME);
        if (!(vfile.exists() || vfile.canRead())) {
            return NO_VERSION;
        }

        try {
            return readVersion(vfile.toURI().toURL());
        } catch (IOException e) {
            log.info("Unable to read version from " + vfile + ": " + e.getMessage());
            return NO_VERSION;
        }
    }


    /**
     * Writes the given version to the version-file in the given application-directory
     */
    public static void writeLocalVersion(File appdir, String version) throws IOException {
        PrintStream out = new PrintStream(new FileOutputStream(new File(appdir, VERSION_FILE_NAME)));
        try {
            out.println((isValidVersion(version)) ? version : NO_VERSION);
        } catch (Exception e) {
            log.warning("Unable to write version file: " + e.getMessage());
        } finally {
            out.close();
        }
    }


    private static String readVersion(URL url) throws IOException {
        URLConnection conn = ConnectionUtil.open(url);
        InputStream in = conn.getInputStream();

        String version = NO_VERSION;
        try {
            version = new BufferedReader(new InputStreamReader(in)).readLine();
        } finally {
            in.close();
        }

        return (isValidVersion(version)) ? version : NO_VERSION;
    }

    /* for internal use & tests only */
    static String sanitize(String version, String separator) {
        return version.replaceAll("^" + separator, "").replaceAll(separator + "$", "").trim();
    }

    private VersionUtil() { /* no instances allowed */ }
}