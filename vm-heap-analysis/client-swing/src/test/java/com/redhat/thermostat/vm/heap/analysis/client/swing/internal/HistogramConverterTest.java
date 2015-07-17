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

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.vm.heap.analysis.common.HistogramRecord;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;


public class HistogramConverterTest {
    
    List<HistogramRecord> histrogramRecords;
    ObjectHistogram histrogram;

    @Before
    public void setUp() throws Exception {
        
       
        /*
         *  This is the classes structure used for the test and built using histogram
         * 
         *                ________com_______                    java
         *               /                  \                     |
         *         __example1__           example2              lang
         *        /            \             |                    |
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
                    "java.lang.Object"
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
    
    @Test
    public final void testconvertToTreeMap() {
        TreeMapNode tree = HistogramConverter.convertToTreeMap(histrogram);

        //tree node is the root element, which has an empty label by default
        assertEquals(tree.getLabel(), "");
     
        assertTrue(tree.getChildren().size() == 2);
        
        TreeMapNode java = tree.getChildren().get(1);
        //java subtree collapses into java node
        assertEquals(java.getLabel(), "java.lang.Object");
        
        TreeMapNode com = tree.getChildren().get(0);
        assertEquals(com.getLabel(), "com"); 
        
        // com node has 2 children
        assertTrue(com.getChildren().size() == 2);
        
        TreeMapNode example2 = com.getChildren().get(1); 
        //example2 subtree has been collapsed in example2 node
        assertEquals(example2.getLabel(), "example2.package1.Class4");
        
        TreeMapNode example1 = com.getChildren().get(0);
        assertTrue(example1.getChildren().size() == 2);

        TreeMapNode package2 = example1.getChildren().get(0);
        //class3 node has been collapsed in package2 node
        assertEquals(package2.getLabel(), "package2.Class3");
        
        TreeMapNode package1 = example1.getChildren().get(1);
        assertTrue(package1.getChildren().size() == 2);
        
        assertEquals(package1.getChildren().get(0).getLabel(), "Class2");
        assertEquals(package1.getChildren().get(1).getLabel(), "Class1");
    }
}