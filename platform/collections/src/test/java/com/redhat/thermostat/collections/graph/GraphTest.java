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

package com.redhat.thermostat.collections.graph;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class GraphTest {

    @Test
    public void testGraphCreation() {
     
        Graph graph = new HashGraph();
        
        Node a = new Node("A");
        Node b = new Node("B");
        
        graph.addRelationship(a, "knows", b);
        
        assertEquals(1, graph.size());
        assertEquals(2, graph.order());
        
        graph.addRelationship(b, "knows", a);
        assertEquals(2, graph.size());
        
        // same nodes, the order is not changed
        assertEquals(2, graph.order());

        // same relationship, no-op
        graph.addRelationship(b, "knows", a);
        assertEquals(2, graph.size());
        assertEquals(2, graph.order());

        graph.addRelationship(a, "plays with", b);
        assertEquals(3, graph.size());

        // same nodes again
        assertEquals(2, graph.order());
        
        Node c = new Node("C");
        graph.addRelationship(a, "knows", c);
        assertEquals(3, graph.order());
    }
    
    @Test
    public void testGraphRelationship() {
        Graph graph = new HashGraph();
        
        Node a = new Node("A");
        Node b = new Node("B");
        
        Relationship r0 = graph.addRelationship(a, "knows", b);
        Relationship r1 = graph.addRelationship(a, "plays football with", b);
        Relationship r2 = graph.addRelationship(a, "watch movies with", b);
        
        Set<Relationship> rel = graph.getRelationships(a);
        assertEquals(3, rel.size());
        assertTrue(rel.contains(r0));
        assertTrue(rel.contains(r1));
        assertTrue(rel.contains(r2));
    }

    @Test
    public void testGraphRelationship1() {
        Graph graph = new HashGraph();

        Node a = new Node("A");
        Node b = new Node("B");
        Node c = new Node("B");

        Relationship r0 = graph.addRelationship(a, "knows", b);
        assertEquals(1, graph.size());
        assertEquals(2, graph.order());

        Relationship r1 = graph.addRelationship(b, "knows", a);
        assertEquals(2, graph.size());
        assertEquals(2, graph.order());

        Relationship r2 = graph.addRelationship(a, "watch movies with", b);
        assertEquals(3, graph.size());
        assertEquals(2, graph.order());

        // represents the same relationship as (A)->[plays with]->(B)
        // - this really depends on nodes being equals just by name, hence
        // the extra assert check
        assertEquals(b, c);
        Relationship r3 = graph.addRelationship(a, "watch movies with", c);
        assertEquals(3, graph.size());
        assertEquals(2, graph.order());
    }

    @Test
    public void testClear() {
        Graph graph = new HashGraph();
        assertEquals(0, graph.size());
        assertEquals(0, graph.order());

        Node a = new Node("A");
        Node b = new Node("B");

        graph.addRelationship(a, "knows", b);

        assertEquals(1, graph.size());
        assertEquals(2, graph.order());

        graph.clear();
        assertEquals(0, graph.size());
        assertEquals(0, graph.order());

        Set<Relationship> rel = graph.getRelationships(a);
        assertNull(rel);

        graph.addRelationship(a, "knows", b);
        assertEquals(1, graph.size());
        assertEquals(2, graph.order());
    }
}
