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
    private final Map<String, Path> exports;
    private final Map<BundleInformation, List<String>> importsMap;
    private final Map<BundleInformation, Set<BundleInformation>> outgoing;
    private final Map<BundleInformation, Set<BundleInformation>> incoming;
    private final Logger logger = LoggingUtils.getLogger(DependencyResolver.class);


    public DependencyResolver(List<Path> paths) {
        this.nodes = new HashMap<>();
        this.exports = new HashMap<>();
        this.importsMap = new HashMap<>();
        this.outgoing = new HashMap<>();
        this.incoming = new HashMap<>();
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
            List<String> bImports = new ArrayList<>();
            Manifest m = new JarFile(jar.toFile()).getManifest();
            Attributes a = m.getMainAttributes();
            String bundleName = a.getValue(Constants.BUNDLE_SYMBOLICNAME);
            String bundleVersion = a.getValue(Constants.BUNDLE_VERSION);
            String bundleImports = a.getValue(Constants.IMPORT_PACKAGE);
            String bundleExports = a.getValue(Constants.EXPORT_PACKAGE);
            if (bundleExports != null) {
                List<String> exports = parseHeader(bundleExports);
                for (String dep : exports) {
                    this.exports.put(dep, jar);
                }
            }
            if (bundleImports != null) {
                List<String> imports = parseHeader(bundleImports);
                for (String dep : imports) {
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
            List<String> bundleImports = importsMap.get(source);
            for (String dep : bundleImports) {
                Path who = exports.get(dep);
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

    private List<String> parseHeader(String header) {
        header = header.concat("\0");
        List<String> packages = new ArrayList<>();
        int index = 0;
        int start = 0;

        boolean invalid = false;
        boolean inQuotes = false;
        boolean newSubstring = true;

        while (index < header.length()) {
            char charAtIndex = header.charAt(index);
            if (charAtIndex == '\"') {
                inQuotes = !inQuotes;
            }
            if (!inQuotes) {
                if (charAtIndex == '=') {
                    invalid = true;
                    newSubstring = false;
                } else if (charAtIndex == ';' || charAtIndex == ',' || charAtIndex == '\0') {
                    if (!invalid && !newSubstring) {
                        packages.add(header.substring(start, index));
                    }
                    start = index + 1;
                    invalid = false;
                    newSubstring = true;
                } else if (newSubstring) {
                    if (!Character.isJavaIdentifierStart(charAtIndex)) {
                        invalid = true;
                    }
                    newSubstring = false;
                } else if (!Character.isJavaIdentifierPart(charAtIndex) && charAtIndex != '.') {
                    invalid = true;
                }
            }
            index++;
        }
        return packages;
    }
}
