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

package com.redhat.thermostat.ui.swing.components.graph;

import com.redhat.thermostat.client.swing.ComponentVisibleListener;
import com.redhat.thermostat.client.swing.components.experimental.StepGradient;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.collections.graph.BFSPayload;
import com.redhat.thermostat.collections.graph.BreadthFirstSearch;
import com.redhat.thermostat.collections.graph.DefaultTraversalListener;
import com.redhat.thermostat.collections.graph.Graph;
import com.redhat.thermostat.collections.graph.HashGraph;
import com.redhat.thermostat.collections.graph.Node;
import com.redhat.thermostat.collections.graph.Relationship;
import com.redhat.thermostat.collections.graph.SearchPayload;
import com.redhat.thermostat.collections.graph.TraversalListener;
import com.redhat.thermostat.platform.swing.components.ThermostatComponent;
import com.redhat.thermostat.ui.swing.model.Model;
import com.redhat.thermostat.ui.swing.model.ModelListener;
import com.redhat.thermostat.ui.swing.model.graph.GraphModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.redhat.thermostat.ui.swing.model.graph.GraphModel.SAMPLE_COUNT_PROPERTY;
import static com.redhat.thermostat.ui.swing.model.graph.GraphModel.TRACE_REAL_ID_PROPERTY;

/**
 */
public class GraphContainer extends ThermostatComponent {
    private static final boolean PRINT_BUILD_GRAPH_STATS = false;

    static final int PREFERRED_HEIGHT = 20;

    /**
     * JComponent property
     */
    static final String COMPONENT_PROPERTY = "component";

    /**
     * Integer property
     */
    static final String DEPTH_PROPERTY = "depth";

    private Graph componentGraph;
    private Node root;
    private Map<String, Node> nodeCache;

    private List<List<Node>> adjacencyTree;
    private Map<Node, Node> parents;

    private GraphModel model;

    private Map<String, Color> knownColours;

    private StepGradient stepGradient;

    public GraphContainer(GraphModel model) {
        this(model, new IcicleLayout(true));
    }

    public GraphContainer(GraphModel model, GraphLayout layout) {

        setLayout(layout);

        setBackground(Color.WHITE);

        stepGradient = new StepGradient(Palette.CLEAN_BLU.getColor(),
                                        Palette.THERMOSTAT_RED.getColor(), 10);

        this.model = model;
        componentGraph = new HashGraph();
        nodeCache = new HashMap<>();
        adjacencyTree = new ArrayList<>();
        parents = new HashMap<>();
        knownColours = new HashMap<>();

        model.addModelListener(new ModelListener<Graph>() {
            @Override
            public void modelRebuilt(Model<Graph> model, final Graph data) {
                // no need to do anything if the graph is zero order
                if (data.order() == 0) {
                    return;
                }

                rebuild(data);
            }

            @Override
            public void modelCleared(Model<Graph> model) {
                clearAll();
            }
        });

        addHierarchyListener(new ComponentVisibleListener() {
            @Override
            public void componentShown(Component component) {
                // trigger a rebuild right away
                getModel().rebuild();
            }

            @Override
            public void componentHidden(Component component) {}
        });
    }

    public GraphModel getModel() {
        return model;
    }

    private void clearAll() {
        removeAll();
        componentGraph.clear();
        nodeCache.clear();
        adjacencyTree.clear();
        parents.clear();
    }

    private void rebuild(Graph modelGraph) {

        long startTime = 0l;
        if (PRINT_BUILD_GRAPH_STATS) {
            System.err.println("graph-> start rebuild");
            startTime = System.nanoTime()/1_000_000;
        }

        adjacencyTree.clear();
        parents.clear();

        rebuildComponents(modelGraph);

        revalidate();
        repaint();

        if (PRINT_BUILD_GRAPH_STATS) {
            long stopTime = System.nanoTime()/1_000_000;
            long totalTime = (stopTime - startTime);
            System.err.println("graph-> " + totalTime + "ms");
        }
    }

