//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2014 Three Rings Design, Inc.
// https://raw.github.com/threerings/getdown/master/LICENSE

package com.threerings.getdown.data;

import com.samskivert.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import static com.threerings.getdown.Log.log;

/**
 * Models a single file resource used by an {@link Application}.
 */
public class Resource {
    private static final Pattern VALID_ARCHIVE_FILE_PATTERN = Pattern.compile("^.*" + Pattern.quote(".") + "(jar|zip)$");

    private final ResourceType type;
    private final String path;
    private final File localFile;
    private final URL remoteUrl;
    private final File marker;


    public static Resource create(File appdir, URL appbase, String path, boolean unpack) throws MalformedURLException {
        return create((unpack) ? ResourceType.RESOURCE_ARCHIVE : ResourceType.RESOURCE_FILE, appdir, appbase, path);
    }

    public static Resource create(ResourceType type, File appdir, URL appbase, String path) throws MalformedURLException {
        return new Resource(type, path, new File(appdir, path).getAbsoluteFile(), new URL(appbase, path));
    }

    private Resource(ResourceType type, String path, File local, URL remote) {
        this.type = type;
        this.path = path;
        this.localFile = local;
        this.remoteUrl = remote;
        marker = new File(localFile.getAbsolutePath() + "v");
    }

    /**
     * Returns the path associated with this resource.
     */
    public final String getPath ()
    {
        return path;
    }

    public final ResourceType getType() {
        return type;
    }

    /**
     * Returns the local location of this resource.
     */
    public final File getLocalFile()
    {
        return localFile;
    }


    /**
     * Returns the remote location of this resource.
     */
    public final URL getRemote ()
    {
        return remoteUrl;
    }

    // @TODO: we've got two methods (isArchive and shouldUnpack) which do almost the same, remove one!
    public boolean isArchive() {
        return type.shouldUnpack() && isValidArchive(getLocalFile());
    }

    /**
     * Returns true if this resource should be unpacked as a part of the
     * validation process.
     */
    public boolean shouldUnpack ()
    {
        return type.shouldUnpack();
    }

    /**
     * Returns true if this resource has an associated "validated" marker
     * file.
     */
    public final boolean isMarkedValid ()
    {
        if (!localFile.exists()) {
            clearMarker();
            return false;
        }
        return marker.exists();
    }

    /**
     * Creates a "validated" marker file for this resource to indicate
     * that its MD5 hash has been computed and compared with the value in
     * the digest file.
     *
     * @throws IOException if we fail to create the marker file.
     */
    public final void markAsValid ()
        throws IOException
    {
        marker.createNewFile();
    }

    /**
     * Removes any "validated" marker file associated with this resource.
     */
    public final void clearMarker ()
    {
        if (marker.exists()) {
            if (!marker.delete()) {
                log.warning("Failed to erase marker file '" + marker + "'.");
            }
        }
    }

   public boolean unpack (File target) {
      if (!isValidArchive(localFile)) {
         log.warning("Requested to unpack invalid archive file '" + localFile + "'.");
         return false;
      }

      if (!target.isAbsolute()) {
         return unpack(new File(localFile.getParentFile().getAbsolutePath() + File.separator + target));
      }

      try {
         return FileUtil.unpackJar(new JarFile(localFile), target);
      } catch (IOException ioe) {
         log.warning("Failed to create JarFile from '" + localFile + "': " + ioe);
         return false;
      }
   }

   static boolean isValidArchive(File file) {
      return VALID_ARCHIVE_FILE_PATTERN.matcher(file.getName()).matches();
   }


   /**
     * Unpacks this resource file into the directory that contains it. Returns
     * false if an error occurs while unpacking it.
     */
    public boolean unpack () {
       return unpack(localFile.getParentFile().getAbsoluteFile());
    }

    /**
     * If our path is equal, we are equal.
     */
    @Override
    public boolean equals (Object other)
    {
        if (other instanceof Resource) {
            return path.equals(((Resource)other).path);
        } else {
            return false;
        }
    }

    /**
     * We hash on our path.
     */
    @Override
    public int hashCode ()
    {
        return path.hashCode();
    }

    /**
     * Returns a string representation of this instance.
     */
    @Override
    public String toString ()
    {
        return path;
    }
}
