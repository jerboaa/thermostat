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

import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.launcher.BundleInformation;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Stack;
import java.util.Collections;

public class DependencyManager {

    private final DependencyResolver handler;
    private final List<BundleInformation> discovered;
    private final Map<BundleInformation, Set<BundleInformation>> outgoing;
    private final Map<BundleInformation, Set<BundleInformation>> incoming;
    private List<Path> locations;

    public DependencyManager(CommonPaths paths) {
        File systemPluginRoot = paths.getSystemPluginRoot();
        File userPluginRoot = paths.getUserPluginRoot();
        File systemLibRoot = paths.getSystemLibRoot();
        locations = new ArrayList<>();
        locations.add(systemPluginRoot.toPath());
        locations.add(userPluginRoot.toPath());
        // System lib root shall not be scanned recursively.
        for (File f: systemLibRoot.listFiles()) {
            locations.add(f.toPath());
        }
        handler = new DependencyResolver(locations);
        outgoing = new HashMap<>(handler.getOutgoing());
        incoming = new HashMap<>(handler.getIncoming());
        discovered = new ArrayList<>();
    }

    /*****
     * Sort the bundle dependencies topologically, that is traverse the dependency graph
     * starting from the given node ensuring that there are no incoming dependencies for each
     * bundle.
     *
     * Uses Kahn's Algorithm
     *
     * @param b Bundle to start from
     * @return A topologically sorted list of bundles
     */
    public List<BundleInformation> getDependencies(BundleInformation b) {
        if (outgoing == null || incoming == null || !outgoing.containsKey(b)) {
            return new LinkedList<>();
        }
        buildSubgraph(b);
        LinkedList<BundleInformation> sorted = new LinkedList<>();
        LinkedList<BundleInformation> queue = new LinkedList<>();
        queue.add(b);
        while (!queue.isEmpty()) {
            BundleInformation n = queue.remove(0);
            sorted.addLast(n);
            Set<BundleInformation> source = getOutgoingRelationShips(n);
            List<BundleInformation> outgoingRels = new LinkedList<>(source);
            for (BundleInformation bundle : outgoingRels) {
                removeEdge(n, bundle);
                if (hasNoIncomingEdge(bundle)) {
                    queue.addLast(bundle);
                }
            }
        }
        return sorted;
    }

    private Set<BundleInformation> getOutgoingRelationShips(BundleInformation key) {
        if (outgoing.get(key) == null) {
            return Collections.emptySet();
        }
        return outgoing.get(key);
    }

    private Set<BundleInformation> getIncomingRelationShips(BundleInformation key) {
        if (incoming.get(key) == null) {
            return Collections.emptySet();
        }
        return incoming.get(key);
    }

    private void removeEdge(BundleInformation from, BundleInformation to) {
        outgoing.get(from).remove(to);
        incoming.get(to).remove(from);
    }

    private boolean hasNoIncomingEdge(BundleInformation key) {
        if (getIncomingRelationShips(key).isEmpty()) {
            return true;
        }
        for (BundleInformation b : getIncomingRelationShips(key)) {
            if (discovered.contains(b)) {
                return false;
            }
        }
        return true;
    }

    private void buildSubgraph(BundleInformation start) {
        Stack<BundleInformation> stack = new Stack<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            BundleInformation current = stack.pop();
            discovered.add(current);
            if (outgoing.get(current) == null) {
                continue;
            }
            for (BundleInformation b : outgoing.get(current)) {
                if (!discovered.contains(b)) {
                    stack.push(b);
                }
            }
        }
    }

    protected List<Path> getLocations() {
        return locations;
    }

}
