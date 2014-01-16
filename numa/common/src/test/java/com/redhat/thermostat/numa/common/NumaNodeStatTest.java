/*
 * Copyright 2012-2014 Red Hat, Inc.
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


package com.redhat.thermostat.numa.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NumaNodeStatTest {

    private NumaNodeStat stat;

    @Before
    public void setUp() {
        stat = new NumaNodeStat();
        stat.setNodeId(1);
        stat.setNumaHit(2);
        stat.setNumaMiss(3);
        stat.setNumaForeign(4);
        stat.setInterleaveHit(5);
        stat.setLocalNode(6);
        stat.setOtherNode(7);
    }

    @After
    public void tearDown() {
        stat = null;
    }

    @Test
    public void testDefaults() {
        NumaNodeStat stat = new NumaNodeStat();
        assertEquals(-1, stat.getNodeId());
        assertEquals(-1, stat.getNumaHit());
        assertEquals(-1, stat.getNumaMiss());
        assertEquals(-1, stat.getNumaForeign());
        assertEquals(-1, stat.getInterleaveHit());
        assertEquals(-1, stat.getLocalNode());
        assertEquals(-1, stat.getOtherNode());
    }

    @Test
    public void testProperties() {

        assertEquals(1, stat.getNodeId());
        assertEquals(2, stat.getNumaHit());
        assertEquals(3, stat.getNumaMiss());
        assertEquals(4, stat.getNumaForeign());
        assertEquals(5, stat.getInterleaveHit());
        assertEquals(6, stat.getLocalNode());
        assertEquals(7, stat.getOtherNode());
        
    }

    @Test
    public void testToString() {
        NumaNodeStat stat = new NumaNodeStat();
        stat.setNodeId(1);
        stat.setNumaHit(2);
        stat.setNumaMiss(3);
        stat.setNumaForeign(4);
        stat.setInterleaveHit(5);
        stat.setLocalNode(6);
        stat.setOtherNode(7);

        String str = stat.toString();
        String expected = "NumaStat: nodeId: 1, numaHit: 2, numaMiss: 3, numaForeign: 4, interleaveHit: 5, localNode: 6, otherNode: 7";
        assertEquals(expected, str);
    }
    
    @Test
    public void testBasicInstantiation() {
        try {
            // pojo converters use this
            NumaNodeStat.class.newInstance();
        } catch (Exception e) {
            fail("should be able to instantiate using no-arg constructor");
        }
    }
}

