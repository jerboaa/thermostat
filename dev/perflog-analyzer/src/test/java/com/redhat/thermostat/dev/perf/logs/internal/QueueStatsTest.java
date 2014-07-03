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

package com.redhat.thermostat.dev.perf.logs.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class QueueStatsTest {

    private static final double DELTA = 0.001;
    private QueueStats stats;
    
    @Before
    public void setup() {
        List<QueueStat> s = buildStats();
        stats = new QueueStats(s);
    }
    
    @Test
    public void testQueueMax() {
        assertEquals(12, stats.getMax());
    }
    
    @Test
    public void testQueueMin() {
        assertEquals(1, stats.getMin());
    }
    
    @Test
    public void testQueueAvg() {
        double avg = (1 + 3 + 12 + 4 + 8)/(double)5;
        assertEquals(avg, stats.getAvgQueueSize(), DELTA);
    }
    
    @Test
    public void testNumberRecords() {
        assertEquals(5, stats.getTotalNumberOfRecords());
    }
    
    @Test
    public void testNoRecords() {
        stats = new QueueStats(Collections.<QueueStat>emptyList());
        assertEquals(0, stats.getTotalNumberOfRecords());
        assertEquals(-1, stats.getAvgQueueSize(), DELTA);
        assertEquals(-1, stats.getMax());
        assertEquals(-1, stats.getMin());
    }
    
    @Test
    public void canGetStats() {
        List<QueueStat> s = stats.getAllStats();
        assertNotNull(s);
        assertEquals(5, s.size());
    }
    
    private List<QueueStat> buildStats() {
        List<QueueStat> sts = new ArrayList<>();
        int[] sizes = new int[] {
          1, 3, 12, 4 , 8     
        };
        for (int size: sizes) {
            QueueStat s = new QueueStat(new Date(), "foo", size);
            sts.add(s);
        }
        return sts;
    }
}
