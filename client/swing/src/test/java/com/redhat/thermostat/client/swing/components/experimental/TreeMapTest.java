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

package com.redhat.thermostat.client.swing.components.experimental;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.Pair;

public class TreeMapTest {

    final private String SOME_ROOT_LABEL = "root";
    final private double SOME_ROOT_WEIGHT = 25.0;
    final private double DELTA = 0.01;
    final private double SOME_WEIGHT = 3.14;
    final private String NODE_SEPARATOR = ".";

    private ArrayList<Pair<String, Double>> data;
    private TreeMapNode root;

    @Before
    public void setUp() {
        /*
         * This is a tree visualization of the data  (weights in parentheses).
         *
         *                          ___________root(25)__________
         *                         /                             \
         *                ________com_______                    java
         *               /                  \                     |
         *        __example1__            example2              lang
         *       /            \              |                    |
         *   Class1(0)     Class2(1)      Class1(2)           Object(3)
         *
         */

        final String[] classes = {
                "com.example1.Class1",
                "com.example1.Class2",
                "com.example2.Class1",
                "java.lang.Object",
        };

        data = new ArrayList<Pair<String, Double>>();

        for (int i = 0; i < classes.length; i++) {
            data.add(new Pair<String, Double>(classes[i], new Double(i)));
        }

        root = new TreeMapNode(SOME_ROOT_LABEL, SOME_ROOT_WEIGHT);
    }

    @Test
    public void testCreateTreeMap() {
        /*
         * This is the expected resulting tree (weights in parentheses).
         *
         *                          ___________root(6)___________
         *                         /                             \
         *                ______com(3)______             java.lang.Object(3)
         *               /                  \
         *        _example1(1)_       example2.Class1(2)
         *       /             \
         *   Class1(0)      Class2(1)
         *
         */

        NodeDataExtractor<ArrayList<Pair<String, Double>>, Pair<String, Double>> extractor =
                new NodeDataExtractor<ArrayList<Pair<String, Double>>, Pair<String, Double>>() {

                    @Override
                    public String[] getNodes(Pair<String, Double> element) {
                        return element.getFirst().split(Pattern.quote(NODE_SEPARATOR));
                    }

                    @Override
                    public double getWeight(Pair<String, Double> element) {
                        return element.getSecond();
                    }

                    @Override
                    public Collection<Pair<String, Double>> getAsCollection(
                            ArrayList<Pair<String, Double>> data) {
                        return data;
                    }
                };
        TreeMap<ArrayList<Pair<String, Double>>, Pair<String, Double>> treeMap = new TreeMap<>(data, extractor);

        assertEquals(6.0, treeMap.getRoot().getRealWeight(), DELTA);
        assertEquals(2, treeMap.getRoot().getChildren().size());

        TreeMapNode javaLangObjectNode = TreeMap.searchNode(treeMap.getRoot(), "java.lang.Object");
        assertNotNull(javaLangObjectNode);
        assertEquals(3.0, javaLangObjectNode.getRealWeight(), DELTA);
        assertTrue(javaLangObjectNode.getChildren().isEmpty());

        TreeMapNode comNode = TreeMap.searchNode(treeMap.getRoot(), "com");
        assertNotNull(comNode);
        assertEquals(3.0, comNode.getRealWeight(), DELTA);
        assertEquals(2, comNode.getChildren().size());

        TreeMapNode example2Class1Node = TreeMap.searchNode(comNode, "example2.Class1");
        assertNotNull(example2Class1Node);
        assertEquals(2.0, example2Class1Node.getRealWeight(), DELTA);
        assertTrue(example2Class1Node.getChildren().isEmpty());

        TreeMapNode example1Node = TreeMap.searchNode(comNode, "example1");
        assertNotNull(example1Node);
        assertEquals(1.0, example1Node.getRealWeight(), DELTA);
        assertEquals(2, example1Node.getChildren().size());

        TreeMapNode class1Node = TreeMap.searchNode(example1Node, "Class1");
        assertNotNull(class1Node);
        assertEquals(0.0, class1Node.getRealWeight(), DELTA);
        assertTrue(class1Node.getChildren().isEmpty());

        TreeMapNode class2Node = TreeMap.searchNode(example1Node, "Class2");
        assertNotNull(class2Node);
        assertEquals(1.0, class2Node.getRealWeight(), DELTA);
        assertTrue(class2Node.getChildren().isEmpty());
    }

    @Test
    public void testFillWeights() {
        assertEquals(SOME_ROOT_WEIGHT, TreeMap.fillWeights(root), DELTA);
        TreeMapNode parent = new TreeMapNode("childA", SOME_WEIGHT);
        root.addChild(parent);

        final double weightAA = 5.0;
        final double weightAB = 10.0;
        parent.addChild(new TreeMapNode("childAA", weightAA));
        parent.addChild(new TreeMapNode("childAB", weightAB));

        assertEquals(SOME_ROOT_WEIGHT, root.getRealWeight(), DELTA);
        TreeMap.fillWeights(root);
        assertEquals(weightAA + weightAB, root.getRealWeight(), DELTA);

        TreeMapNode childAANode = TreeMap.searchNode(parent, "childAA");
        assertNotNull(childAANode);
        assertEquals(weightAA, childAANode.getRealWeight(), DELTA);

        TreeMapNode childABNode = TreeMap.searchNode(parent, "childAB");
        assertNotNull(childABNode);
        assertEquals(weightAB, childABNode.getRealWeight(), DELTA);
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

        TreeMap.packTree(root);

        assertEquals(0, child.getChildren().size());
        assertEquals("childB.childBA.childBAA", child.getLabel());

        child = TreeMap.searchNode(root, "childA");
        assertNotNull(child);
        assertNotNull(TreeMap.searchNode(child, "childAA"));
        assertNotNull(TreeMap.searchNode(child, "childAB"));
    }
}
