//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2014 Three Rings Design, Inc.
// https://raw.github.com/threerings/getdown/master/LICENSE

package com.threerings.getdown.util;

import com.samskivert.io.StreamUtil;
import com.threerings.getdown.data.SysProps;
import org.apache.commons.codec.binary.Base64;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;

public class ConnectionUtil
{
    /**
     * Opens a connection to a URL, setting the authentication header if user info is present.
     */
    public static URLConnection open(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        // we have to tell Java not to use caches, otherwise it will cache any request for
        // same URL for the lifetime of this JVM (based on the URL string, not the URL object);
        // Turning off caches is not a performance concern, because when Getdown asks
        // to download a file, it expects it to come over the wire, not from a cache
        conn.setUseCaches(false);

        int timeout = SysProps.connectTimeout();
        if (timeout > 0) {
            conn.setConnectTimeout(timeout * 1000);
        }

        // If URL has a username:password@ before hostname, use HTTP basic auth
        String userInfo = url.getUserInfo();
        if (userInfo != null) {
            // Remove any percent-encoding in the username/password
            userInfo = URLDecoder.decode(userInfo, "UTF-8");
            // Now base64 encode the auth info and make it a single line
            String encoded = Base64.encodeBase64String(userInfo.getBytes("UTF-8")).
                replaceAll("\\n","").replaceAll("\\r", "");
            conn.setRequestProperty("Authorization", "Basic " + encoded);
        }

        return conn;
    }

    public static File download(File target, URL url) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = open(url).getInputStream();
            out = new FileOutputStream(target);
            StreamUtil.copy(in, out);
            return target;
        } finally {
            StreamUtil.close(in);
            StreamUtil.close(out);
        }
    }

    /**
     * Opens a connection to a http or https URL, setting the authentication header if user info
     * is present. Throws a class cast exception if the connection returned is not the right type.
     */
    public static HttpURLConnection openHttp (URL url)
        throws IOException
    {
        return (HttpURLConnection)open(url);
    }
}
