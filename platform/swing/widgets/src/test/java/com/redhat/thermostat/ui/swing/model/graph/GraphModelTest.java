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

package com.redhat.thermostat.ui.swing.model.graph;

import com.redhat.thermostat.collections.graph.BreadthFirstSearch;
import com.redhat.thermostat.collections.graph.DefaultTraversalListener;
import com.redhat.thermostat.collections.graph.Graph;
import com.redhat.thermostat.collections.graph.HashGraph;
import com.redhat.thermostat.collections.graph.Node;
import com.redhat.thermostat.collections.graph.TraversalListener;
import com.redhat.thermostat.ui.swing.model.Model;
import com.redhat.thermostat.ui.swing.model.ModelListener;
import com.redhat.thermostat.ui.swing.model.Trace;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.redhat.thermostat.ui.swing.model.graph.GraphModel.SAMPLE_COUNT_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 */
public class GraphModelTest {

    @Test
    public void populateGraphCorrectly() {
        Graph graph = new HashGraph();

        GraphModel model = new GraphModel("GraphModelTest", graph);
        populate(model);

        Node root = model.getCache().get("GraphModelTest");
        TraversalListener listener = new DefaultTraversalListener() {};
        BreadthFirstSearch bfs = new BreadthFirstSearch(graph);
        bfs.search(root, listener);
        Map<Node, Node> parents = bfs.getPayload().getParents();

        List<String> path = new ArrayList<>();
        Node node = model.getCache().get("F#0.GraphModelTest");
        getPath(node, root, parents, path);

        assertEquals(2, path.size());
        assertEquals("GraphModelTest", path.get(0));
        assertEquals("F#0.GraphModelTest", path.get(1));

        path = new ArrayList<>();
        node = model.getCache().get("D#3.C#2.B#1.A#0.GraphModelTest");
        getPath(node, root, parents, path);

        assertEquals(5, path.size());
        assertEquals("GraphModelTest", path.get(0));
        assertEquals("A#0.GraphModelTest", path.get(1));
        assertEquals("B#1.A#0.GraphModelTest", path.get(2));
        assertEquals("C#2.B#1.A#0.GraphModelTest", path.get(3));
        assertEquals("D#3.C#2.B#1.A#0.GraphModelTest", path.get(4));

        path = new ArrayList<>();
        node = model.getCache().get("C#1.F#0.GraphModelTest");
        getPath(node, root, parents, path);

        assertEquals(3, path.size());
        assertEquals("GraphModelTest", path.get(0));
        assertEquals("F#0.GraphModelTest", path.get(1));
        assertEquals("C#1.F#0.GraphModelTest", path.get(2));

        assertEquals(model.getCache().get("GraphModelTest").getProperty(SAMPLE_COUNT_PROPERTY), 45l);
        assertEquals(model.getCache().get("A#0.GraphModelTest").getProperty(SAMPLE_COUNT_PROPERTY), 35l);
        assertEquals(model.getCache().get("D#3.C#2.B#1.A#0.GraphModelTest").getProperty(SAMPLE_COUNT_PROPERTY), 15l);
        assertEquals(model.getCache().get("C#2.B#1.A#0.GraphModelTest").getProperty(SAMPLE_COUNT_PROPERTY), 15l);
        assertEquals(model.getCache().get("E#2.B#1.A#0.GraphModelTest").getProperty(SAMPLE_COUNT_PROPERTY), 10l);
        assertEquals(model.getCache().get("F#0.GraphModelTest").getProperty(SAMPLE_COUNT_PROPERTY), 10l);
        assertEquals(model.getCache().get("C#1.F#0.GraphModelTest").getProperty(SAMPLE_COUNT_PROPERTY), 5l);
    }

    @Test
    public void buildGraph() {
        GraphModel model = new GraphModel("GraphModelTest");
        populate(model);

        final boolean [] result = new boolean[1];
        model.addModelListener(new ModelListener<Graph>() {
            @Override
            public void modelRebuilt(Model<Graph> model, Graph data) {
                result[0] = true;
            }

            @Override
            public void modelCleared(Model<Graph> model) {}
        });
        model.rebuild();

        assertTrue(result[0]);
    }

    private static void populate(GraphModel model) {
        Trace trace0 = new Trace("trace0");
        trace0.add("A");
        for (int i = 0; i  < 5; i++) {
            model.addTrace(trace0);
        }

        Trace trace1 = new Trace("trace0");
        trace1.add("A").add("B").add("C").add("D");
        for (int i = 0; i  < 15; i++) {
            model.addTrace(trace1);
        }

        Trace trace2 = new Trace("trace0");
        trace2.add("A").add("B").add("E");
        for (int i = 0; i  < 10; i++) {
            model.addTrace(trace2);
        }

        Trace trace3 = new Trace("trace0");
        trace3.add("A").add("B");
        for (int i = 0; i  < 5; i++) {
            model.addTrace(trace3);
        }

        Trace trace4 = new Trace("trace1");
        trace4.add("F").add("C");
        for (int i = 0; i  < 5; i++) {
            model.addTrace(trace4);
        }

        Trace trace5 = new Trace("trace1");
        trace5.add("F");
        for (int i = 0; i  < 5; i++) {
            model.addTrace(trace5);
        }
    }

    private static void getPath(Node node, Node root, Map<Node, Node> nodes, List<String> path) {
        if (node.equals(root)) {
            path.add(node.getName());
            return;
        } else {
            Node parent = nodes.get(node);
            getPath(parent, root, nodes, path);
            path.add(node.getName());
        }
    }
}
