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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.Pair;

public class TreeConverterTest {

    final private String SPLIT_TOKEN = ".";
    final private String SOME_ROOT_LABEL = "root";
    final private double SOME_ROOT_WEIGHT = 25.0;
    final private double DELTA = 0.01;
    final private double SOME_WEIGHT = 3.14;

    private ArrayList<Pair<String, Double>> data;
    private TreeMapNode root;

    @Before
    public void setUp() {
        /*
         * This is the classes structure used for the test and built using
         * the data. Nodes are compacted at the point of branching
         * so for example the node relative to "example2.package1.Class1" is
         * still just one node "example2.package1.Class1" but the node relative
         * to "com.example1.package1.Class1" branches at "com" so becomes the
         * common parent node "com" with children "example1" and "example2",
         * and so forth:
         *
         *                ________com_______                    java
         *               /                  \                     |
         *        __example1__            example2              lang
         *       /            \             |                     |
         *    package1      package2      package1              Object
         *    /     \           |            |
         * Class1  Class2     Class3       Class4
         *
         *
         *
         *
         * Expected tree after conversion:
         *
         *                           ________________________root_______________
         *                          /                                           \
         *                ________com_______                                java.lang.Object
         *               /                  \
         *         __example1__           example2.package1.Class4
         *        /            \
         *    package1      package2.Class3
         *    /     \
         *  Class1  Class2
         *
         */
        final String[] classes = {
                "com.example1.package1.Class1",
                "com.example1.package1.Class2",
                "com.example1.package2.Class3",
                "com.example2.package1.Class4",
                "example2.package1.Class1",
                "java.lang.Object",
                "repeat1.repeat1.RepeatClass",
                "repeat2.repeat2A.RepeatClass",
                "repeat2.repeat2B.RepeatClass",
        };

        data = new ArrayList<Pair<String, Double>>();

        for (int i = 0; i < classes.length; i++) {
            data.add(new Pair<String, Double>(classes[i], new Double(i)));
        }

        root = new TreeMapNode(SOME_ROOT_LABEL, SOME_ROOT_WEIGHT);
    }

    @Test
    public void testConvertToTreeMap() {
        AbstractTreeAssembler<ArrayList<Pair<String, Double>>> assembler =
                new AbstractTreeAssembler<ArrayList<Pair<String, Double>>>() {

            @Override
            public void buildTree(ArrayList<Pair<String, Double>> data, TreeMapNode root) {
                for (Pair<String, Double> element: data) {
                    TreeMapNode lastProcessed = processRecord(element.getFirst(), SPLIT_TOKEN, root);
                    lastProcessed.setRealWeight(element.getSecond());
                }
            }
        };
        TreeMapNode tree = TreeConverter.convertToTreeMap(data, assembler);

        List<TreeMapNode> nodes = tree.getChildren();
        assertEquals(5, nodes.size());

        TreeMapNode node = AbstractTreeAssembler.searchNode(nodes, "com");
        assertNotNull(node);

        List<TreeMapNode> nodesFromCom = node.getChildren();

        // example1 and example2
        assertEquals(2, nodesFromCom.size());
        node = AbstractTreeAssembler.searchNode(nodesFromCom, "example1");
        assertNotNull(node);

        // package2.Class3 and package1
        assertEquals(2, node.getChildren().size());

        node = AbstractTreeAssembler.searchNode(node.getChildren(), "package2.Class3");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());

        node = AbstractTreeAssembler.searchNode(nodes, "java.lang.Object");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());

        node = AbstractTreeAssembler.searchNode(nodes, "example2.package1.Class1");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());

        // now on to the  "repeat1.repeat1.RepeatClass" bunch
        node = AbstractTreeAssembler.searchNode(nodes, "repeat1.repeat1.RepeatClass");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());

        node = AbstractTreeAssembler.searchNode(nodes, "repeat2");
        assertNotNull(node);
        assertEquals(2, node.getChildren().size());

        List<TreeMapNode> nodesFromRepeat2 = node.getChildren();
        node = AbstractTreeAssembler.searchNode(nodesFromRepeat2, "repeat2A.RepeatClass");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());

        node = AbstractTreeAssembler.searchNode(nodesFromRepeat2, "repeat2B.RepeatClass");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());
    }

    @Test
    public void testFillWeights() {
        assertEquals(SOME_ROOT_WEIGHT, TreeConverter.fillWeights(root), DELTA);
        TreeMapNode parent = new TreeMapNode("childA", SOME_WEIGHT);
        root.addChild(parent);

        final double weightAA = 5.0;
        final double weightAB = 10.0;
        parent.addChild(new TreeMapNode("childAA", weightAA));
        parent.addChild(new TreeMapNode("childAB", weightAB));

        assertEquals(SOME_ROOT_WEIGHT, root.getRealWeight(), DELTA);
        TreeConverter.fillWeights(root);
        assertEquals(weightAA + weightAB, root.getRealWeight(), DELTA);

        List<TreeMapNode> children = parent.getChildren();

        TreeMapNode child = AbstractTreeAssembler.searchNode(children, "childAA");
        assertNotNull(child);
        assertEquals(weightAA, child.getRealWeight(), DELTA);

        child = AbstractTreeAssembler.searchNode(children, "childAB");
        assertNotNull(child);
        assertEquals(weightAB, child.getRealWeight(), DELTA);
    }

    @Test
    public void testPackTree() {
        TreeMapNode child = new TreeMapNode("childA", SOME_WEIGHT);
        root.addChild(child);
        child.addChild(new TreeMapNode("childAA", SOME_WEIGHT));
        child.addChild(new TreeMapNode("childAB", SOME_WEIGHT));
        child = new TreeMapNode("childB", SOME_WEIGHT);
        root.addChild(child);
        TreeMapNode grandchild = new TreeMapNode("childBA", SOME_WEIGHT);
        child.addChild(grandchild);
        grandchild.addChild(new TreeMapNode("childBAA", SOME_WEIGHT));

        TreeConverter.packTree(root);

        assertEquals(0, child.getChildren().size());
        assertEquals("childB.childBA.childBAA", child.getLabel());

        child = AbstractTreeAssembler.searchNode(root.getChildren(), "childA");
        assertNotNull(child);
        assertNotNull(AbstractTreeAssembler.searchNode(child.getChildren(), "childAA"));
        assertNotNull(AbstractTreeAssembler.searchNode(child.getChildren(), "childAB"));
    }
}