    Graph getComponentGraph() {
        return componentGraph;
    }

    Node getRoot() {
        return root;
    }

    Map<String, Node> getNodeCache() {
        return nodeCache;
    }

    Map<Node, Node> getParents() {
        return parents;
    }

    List<List<Node>> getAdjacencyTree() {
        return adjacencyTree;
    }

    private void rebuildComponents(Graph graph) {
        // rebuild the graph with actual components for display
        // we create an adjacency list of the resulting BFS tree and a
        // parent/child lookup table, so the layout
        // manager won't need to traverse the graph again
        TraversalListener listener = new DefaultTraversalListener() {
            @Override
            protected void processRelationshipImpl(Relationship relationship) {
                Node from = getOrCloneNode(relationship.getFrom());
                Node to = getOrCloneNode(relationship.getTo());

                Relationship rel = new Relationship(from, ">", to);
                componentGraph.addRelationship(rel);
            }

            @Override
            public Status postProcessNode(Node node, SearchPayload _payload) {
                BFSPayload payload = (BFSPayload) _payload;
                Node parent = payload.getParents().get(node);

                int depth = 0;
                if (parent != null) {
                    depth = getNodeCache().get(parent.getName()).getProperty(DEPTH_PROPERTY);
                    depth++;
                }

                Node target = getNodeCache().get(node.getName());
                target.setProperty(DEPTH_PROPERTY, depth);

                setNodeColour(target);

                parents.put(target, parent);
                List<Node> adjacentNodes = getAdjacentNodes(depth);
                adjacentNodes.add(target);

                return super.postProcessNode(node, payload);
            }
        };

        Node root = model.getRoot();
        this.root = getOrCloneNode(root);

        BreadthFirstSearch bfs = new BreadthFirstSearch(graph);
        bfs.search(root, listener);
    }

    private void setNodeColour(Node node) {
        Tile component = node.getProperty(COMPONENT_PROPERTY);
        String realName = node.getProperty(TRACE_REAL_ID_PROPERTY);
        Color color = knownColours.get(realName);
        if (color == null) {
            color = stepGradient.sample();
            knownColours.put(realName, color);
        }
        component.setForeground(color);
    }

    private List<Node> getAdjacentNodes(int depth) {
        List<Node> adjacentNodes = null;
        // the assumption here is that we are always at most one behind
        if (adjacencyTree.size() > depth) {
            adjacentNodes = adjacencyTree.get(depth);
        }

        if (adjacentNodes == null) {
            // change to priority queue to keep component sorted
            // in different order
            adjacentNodes = new LinkedList<>();
            adjacencyTree.add(depth, adjacentNodes);
        }
        return adjacentNodes;
    }

    private Node getOrCloneNode(Node source) {

        Node node = getNodeCache().get(source.getName());
        if (node == null) {
            node = new Node(source.getName());
            node.setProperty(SAMPLE_COUNT_PROPERTY, source.getProperty(SAMPLE_COUNT_PROPERTY));
            node.setProperty(TRACE_REAL_ID_PROPERTY, source.getProperty(TRACE_REAL_ID_PROPERTY));
            Tile component = new Tile((String) source.getProperty(TRACE_REAL_ID_PROPERTY));
            component.setToolTipText("samples: " + source.getProperty(SAMPLE_COUNT_PROPERTY));
            add(component);

            node.setProperty(COMPONENT_PROPERTY, component);

            getNodeCache().put(node.getName(), node);
        } else {
            node.setProperty(SAMPLE_COUNT_PROPERTY, source.getProperty(SAMPLE_COUNT_PROPERTY));
        }

        return node;
    }

    @Override
    protected void paintComponent(Graphics2D graphics) {

        GradientPaint gradient = new GradientPaint(0, 0,
                                                   Palette.WHITE.getColor(), 0,
                                                   getHeight(),
                                                   Palette.LIGHT_GRAY.getColor(),
                                                   false);

        graphics.setPaint(gradient);
        graphics.fillRect(0, 0, getWidth(), getHeight());
    }
}
