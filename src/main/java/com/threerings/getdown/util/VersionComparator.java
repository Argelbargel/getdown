package com.threerings.getdown.util;

import paour.NaturalOrderComparator;

import java.util.Comparator;

import static com.threerings.getdown.util.VersionUtil.*;


final class VersionComparator implements Comparator<String> {
    private final String tokenSeparator;

    public VersionComparator(String separator) {
        tokenSeparator = separator;
    }

    @Override
    public int compare(String v1, String v2) {
        if (!isValidVersion(v1))  return (isValidVersion(v2)) ? -1 : 0;
        if (!isValidVersion(v2))  return 1; // v1 cannot be valid here

        if (LATEST_VERSION.equalsIgnoreCase(v1)) return (!LATEST_VERSION.equalsIgnoreCase(v2)) ? 1 : 0;
        if (LATEST_VERSION.equalsIgnoreCase(v2)) return -1; // v1 cannot be LATEST_VERSION here

        String[] parts1 = tokenize(v1);
        String[] parts2 = tokenize(v2);

        return compare(parts1, parts2, Math.min(parts1.length, parts2.length));
    }

    private String[] tokenize(String version) {
        return sanitize(version, tokenSeparator).toLowerCase().split(tokenSeparator);
    }

    private int compare(String[] parts1, String[] parts2, int partsToCompare) {
        int result = 0;

        NaturalOrderComparator noc = new NaturalOrderComparator();

        int part = 0;
        while (result == 0 && part < partsToCompare) {
            result = noc.compare(parts1[part], parts2[part]);
            ++part;
        }

        return (result != 0) ? result : parts1.length - parts2.length;
    }
}
