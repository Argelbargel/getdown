//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2014 Three Rings Design, Inc.
// https://raw.github.com/threerings/getdown/master/LICENSE

package com.threerings.getdown.data;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.StringUtil;
import com.threerings.getdown.util.*;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.cert.Certificate;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

import static com.threerings.getdown.Log.log;

/**
 * Parses and provide access to the information contained in the <code>getdown.txt</code>
 * configuration file.
 */
public class Application {

    /** System properties that are prefixed with this string will be passed through to our
     * application (minus this prefix). */
    public static final String PROP_PASSTHROUGH_PREFIX = "app.";

    public File getAppdir() {
        return _appdir;
    }


    /**
     * Used by {@link #verifyMetadata} to communicate status in circumstances where it needs to
     * take network actions.
     */
    public static interface StatusDisplay
    {
        /** Requests that the specified status message be displayed. */
        public void updateStatus (String message);
    }

    /**
     * Creates an application instance with no signers.
     *
     * @see #Application(File, String, List, String[], String[])
     */
    public Application (File appdir, String appid)
    {
        this(appdir, appid, null, null, null);
    }

    /**
     * Creates an application instance which records the location of the <code>getdown.txt</code>
     * configuration file from the supplied application directory.
     *
     * @param appid usually null but a string identifier if a secondary application is desired to
     * be launched. That application will use {@code appid.class} and {@code appid.apparg} to
     * configure itself but all other parameters will be the same as the primary application.
     * @param signers a list of possible signers of this application. Used to verify the digest.
     * @param jvmargs additional arguments to pass on to launched jvms.
     * @param appargs additional arguments to pass on to launched application; these will be added
     * after the args in the getdown.txt file.
     */
    public Application (File appdir, String appid, List<Certificate> signers,
                        String[] jvmargs, String[] appargs)
    {
        _appdir = appdir;
        _appid = appid;
        _signers = (signers == null) ? Collections.<Certificate>emptyList() : signers;
        _extraJvmArgs = (jvmargs == null) ? ArrayUtil.EMPTY_STRING : jvmargs;
        _extraAppArgs = (appargs == null) ? ArrayUtil.EMPTY_STRING : appargs;
    }

    /**
     * Returns a resource that refers to the application configuration file itself.
     * todo: remove!
     */
    public Resource getConfigResource () {
        return ConfigUtil.getConfigResource(getAppdir(), getAppbase());
    }

    /**
     * @deprecated use Configuration#getResources() instead!
     */
    public ResourceGroup getResources() { return _resources; }

    // todo: rename to getResources()
    public ResourceGroup getActiveResources() {
        return getResources().getActiveResources(getAppdir());
    }

    /**
     * Returns a resource that can be used to download a patch file that will bring this
     * application from its current version to the target version.
     *
     * @param auxgroup the auxiliary resource group for which a patch resource is desired or null
     * for the main application patch resource.
     * @param version the version to patch to
     */
    public Resource getPatchResource(String auxgroup, String version) {
        String infix = (auxgroup == null) ? "" : ("-" + auxgroup);
        String pfile = "patch" + infix + getVersion() + ".dat";
        try {
            return Resource.create(getAppdir(), getAppbase(), pfile, false);
        } catch (Exception e) {
            log.warning("Failed to create patch resource path",
                "pfile", pfile, "appbase", getAppbase(), "tvers", version, "error", e);
            return null;
        }
    }

    /**
     * Returns a resource for a zip file containing a Java VM that can be downloaded to use in
     * place of the installed VM (in the case where the VM that launched Getdown does not meet the
     * application's version requirements) or null if no VM is available for this platform.
     */
    public Resource getJavaVMResource() {
        if (StringUtil.isBlank(_javaLocation)) {
            return null;
        }
        try {
            return Resource.create(ResourceType.JRE_ARCHIVE, getAppdir(), getAppbase(), _javaLocation);
        } catch (Exception e) {
            log.warning("Failed to create VM resource", "appdir", getAppdir(),
                    "javaloc", _javaLocation, "error", e);
            return null;
        }
    }

