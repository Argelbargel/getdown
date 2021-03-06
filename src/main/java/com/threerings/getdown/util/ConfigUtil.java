//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2014 Three Rings Design, Inc.
// https://raw.github.com/threerings/getdown/master/LICENSE

package com.threerings.getdown.util;

import com.samskivert.text.MessageUtil;
import com.samskivert.util.StringUtil;
import com.threerings.getdown.data.Configuration;
import com.threerings.getdown.data.Resource;
import com.threerings.getdown.data.SysProps;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.threerings.getdown.Log.log;

/**
 * Parses a file containing key/value pairs and returns a {@link HashMap} with the values. Keys may
 * be repeated, in which case they will be made to reference an array of values.
 */
public class ConfigUtil {
    /* for internal use & tests only */
    static final String CONFIG_FILE = "getdown.txt";

    public static Resource getConfigResource(File appdir, URL appbase) {
        try {
            return Resource.create(appdir, appbase, ConfigUtil.CONFIG_FILE, false);
        } catch (Exception e) {
            throw new RuntimeException("Invalid appbase '" + appbase + "'.", e);
        }
    }

    public static Configuration downloadConfigFile(File appdir, URL appbase) throws IOException {
        ConnectionUtil.download(new File(appdir, CONFIG_FILE), new URL(appbase, CONFIG_FILE));
        return readConfigFile(appdir, false);
    }

    public static Configuration readConfigFile(File appdir, boolean checkPlatform) throws IOException {
        File configFile = new File(appdir, CONFIG_FILE);
        Map<String,Object> cdata = null;
        try {
            // if we have a configuration file, read the data from it
            if (configFile.exists()) {
                cdata = ConfigUtil.parseConfig(configFile, checkPlatform);
            }
            // otherwise, try reading data from our backup config file; thanks to funny windows
            // bullshit, we have to do this backup file fiddling in case we got screwed while
            // updating getdown.txt during normal operation
            else if ((configFile = new File(appdir, CONFIG_FILE + "_old")).exists()) {
                cdata = ConfigUtil.parseConfig(configFile, checkPlatform);
            }
        } catch (Exception e) {
            log.warning("Failure reading config file", "file", configFile, e);
        }

        // if we failed to read our config file, check for an appbase specified via a system
        // property; we can use that to bootstrap ourselves back into operation
        if (cdata == null) {
            String appbase = SysProps.appBase();
            if (appbase == null) {
                throw new RuntimeException("m.missing_appbase");
            }

            log.info("Attempting to obtain 'appbase' from system property", "appbase", appbase);
            cdata = new HashMap<String,Object>();
            cdata.put("appbase", appbase);
        }

        return createConfiguration(appdir, cdata);
    }

    private static Configuration createConfiguration(File appdir, Map<String, Object> data) throws MalformedURLException {
        return new Configuration(appdir, createAppbase((String) data.get("appbase")), data);
    }

    private static URL createAppbase(String url) throws MalformedURLException {
        // check if we're overriding the domain in the appbase
        String appbaseDomain = SysProps.appbaseDomain();
        if (appbaseDomain != null) {
            Matcher m = Pattern.compile("(http://[^/]+)(.*)").matcher(url);
            url = m.replaceAll(appbaseDomain + "$2");
        }

        // make sure there's a trailing slash
        if (!url.endsWith("/")) {
            url = url + "/";
        }

        try {
            return new URL(url);
        } catch (MalformedURLException mue) {
            throw new RuntimeException(MessageUtil.tcompose("m.invalid_appbase", url), mue);
        }
    }


    /**
     * Parses a configuration file containing key/value pairs. The file must be in the UTF-8
     * encoding.
     *
     * @param checkPlatform if true, platform qualifiers will be used to filter out pairs that do
     * not match the current platform; if false, all pairs will be returned.
     *
     * @return a list of <code>String[]</code> instances containing the key/value pairs in the
     * order they were parsed from the file.
     */
    public static List<String[]> parsePairs (File config, boolean checkPlatform)
        throws IOException
    {
        // annoyingly FileReader does not allow encoding to be specified (uses platform default)
        return parsePairs(
            new InputStreamReader(new FileInputStream(config), "UTF-8"), checkPlatform);
    }

    /**
     * See {@link #parsePairs(File,boolean)}.
     */
    public static List<String[]> parsePairs (Reader config, boolean checkPlatform)
        throws IOException
    {
        return parsePairs(
            config,
            checkPlatform ? SysProps.osName() : null,
            checkPlatform ? SysProps.osArch() : null);
    }

