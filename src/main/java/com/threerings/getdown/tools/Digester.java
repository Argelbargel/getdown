//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2014 Three Rings Design, Inc.
// https://raw.github.com/threerings/getdown/master/LICENSE

package com.threerings.getdown.tools;

import java.io.File;
import java.io.IOException;

import java.security.GeneralSecurityException;

import java.util.ArrayList;
import java.util.List;

import com.threerings.getdown.data.Application;
import com.threerings.getdown.data.Digest;
import com.threerings.getdown.data.Resource;

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

        createDigest(new File(args[0]));
        if (args.length == 4) {
            signDigest(new File(args[0]), new File(args[1]), args[2], args[3]);
        }
    }

    /**
     * Creates a digest file in the specified application directory.
     */
    public static void createDigest (File appdir)
        throws IOException
    {
        File target = new File(appdir, Digest.DIGEST_FILE);
        System.out.println("Generating digest file '" + target + "'...");

        // create our application and instruct it to parse its business
        Application app = new Application(appdir, null);
        app.init(false);

        List<Resource> rsrcs = new ArrayList<Resource>();
        rsrcs.add(app.getConfigResource());
        rsrcs.addAll(app.getCodeResources());
        rsrcs.addAll(app.getResources());
        for (Application.AuxGroup ag : app.getAuxGroups()) {
            rsrcs.addAll(ag.codes);
            rsrcs.addAll(ag.rsrcs);
        }

        // now generate the digest file
        Digest.createDigest(rsrcs, target);
    }

    /**
     * Creates a digest file in the specified application directory.
     */
    public static void signDigest (File appdir, File storePath, String storePass, String storeAlias)
        throws IOException, GeneralSecurityException
    {
        Digest.signDigest(appdir, storePath, storePass, storeAlias);
    }
}
