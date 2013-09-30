/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import static org.junit.Assert.*;

import org.junit.Test;

public class AccordionModelTest {
    
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
