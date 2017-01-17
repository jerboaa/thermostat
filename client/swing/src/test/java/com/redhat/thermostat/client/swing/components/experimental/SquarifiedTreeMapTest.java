/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class SquarifiedTreeMapTest {

    private static final double REGION_WIDTH = 10.0;
    private static final double REGION_HEIGHT = 5.0;
    private static final double BASE = 2.0;
    private static final double DELTA = 0.01;

    private SquarifiedTreeMap algorithm;
    Rectangle2D.Double region;
    LinkedList<TreeMapNode> elements;

    @Before
    public void setUp() throws Exception {
        region = new Rectangle2D.Double(0, 0, REGION_WIDTH, REGION_HEIGHT);
        elements = new LinkedList<>();
    }

    @Test
    public void validateNan() {
        algorithm = new SquarifiedTreeMap(region, elements);
        assertEquals(0, algorithm.validate(Double.NaN), DELTA);
    }

    @Test
    public final void testSquarifiedTreeMapWithInvalidParameters() {
        //check every parameters combinations
        boolean caught = false;
        try {
            algorithm = new SquarifiedTreeMap(null, null);
        } catch(NullPointerException e) {
            caught = true;
        }
        assertTrue(caught);
        caught = false;
        
        try {
            algorithm = new SquarifiedTreeMap(region, null);
        } catch(NullPointerException e) {
            caught = true;
        }
        assertTrue(caught);
        caught = false;
        
        try {
            algorithm = new SquarifiedTreeMap(null, elements);
        } catch(NullPointerException e) {
            caught = true;
        }
        assertTrue(caught);
    }

    @Test
    public final void testSquarifyWithoutData() {
        algorithm = new SquarifiedTreeMap(region, new LinkedList<TreeMapNode>());
        assertTrue(algorithm.squarify().isEmpty());
    }

    @Test
    public final void testSquarifyWithData() {
        int numSiblings = 5;
        for (int i = (numSiblings - 1); i >= 0; i--) {
            elements.add(new TreeMapNode(Math.pow(BASE, i)));
        }

        algorithm = new SquarifiedTreeMap(region, elements);
        Map<TreeMapNode, Rectangle2D.Double> squarifiedMap = algorithm.squarify();
        assertEquals(numSiblings, squarifiedMap.size());

        double refArea = squarifiedMap.get(elements.get(0)).getHeight() *
                squarifiedMap.get(elements.get(0)).getWidth();
        double totArea = refArea;
        for (int i = 1; i < numSiblings; i++) {
            double currArea = squarifiedMap.get(elements.get(i)).getHeight() *
                    squarifiedMap.get(elements.get(i)).getWidth();
            totArea += currArea;
            // check that the ratio between areas is 2 (which is the BASE)
            assertEquals(BASE, refArea / currArea, DELTA);
            refArea = currArea;
        }
        
        assertEquals(region.getWidth() * region.getHeight(), totArea, DELTA);
    }
}
