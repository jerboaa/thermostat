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

package com.redhat.thermostat.collections.graph;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BreadthFirstSearchTest {

    @Test
    public void testBFSTraversal() {
        
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

        
        Relationship ab = graph.addRelationship(a, b, "knows");
        Relationship ac = graph.addRelationship(a, c, "knows");
        
        Relationship bd = graph.addRelationship(b, d, "knows");
        Relationship be = graph.addRelationship(b, e, "knows");
        
        Relationship cf = graph.addRelationship(c, f, "knows");
        Relationship cg = graph.addRelationship(c, g, "knows");
        
        Relationship eh = graph.addRelationship(e, h, "knows");
        
        Relationship ei = graph.addRelationship(e, i, "knows");
        Relationship ia = graph.addRelationship(i, a, "knows");
        
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
        
        BreadthFirstSearch bfs = new BreadthFirstSearch(graph);
        bfs.search(a, listener);
        
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

}