    /**
     * Parses a configuration file containing key/value pairs. The file must be in the UTF-8
     * encoding.
     *
     * @return a map from keys to values, where a value will be an array of strings if more than
     * one key/value pair in the config file was associated with the same key.
     */
    public static Map<String, Object> parseConfig (File config, boolean checkPlatform)
        throws IOException
    {
        Map<String, Object> data = new HashMap<String, Object>();

        // I thought that we could use HashMap<String, String[]> and put new String[] {pair[1]} for
        // the null case, but it mysteriously dies on launch, so leaving it as HashMap<String,
        // Object> for now
        for (String[] pair : parsePairs(config, checkPlatform)) {
            Object value = data.get(pair[0]);
            if (value == null) {
                data.put(pair[0], pair[1]);
            } else if (value instanceof String) {
                data.put(pair[0], new String[] { (String)value, pair[1] });
            } else if (value instanceof String[]) {
                String[] values = (String[])value;
                String[] nvalues = new String[values.length+1];
                System.arraycopy(values, 0, nvalues, 0, values.length);
                nvalues[values.length] = pair[1];
                data.put(pair[0], nvalues);
            }
        }

        return data;
    }

    /** A helper function for {@link #parsePairs(Reader,boolean)}. */
    protected static List<String[]> parsePairs (Reader config, String osname, String osarch)
        throws IOException
    {
        List<String[]> pairs = new ArrayList<String[]>();
        for (String line : FileUtil.readLines(config)) {
            // nix comments
            int cidx = line.indexOf("#");
            if (cidx != -1) {
                line = line.substring(0, cidx);
            }

            // trim whitespace and skip blank lines
            line = line.trim();
            if (StringUtil.isBlank(line)) {
                continue;
            }

            // parse our key/value pair
            String[] pair = new String[2];
            int eidx = line.indexOf("=");
            if (eidx != -1) {
                pair[0] = line.substring(0, eidx).trim();
                pair[1] = line.substring(eidx+1).trim();
            } else {
                pair[0] = line;
                pair[1] = "";
            }

            // if the pair has an os qualifier, we need to process it
            if (pair[1].startsWith("[")) {
                int qidx = pair[1].indexOf("]");
                if (qidx == -1) {
                    log.warning("Bogus platform specifier", "key", pair[0], "value", pair[1]);
                    continue; // omit the pair entirely
                }
                // if we're checking qualifiers and the os doesn't match this qualifier, skip it
                String quals = pair[1].substring(1, qidx);
                if (osname != null && !checkQualifiers(quals, osname, osarch)) {
                    log.info("Skipping", "quals", quals, "osname", osname, "osarch", osarch,
                             "key", pair[0], "value", pair[1]);
                    continue;
                }
                // otherwise filter out the qualifier text
                pair[1] = pair[1].substring(qidx+1).trim();
            }

            pairs.add(pair);
        }

        return pairs;
    }

    /**
     * A helper function for {@link #parsePairs(Reader,String,String)}. Qualifiers have the
     * following form:
     * <pre>
     * id = os[-arch]
     * ids = id | id,ids
     * quals = !id | ids
     * </pre>
     * Examples: [linux-amd64,linux-x86_64], [windows], [mac os x], [!windows]. Negative qualifiers
     * must appear alone, they cannot be used with other qualifiers (positive or negative).
     */
    protected static boolean checkQualifiers (String quals, String osname, String osarch)
    {
        if (quals.startsWith("!")) {
            if (quals.contains(",")) { // sanity check
                log.warning("Multiple qualifiers cannot be used when one of the qualifiers " +
                            "is negative", "quals", quals);
                return false;
            }
            return !checkQualifier(quals.substring(1), osname, osarch);
        }
        for (String qual : quals.split(",")) {
            if (checkQualifier(qual, osname, osarch)) {
                return true; // if we have a positive match, we can immediately return true
            }
        }
        return false; // we had no positive matches, so return false
    }

    /** A helper function for {@link #checkQualifiers}. */
    protected static boolean checkQualifier (String qual, String osname, String osarch)
    {
        String[] bits = qual.trim().toLowerCase().split("-");
        String os = bits[0], arch = (bits.length > 1) ? bits[1] : "";
        return (osname.contains(os)) && (osarch.contains(arch));
    }
}
