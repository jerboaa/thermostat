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

package com.redhat.thermostat.client.swing.components.experimental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.geom.Rectangle2D;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class TreeProcessorTest {

    private static final double DELTA = 0.1;
    private static final double BASE = 2.0;

    TreeMapNode node;
    Rectangle2D.Double area;

    @Before
    public void setUp() throws Exception {
        node = new TreeMapNode(1);
        area = new Rectangle2D.Double(0, 0, 500, 500);
    }

    @Test
    public final void testTreeProcessor() {
        boolean caught = false;
        // this test check all wrong combinations for constructor parameters
        try {
            TreeProcessor.processTreeMap(null, area);
        } catch(NullPointerException e) {
            caught = true;
        }
        assertTrue(caught);
        caught = false;

        try {
            TreeProcessor.processTreeMap(node, null);
        } catch(NullPointerException e) {
            caught = true;
        }
        assertTrue(caught);
        caught = false;

        try {
            TreeProcessor.processTreeMap(null, null);
        } catch(NullPointerException e) {
            caught = true;
        }
        assertTrue(caught);
    }


    @Test
    public final void testProcessTreeMapProcessesWholeTree() {
        generateTree(node, 5, 5);
        TreeProcessor.processTreeMap(node, area);

        // the test will check if any drawable node in the tree has a rectangle and a 
        // color, which means the processor function has processed the whole tree
        traverse(node);        
    }

    private void traverse(TreeMapNode tree) {
        if (tree.isDrawable() && (tree.getRectangle() == null || tree.getColor() == null)) {
            fail("node " + tree.getId() + " not processed");
        }
        for (TreeMapNode child : tree.getChildren()) {
            traverse(child);
        }
    }

    private void generateTree(TreeMapNode root, int levels, int childrenNumber) {        
        if (levels == 0) {
            return;
        } else {
            for (int i = 0; i < childrenNumber; i++) {
                root.addChild(new TreeMapNode(100));
            }
            for (TreeMapNode child : root.getChildren()) {
                generateTree(child, levels-1, childrenNumber);
            }
        }
    }

    @Test
    public final void testProcessTreeMapNodeSizing() {
        final int numSiblings = 5;
        final double originalDimension = 64.0;
        final double smallerDimension = 32.0;

        generateSiblingTree(node, numSiblings);

        TreeProcessor.processTreeMap(node, new Rectangle2D.Double(0, 0, originalDimension, originalDimension));
        checkNodeWeightRatios(numSiblings);

        //now resize smaller
        TreeProcessor.processTreeMap(node, new Rectangle2D.Double(0, 0, smallerDimension, smallerDimension));

        //now resize back to original size
        TreeProcessor.processTreeMap(node, new Rectangle2D.Double(0, 0, originalDimension, originalDimension));
        //if the first call to checkNodeWeightRatios(numSiblings) worked, this should too
        checkNodeWeightRatios(numSiblings);
    }

    private void generateSiblingTree(TreeMapNode root, int numSiblings) {
        assertTrue(numSiblings > 1);
        root.addChild(new TreeMapNode(1.0)); //this is not a random weight
        for(int i = 0; i < (numSiblings - 1); i++) {
            root.addChild(new TreeMapNode(Math.pow(BASE, i)));
        }
    }

    private void checkNodeWeightRatios(int numSiblings) {
        List<TreeMapNode> children = node.getChildren();
        assertEquals(numSiblings, children.size());
        TreeMapNode.sort(children);

        Rectangle2D.Double referenceRectangle = children.get(0).getRectangle();
        double referenceArea = referenceRectangle.getHeight() * referenceRectangle.getWidth();

        for (int i = 1; i < numSiblings - 1; i++) {
            Rectangle2D.Double currentRectangle = children.get(i).getRectangle();
            double currentArea = currentRectangle.getHeight() * currentRectangle.getWidth();
            //check that the ratio between areas is approximately 2 (which is the BASE)
            assertEquals(BASE, referenceArea / currentArea, DELTA);
            referenceArea = currentArea;
        }
    }
}
