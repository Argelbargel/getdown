package com.threerings.getdown.data;

import com.samskivert.util.RandomUtil;
import com.samskivert.util.StringUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import static com.threerings.getdown.Log.log;

public class Tracking {
    private final String trackingUrl;
    private final Set<Integer> percents;
    private final String cookieName;
    private final String cookieProperty;
    private final String urlSuffix;
    private final String gaHash;

    public Tracking() {
        trackingUrl = "";
        percents = new HashSet<Integer>();
        cookieName = "";
        cookieProperty = "";
        urlSuffix = "";
        gaHash = "";
    }

    public Tracking(String url, String suffix, String cname, String cproperty, String hash, String pcts) {
        trackingUrl = url;
        urlSuffix = suffix;
        cookieName = cname;
        cookieProperty = cproperty;
        gaHash = hash;

        percents = new HashSet<Integer>();
        percents.add(50);
        if (!StringUtil.isBlank(pcts)) {
            for (int pct : StringUtil.parseIntArray(pcts)) {
                percents.add(pct);
            }
        }

    }


    /** Possibly generates and returns a google analytics tracking cookie.
     *
     * @return the tracking hash or an empty String
     */
    private String getGATrackingCode()
    {
        if (gaHash == null) {
            return "";
        }
        long time = System.currentTimeMillis() / 1000;
        int id = RandomUtil.getInRange(100000000, 1000000000);
        StringBuilder cookie = new StringBuilder("&utmcc=__utma%3D").append(gaHash);
        cookie.append(".").append(id);
        cookie.append(".").append(time).append(".").append(time);
        cookie.append(".").append(time).append(".1%3B%2B");
        cookie.append("__utmz%3D").append(gaHash).append(".");
        cookie.append(time).append(".1.1.");
        cookie.append("utmcsr%3D(direct)%7Cutmccn%3D(direct)%7Cutmcmd%3D(none)%3B");
        cookie.append("&utmn=").append(RandomUtil.getInRange(1000000000, 2000000000));
        return cookie.toString();
    }


    /**
     * Returns the URL to use to report an initial download event. Returns null if no tracking
     * start URL was configured for this application.
     *
     * @param event the event to be reported: start, jvm_start, jvm_complete, complete.
     */
    public URL getURL(String event) {
        String suffix = urlSuffix;
        try {
            suffix = suffix == null ? "" : suffix;
            String ga = getGATrackingCode();
            return trackingUrl == null ? null : new URL(trackingUrl + event + suffix + ga);
        } catch (MalformedURLException mue) {
            log.warning("Invalid tracking URL", "path", trackingUrl, "event", event, "error", mue);
            return null;
        }
    }

    /**
     * Returns the URL to request to report that we have reached the specified percentage of our
     * initial download. Returns null if no tracking request was configured for the specified
     * percentage.
     */
    public URL getProgressURL(int percent) {
        if (percents == null || !percents.contains(percent)) {
            return null;
        }
        return getURL("pct" + percent);
    }

    /**
     * Returns the name of our tracking cookie or null if it was not set.
     * todo: move to own data-class or utility
     */
    public String getCookieName() {
        return cookieName;
    }

    /**
     * Returns the name of our tracking cookie system property or null if it was not set.
     * todo: move to own data-class or utility
     */
    public String getCookieProperty() {
        return cookieProperty;
    }
}