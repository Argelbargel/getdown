//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2014 Three Rings Design, Inc.
// https://raw.github.com/threerings/getdown/master/LICENSE

package com.threerings.getdown.tools;

import com.threerings.getdown.data.*;
import com.threerings.getdown.util.ConfigUtil;
import com.threerings.getdown.util.DigestsUtil;
import com.threerings.getdown.util.SecurityUtil;
import com.threerings.getdown.util.VersionUtil;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

/**
 * Handles the generation of the digest.txt file.
 */
public class Digester
{

   /**
     * A command line entry point for the digester.
     */
    public static void main (String[] args)
        throws IOException, GeneralSecurityException
    {
        if (args.length != 1 && args.length != 4) {
            System.err.println("Usage: Digester app_dir [keystore_path password alias]");
            System.exit(255);
        }

        File appdir = new File(args[0]);
        File keystore = (args.length > 1) ? new File(args[1]) : null;
        String password = (args.length > 2) ? args[2] : "";
        String alias = (args.length > 3) ? args[3] : "";

        writeDigests(appdir, keystore, password, alias);
    }

    public static void writeDigests(File appdir, File keystore, String password, String alias) throws IOException, GeneralSecurityException {
        Digests digests = createDigests(appdir);
        PrivateKey key = null;
        if (keystore != null) {
            key = SecurityUtil.loadPrivateKey(keystore, password, alias);
        }
        DigestsUtil.writeDigests(appdir, digests, key);
    }

    /**
     * Creates a digest file in the specified application directory.
     */
    private static Digests createDigests(File appdir) throws IOException {
        // read the local configuration for all possible os-environments
        Configuration config = ConfigUtil.readConfigFile(appdir, false);

        ResourceGroup rsrcs = new ResourceGroup();
        rsrcs.addResources(ConfigUtil.getConfigResource(appdir, config.getAppbase()));
        rsrcs.addResources(config.getResources().getResources(ResourceType.CONFIGURABLE_RESOURCES));
        for (ResourceGroup ag : config.getResources().getSubgroups()) {
            ResourceGroup srsrcs = rsrcs.getSubgroup(ag.getName());
            srsrcs.addResources(ag.getResources(ResourceType.CONFIGURABLE_RESOURCES));
        }


        return Digests.create(rsrcs, VersionUtil.getLocalVersion(appdir));
    }
}
