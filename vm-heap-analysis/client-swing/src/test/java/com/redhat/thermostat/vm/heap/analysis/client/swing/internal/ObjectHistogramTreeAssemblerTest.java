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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.swing.components.experimental.AbstractTreeAssembler;
import com.redhat.thermostat.client.swing.components.experimental.TreeMapNode;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;

public class ObjectHistogramTreeAssemblerTest {

    private final String ROOT_LABEL = "root";
    private final double ROOT_WEIGHT = 10.0;

    private ObjectHistogram histogram;
    private TreeMapNode root;

    @Before
    public void setup() {

        final String[] classes = {
                "com.example1.Class1",
                "com.example1.Class2",
                "com.example2",
                "java.lang.Object",
        };

        histogram = new ObjectHistogram();
        for (int i = 0; i < classes.length; i++) {
            final String className = classes[i];

            histogram.addThing(new JavaHeapObject() {
                public int getSize() {
                    return 0;
                }

                public long getId() {
                    return 0;
                }

                public JavaClass getClazz() {
                    return new JavaClass(className, 0, 0, 0, 0, null, null, 0);
                }
            });
        }

        root = new TreeMapNode(ROOT_LABEL, ROOT_WEIGHT);
    }

    @Test
    public void testBuildTree() {
        /*
         * This is a visualization of the expected resulting tree.
         *
         *                          _____________root____________
         *                         /                             \
         *                ________com_______                    java
         *               /                  \                     |
         *        __example1__            example2              lang
         *       /            \                                   |
         *    Class1         Class2                            Object
         *
         */

        ObjectHistogramTreeAssembler assembler = new ObjectHistogramTreeAssembler();
        assembler.buildTree(histogram, root);

        List<TreeMapNode> children = root.getChildren();
        assertEquals(2, children.size());

        TreeMapNode node = AbstractTreeAssembler.searchNode(children, "java");
        assertNotNull(node);
        children = node.getChildren();
        assertEquals(1, children.size());
        node = AbstractTreeAssembler.searchNode(children, "lang");
        assertNotNull(node);
        children = node.getChildren();
        assertEquals(1, children.size());
        node = AbstractTreeAssembler.searchNode(children, "Object");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());

        children = root.getChildren();
        node = AbstractTreeAssembler.searchNode(children, "com");
        assertNotNull(node);
        children = node.getChildren();
        assertEquals(2, children.size());

        node = AbstractTreeAssembler.searchNode(children, "example2");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());

        node = AbstractTreeAssembler.searchNode(root.getChildren(), "com");
        assertNotNull(node);
        node = AbstractTreeAssembler.searchNode(node.getChildren(), "example1");
        assertNotNull(node);
        children = node.getChildren();
        assertEquals(2, children.size());

        node = AbstractTreeAssembler.searchNode(children, "Class1");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());

        node = AbstractTreeAssembler.searchNode(children, "Class2");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());
    }

}
