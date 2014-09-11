package com.threerings.getdown.util;

import com.samskivert.text.MessageUtil;
import com.threerings.getdown.data.SysProps;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppbaseUtil {
    // @TODO: shoould return URL
    public static String createAppbase(String url) throws MalformedURLException {
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
            return new URL(url).toString();
        } catch (MalformedURLException mue) {
            throw new RuntimeException(MessageUtil.tcompose("m.invalid_appbase", url), mue);
        }
    }

    private AppbaseUtil() { /* no instances allowed */ }
}
