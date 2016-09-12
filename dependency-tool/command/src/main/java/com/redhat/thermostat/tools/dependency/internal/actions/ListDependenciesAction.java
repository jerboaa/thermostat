/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.tools.dependency.internal.actions;

import com.redhat.thermostat.collections.graph.DepthFirstSearch;
import com.redhat.thermostat.collections.graph.Graph;
import com.redhat.thermostat.collections.graph.Node;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.tools.dependency.internal.DependencyGraphBuilder;
import com.redhat.thermostat.tools.dependency.internal.PathProcessorHandler;
import com.redhat.thermostat.collections.graph.TopologicalSort;
import com.redhat.thermostat.tools.dependency.internal.Utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 */
public class ListDependenciesAction {
    public static void execute(PathProcessorHandler handler, String library, CommandContext ctx) {
        execute(handler, library, ctx, false);
    }

    public static void execute(PathProcessorHandler handler, String library, CommandContext ctx, boolean linksTo) {
        Path path = Paths.get(library);
        if (!path.toFile().exists()) {
            Utils.getInstance().cannotAccessPathError(ctx, path);
            return;
        }

        DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder();
        handler.process(graphBuilder);

        Graph dependencyGraph = null;
        if (linksTo) {
            dependencyGraph = graphBuilder.buildReverse();
        } else {
            dependencyGraph = graphBuilder.build();
        }

        DepthFirstSearch dfs = new DepthFirstSearch(dependencyGraph);

        Node node = graphBuilder.getNode(path);

        TopologicalSort topological = new TopologicalSort();
        dfs.search(node, topological);

        List<Node> deps = new ArrayList<>(topological.getOrdered());
        deps.remove(node);

        Collections.sort(deps, new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                String name1 = o1.getName();
                String name2 = o2.getName();

                return name1.compareTo(name2);
            }
        });

        String plugin = path.getParent().getFileName().toString();

        Utils.getInstance().printHeader(ctx, path.getFileName() + " (" + plugin + ")");
        for (Node dep : deps) {
            Utils.getInstance().print(ctx, dep);
        }
    }
}
