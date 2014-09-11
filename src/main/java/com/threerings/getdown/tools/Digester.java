//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2014 Three Rings Design, Inc.
// https://raw.github.com/threerings/getdown/master/LICENSE

package com.threerings.getdown.tools;

import com.threerings.getdown.data.Application;
import com.threerings.getdown.data.Digests;
import com.threerings.getdown.data.ResourceGroup;
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
        // create our application and instruct it to parse its business
        Application app = new Application(appdir, null);
        app.init(false);

        ResourceGroup rsrcs = new ResourceGroup();
        rsrcs.addResources(app.getConfigResource());
        rsrcs.addResources(app.getCodeResources());
        rsrcs.addResources(app.getResources());
        for (Application.AuxGroup ag : app.getAuxGroups()) {
            ResourceGroup srsrcs = rsrcs.getSubgroup(ag.name);
            srsrcs.addResources(ag.codes);
            srsrcs.addResources(ag.rsrcs);
        }


        return Digests.create(rsrcs, VersionUtil.readLocalVersion(appdir));
    }
}
