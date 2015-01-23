/*
 * Copyright 2012-2015 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.launcher.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.launch.Framework;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.BundleInformation;
import com.redhat.thermostat.launcher.BundleManager;
import com.redhat.thermostat.shared.config.CommonPaths;

public class BundleManagerImpl extends BundleManager {

    private static final Logger logger = LoggingUtils.getLogger(BundleManagerImpl.class);

    // Bundle Name and version -> path (with symlinks resolved)
    // Match FrameworkProvider which uses canonical/symlink-resolved paths. If
    // there is a mismatch, there are going to be clashes with trying to load
    // two files (which are really the same) with the same bundle symbolic
    // name/version. See
    // http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=1514
    private final Map<BundleInformation, Path> known;
    private CommonPaths paths;
    private boolean printOSGiInfo = false;
    private boolean ignoreBundleVersions = false;
    private BundleLoader loader;

    BundleManagerImpl(CommonPaths paths) throws FileNotFoundException, IOException {
        known = new HashMap<>();

        this.paths = paths;
        loader = new BundleLoader();

        scanForBundles();
    }

    private void scanForBundles() {
        long t1 = System.nanoTime();

        try {
            for (File root : new File[] { paths.getSystemLibRoot(), paths.getSystemPluginRoot() }) {
                Files.walkFileTree(root.toPath(), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.toFile().getName().endsWith(".jar")) {
                            try (JarFile jf = new JarFile(file.toFile())) {
                                Manifest mf = jf.getManifest();
                                String name = mf.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                                String version = mf.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
                                if (name == null || version == null) {
                                    logger.config("file " + file.toString() + " is missing osgi metadata; wont be usable for dependencies");
                                } else {
                                    BundleInformation info = new BundleInformation(name, version);
                                    Path old = known.get(info);
                                    // Path is completely resolved. First one wins.
                                    if (old == null) {
                                        known.put(info, file.toRealPath());
                                    } else {
                                        if (!old.equals(file.toRealPath())) {
                                            logger.warning("bundles " + old + " and " + file + " both provide " + info);
                                        }
                                        // leave old
                                    }
                                }
                            } catch (IOException e) {
                                logger.severe("Error in reading " + file);
                                // continue with other files, even if one file is broken
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error scanning bundles for metadata", e);
        }

        long t2 = System.nanoTime();
        if (printOSGiInfo) {
            logger.fine("Found: " + known.size() + " bundles");
            logger.fine("Took " + (t2 -t1) + "ns");
        }

        for (Entry<BundleInformation, Path> bundles : known.entrySet()) {
            logger.finest(bundles.getKey().toString() + " is at " + bundles.getValue().toString());
        }
    }

    /** For TESTS only: explicitly specify known bundles */
    void setKnownBundles(Map<BundleInformation,Path> knownData) {
        known.clear();
        for (Entry<BundleInformation, Path> entry : knownData.entrySet()) {
            known.put(entry.getKey(), entry.getValue());
        }
    }

    /* Used via reflection from launcher */
    public void setPrintOSGiInfo(boolean printOSGiInfo) {
        this.printOSGiInfo = printOSGiInfo;
        loader.setPrintOSGiInfo(printOSGiInfo);
    }

    /**
     * Indicates that versions in thermostat-specific config files (including
     * thermostat-plugin.xml files) should be ignored and the latest version
     * used.
     * <p>
     * This does not change OSGi's requirements; if OSGi bundles need specific
     * versions and the latest version is not within the asked range, things
     * will break.
     */
    /* Used via reflection from launcher */
    public void setIgnoreBundleVersions(boolean ignore) {
        this.ignoreBundleVersions = ignore;
    }

    @Override
    public void loadBundlesByName(List<BundleInformation> bundles) throws BundleException, IOException {
        List<String> paths = new ArrayList<>();
        for (BundleInformation info : bundles) {
            Path bundlePath = null;

            if (ignoreBundleVersions) {
                bundlePath = findLatestVersion(info.getName());
            } else {
                bundlePath = known.get(info);
            }

            if (bundlePath == null) {
                logger.warning("no known bundle matching " + info.toString());
                continue;
            }
            paths.add(bundlePath.toFile().getCanonicalFile().toURI().toString());
        }
        loadBundlesByPath(paths);
    }

    private Path findLatestVersion(String bundleSymbolicName) {
        BundleInformation bestBundleInformation = null;
        Version bestVersion = null;

        Path bundlePath = null;

        for (Entry<BundleInformation, Path> entry: known.entrySet()) {
            if (bundleSymbolicName.equals(entry.getKey().getName())) {
                Version version = Version.parseVersion(entry.getKey().getVersion());
                if (bestVersion == null || version.compareTo(bestVersion) > 0) {
                    bestVersion = version;
                    bestBundleInformation = entry.getKey();
                }
            }
        }
        if (bestBundleInformation != null) {
            bundlePath = known.get(bestBundleInformation);
        }

        logger.fine("Best match for " + bundleSymbolicName + " is " + bestBundleInformation);

        return bundlePath;
    }

    /* package private for testing only */
    void loadBundlesByPath(List<String> requiredBundles) throws BundleException, IOException {
        Framework framework = getFramework(this.getClass());
        BundleContext context = framework.getBundleContext();

        List<String> bundlesToLoad = new ArrayList<>();
        if (requiredBundles != null) {
            for (String resource : requiredBundles) {
                if (!isBundleActive(context, resource)) {
                    bundlesToLoad.add(resource);
                }
            }
        }
        loader.installAndStartBundles(framework, bundlesToLoad);
    }

    private boolean isBundleActive(BundleContext context, String location) {
        Bundle bundle = context.getBundle(location);
        return (bundle != null) && (bundle.getState() == Bundle.ACTIVE);
    }

    private Framework getFramework(Class<?> cls) {
        return (Framework) FrameworkUtil.getBundle(cls).getBundleContext().getBundle(0);
    }

    @Override
    public CommonPaths getCommonPaths() {
        return paths;
    }
}