    /**
     * Returns a resource that can be used to download an archive containing all files belonging to
     * the application.
     */
    public Resource getFullResource (String version)
    {
        try {
            return Resource.create(getAppdir(), getAppbase(), "full", false);
        } catch (Exception e) {
            log.warning("Failed to create full resource path",
                "file", "full", "appbase", getAppbase(), "tvers", version, "error", e);
            return null;
        }
    }

    public Tracking.TrackingReporter createTrackingReporter(int progress) {
        return tracking.createReporter(+progress);
    }

    public Tracking.TrackingReporter createTrackingReporter(String event) {
        return tracking.createReporter(event);
    }

    /**
     * Instructs the application to parse its {@code getdown.txt} configuration and prepare itself
     * for operation. The application base URL will be parsed first so that if there are errors
     * discovered later, the caller can use the application base to download a new {@code
     * getdown.txt} file and try again.
     *
     * @return a configured UpdateInterface instance that will be used to configure the update UI.
     *
     * @exception IOException thrown if there is an error reading the file or an error encountered
     * during its parsing.
     * todo: move setting of _class, _appbase, _version, to updateMetaData
     */
    public UpdateInterface init (boolean checkPlatform)
        throws IOException
    {
        log.info("(re-)initializing application " + _appid + " at " + getAppdir());
        // @todo: make this a field?
        Configuration config = ConfigUtil.readConfigFile(getAppdir(), checkPlatform);


        // first determine our application base, this way if anything goes wrong later in the
        // process, our caller can use the appbase to download a new configuration file
        _appbase = config.getAppbase();

        // extract our version information
        _version = VersionUtil.getLatestVersion(getAppdir(), _appbase);

        String prefix = StringUtil.isBlank(_appid) ? "" : (_appid + ".");

        // determine our application class name
        _class = config.getString(prefix + "class");
        if (_class == null) {
            throw new IOException("m.missing_class");
        }

        tracking = new Tracking(config.getString("tracking_url"),
                                config.getString("tracking_url_suffix"),
                                config.getString("tracking_cookie_name"),
                                config.getString("tracking_cookie_property"),
                                config.getString("tracking_ga_hash"),
                                config.getString("tracking_percents")
                                );


        // clear our arrays as we may be reinitializing
        clear();

        // parse our code resources
        if (config.getStringArray("code") == null) {
            throw new IOException("m.missing_code");
        }

        _resources = config.getResources();

        // transfer our JVM arguments
        String[] jvmargs = config.getStringArray("jvmarg");
        if (jvmargs != null) {
            Collections.addAll(_jvmargs, jvmargs);
        }

        // Add the launch specific JVM arguments
        Collections.addAll(_jvmargs, _extraJvmArgs);

        // get the set of optimum JVM arguments
        _optimumJvmArgs = config.getStringArray("optimum_jvmarg");

        // transfer our application arguments
        String[] appargs = config.getStringArray(prefix + "apparg");
        if (appargs != null) {
            Collections.addAll(_appargs, appargs);
        }

        // add the launch specific application arguments
        Collections.addAll(_appargs, _extraAppArgs);

        // look for custom arguments
        fillAssignmentListFromPairs("extra.txt", _txtJvmArgs);

        // determine whether we want to allow offline operation (defaults to false)
        _allowOffline = Boolean.parseBoolean(config.getString("allow_offline"));

        // look for a debug.txt file which causes us to run in java.exe on Windows so that we can
        // obtain a thread dump of the running JVM
        _windebug = getLocalPath("debug.txt").exists();

        // @todo: should these properties have the ui.-prefix?
        _name = config.getString("ui.name");
        _dockIconPath = config.getString("ui.mac_dock_icon");
        if (_dockIconPath == null) {
            _dockIconPath = "../desktop.icns"; // use a sensible default
        }


        return UpdateInterface.create(config);
    }

