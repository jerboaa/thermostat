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

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import com.redhat.thermostat.collections.graph.Graph;
import com.redhat.thermostat.collections.graph.Node;
import com.redhat.thermostat.collections.graph.Relationship;
import com.redhat.thermostat.tools.dependency.internal.utils.TestHelper;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DependencyGraphBuilderTest {

    private File underneathTheBridge;

    private Path b1, b2, b3, b4, b5, b6;
    private Path v1, v2, v31, v41, v30, v40;

    @Before
    public void setup() throws Exception {
        underneathTheBridge = TestHelper.createTestDirectory();
        b1 = TestHelper.createJar("Bundle1", "Bundle2", underneathTheBridge.toPath());
        b2 = TestHelper.createJar("Bundle2", "Bundle3,Bundle4", underneathTheBridge.toPath());
        b3 = TestHelper.createJar("Bundle3", "Bundle4,Bundle5,Bundle6", underneathTheBridge.toPath());
        b4 = TestHelper.createJar("Bundle4", "Bundle5,Bundle6", underneathTheBridge.toPath());
        b5 = TestHelper.createJar("Bundle5", "Bundle6", underneathTheBridge.toPath());
        b6 = TestHelper.createJar("Bundle6", "", underneathTheBridge.toPath());

        v1 = TestHelper.createJarWithExports("test1", "test1;version=1.0", "test2;version=[1,2)", underneathTheBridge.toPath());
        v2 = TestHelper.createJarWithExports("test2", "test2;version=1.0", "test3;version=[3.1,4)", underneathTheBridge.toPath());
        v31 = TestHelper.createJarWithExports("test-3.1", "test3;version=3.1", "test4;version=[4.1,5)", underneathTheBridge.toPath());
        v41 = TestHelper.createJarWithExports("test-4.1", "test4;version=4.1", "", underneathTheBridge.toPath());
        v30 = TestHelper.createJarWithExports("test-3.0", "test3;version=3.0", "test4;version=[4.0,4.1)", underneathTheBridge.toPath());
        v40 = TestHelper.createJarWithExports("test-4.0", "test4;version=4.0", "", underneathTheBridge.toPath());
    }

    @Test
    public void testSimpleGraphBuild() {
        JarLocations locations = mock(JarLocations.class);
        when(locations.getLocations()).thenReturn(Arrays.asList(b1, b2, b3, b4, b5, b6));
        Node n1 = new Node(b1.toString());
        Node n2 = new Node(b2.toString());
        Node n3 = new Node(b3.toString());
        Node n4 = new Node(b4.toString());
        Node n5 = new Node(b5.toString());
        Node n6 = new Node(b6.toString());
        PathProcessorHandler handler = new PathProcessorHandler(locations);
        DependencyGraphBuilder dgb = new DependencyGraphBuilder();
        handler.process(dgb);
        Graph g = dgb.build();
        assertNotNull(g);
        Set<Relationship> edges = g.getRelationships(n1);
        assertEquals(1, edges.size());
        assertTrue(edges.contains(new Relationship(n1, "->", n2)));
        edges = g.getRelationships(n2);
        assertEquals(2, edges.size());
        assertTrue(edges.contains(new Relationship(n2, "->", n3)));
        assertTrue(edges.contains(new Relationship(n2, "->", n4)));
        edges = g.getRelationships(n3);
        assertEquals(3, edges.size());
        assertTrue(edges.contains(new Relationship(n3, "->", n4)));
        assertTrue(edges.contains(new Relationship(n3, "->", n5)));
        assertTrue(edges.contains(new Relationship(n3, "->", n6)));
        edges = g.getRelationships(n4);
        assertEquals(2, edges.size());
        assertTrue(edges.contains(new Relationship(n4, "->", n5)));
        assertTrue(edges.contains(new Relationship(n4, "->", n6)));
        edges = g.getRelationships(n5);
        assertEquals(1, edges.size());
        assertTrue(edges.contains(new Relationship(n5, "->", n6)));
        edges = g.getRelationships(n6);
        assertEquals(0, edges.size());
    }

    @Test
    public void testGraphWithSpecificVersions() {
        JarLocations locations = mock(JarLocations.class);
        when(locations.getLocations()).thenReturn(Arrays.asList(v1, v2, v30, v31, v40, v41));
        Node n1 = new Node(v1.toString());
        Node n2 = new Node(v2.toString());
        Node n3 = new Node(v30.toString());
        Node n4 = new Node(v31.toString());
        Node n5 = new Node(v40.toString());
        Node n6 = new Node(v41.toString());
        PathProcessorHandler handler = new PathProcessorHandler(locations);
        DependencyGraphBuilder dgb = new DependencyGraphBuilder();
        handler.process(dgb);
        Graph g = dgb.build();
        assertNotNull(g);
        Set<Relationship> edges = g.getRelationships(n1);
        assertEquals(1, edges.size());
        assertTrue(edges.contains(new Relationship(n1, "->", n2)));
        edges = g.getRelationships(n2);
        assertEquals(1, edges.size());
        assertTrue(edges.contains(new Relationship(n2, "->", n4)));
        edges = g.getRelationships(n3);
        assertEquals(1, edges.size());
        assertTrue(edges.contains(new Relationship(n3, "->", n5)));
        edges = g.getRelationships(n4);
        assertEquals(1, edges.size());
        assertTrue(edges.contains(new Relationship(n4, "->", n6)));
        edges = g.getRelationships(n5);
        assertEquals(0, edges.size());
        edges = g.getRelationships(n6);
        assertEquals(0, edges.size());
    }
}
