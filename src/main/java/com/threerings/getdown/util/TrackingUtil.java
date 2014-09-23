package com.threerings.getdown.util;

import com.samskivert.util.RandomUtil;
import com.threerings.getdown.data.Application;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import static com.threerings.getdown.Log.log;

/**
 * Created by karsten on 12.09.14.
 */
public class TrackingUtil {
    /** Possibly generates and returns a google analytics tracking cookie.
     * @param hash
     * @param start
     * @param id*/
    public static String getGATrackingCode(String hash, long start, int id)
    {
        if (hash == null) {
            return "";
        }
        long time = System.currentTimeMillis() / 1000;
        if (start == 0) {
            start = time;
        }
        if (id == 0) {
            id = RandomUtil.getInRange(100000000, 1000000000);
        }
        StringBuilder cookie = new StringBuilder("&utmcc=__utma%3D").append(hash);
        cookie.append(".").append(id);
        cookie.append(".").append(start).append(".").append(start);
        cookie.append(".").append(time).append(".1%3B%2B");
        cookie.append("__utmz%3D").append(hash).append(".");
        cookie.append(start).append(".1.1.");
        cookie.append("utmcsr%3D(direct)%7Cutmccn%3D(direct)%7Cutmcmd%3D(none)%3B");
        cookie.append("&utmn=").append(RandomUtil.getInRange(1000000000, 2000000000));
        return cookie.toString();
    }

    /**
     * Returns the URL to use to report an initial download event. Returns null if no tracking
     * start URL was configured for this application.
     *
     * @param event the event to be reported: start, jvm_start, jvm_complete, complete.
     * @param suffix
     * @param url
     * @param hash
     * @param start
     * @param id
     */
    public static URL getTrackingURL(String event, String suffix, String url, String hash, long start, int id)
    {
        try {
            suffix = suffix == null ? "" : suffix;
            String ga = getGATrackingCode(hash, start, id);
            return url == null ? null : new URL(url + event + suffix + ga);
        } catch (MalformedURLException mue) {
            log.warning("Invalid tracking URL", "path", url, "event", event, "error", mue);
            return null;
        }
    }

    /**
     * Returns the URL to request to report that we have reached the specified percentage of our
     * initial download. Returns null if no tracking request was configured for the specified
     * percentage.
     */
    public static URL getTrackingProgressURL(Application application, int percent, Set<Integer> allowed)
    {
        if (allowed == null || !allowed.contains(percent)) {
            return null;
        }
        return application.getTrackingURL("pct" + percent);
    }
}
