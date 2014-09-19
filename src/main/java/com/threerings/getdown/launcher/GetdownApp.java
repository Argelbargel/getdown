//
// Getdown - application installer, patcher and launcher
// Copyright (C) 2004-2014 Three Rings Design, Inc.
// https://raw.github.com/threerings/getdown/master/LICENSE

package com.threerings.getdown.launcher;

import java.awt.Container;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.ArrayUtil;
import com.samskivert.util.RunAnywhere;
import com.samskivert.util.StringUtil;

import com.threerings.getdown.data.SysProps;
import static com.threerings.getdown.Log.log;

/**
 * The main application entry point for Getdown.
 */
public class GetdownApp
{
    public static void main (String[] argv)
    {
        int aidx = 0;
        List<String> args = Arrays.asList(argv);

        // check for app dir in a sysprop and then via argv
        String adarg = SysProps.appDir();
        if (StringUtil.isBlank(adarg)) {
            if (args.isEmpty()) {
                System.err.println("Usage: java -jar getdown.jar app_dir [app_id] [app args]");
                System.exit(-1);
            }
            adarg = args.get(aidx++);
        }

        // check for an app identifier in a sysprop and then via argv
        String appId = SysProps.appId();
        if (StringUtil.isBlank(appId) && aidx < args.size()) {
            appId = args.get(aidx++);
        }

        // pass along anything after that as app args
        String[] appArgs = (aidx >= args.size()) ? null :
            args.subList(aidx, args.size()).toArray(ArrayUtil.EMPTY_STRING);

        // ensure a valid directory was supplied
        File appDir = new File(adarg);
        if (!appDir.exists() || !appDir.isDirectory()) {
            log.warning("Invalid app_dir '" + adarg + "'.");
            System.exit(-1);
        }

        // pipe our output into a file in the application directory
        if (!SysProps.noLogRedir()) {
            File logFile = new File(appDir, "launcher.log");
            try {
                PrintStream logOut = new PrintStream(
                    new BufferedOutputStream(new FileOutputStream(logFile)), true);
                System.setOut(logOut);
                System.setErr(logOut);
            } catch (IOException ioe) {
                log.warning("Unable to redirect output to '" + logFile + "': " + ioe);
            }
        }

        List<Certificate> signers = new ArrayList<Certificate>();
        Object[] classSigners = GetdownApp.class.getSigners();
        if (classSigners != null) {
           for (Object signer : classSigners) {
              if (signer instanceof Certificate) {
                 Certificate c = ((Certificate) signer);
                 signers.add(c);
              }
           }
        }

        // record a few things for posterity
        SysProps.logJVMProperties(log);
        if (!signers.isEmpty()) {
           log.info("class " + GetdownApp.class.getSimpleName() + " is signed. Using signers to validate resources");
        }
        log.info("---------------------------------------------");


        try {
            GetdownController app = new GetdownController(appDir, appId, signers, null, appArgs) {
                @Override
                protected Container createContainer () {
                    // create our user interface, and display it
                    String title = StringUtil.isBlank(_ifc.name) ? "" : _ifc.name;
                    if (_frame == null) {
                        _frame = new JFrame(title);
                        _frame.addWindowListener(new WindowAdapter() {
                            @Override
                            public void windowClosing (WindowEvent evt) {
                                handleWindowClose();
                            }
                        });
                        _frame.setResizable(false);
                    } else {
                        _frame.setTitle(title);
                        _frame.getContentPane().removeAll();
                    }

                    if (_ifc.iconImages != null) {
                        ArrayList<Image> icons = new ArrayList<Image>();
                        for (String path : _ifc.iconImages) {
                            Image img = loadImage(path);
                            if (img == null) {
                                log.warning("Error loading icon image", "path", path);
                            } else {
                                icons.add(img);
                            }
                        }
                        if (icons.isEmpty()) {
                            log.warning("Failed to load any icons", "iconImages", _ifc.iconImages);
                        } else {
                            SwingUtil.setFrameIcons(_frame, icons);
                        }
                    }

                    _frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                    return _frame.getContentPane();
                }
                @Override
                protected void showContainer () {
                    if (_frame != null) {
                        _frame.pack();
                        SwingUtil.centerWindow(_frame);
                        _frame.setVisible(true);
                    }
                }
                @Override
                protected void disposeContainer () {
                    if (_frame != null) {
                        _frame.dispose();
                        _frame = null;
                    }
                }
                @Override
                protected void showDocument (String url) {
                    String[] cmdarray;
                    if (RunAnywhere.isWindows()) {
                        String osName = SysProps.osName();
                        if (osName.contains("9") || osName.contains("Me")) {
                            cmdarray = new String[] {
                                "command.com", "/c", "start", "\"" + url + "\"" };
                        } else {
                            cmdarray = new String[] {
                                "cmd.exe", "/c", "start", "\"\"", "\"" + url + "\"" };
                        }
                    } else if (RunAnywhere.isMacOS()) {
                        cmdarray = new String[] { "open", url };
                    } else { // Linux, Solaris, etc.
                        cmdarray = new String[] { "firefox", url };
                    }
                    try {
                        Runtime.getRuntime().exec(cmdarray);
                    } catch (Exception e) {
                        log.warning("Failed to open browser.", "cmdarray", cmdarray, e);
                    }
                }
                @Override
                protected void exit (int exitCode) {
                    System.exit(exitCode);
                }
                protected JFrame _frame;
            };
            app.start();

        } catch (Exception e) {
            log.warning("main() failed.", e);
        }
    }
}
