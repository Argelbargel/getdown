package com.threerings.getdown.data;

import com.samskivert.text.MessageUtil;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.StringUtil;
import com.threerings.getdown.util.RuntimeVersionParser;
import com.threerings.getdown.util.VersionUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;

import static com.threerings.getdown.Log.log;

public class Configuration {

    private final File appdir;
    private final URL appbase;
    private final Map<String, Object> data;


    public Configuration(File file, URL appbase, Map<String, Object> data) {
        this.appdir = file;
        this.appbase = appbase;
        this.data = data;
    }

    public final File getAppdir() {
        return appdir;
    }

    public final URL getAppbase() {
        return appbase;
    }

    public String[] getStringArray(String name) {
        return getMultiValue(data, name);
    }

    /**
     * Massages a single string into an array and leaves existing array values as is. Simplifies
     * access to parameters that are expected to be arrays.
     */
    private static String[] getMultiValue (Map<String, Object> data, String name)
    {
        Object value = data.get(name);
        if (value == null) {
            return new String[0];
        } else if (value instanceof String) {
            return new String[] { (String)value };
        } else {
            return (String[])value;
        }
    }

    public Object getValue(String key) {
        return data.get(key);
    }

    public void parseResources(String version, String name, boolean unpack, List<Resource> list) {
        String[] rsrcs = getMultiValue(data, name);
        if (rsrcs == null) {
            return;
        }
        for (String rsrc : rsrcs) {
            try {
                list.add(Resource.create(appdir, VersionUtil.createVersionedUrl(appbase, version), rsrc, unpack));
            } catch (Exception e) {
                log.warning("Invalid resource '" + rsrc + "'. " + e);
            }
        }
    }


    public Rectangle getRectangle(String name, Rectangle def) {
        String value = (String) data.get(name);
        Rectangle rect = parseRect(name, value);
        return (rect == null) ? def : rect;
    }

    /**
     * Make an immutable List from the specified int array.
     */
    public static List<Integer> intsToList (int[] values)
    {
        List<Integer> list = new ArrayList<Integer>(values.length);
        for (int val : values) {
            list.add(val);
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Takes a comma-separated String of four integers and returns a rectangle using those ints as
     * the its x, y, width, and height.
     */
    public static Rectangle parseRect (String name, String value) {
        if (!StringUtil.isBlank(value)) {
            int[] v = StringUtil.parseIntArray(value);
            if (v != null && v.length == 4) {
                return new Rectangle(v[0], v[1], v[2], v[3]);
            }
            log.warning("Ignoring invalid '" + name + "' config '" + value + "'.");
        }
        return null;
    }

    /** Used to parse color specifications from the config file. */
    public Color getColor(String name, Color def) {
        String value = (String) data.get(name);
        Color color = parseColor(value);
        return (color == null) ? def : color;
    }

    /**
     * Parses the given hex color value (e.g. FFFFF) and returns a Color object with that value. If
     * the given value is null of not a valid hexadecimal number, this will return null.
     */
    public static Color parseColor (String hexValue)
    {
        if (!StringUtil.isBlank(hexValue)) {
            try {
                return new Color(Integer.parseInt(hexValue, 16));
            } catch (NumberFormatException e) {
                log.warning("Ignoring invalid color", "hexValue", hexValue, "exception", e);
            }
        }
        return null;
    }

    /** Parses a list of strings from the config file. */
    public String[] getList(String name) {
        String value = (String) data.get(name);
        return (value == null) ? ArrayUtil.EMPTY_STRING : StringUtil.parseStringArray(value);
    }


    /**
     * Parses a URL from the config file, checking first for a localized version.
     */
    public String getUrl(String name, String def) {
        String value = (String) data.get(name + "." + Locale.getDefault().getLanguage());
        if (!StringUtil.isBlank(value)) {
            return value;
        }
        value = (String) data.get(name);
        return StringUtil.isBlank(value) ? def : value;
    }


    protected static int parseJavaVersion (String value, String errkey) throws IOException {
      try {
         return new RuntimeVersionParser().parse(value);
      } catch (Exception e) {
         String err = MessageUtil.tcompose(errkey, value);
         throw (IOException) new IOException(err).initCause(e);
      }
   }

    public String getString(String key) {
        return (String) data.get(key);
    }


    public final ResourceGroup getResources() {
        URL vappbase = VersionUtil.createVersionedUrl(appbase, VersionUtil.getLocalVersion(appdir));

        ResourceGroup resources = new ResourceGroup();
        addResources(resources, vappbase);

        for (String auxgroup : getList("auxgroups")) {
            addResources(resources.getSubgroup(auxgroup), vappbase);
        }


        return resources;
    }

    public final ResourceGroup getActiveResources() {
        return getResources().getActiveResources(appdir);
    }


    private void addResources(ResourceGroup resources, URL appbase) {
        String prefix = resources.getName();
        if (!StringUtil.isBlank(prefix)) {
            prefix += ".";
        }

        for (ResourceType type : ResourceType.ANY) {
            for (String path : getStringArray(prefix + type.getId())) {
                try {
                    resources.addResources(Resource.create(type, appdir, appbase, path));
                } catch (MalformedURLException e) {
                    log.warning("ignoring invalid resource-path " + path);
                }
            }
        }
    }
}
