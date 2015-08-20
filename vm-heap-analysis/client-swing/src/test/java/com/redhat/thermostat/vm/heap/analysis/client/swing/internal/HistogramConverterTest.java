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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;

public class HistogramConverterTest {
    
    ObjectHistogram histrogram;

    @Before
    public void setUp() throws Exception {
        /*
         * This is the classes structure used for the test and built using
         * histogram. Nodes are compacted at the point of branching
         * so for example the node relative to "example2.package1.Class1" is
         * still just one node "example2.package1.Class1" but the node relative
         * to "com.example1.package1.Class1" branches at "com" so becomes the
         * common parent node "com" with children "example1" and "example2",
         * and so fort:
         * 
         *                ________com_______                    java
         *               /                  \                     |
         *         __example1__           example2              lang
         *       /            \             |                    |
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
        
        histrogram = new ObjectHistogram();
        
        for (int i = 0; i < classes.length; i++) {
            final String className = classes[i];
            
            histrogram.addThing(new JavaHeapObject() {
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
    }
    
    private static TreeMapNode searchNode(List<TreeMapNode> nodes, String nodeId) {
        for (TreeMapNode node : nodes) {
            if (node.getLabel().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }
    
    @Test
    public void testconvertToTreeMap() {
        TreeMapNode tree = HistogramConverter.convertToTreeMap(histrogram);

        List<TreeMapNode> nodes = tree.getChildren();
        assertEquals(5, nodes.size());
        
        TreeMapNode node = searchNode(nodes, "com");
        assertNotNull(node);

        List<TreeMapNode> nodesFromCom = node.getChildren();
        
        // example1 and example2
        assertEquals(2, nodesFromCom.size());
        node = searchNode(nodesFromCom, "example1");
        assertNotNull(node);
        
        // package2.Class3 and package1
        assertEquals(2, node.getChildren().size());
        
        node = searchNode(node.getChildren(), "package2.Class3");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());

        node = searchNode(nodes, "java.lang.Object");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());

        node = searchNode(nodes, "example2.package1.Class1");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());
        
        // now on to the  "repeat1.repeat1.RepeatClass" bunch
        node = searchNode(nodes, "repeat1.repeat1.RepeatClass");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());
        
        node = searchNode(nodes, "repeat2");
        assertNotNull(node);
        assertEquals(2, node.getChildren().size());
        
        List<TreeMapNode> nodesFromRepeat2 = node.getChildren();
        node = searchNode(nodesFromRepeat2, "repeat2A.RepeatClass");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());
        
        node = searchNode(nodesFromRepeat2, "repeat2B.RepeatClass");
        assertNotNull(node);
        assertTrue(node.getChildren().isEmpty());
    }

}