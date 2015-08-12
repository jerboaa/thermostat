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

package com.redhat.thermostat.vm.heap.analysis.client.swing.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;


public class TreeMapNodeTest {

    private TreeMapNode node;

    @Before
    public void setUp() {
        node = new TreeMapNode(null, 1);
    }

    @Test
    public final void testGetId() {
        TreeMapNode node1 = new TreeMapNode(null, 1);
        TreeMapNode node2 = new TreeMapNode(null, 1);
        assertTrue(node1.getId() != node2.getId());
        assertTrue(node1.getId() + 1 == node2.getId());
    }

    @Test
    public final void testGetSetParent() {
        TreeMapNode parent = new TreeMapNode(null, 1);
        assertTrue(node.getParent() == null);
        node.setParent(parent);
        assertTrue(node.getParent() == parent);
    }

    @Test
    public final void testGetSetLabel() {
        TreeMapNode node = new TreeMapNode("MyLabel", 1);
        assertTrue(node.getLabel().equals("MyLabel"));
        node.setLabel("MyNewLabel");
        assertTrue(node.getLabel().equals("MyNewLabel"));
    }

    @Test
    public final void testGetSetChildren() {
        assertTrue(node.getChildren().isEmpty());

        TreeMapNode node = new TreeMapNode(null, 1);
        List<TreeMapNode> children = new ArrayList<>();
        children.add(node);

        node.setChildren(children);
        assertTrue(1 == node.getChildren().size());
    }

    @Test
    public final void testIsLeaf() {
        assertTrue(node.isLeaf());
        node.addChild(new TreeMapNode(null, 1));
        assertFalse(node.isLeaf());
        node.setChildren(Collections.<TreeMapNode>emptyList());
        assertTrue(node.isLeaf());
    }

    @Test
    public final void testGetAddInfo() {
        node.addInfo("exampleKey", "exampleValue");
        assertEquals("exampleValue", node.getInfo("exampleKey"));
    }

    @Test
    public final void testAddChild() {
        assertTrue(node.getChildren().size() == 0);
        node.addChild(new TreeMapNode(null, 1));
        assertTrue(node.getChildren().size() == 1);

        node.addChild(null);
        assertTrue(node.getChildren().size() == 1); // null has not been added
    }

    @Test
    public final void testGetSetWeight() {
        assertTrue(1 == node.getWeight());
        node.setWeight(5);
        assertTrue(5 == node.getWeight());
    }

    @Test
    public final void testGetSetRectangle() {        
        Rectangle2D.Double r = new Rectangle2D.Double(5, 5, 5, 5);
        node.setRectangle(r);
        assertEquals(r, node.getRectangle());

        node.setRectangle(null);
        boolean catched = false;
        try {
            node.getRectangle();
        } catch(RuntimeException e) {
            catched = true;
        }
        assertTrue(catched);
    }


    @Test
    public final void testGetSetRealWeight() {
        node = new TreeMapNode(null, 5);
        assertTrue(node.getRealWeight() == 5);
        node.setRealWeight(8);
        assertTrue(node.getRealWeight() == 8);
    }

    @Test
    public final void testAllowNonPositiveWeight() {
        assertFalse(TreeMapNode.isAllowNonPositiveWeight());
        node.setWeight(-5);
        assertTrue(node.getWeight() == 1); // the real node weight
        assertTrue(node.getRealWeight() == 1); 


        TreeMapNode.setAllowNonPositiveWeight(true);
        node.setWeight(-5);
        assertTrue(node.getWeight() == -5);
    }

    @Test
    public final void testIsDrawable() {
        Rectangle2D.Double r = new Rectangle2D.Double(5, 5, 5, 5);
        node.setRectangle(r);
        assertTrue(node.isDrawable());

        r.setRect(0,  0,  0.5f, 0.5f);
        assertFalse(node.isDrawable());

        r.setRect(0,  0,  5f, 0.5f);
        assertFalse(node.isDrawable());

        r.setRect(0,  0,  0.5f, 5f);
        assertFalse(node.isDrawable());
    }

    @Test
    public final void testGetSetColor() {
        assertNull(node.getColor());
        node.setColor(Color.black);
        assertEquals(Color.black, node.getColor());
    }

    @Test
    public final void testGetDepth() {
        TreeMapNode depth1 = new TreeMapNode(null, 1);
        TreeMapNode depth2 = new TreeMapNode(null, 1);

        node.addChild(depth1);
        depth1.addChild(depth2);

        assertTrue(node.getDepth() == 0);
        assertTrue(depth1.getDepth() == 1);
        assertTrue(depth2.getDepth() == 2);
    }

    @Test
    public final void testSearchByLabel() {
        TreeMapNode root = new TreeMapNode("root", 0);

        TreeMapNode a = new TreeMapNode("a", 3);
        TreeMapNode b = new TreeMapNode("b", 2);
        TreeMapNode c = new TreeMapNode("c", 1);
        root.addChild(a);
        root.addChild(b);
        root.addChild(c);

        TreeMapNode aa = new TreeMapNode("aa", 3);
        TreeMapNode ab = new TreeMapNode("ab", 3);
        TreeMapNode ac = new TreeMapNode("ac", 3);
        a.addChild(aa);
        a.addChild(ab);
        a.addChild(ac);        

        assertEquals(aa, root.searchNodeByLabel("aa"));
        assertEquals(b, root.searchNodeByLabel("b"));
        assertEquals(ac, root.searchNodeByLabel("ac"));
        assertEquals(c, root.searchNodeByLabel("c"));
    }



    @Test
    public final void testSort() {

        TreeMapNode n1 = new TreeMapNode(null, 5);
        TreeMapNode n2 = new TreeMapNode(null, 4);
        TreeMapNode n4 = new TreeMapNode(null, 2);
        TreeMapNode n3 = new TreeMapNode(null, 3);
        TreeMapNode n5 = new TreeMapNode(null, 0);
        TreeMapNode n6 = new TreeMapNode(null, 7);
        TreeMapNode n7 = new TreeMapNode(null, 1);
        TreeMapNode n8 = new TreeMapNode(null, 9);

        List<TreeMapNode> toSort = new ArrayList<>();
        toSort.add(n3);
        toSort.add(n2);
        toSort.add(n4);
        toSort.add(n1);
        toSort.add(n5);
        toSort.add(n6);
        toSort.add(n7);
        toSort.add(n8);

        TreeMapNode.sort(toSort);

        assertEquals(toSort.get(0), n8);
        assertEquals(toSort.get(1), n6);
        assertEquals(toSort.get(2), n1);
        assertEquals(toSort.get(3), n2);
        assertEquals(toSort.get(4), n3);
        assertEquals(toSort.get(5), n4);
        assertEquals(toSort.get(6), n7);
        assertEquals(toSort.get(7), n5);
    }

    @Test
    public final void testGetInfo() {
        Map<String, String> map = node.getInfo();
        assertNotNull(map);
        assertEquals(0, map.keySet().size());
    }

    @Test
    public final void testToString() {
        assertNotNull(node.toString());
    }
    
    
    @Test
    public final void testGetNextColor() {
        assertNull(node.getColor());
        assertTrue(node.getNextColor().equals(node.START_COLOR));
        
        Color start = node.START_COLOR;
        node.setColor(start);

        for (int i = 0; i < TreeMapNode.colors.length; i++) {
            assertEquals(TreeMapNode.colors[i], node.getColor());
            node.setColor(node.getNextColor());
        }
    }
}
