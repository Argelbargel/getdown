//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2014 Three Rings Design, Inc.
// https://raw.github.com/threerings/getdown/master/LICENSE

package com.threerings.getdown.data;

import com.samskivert.text.MessageUtil;
import com.samskivert.util.StringUtil;
import com.threerings.getdown.util.DigestsUtil;

import java.io.IOException;
import java.util.*;
import java.util.Properties;


/**
 * Manages the <code>digest.txt</code> file
 */
public final class Digests {
    private static final String METADIGEST_KEY = ".";

    public static Digests create(ResourceGroup app, String version) throws IOException {
        Digests digests = new Digests();
        for (Resource rsrc : collectResources(app)) {
            try {
                digests.addResource(rsrc.getPath(), DigestsUtil.computeResourceDigest(rsrc, null));
            } catch (Throwable t) {
                throw (IOException) new IOException("Error computing digest for: " + rsrc).initCause(t);
            }
        }

        digests.setMetaDigest(DigestsUtil.computeDigestsDigest(digests, version, null));
        return digests;
    }

    public static Digests create(Properties contents, String version) throws IOException {
        Digests digests = new Digests();
        String metaDigest = "";
        for (String file : contents.stringPropertyNames()) {
            if (file.equals(METADIGEST_KEY)) {
                metaDigest = contents.getProperty(METADIGEST_KEY);
            } else {
                digests.addResource(file, contents.getProperty(file));
            }
        }

        if (StringUtil.isBlank(metaDigest) || !metaDigest.equals(DigestsUtil.computeDigestsDigest(digests, version, null))) {
            throw new IOException(MessageUtil.tcompose("m.invalid_digest_file"));
        }

        digests.setMetaDigest(metaDigest);
        return digests;
    }

    private static List<Resource> collectResources(ResourceGroup group) {
        List<Resource> resources = new ArrayList<Resource>();
        resources.addAll(group.getResources(ResourceType.CONFIG_FILE));
        resources.addAll(group.getResources(ResourceType.CONFIGURABLE_RESOURCES));
        for (ResourceGroup ag : group.getSubgroups()) {
            resources.addAll(ag.getResources(ResourceType.CONFIGURABLE_RESOURCES));
        }
        return resources;
    }


    private final SortedMap<String, String> digests;
    private String metaDigest = "";

    private Digests() {
        digests = new TreeMap<String, String>();
    }


    private void addResource(String path, String digest) {
        digests.put(path, digest);
    }

    private void setMetaDigest(String digest) {
        metaDigest = digest;
    }


    private void appendDigest(StringBuilder target, String key, String digest) {
        target.append(key).append(" = ").append(digest).append("\n");
    }


    public String getMetaDigest ()
    {
        return metaDigest;
    }

    public String getResourceDigest(Resource resource) {
        if (!digests.containsKey(resource.getPath())) {
            throw new NoSuchElementException("no digest present for resource " + resource.getPath());
        }

        return digests.get(resource.getPath());
    }


    public String getContents() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : digests.entrySet()) {
            appendDigest(sb, entry.getKey(), entry.getValue());
        }
        return sb.toString();
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getContents());
        appendDigest(sb, METADIGEST_KEY, getMetaDigest());
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Digests digests = (Digests) o;
        return this.digests.equals(digests.digests) && metaDigest.equals(digests.metaDigest);
    }

    @Override
    public int hashCode() {
        int result = digests.hashCode();
        result = 31 * result + metaDigest.hashCode();
        return result;
    }
}