    private void clear() {
        _resources.clear();
        _jvmargs.clear();
        _appargs.clear();
        _txtJvmArgs.clear();
    }

    /**
     * Adds strings of the form pair0=pair1 to collector for each pair parsed out of pairLocation.
     */
    protected void fillAssignmentListFromPairs (String pairLocation, List<String> collector)
    {
        File pairFile = getLocalPath(pairLocation);
        if (pairFile.exists()) {
            try {
                List<String[]> args = ConfigUtil.parsePairs(pairFile, false);
                for (String[] pair : args) {
                    if (pair[1].length() == 0) {
                        collector.add(pair[0]);
                    } else {
                        collector.add(pair[0] + "=" + pair[1]);
                    }
                }
            } catch (Throwable t) {
                log.warning("Failed to parse '" + pairFile + "': " + t);
            }
        }
    }

    /**
     * Returns the local path to the specified resource.
     */
    public File getLocalPath (String path)
    {
        return new File(getAppdir(), path);
    }

    /**
     * Returns true if we either have no version requirement, are running in a JVM that meets our
     * version requirements or have what appears to be a version of the JVM that meets our
     * requirements.
     */
    public boolean haveValidJavaVersion () {
        try {
            Configuration config = ConfigUtil.readConfigFile(getAppdir(), true);
            return config.haveValidJavaVersion();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Checks whether the app has a set of "optimum" JVM args that we wish to try first, detecting
     * whether the launch is successful and, if necessary, trying again without the optimum
     * arguments.
     */
    public boolean hasOptimumJvmArgs ()
    {
        return _optimumJvmArgs != null;
    }

    /**
     * Returns true if the app should attempt to run even if we have no Internet connection.
     */
    public boolean allowOffline ()
    {
        return _allowOffline;
    }

    /**
     * Attempts to redownload the <code>getdown.txt</code> file based on information parsed from a
     * previous call to {@link #init}.
     */
    public void attemptRecovery (StatusDisplay status)
        throws IOException
    {
        status.updateStatus("m.updating_metadata");
        ConfigUtil.downloadConfigFile(getAppdir(), getAppbase());
    }

    /**
     * Downloads and replaces the <code>getdown.txt</code> and <code>digest.txt</code> files with
     * those for the target version of our application.
     * @param targetVersion the target-version to update the meta-data to
     */
    public void updateMetadata(String targetVersion)
        throws IOException
    {
        _version = targetVersion;

        try {
            // now re-download our control files; we download the digest first so that if it fails,
            // our config file will still reference the old version and re-running the updater will
            // start the whole process over again
            digests = DigestsUtil.downloadDigests(getAppdir(), getAppbase(), getVersion(), _signers);
            ConfigUtil.downloadConfigFile(getAppdir(), getAppbase());

        } catch (IOException ex) {
            // if we are allowing offline execution, we want to allow the application to run in its
            // current form rather than aborting the entire process; to do this, we delete the
            // version.txt file and "trick" Getdown into thinking that it just needs to validate
            // the application as is; next time the app runs when connected to the internet, it
            // will have to rediscover that it needs updating and reattempt to update itself
            // @todo: ensure that this happens only if there is no internet-connection (ioexception might have other causes to)
            if (_allowOffline) {
                log.warning("Failed to update digest files.  Attempting offline operaton.", ex);
                try {
                    VersionUtil.setLocalVersion(getAppdir(), getVersion());
                } catch (IOException e) {
                    log.warning("(Re-)setting local version failed!  This probably isn't going to work.");
                }
            } else {
                throw ex;
            }
        }
    }

    /**
     * Invokes the process associated with this application definition.
     *
     * @param optimum whether or not to include the set of optimum arguments (as opposed to falling
     * back).
     */
    public Process createProcess (boolean optimum)
        throws IOException
    {
        // create our classpath
        StringBuilder cpbuf = new StringBuilder();
        for (Resource rsrc : getActiveResources().getResources(ResourceType.CODE_FILE)) {
            if (cpbuf.length() > 0) {
                cpbuf.append(File.pathSeparator);
            }
            cpbuf.append(rsrc.getLocalFile().getAbsolutePath());
        }

        ArrayList<String> args = new ArrayList<String>();

        // reconstruct the path to the JVM
        args.add(LaunchUtil.getJVMPath(getAppdir(), _windebug || optimum));

        // add the classpath arguments
        args.add("-classpath");
        args.add(cpbuf.toString());

        // we love our Mac users, so we do nice things to preserve our application identity
        if (RunAnywhere.isMacOS()) {
            args.add("-Xdock:icon=" + getLocalPath(_dockIconPath).getAbsolutePath());
            args.add("-Xdock:name=" + _name);
        }

        // pass along our proxy settings
        String proxyHost = SysProps.proxyHost();
        if (proxyHost != null) {
            args.add("-Dhttp.proxyHost=" + proxyHost);
            args.add("-Dhttp.proxyPort=" + SysProps.proxyPort());
        }

        // add the marker indicating the app is running in getdown
        args.add("-D" + Properties.GETDOWN + "=true");

        // pass along any pass-through arguments
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = (String)entry.getKey();
            if (key.startsWith(PROP_PASSTHROUGH_PREFIX)) {
                key = key.substring(PROP_PASSTHROUGH_PREFIX.length());
                args.add("-D" + key + "=" + entry.getValue());
            }
        }

        // add the JVM arguments
        for (String string : _jvmargs) {
            args.add(processArg(string));
        }

        // add the optimum arguments if requested and available
        if (optimum && _optimumJvmArgs != null) {
            for (String string : _optimumJvmArgs) {
                args.add(processArg(string));
            }
        }

        // add the arguments from extra.txt (after the optimum ones, in case they override them)
        for (String string : _txtJvmArgs) {
            args.add(processArg(string));
        }

        // add the application class name
        args.add(_class);

        // finally add the application arguments
        for (String string : _appargs) {
            args.add(processArg(string));
        }

        String[] envp = createEnvironment();
        String[] sargs = args.toArray(new String[args.size()]);
        log.info("Running " + StringUtil.join(sargs, "\n  "));

        return Runtime.getRuntime().exec(sargs, envp, getAppdir());
    }

    /**
     * If the application provided environment variables, combine those with the current
     * environment and return that in a style usable for {@link Runtime#exec(String, String[])}.
     * If the application didn't provide any environment variables, null is returned to just use
     * the existing environment.
     */
    protected String[] createEnvironment ()
    {
        List<String> envvar = new ArrayList<String>();
        fillAssignmentListFromPairs("env.txt", envvar);
        if (envvar.isEmpty()) {
            log.info("Didn't find any custom environment variables, not setting any.");
            return null;
        }

        List<String> envAssignments = new ArrayList<String>();
        for (String assignment : envvar) {
            envAssignments.add(processArg(assignment));
        }
        for (Entry<String, String> environmentEntry : System.getenv().entrySet()) {
            envAssignments.add(environmentEntry.getKey() + "=" + environmentEntry.getValue());
        }
        String[] envp = envAssignments.toArray(new String[envAssignments.size()]);
        log.info("Environment " + StringUtil.join(envp, "\n "));
        return envp;
    }

    /**
     * Runs this application directly in the current VM.
     */
    public void invokeDirect (JApplet applet)
    {
        // create a custom class loader
        ArrayList<URL> jars = new ArrayList<URL>();
        for (Resource rsrc : getActiveResources().getResources(ResourceType.CODE_FILE)) {
            try {
                jars.add(new URL("file", "", rsrc.getLocalFile().getAbsolutePath()));
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        URLClassLoader loader = new URLClassLoader(
            jars.toArray(new URL[jars.size()]),
            ClassLoader.getSystemClassLoader()) {
            @Override protected PermissionCollection getPermissions (CodeSource code) {
                Permissions perms = new Permissions();
                perms.add(new AllPermission());
                return perms;
            }
        };

        // configure any system properties that we can
        for (String jvmarg : _jvmargs) {
            if (jvmarg.startsWith("-D")) {
                jvmarg = processArg(jvmarg.substring(2));
                int eqidx = jvmarg.indexOf("=");
                if (eqidx == -1) {
                    log.warning("Bogus system property: '" + jvmarg + "'?");
                } else {
                    System.setProperty(jvmarg.substring(0, eqidx), jvmarg.substring(eqidx+1));
                }
            }
        }

        // pass along any pass-through arguments
        Map<String, String> passProps = new HashMap<String, String>();
        for (Map.Entry<Object, Object> entry : System.getProperties().entrySet()) {
            String key = (String)entry.getKey();
            if (key.startsWith(PROP_PASSTHROUGH_PREFIX)) {
                key = key.substring(PROP_PASSTHROUGH_PREFIX.length());
                passProps.put(key, (String)entry.getValue());
            }
        }
        // we can't set these in the above loop lest we get a ConcurrentModificationException
        for (Map.Entry<String, String> entry : passProps.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }

        // make a note that we're running in "applet" mode
        System.setProperty("applet", "true");

        try {
            Class<?> appclass = loader.loadClass(_class);
            String[] args = _appargs.toArray(new String[_appargs.size()]);
            Method main;
            try {
                // first see if the class has a special applet-aware main
                main = appclass.getMethod("main", JApplet.class, SA_PROTO.getClass());
                main.invoke(null, applet, args);
            } catch (NoSuchMethodException nsme) {
                main = appclass.getMethod("main", SA_PROTO.getClass());
                main.invoke(null, new Object[] { args });
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    /** Replaces the application directory and version in any argument. */
    protected String processArg (String arg)
    {
        arg = arg.replace("%APPDIR%", getAppdir().getAbsolutePath());
        arg = arg.replace("%VERSION%", getVersion());
        return arg;
    }

    /**
     * Loads the <code>digest.txt</code> file and verifies the contents of both that file and the
     * <code>getdown.text</code> file. Then it loads the <code>version.txt</code> and decides
     * whether or not the application needs to be updated or whether we can proceed to verification
     * and execution.
     *
     * @return true if the application needs to be updated, false if it is up to date and can be
     * verified and executed.
     *
     * @exception IOException thrown if we encounter an unrecoverable error while verifying the
     * metadata.
     */
    public String verifyMetadata (StatusDisplay status) throws IOException
    {
        log.info("Verifying application from: " + getAppbase());
        log.info("Version: " + getVersion());
        log.info("Class: " + _class);

        // this will read in the contents of the digest file and validate itself
        try {
            digests = DigestsUtil.readDigests(getAppdir(), getVersion());
        } catch (IOException ioe) {
            log.info("Failed to load digest: " + ioe.getMessage() + ". Attempting recovery...");
        }

        // if we have no version, then we are running in unversioned mode so we need to download
        // our digest.txt file on every invocation
        if (!VersionUtil.isValidVersion(getVersion())) {
            // make a note of the old meta-digest, if this changes we need to revalidate all of our
            // resources as one or more of them have also changed
            String olddig = (digests == null) ? "" : digests.getMetaDigest();
            try {
                status.updateStatus("m.checking");
                digests = DigestsUtil.downloadDigests(getAppdir(), getAppbase(), getVersion(), _signers);
                if (!olddig.equals(digests.getMetaDigest())) {
                    log.info("Unversioned digest changed. Revalidating...");
                    status.updateStatus("m.validating");
                    clearValidationMarkers();
                }
            } catch (IOException ioe) {
                log.warning("Failed to refresh non-versioned digest: " +
                            ioe.getMessage() + ". Proceeding...");
            }
        }

        // regardless of whether we're versioned, if we failed to read the digest from disk, try to
        // redownload the digest file and give it another good college try; this time we allow
        // exceptions to propagate up to the caller as there is nothing else we can do
        if (digests == null) {
            status.updateStatus("m.updating_metadata");
            digests = DigestsUtil.downloadDigests(getAppdir(), getAppbase(), getVersion(), _signers);
        }

        // now verify the contents of our main config file
        Resource crsrc = getConfigResource();
        if (!DigestsUtil.validateResourceDigest(crsrc, digests)) {
            status.updateStatus("m.updating_metadata");
            // attempt to redownload both of our metadata files; again we pass errors up to our
            // caller because there's nothing we can do to automatically recover
            ConfigUtil.downloadConfigFile(getAppdir(), getAppbase());

            digests = DigestsUtil.downloadDigests(getAppdir(), getAppbase(), getVersion(), _signers);
            // revalidate everything if we end up downloading new metadata
            clearValidationMarkers();
            // if the new copy validates, reinitialize ourselves; otherwise report baffling hoseage
            if (DigestsUtil.validateResourceDigest(crsrc, digests)) {
                init(true);
            } else {
                log.warning("config-file failed to validate even after redownloading. " +
                            "Blindly forging onward.");
            }
        }

        // start by assuming we are happy with our version
        String latestVersion = getVersion();

        // if we are a versioned application, check for latest version
        if (VersionUtil.isValidVersion(getVersion())) {
            latestVersion = VersionUtil.getLatestVersion(getAppdir(), ConfigUtil.readConfigFile(getAppdir(), false).getAppbase());
        }

        if (VersionUtil.compareVersions(latestVersion, getVersion()) > 0) {
            clearValidationMarkers();
        }

        return latestVersion;
    }

    /**
     * Verifies the code and media resources associated with this application. A list of resources
     * that do not exist or fail the verification process will be returned. If all resources are
     * ready to go, null will be returned and the application is considered ready to run.
     *
     * @param alreadyValid if non-null a 1 element array that will have the number of "already
     * validated" resources filled in.
     * @param unpacked a set to populate with unpacked resources.
     */
    public List<Resource> verifyResources (
        ProgressObserver obs, int[] alreadyValid, Set<Resource> unpacked)
            throws InterruptedException
    {
        Collection<Resource> rsrcs = getActiveResources().getResources(ResourceType.CONFIGURABLE_RESOURCES);
        List<Resource> failures = new ArrayList<Resource>();

        // total up the file size of the resources to validate
        long totalSize = 0L;
        for (Resource rsrc : rsrcs) {
            totalSize += rsrc.getLocalFile().length();
        }

        MetaProgressObserver mpobs = new MetaProgressObserver(obs, totalSize);
        boolean noUnpack = SysProps.noUnpack();
        for (Resource rsrc : rsrcs) {
            if (Thread.interrupted()) {
                throw new InterruptedException("m.applet_stopped");
            }
            mpobs.startElement(rsrc.getLocalFile().length());

            if (rsrc.isMarkedValid()) {
                if (alreadyValid != null) {
                    alreadyValid[0]++;
                }
                mpobs.progress(100);
                continue;
            }

            try {
                if (DigestsUtil.validateResourceDigest(rsrc, digests, mpobs)) {
                    // unpack this resource if appropriate
                    if (noUnpack || !rsrc.shouldUnpack()) {
                        // finally note that this resource is kosher
                        rsrc.markAsValid();
                        continue;
                    }
                    if (rsrc.unpack()) {
                        unpacked.add(rsrc);
                        rsrc.markAsValid();
                        continue;
                    }
                    log.info("Failure unpacking resource", "rsrc", rsrc);
                }

            } catch (Exception e) {
                log.info("Failure validating resource. Requesting redownload...",
                    "rsrc", rsrc, "error", e);

            } finally {
                mpobs.progress(100);
            }
            failures.add(rsrc);
        }

        return (failures.size() == 0) ? null : failures;
    }

    /**
     * Unpacks the resources that require it (we know that they're valid).
     *
     * @param unpacked a set of resources to skip because they're already unpacked.
     */
    public void unpackResources (ProgressObserver obs, Set<Resource> unpacked)
        throws InterruptedException
    {
        Collection<Resource> rsrcs = getActiveResources().getResources(ResourceType.RESOURCE_ARCHIVE);

        // total up the file size of the resources to unpack
        long totalSize = 0L;
        for (Iterator<Resource> it = rsrcs.iterator(); it.hasNext(); ) {
            Resource rsrc = it.next();
            if (rsrc.shouldUnpack() && !unpacked.contains(rsrc)) {
                totalSize += rsrc.getLocalFile().length();
            } else {
                it.remove();
            }
        }

        MetaProgressObserver mpobs = new MetaProgressObserver(obs, totalSize);
        for (Resource rsrc : rsrcs) {
            if (Thread.interrupted()) {
                throw new InterruptedException("m.applet_stopped");
            }
            mpobs.startElement(rsrc.getLocalFile().length());
            if (!rsrc.unpack()) {
                log.info("Failure unpacking resource", "rsrc", rsrc);
            }
            mpobs.progress(100);
        }
    }

    /**
     * Clears all validation marker files.
     */
    private void clearValidationMarkers() {
        for (Resource resource : getActiveResources().getResources(ResourceType.CONFIGURABLE_RESOURCES)) {
            resource.clearMarker();
        }
    }

    /**
     * Returns the version number for the application.  Should only be called after successful
     * return of verifyMetadata.
     */
    public String getVersion () {
        return _version;
    }

    public URL getAppbase() {
        return VersionUtil.createVersionedUrl(_appbase, getVersion());
    }


    /**
     * @return true if gettingdown.lock was unlocked, already locked by this application or if
     * we're not locking at all.
     */
    public synchronized boolean lockForUpdates ()
    {
        if (_lock != null && _lock.isValid()) {
            return true;
        }
        try {
            _lockChannel = new RandomAccessFile(getLocalPath("gettingdown.lock"), "rw").getChannel();
        } catch (FileNotFoundException e) {
            log.warning("Unable to create lock file", "message", e.getMessage(), e);
            return false;
        }
        try {
            _lock = _lockChannel.tryLock();
        } catch (IOException e) {
            log.warning("Unable to create lock", "message", e.getMessage(), e);
            return false;
        } catch (OverlappingFileLockException e) {
            log.warning("The lock is held elsewhere in this JVM", e);
            return false;
        }
        log.info("Able to lock for updates: " + (_lock != null));
        return _lock != null;
    }

    /**
     * Release gettingdown.lock
     */
    public synchronized void releaseLock ()
    {
        if (_lock != null) {
            log.info("Releasing lock");
            try {
                _lock.release();
            } catch (IOException e) {
                log.warning("Unable to release lock", "message", e.getMessage(), e);
            }
            try {
                _lockChannel.close();
            } catch (IOException e) {
                log.warning("Unable to close lock channel", "message", e.getMessage(), e);
            }
            _lockChannel = null;
            _lock = null;
        }
    }

    private Tracking tracking = new Tracking();
    private File _appdir;
    protected String _appid;
    private Digests digests;

    private String _version = VersionUtil.NO_VERSION;
    private URL _appbase;
    protected String _class;
    protected String _name;
    protected String _dockIconPath;
    protected boolean _windebug;
    protected boolean _allowOffline;

    protected String _javaLocation;

    private ResourceGroup _resources = new ResourceGroup();

    protected List<String> _jvmargs = new ArrayList<String>();
    protected List<String> _appargs = new ArrayList<String>();

    protected String[] _extraJvmArgs;
    protected String[] _extraAppArgs;

    protected String[] _optimumJvmArgs;

    protected List<String> _txtJvmArgs = new ArrayList<String>();

    protected List<Certificate> _signers;

    /** Locks gettingdown.lock in the app dir. Held the entire time updating is going on.*/
    protected FileLock _lock;

    /** Channel to the file underlying _lock.  Kept around solely so the lock doesn't close. */
    protected FileChannel _lockChannel;

    protected static final String[] SA_PROTO = ArrayUtil.EMPTY_STRING;
}
