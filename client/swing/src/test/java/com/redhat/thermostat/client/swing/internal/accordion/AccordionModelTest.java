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

package com.redhat.thermostat.client.swing.internal.accordion;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;

public class AccordionModelTest {
    
    @Test
    public void testAccessHeader() {
        AccordionModel<String, String> testModel = new AccordionModel<>();
        testModel.addComponent("test0", "testComponent0");
        testModel.addComponent("test0", "testComponent1");
        testModel.addComponent("test0", "testComponent2");
        testModel.addComponent("test1", "testComponent3");
        testModel.addHeader("test2");
        
        List<String> result = testModel.getHeaders();
        assertEquals(3, result.size());

        assertTrue(result.contains("test0"));
        assertTrue(result.contains("test1"));
        assertTrue(result.contains("test2"));
        
        testModel.addHeader("test3");
        testModel.addHeader("test4");
        testModel.addHeader("test5");

        result = testModel.getHeaders();
        assertEquals(6, result.size());

        assertTrue(result.contains("test0"));
        assertTrue(result.contains("test1"));
        assertTrue(result.contains("test2"));
        assertTrue(result.contains("test3"));
        assertTrue(result.contains("test4"));
        assertTrue(result.contains("test5"));
        
        testModel.removeHeader("test0");
        
        result = testModel.getHeaders();
        assertEquals(5, result.size());
        assertFalse(result.contains("test0"));
    }

    @Test
    public void testAccessComponents() {
        AccordionModel<String, String> testModel = new AccordionModel<>();
        testModel.addComponent("test0", "testComponent0");
        testModel.addComponent("test0", "testComponent1");
        testModel.addComponent("test0", "testComponent2");
        testModel.addComponent("test1", "testComponent3");
        testModel.addComponent("test1", "testComponent4");
        
        List<String> result = testModel.getComponents("test0");
        assertEquals(3, result.size());
        
        assertTrue(result.contains("testComponent0"));
        assertTrue(result.contains("testComponent1"));
        assertTrue(result.contains("testComponent2"));
        
        result = testModel.getComponents("test1");
        assertEquals(2, result.size());
        
        assertTrue(result.contains("testComponent3"));
        assertTrue(result.contains("testComponent4"));
        
        result = testModel.getComponents("test2");
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        testModel.addComponent("test1", "testComponent5");
        result = testModel.getComponents("test1");
        assertEquals(3, result.size());
        
        testModel.removeComponent("test0", "testComponent1");
        
        result = testModel.getComponents("test0");
        assertEquals(2, result.size());
        assertFalse(result.contains("testComponent1"));
    }
    
    @Test
    public void testAddRemove() {
        
        final int [] results = new int[4];
        final Object [] resultsComponents = new Object[results.length];

        AccordionModelChangeListener<String, String> l =
                new AccordionModelChangeListener<String, String>() {

            @Override
            public void headerAdded(AccordionHeaderEvent<String> e) {
                results[2]++;
                resultsComponents[2] = e.getSource();
            }

            @Override
            public void componentAdded(AccordionComponentEvent<String, String> e) {
                results[1]++;
                resultsComponents[1] = e.getSource();
            }

            @Override
            public void componentRemoved(AccordionComponentEvent<String, String> e) {
                results[0]++;
                resultsComponents[0] = e.getSource();
            }
            
            @Override
            public void headerRemoved(AccordionHeaderEvent<String> e) {
                results[3]++;
                resultsComponents[3] = e.getSource();
            }
        };
        
        AccordionModel<String, String> testModel = new AccordionModel<>();
        testModel.addAccordionModelChangeListener(l);
        
        boolean result = testModel.addComponent("test", "testComponent");
        assertTrue(result);
        
        assertEquals(2, testModel.size());
        assertEquals(1, testModel.headerSize());
        
        result = testModel.removeComponent("test", "testComponent");
        assertTrue(result);
        
        assertEquals(1, testModel.size());
        assertEquals(1, testModel.headerSize());
        
        assertEquals(1, results[0]);
        assertEquals(1, results[1]);
        assertEquals(1, results[2]);
        
        assertEquals("testComponent", resultsComponents[0]);
        assertEquals("testComponent", resultsComponents[1]);
        assertEquals("test", resultsComponents[2]);
        
        result = testModel.removeHeader("test");
        assertTrue(result);
        assertEquals(1, results[0]);
        assertEquals(1, results[1]);
        assertEquals(1, results[2]);
        assertEquals(1, results[3]);
    }
}

