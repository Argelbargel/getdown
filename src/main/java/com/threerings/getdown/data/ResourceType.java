package com.threerings.getdown.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public enum ResourceType implements Iterable<ResourceType> {
    CONFIG_FILE("", false),
    CODE_FILE("code", false),
    DIGEST("", false),
    DIGEST_SIGNATURE("", false),
    FULL_APPLICATION("full", false),
    JRE_ARCHIVE("java_location", true),
    PATCH("", false),
    RESOURCE_FILE("resource", false),
    RESOURCE_ARCHIVE("uresource", true),

    NON_CODE_RESOURCES("", false, RESOURCE_FILE, RESOURCE_ARCHIVE),
    CONFIGURABLE_RESOURCES("", false, RESOURCE_FILE, RESOURCE_ARCHIVE, CODE_FILE),
    ANY("", false, CONFIG_FILE, CODE_FILE, DIGEST, DIGEST_SIGNATURE, FULL_APPLICATION, JRE_ARCHIVE, PATCH, RESOURCE_FILE, RESOURCE_ARCHIVE);

    private final String id;
    private final boolean unpack;
    private final Collection<ResourceType> parts;

    private ResourceType(String id, boolean unpack, ResourceType... parts) {
        this.id = id;
        this.unpack = unpack;
        this.parts = Arrays.asList(parts);
    }

    public boolean isAbstract() {
        return !parts.isEmpty();
    }

    public String getId() {
        return id;
    }

    public boolean shouldUnpack() {
        return unpack;
    }

    public boolean contains(ResourceType other) {
        return (parts.isEmpty()) ? other == this : parts.contains(other);
    }

    public Iterator<ResourceType> iterator() {
        return (parts.isEmpty()) ? Arrays.asList(this).iterator() : parts.iterator();
    }
}
