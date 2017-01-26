/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.BundleInformation;
import com.redhat.thermostat.launcher.PluginDirFileVisitor;

import org.osgi.framework.Constants;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.FileVisitor;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DependencyResolver {

    private final Map<Path, BundleInformation> nodes;
    private final Map<BundleInformation, Path> exports;
    private final Map<BundleInformation, List<BundleInformation>> importsMap;
    private final Map<BundleInformation, Set<BundleInformation>> outgoing;
    private final Map<BundleInformation, Set<BundleInformation>> incoming;
    // Provides a mapping of package names to the specific <name, version> pairs that provide it.
    private final Map<String, List<BundleInformation>> providedVersions;
    private final Logger logger = LoggingUtils.getLogger(DependencyResolver.class);
    private final MetadataHandler handler;


    public DependencyResolver(List<Path> paths) {
        this.nodes = new HashMap<>();
        this.exports = new HashMap<>();
        this.importsMap = new HashMap<>();
        this.outgoing = new HashMap<>();
        this.incoming = new HashMap<>();
        this.handler = new MetadataHandler();
        this.providedVersions = new HashMap<>();

        try {
            final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.jar");
            FileVisitor visitor = new PluginDirFileVisitor() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path name = file.getFileName();
                    if (name != null && matcher.matches(name)) {
                        process(file);
                    }

                    return FileVisitResult.CONTINUE;
                }
            };
            for (Path p : paths) {
                Files.walkFileTree(p, visitor);
            }
        }
        catch (IOException e){
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
        buildGraph();
    }

    protected Map<BundleInformation, Set<BundleInformation>> getOutgoing() {
        return outgoing;
    }

    protected Map<BundleInformation, Set<BundleInformation>> getIncoming() {
        return incoming;
    }

    protected void process(Path jar) {
        try {
            List<BundleInformation> bImports = new ArrayList<>();
            Manifest m = new JarFile(jar.toFile()).getManifest();
            Attributes a = m.getMainAttributes();
            String bundleName = a.getValue(Constants.BUNDLE_SYMBOLICNAME);
            String bundleVersion = a.getValue(Constants.BUNDLE_VERSION);
            String bundleImports = a.getValue(Constants.IMPORT_PACKAGE);
            String bundleExports = a.getValue(Constants.EXPORT_PACKAGE);
            if (bundleExports != null) {
                List<BundleInformation> exports = handler.parseHeader(bundleExports);
                for (BundleInformation dep : exports) {
                    this.exports.put(dep, jar);
                    if (providedVersions.get(dep.getName()) == null) {
                        providedVersions.put(dep.getName(), new ArrayList<BundleInformation>());
                    }
                    providedVersions.get(dep.getName()).add(dep);
                }
            }
            if (bundleImports != null) {
                List<BundleInformation> imports = handler.parseHeader(bundleImports);
                for (BundleInformation dep : imports) {
                    bImports.add(dep);
                }
            }
            BundleInformation bundle = new BundleInformation(bundleName, bundleVersion);
            importsMap.put(bundle, bImports);
            nodes.put(jar, bundle);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    private void buildGraph() {
        for (BundleInformation source : nodes.values()) {
            List<BundleInformation> bundleImports = importsMap.get(source);
            for (BundleInformation dep : bundleImports) {
                Path who = exports.get(dep);
                if (who == null && providedVersions.get(dep.getName()) != null) {
                    for (BundleInformation export : providedVersions.get(dep.getName())) {
                        if (handler.isVersionRange(dep.getVersion())) {
                            who = exports.get(handler.parseAndCheckBounds(
                                    dep.getVersion(), export.getVersion(), export));
                        } else {
                            // If a version range is specified as a single version, it must be interpreted
                            // as the range [version, infinity) according to the osgi specification.
                            who = exports.get(handler.parseAndCheckBounds(
                                    "[" + dep.getVersion() + "," + Integer.MAX_VALUE + ")",
                                    export.getVersion(), export));
                        }
                        if (who != null) {
                            break;
                        }
                    }
                }
                if (who != null) {
                    BundleInformation destination = nodes.get(who);
                    if (source.equals(destination)) {
                        continue;
                    }
                    if (outgoing.get(source) == null) {
                        outgoing.put(source, new HashSet<BundleInformation>());
                    }
                    if (incoming.get(destination) == null) {
                        incoming.put(destination, new HashSet<BundleInformation>());
                    }
                    outgoing.get(source).add(destination);
                    incoming.get(destination).add(source);
                }
            }
        }
    }
}
