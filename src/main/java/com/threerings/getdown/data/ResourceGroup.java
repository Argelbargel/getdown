package com.threerings.getdown.data;

import com.threerings.getdown.util.LaunchUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class ResourceGroup {
    private static final String GROUP_SEPARATOR = ".";
    private final String name;
    private final Map<String, Collection<Resource>> resourceMap;

    public ResourceGroup() {
        this("", new HashMap<String, Collection<Resource>>());

    }

    private ResourceGroup(String name, Map<String, Collection<Resource>> resources) {
        this.name = name;
        this.resourceMap = resources;
    }

    public final String getName() {
        return (!name.contains(GROUP_SEPARATOR)) ? name : name.substring(name.lastIndexOf(GROUP_SEPARATOR) + GROUP_SEPARATOR.length());
    }

    public final void addResources(Resource... resources) {
        addResources(Arrays.asList(resources));
    }

    public final void addResources(Collection<Resource> resources) {
        if (resources.contains(null)) {
            throw new IllegalArgumentException("resources must not contain null");
        }

        if (!resourceMap.containsKey(name)) {
            resourceMap.put(name, new LinkedList<Resource>());
        }

        resourceMap.get(name).addAll(resources);
    }


    public final Collection<Resource> getResources(ResourceType... types) {
        return filterResources(resourceMap.get(name), types);
    }

    public final ResourceGroup getActiveResources(File appdir) {
        ResourceGroup active = new ResourceGroup();
        for (Map.Entry<String, Collection<Resource>> entry : resourceMap.entrySet()) {
            if (LaunchUtil.isAuxGroupActive(appdir, entry.getKey())) {
                active.addResources(entry.getValue());
            }
        }

        return active;
    }

    private Collection<Resource> filterResources(Collection<Resource> resources, ResourceType... allowedTypes) {
        if (allowedTypes == null || allowedTypes.length < 1) {
            return Collections.unmodifiableCollection(resources);
        }

        Collection<Resource> result = new LinkedList<Resource>();
        for (Resource resource : resources) {
            ResourceType resourceType = resource.getType();
            for (ResourceType allowedType : allowedTypes) {
                if (allowedType.contains(resourceType)) {
                    result.add(resource);
                }
            }
        }

        return Collections.unmodifiableCollection(result);
    }

    public final boolean hasSubgroups() {
        return !getSubgroups().isEmpty();
    }

    public final ResourceGroup getSubgroup(String group) {
        return new ResourceGroup(name + GROUP_SEPARATOR + group, resourceMap);
    }

    public final Collection<ResourceGroup> getSubgroups() {
        Collection<ResourceGroup> auxGroups = new LinkedList<ResourceGroup>();
        for (String name : resourceMap.keySet()) {
            if (!getName().equals(name)) {
                auxGroups.add(new ResourceGroup(name, resourceMap));
            }
        }

        return auxGroups;
    }

    public final void clear() {
        if (resourceMap.containsKey(name)) {
            resourceMap.remove(name);
        }
    }

    public final int size() {
        return (resourceMap.containsKey(name)) ? resourceMap.get(name).size() : 0;
    }

    public final boolean isEmpty() {
        return size() < 1;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceGroup that = (ResourceGroup) o;

        if (!resourceMap.equals(that.resourceMap)) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = resourceMap.hashCode();
        if (name != null) {
            result += 31 * name.hashCode();
        }
        return result;
    }
}
