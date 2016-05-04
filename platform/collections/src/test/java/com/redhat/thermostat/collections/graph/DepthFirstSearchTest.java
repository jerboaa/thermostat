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

package com.redhat.thermostat.collections.graph;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class DepthFirstSearchTest {

    private static final String WEIGHT = "WEIGHT";
    
    @Test
    public void testDFSTraversal() {
        
        Graph graph = new HashGraph();
        
        Node a = new Node("A");
        Node b = new Node("B");
        Node c = new Node("C");
        Node d = new Node("D");
        Node e = new Node("E");
        Node f = new Node("F");
        Node g = new Node("G");
        Node h = new Node("H");
        
        Node i = new Node("I");
        
        Relationship ab = graph.addRelationship(a, "knows", b);
        Relationship ac = graph.addRelationship(a, "knows", c);
        
        Relationship bd = graph.addRelationship(b, "knows", d);
        Relationship be = graph.addRelationship(b, "knows", e);
        
        Relationship cf = graph.addRelationship(c, "knows", f);
        Relationship cg = graph.addRelationship(c, "knows", g);
        
        Relationship eh = graph.addRelationship(e, "knows", h);
        
        Relationship ei = graph.addRelationship(e, "knows", i);
        Relationship ia = graph.addRelationship(i, "knows", a);

        final List<Relationship> results = new ArrayList<>();
        final List<Node> nodes = new ArrayList<>();
        TraversalListener listener = new DefaultTraversalListener() {
            @Override
            protected void processRelationshipImpl(Relationship relationship) {
                results.add(relationship);
            }
            
            @Override
            public void preProcessNodeImpl(Node node) {
                nodes.add(node);
            }
        };
        
        DepthFirstSearch dfs = new DepthFirstSearch(graph);
        dfs.search(a, listener);
        
        assertTrue(results.contains(ab));
        assertTrue(results.contains(ac));
        assertTrue(results.contains(bd));
        assertTrue(results.contains(be));
        assertTrue(results.contains(cf));
        assertTrue(results.contains(cg));
        assertTrue(results.contains(eh));
        assertTrue(results.contains(ei));
        assertTrue(results.contains(ia));

        assertEquals(9, results.size());
        
        assertEquals(graph.order(), nodes.size());
    }
    
    @Test
    public void testBackNodes() {
        
        Graph graph = new HashGraph();
        
        Node a = new Node("A");
        Node b = new Node("B");
        Node c = new Node("C");
        Node d = new Node("D");
        Node e = new Node("E");
        Node f = new Node("F");
        Node g = new Node("G");
        Node h = new Node("H");
        
        Node i = new Node("I");
        
        Relationship ab = graph.addRelationship(a, "knows", b);
        Relationship ac = graph.addRelationship(a, "knows", c);
        
        Relationship bd = graph.addRelationship(b, "knows", d);
        Relationship be = graph.addRelationship(b, "knows", e);
        
        Relationship cf = graph.addRelationship(c, "knows", f);
        Relationship cg = graph.addRelationship(c, "knows", g);
        
        Relationship eh = graph.addRelationship(e, "knows", h);
        
        Relationship ei = graph.addRelationship(e, "knows", i);
        Relationship ia = graph.addRelationship(i, "knows", a);

        final Map<String, Relationship> backNodes = new HashMap<>();

        final Map<Integer, Integer> depthWeights = new HashMap<>();
        final Map<Node, Integer> depths = new HashMap<>();
        
        TraversalListener listener = new DefaultTraversalListener() {
            
            @Override
            public Status preProcessNode(Node node, SearchPayload _payload) {
                node.setProperty(WEIGHT, new Integer(1));
                
                Integer depth = depths.get(node);
                if (depth == null) {
                    DFSPayload payload = (DFSPayload) _payload;
                    Node parent = payload.getParents().get(node);
                    if (parent == null) {
                        depth = new Integer(0);
                    } else {
                        depth = new Integer(depths.get(parent).intValue() + 1);
                    }
                }
                depths.put(node, depth);
                return super.preProcessNode(node, _payload);
            }
            
            @Override
            public Status processRelationship(Relationship relationship,
                                              SearchPayload payload)
            {
                RelationshipType type =
                        DepthFirstSearch.classify(relationship, (DFSPayload) payload);
                
                switch (type) {
                case BACK:
                    backNodes.put(relationship.getName(), relationship);
                    break;
                
                case TREE:
                    break;
                    
                default:
                    break;
                }
                
                return super.processRelationship(relationship, payload);
            }
            
            @Override
            public Status postProcessNode(Node node, SearchPayload _payload) {
                
                Status status = super.postProcessNode(node, _payload);
                
                DFSPayload payload = (DFSPayload) _payload;
                
                Node parent = payload.getParents().get(node);
                if (parent == null) {
                    // this is root already
                    depthWeights.put(new Integer(0), (Integer) node.getProperty(WEIGHT));
                    return status;
                }
                
                // calculate the weight for the node itself
                Integer weight = node.getProperty(WEIGHT);
                Integer parentWeigth = parent.getProperty(WEIGHT);
                
                parentWeigth = new Integer(weight.intValue() + parentWeigth.intValue());
                parent.setProperty(WEIGHT, parentWeigth);

                // now the total weight of the circle this 
                Integer depth = depths.get(node);                
                Integer currentLevelWeigth = depthWeights.get(depth);
                if (currentLevelWeigth == null) {
                    // first time we see it
                    currentLevelWeigth = new Integer(0);
                }
                
                Integer depthWeight = new Integer(currentLevelWeigth.intValue() + weight.intValue());
                depthWeights.put(depth, depthWeight);
                
                return status;
            }
        };
        
        DepthFirstSearch dfs = new DepthFirstSearch(graph);
        dfs.search(a, listener);
        
        assertEquals(1, backNodes.size());
        assertTrue(backNodes.containsKey(ia.getName()));

        assertEquals(new Integer(2), depthWeights.get(3));
        assertEquals(new Integer(6), depthWeights.get(2));
        assertEquals(new Integer(8), depthWeights.get(1));
        assertEquals(new Integer(9), depthWeights.get(0));
        
        assertEquals(4, depthWeights.size());
    }

    private class TopologicalSort extends DefaultTraversalListener {
        private Stack<Node> ordered;
        private Set<Node> discovered;

        public TopologicalSort(Stack<Node> ordered, Set<Node> discovered) {
            this.ordered = ordered;
            this.discovered = discovered;
        }

        public Stack<Node> getOrdered() {
            return ordered;
        }

        @Override
        protected void preProcessNodeImpl(Node node) {
            discovered.add(node);
        }

        @Override
        protected void postProcessNodeImpl(Node node) {
            ordered.add(node);
        }

        @Override
        public Status processRelationship(Relationship relationship,
                                          SearchPayload payload)
        {
            RelationshipType type =
                    DepthFirstSearch.classify(relationship, (DFSPayload) payload);

            if (type.equals(RelationshipType.BACK)) {
                throw new IllegalStateException("back node! " + relationship);
            }

            return Status.CONTINUE;
        }
    }

    @Test
    public void testBackNodes2() {
        Graph graph = new HashGraph();

        Node a = new Node("A");
        Node b = new Node("B");
        Node c = new Node("C");
        Node d = new Node("D");
        Node e = new Node("E");
        Node f = new Node("F");
        Node g = new Node("G");

        Node h = new Node("H");

        Node i = new Node("I");
        Node l = new Node("L");

        Relationship ab = graph.addRelationship(a, "knows", b);
        Relationship ac = graph.addRelationship(a, "knows", c);
        Relationship ae = graph.addRelationship(a, "knows", e);
        Relationship ad = graph.addRelationship(a, "knows", d);
        Relationship al = graph.addRelationship(a, "knows", l);

        Relationship bc = graph.addRelationship(b, "knows", c);
        Relationship bd = graph.addRelationship(b, "knows", d);

        Relationship ce = graph.addRelationship(c, "knows", e);
        Relationship cf = graph.addRelationship(c, "knows", f);

        Relationship ed = graph.addRelationship(e, "knows", d);

        Relationship fe = graph.addRelationship(f, "knows", e);

        Relationship gf = graph.addRelationship(g, "knows", f);
        Relationship ga = graph.addRelationship(g, "knows", a);

        Relationship hc = graph.addRelationship(h, "knows", c);

        Relationship il = graph.addRelationship(i, "knows", l);

        DepthFirstSearch dfs = new DepthFirstSearch(graph);

        Stack<Node> ordered = new Stack<>();
        Set<Node> discovered = new HashSet<>();

        dfs.search(a, new TopologicalSort(ordered, discovered));

        for (Node node : graph) {
            if (!discovered.contains(node)) {
                TopologicalSort topological = new TopologicalSort(ordered, discovered);
                dfs.search(node, topological);
            }
        }

        assertFalse(ordered.isEmpty());
    }
}
