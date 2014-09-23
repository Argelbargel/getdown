//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2014 Three Rings Design, Inc.
// https://raw.github.com/threerings/getdown/master/LICENSE

package com.threerings.getdown.data;

import com.samskivert.util.Logger;
import com.samskivert.util.StringUtil;

/**
 * This class encapsulates all system properties that are read and processed by Getdown. Don't
 * stick a call to {@code System.getProperty} randomly into the code, put it in here and give it an
 * accessor so that it's easy to see all of the secret system property arguments that Getdown makes
 * use of.
 */
public class SysProps
{
    /** Configures the appdir (in lieu of passing it in argv). Usage: {@code -Dappdir=foo}. */
    public static String appDir () {
        return getString("appdir");
    }

    /** Configures the appid (in lieu of passing it in argv). Usage: {@code -Dappid=foo}. */
    public static String appId () {
        return getString("appid");
    }

    /** Configures the appbase (in lieu of providing a skeleton getdown.txt, and as a last resort
     * fallback). Usage: {@code -Dappbase=someurl}. */
    public static String appBase () {
        return getString("appbase");
    }

    /** If true, disables redirection of logging into {@code launcher.log}.
     * Usage: {@code -Dno_log_redir}. */
    public static boolean noLogRedir () {
        return getString("no_log_redir") != null;
    }

    /** Overrides the domain on {@code appbase}. Usage: {@code -Dappbase_domain=foo}. */
    public static String appbaseDomain () {
        return getString("appbase_domain");
    }

    public static String osName() { return StringUtil.deNull(getString("os.name")).toLowerCase(); }

    public static String proxyHost() { return getString("http.proxyHost"); }

    public static String silent () {
        return getString("silent");
    }

    /** Specifies the a delay (in minutes) to wait before starting the update and install process.
     * Usage: {@code -Ddelay=N}. */
    public static int startDelay () {
        return getInteger("delay");
    }

    /** If true, Getdown will not unpack {@code uresource} jars. Usage: {@code -Dno_unpack}. */
    public static boolean noUnpack () {
        return getBoolean("no_unpack");
    }

    /** If true, Getdown will run the application in the same VM in which Getdown is running. If
     * false (the default), Getdown will fork a new VM. Note that reusing the same VM prevents
     * Getdown from configuring some launch-time-only VM parameters (like -mxN etc.).
     * Usage: {@code -Ddirect}. */
    public static boolean direct () {
        return getBoolean("direct");
    }

    /** Specifies the connection timeout (in seconds) to use when downloading control files from
     * the server. This is chiefly useful when you are running in versionless mode and want Getdown
     * to more quickly timeout its startup update check if the server with which it is
     * communicating is not available. Usage: {@code -Dconnect_timeout=N}. */
    public static int connectTimeout () {
        return getInteger("connect_timeout");
    }

    public static String proxyPort() {
        return getString("http.proxyPort");
    }

    public static String javaVersion() {
        return getString("java.version");
    }

    public static String osArch() {
        return StringUtil.deNull(getString("os.arch")).toLowerCase();
    }

    public static String osVersion() {
        return StringUtil.deNull(getString("os.version")).toLowerCase();
    }

    public static String javaHome() {
        return getString("java.home");
    }

    public static String userName() {
        return getString("user.name");
    }

    public static String userHome() {
        return getString("user.home");
    }

    public static String workingDirectory() {
        return getString("user.dir");
    }

    public static void logJVMProperties(Logger log) {
        // record a few things for posterity
        log.info("------------------ VM Info ------------------");
        log.info("-- OS Name: " + osName());
        log.info("-- OS Arch: " + osArch());
        log.info("-- OS Vers: " + osVersion());
        log.info("-- Java Vers: " + javaVersion());
        log.info("-- Java Home: " + javaHome());
        log.info("-- User Name: " + userName());
        log.info("-- User Home: " + userHome());
        log.info("-- Current directory: " + workingDirectory());
    }

    public static void logProxyInfo(Logger log) {
        log.info("---------------- Proxy Info -----------------");
        log.info("-- Proxy Host: " + getString("http.proxyHost"));
        log.info("-- Proxy Port: " + getString("http.proxyPort"));
        log.info("---------------------------------------------");
    }

    private static String getString(String key) {
        try {
             return System.getProperty(key);
        } catch (SecurityException e) {
            // ignore problems when accessing system-property protected by Webstart- or Applet-SecurityManager
            return null;
        }
    }

    private static boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

    private static int getInteger(String key) {
        String value = getString(key);
        try {
            return (!StringUtil.isBlank(value)) ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
