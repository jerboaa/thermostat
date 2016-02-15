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

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Using eclEmma tool has been proved that this test covers 100% 
 * of {@link SquarifiedTreeMap} code and also 90% of {@link TreeMapBuilder} code.
 */
public class SquarifiedTreeMapTest {
    
    private SquarifiedTreeMap algorithm;
    Rectangle2D.Double bounds;
    List<TreeMapNode> list;

    @Before
    public void setUp() throws Exception {
        bounds = new Rectangle2D.Double(0, 0, 10, 5);
        list = new ArrayList<>();
    }

    @Test
    public final void testSquarifiedTreeMap() {
        //check every parameters combinations
        boolean catched = false;
        try {
            algorithm = new SquarifiedTreeMap(null, null);
        } catch(NullPointerException e) {
            catched = true;
        }
        assertTrue(catched);
        catched = false;
        
        try {
            algorithm = new SquarifiedTreeMap(bounds, null);
        } catch(NullPointerException e) {
            catched = true;
        }
        assertTrue(catched);
        catched = false;
        
        try {
            algorithm = new SquarifiedTreeMap(null, list);
        } catch(NullPointerException e) {
            catched = true;
        }
        assertTrue(catched);
    }
    
    @Test
    public final void testSquarify() {
        // test using an empty node list
        algorithm = new SquarifiedTreeMap(bounds, new ArrayList<TreeMapNode>());
        assertEquals(0, algorithm.squarify().size());
        
        // test using a correct list
        int n = 10;
        for (int i = 0; i < n; i++) {
            list.add(new TreeMapNode(i+1));
        }
        // process the list
        algorithm = new SquarifiedTreeMap(bounds, list);
        list = algorithm.squarify();
        
        assertEquals(n, list.size());
        
        for (int i = 0; i < n; i++) {
            // node has been processed
            assertNotNull(list.get(i).getRectangle());
        }
        
        assertEquals(list, algorithm.getSquarifiedNodes());
    }
}
