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

package com.redhat.thermostat.tools.dependency.internal;

import com.redhat.thermostat.collections.graph.Graph;
import com.redhat.thermostat.collections.graph.HashGraph;
import com.redhat.thermostat.collections.graph.Node;
import com.redhat.thermostat.collections.graph.Relationship;
import com.redhat.thermostat.common.utils.LoggingUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 */
public class DependencyGraphBuilder extends PathProcessor {

    private final Logger logger = LoggingUtils.getLogger(DependencyGraphBuilder.class);

    private Map<Dependency, Path> exports;

    private Map<Path, Node> nodes;

    private Graph graph;

    public DependencyGraphBuilder() {
        exports = new HashMap<>();
        graph = new HashGraph();
        nodes = new HashMap<>();
    }

    @Override
    protected void process(Path path) {
        scanManifest(path);
    }

    private void scanManifest(Path jar) {
        try {
            Manifest manifest = new JarFile(jar.toFile()).getManifest();

            List<Dependency> thisExports = new ArrayList<>();
            List<Dependency> thisImports = new ArrayList<>();

            Attributes attributes = manifest.getMainAttributes();

            String exports = attributes.getValue(BundleProperties.EXPORT.id());
            if (exports != null) {
                List<Dependency> dependencies = OSGIManifestScanner.parseHeader(exports);
                for (Dependency dependency : dependencies) {
                    this.exports.put(dependency, jar);
                    thisExports.add(dependency);
                }
            }

            String imports = attributes.getValue(BundleProperties.IMPORT.id());
            if (imports != null) {
                List<Dependency> dependencies = OSGIManifestScanner.parseHeader(imports);
                for (Dependency dependency : dependencies) {
                    thisImports.add(dependency);
                }
            }

            // create the node relative to this entry
            Node node = new Node(jar.toString());
            node.setProperty(BundleProperties.PATH.id(), jar);
            node.setProperty(BundleProperties.EXPORT.id(), thisExports);
            node.setProperty(BundleProperties.IMPORT.id(), thisImports);

            nodes.put(jar, node);

        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        }
    }

    public Node getNode(Path path) {
        return nodes.get(path);
    }

    public Graph buildReverse() {
        return build(true);
    }

    private Graph build(boolean swap) {
        for (Node source : nodes.values()) {
            List<Dependency> thisImports = source.getProperty(BundleProperties.IMPORT.id());
            Path location;
            for (Dependency dep : thisImports) {
                location = exports.get(dep);
                if (location == null) {
                    for (Dependency bundle : exports.keySet()) {
                        if (OSGIManifestScanner.isVersionRange(dep.getVersion())) {
                            if ((bundle.getName().equals(dep.getName()))) {
                                location = exports.get(OSGIManifestScanner.parseAndCheckBounds(
                                        dep.getVersion(), bundle.getVersion(), bundle));
                            }
                        } else {
                            // If a version range is specified as a single version, it must be interpreted
                            // as the range [version, infinity) according to the osgi specification.
                            if ((bundle.getName().equals(dep.getName()))) {
                                location = exports.get(OSGIManifestScanner.parseAndCheckBounds(
                                        "[" + dep.getVersion() + "," + Integer.MAX_VALUE + ")",
                                        bundle.getVersion(), bundle));
                            }
                        }
                        if (location != null) {
                            break;
                        }
                    }
                }
                if (location != null) {
                    Node destination = nodes.get(location);
                    // some package seems to have dependencies on themselves, if
                    // we create a relationship we will cause a cycle
                    if (source.equals(destination)) {
                        continue;
                    }
                    Relationship relationship;
                    Set<Relationship> relationships;
                    if (swap) {
                        relationship = new Relationship(destination, "<-", source);
                        relationships = graph.getRelationships(destination);
                    } else {
                        relationship = new Relationship(source, "->", destination);
                        relationships = graph.getRelationships(source);
                    }

                    if (relationships == null || !relationships.contains(relationship)) {
                        graph.addRelationship(relationship);
                    }
                }
            }
        }

        return graph;
    }

    public Graph build() {
        return build(false);
    }
}

