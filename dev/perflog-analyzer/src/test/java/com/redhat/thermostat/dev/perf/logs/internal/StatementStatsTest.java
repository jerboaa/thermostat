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

package com.redhat.thermostat.dev.perf.logs.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.dev.perf.logs.Direction;
import com.redhat.thermostat.dev.perf.logs.SortBy;
import com.redhat.thermostat.dev.perf.logs.StatsConfig;

public class StatementStatsTest {

    private static final double AVG_DELTA = 0.001;
    private static final String desc1 = "QUERY foo WHERE 'a' = ?s";
    private static final String desc2 = "QUERY foo-bar WHERE 'x' = ?l";
    private static final String desc3 = "ADD foo SET 'a' = ?s";
    private static final int desc1Id = 0;
    private static final int desc2Id = 1;
    private static final int desc3Id = 2;
    private StatementStat s1;
    private StatementStat s2;
    private StatementStat s3;
    private StatementStat s4;
    private StatementStat s5;
    private StatementStat s6;
    private StatementStats stats;
    private SharedStatementState state;
    
    @Before
    public void setup() {
        state = mock(SharedStatementState.class);
        when(state.getMappedStatementId(desc1)).thenReturn(desc1Id);
        when(state.getMappedStatementId(desc2)).thenReturn(desc2Id);
        when(state.getMappedStatementId(desc3)).thenReturn(desc3Id);
        List<StatementStat> s = buildStats(state);
        stats = new StatementStats(s, state);
    }
    
    @Test
    public void canGetMaxForDescriptors() {
        assertEquals(120, stats.getMaxExecTime(desc1));
        assertEquals(333, stats.getMaxExecTime(desc2));
        assertEquals(44, stats.getMaxExecTime(desc3));
    }
    
    @Test
    public void canGetMinForDescriptors() {
        assertEquals(2, stats.getMinExecTime(desc1));
        assertEquals(300, stats.getMinExecTime(desc2));
        assertEquals(44, stats.getMinExecTime(desc3));
    }
    
    @Test
    public void canGetAvgForDescriptors() {
        double avg = (12 + 120 + 2)/(double)3;
        assertEquals(avg, stats.getAverage(desc1), AVG_DELTA);
        avg = (300 + 333)/(double)2;
        assertEquals(avg, stats.getAverage(desc2), AVG_DELTA);
        assertEquals(44, stats.getAverage(desc3), AVG_DELTA);
    }
    
    @Test
    public void testNumberRecords() {
        assertEquals(6, stats.getTotalNumberOfRecords());
    }
    
    @Test
    public void canGetDistinctStatements() {
        Set<Integer> expected = new HashSet<>();
        expected.add(desc1Id);
        expected.add(desc2Id);
        expected.add(desc3Id);
        List<StatementStat> distincts = stats.getDistinctStatements();
        assertEquals(3, distincts.size());
        Set<Integer> actual = new HashSet<>();
        for (StatementStat s: distincts) {
            actual.add(s.getDescId());
        }
        assertEquals(expected, actual);
    }
    
    @Test
    public void canGetSortedDistincts() {
        // count sorts
        List<StatementStat> distincts = stats.getDistinctStatements(SortBy.COUNT, Direction.DSC);
        assertEquals(3, distincts.size());
        assertEquals(desc1Id, distincts.get(0).getDescId());
        assertEquals(desc2Id, distincts.get(1).getDescId());
        assertEquals(desc3Id, distincts.get(2).getDescId());
        distincts = stats.getDistinctStatements(SortBy.COUNT, Direction.ASC);
        assertEquals(3, distincts.size());
        assertEquals(desc3Id, distincts.get(0).getDescId());
        assertEquals(desc2Id, distincts.get(1).getDescId());
        assertEquals(desc1Id, distincts.get(2).getDescId());
        
        // min sorts
        distincts = stats.getDistinctStatements(SortBy.MIN, Direction.ASC);
        assertEquals(3, distincts.size());
        assertEquals(desc1Id, distincts.get(0).getDescId());
        assertEquals(desc3Id, distincts.get(1).getDescId());
        assertEquals(desc2Id, distincts.get(2).getDescId());
        distincts = stats.getDistinctStatements(SortBy.MIN, Direction.DSC);
        assertEquals(3, distincts.size());
        assertEquals(desc2Id, distincts.get(0).getDescId());
        assertEquals(desc3Id, distincts.get(1).getDescId());
        assertEquals(desc1Id, distincts.get(2).getDescId());
        
        // max sorts
        distincts = stats.getDistinctStatements(SortBy.MAX, Direction.ASC);
        assertEquals(3, distincts.size());
        assertEquals(desc3Id, distincts.get(0).getDescId());
        assertEquals(desc1Id, distincts.get(1).getDescId());
        assertEquals(desc2Id, distincts.get(2).getDescId());
        distincts = stats.getDistinctStatements(SortBy.MAX, Direction.DSC);
        assertEquals(3, distincts.size());
        assertEquals(desc2Id, distincts.get(0).getDescId());
        assertEquals(desc1Id, distincts.get(1).getDescId());
        assertEquals(desc3Id, distincts.get(2).getDescId());
        
        // avg sorts
        distincts = stats.getDistinctStatements(SortBy.AVG, Direction.ASC);
        assertEquals(3, distincts.size());
        assertEquals(desc3Id, distincts.get(0).getDescId());
        assertEquals(desc1Id, distincts.get(1).getDescId());
        assertEquals(desc2Id, distincts.get(2).getDescId());
        distincts = stats.getDistinctStatements(SortBy.AVG, Direction.DSC);
        assertEquals(3, distincts.size());
        assertEquals(desc2Id, distincts.get(0).getDescId());
        assertEquals(desc1Id, distincts.get(1).getDescId());
        assertEquals(desc3Id, distincts.get(2).getDescId());
    }
    
    @Test
    public void canGetCountPerDescriptor() {
        assertEquals(3, stats.getTotalCount(desc1));
        assertEquals(2, stats.getTotalCount(desc2));
        assertEquals(1, stats.getTotalCount(desc3));
    }
    
    @Test
    public void canGetNumReads() {
        assertEquals(5, stats.getNumReads());
    }
    
    @Test
    public void canGetNumWrites() {
        assertEquals(1, stats.getNumWrites());
    }
    
    @Test
    public void canGetStats() {
        List<StatementStat> s = stats.getAllStats();
        assertNotNull(s);
        assertEquals(6, s.size());
    }
    
    @Test
    public void verifyNoArgConstructor() throws InstantiationException, IllegalAccessException {
        StatementStats stats = (StatementStats)StatementStats.class.newInstance();
        assertNotNull("stats factory uses no-arg constructor", stats);
    }
    
    @Test
    public void verifySummaryPrinting() {
        when(state.getDescriptor(desc1Id)).thenReturn(desc1);
        when(state.getDescriptor(desc2Id)).thenReturn(desc2);
        when(state.getDescriptor(desc3Id)).thenReturn(desc3);
        
        StatsConfig config = new StatsConfig(new File("foo-bar"), SortBy.AVG, Direction.DSC, false);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pStream = new PrintStream(baos);
        stats.setConfig(config);
        stats.printSummary(pStream);
        String expected = "Statement statistics (6 records): 3 distinct statements (5 reads, 1 writes)\n" +
                          "\n" +
                          "Statement details (sorted by avg exec time):\n" +
                          "Total #: 2, 300µs (min), 333µs (max), 316.50µs (avg), DESCRIPTOR: QUERY foo-bar WHERE 'x' = ?l\n" +
                          "Total #: 3, 2ms (min), 120ms (max), 44.67ms (avg), DESCRIPTOR: QUERY foo WHERE 'a' = ?s\n" +
                          "Total #: 1, 44ns (min), 44ns (max), 44.00ns (avg), DESCRIPTOR: ADD foo SET 'a' = ?s\n\n";
        String summary = baos.toString();
        assertEquals(expected, summary);
    }
    
    private List<StatementStat> buildStats(SharedStatementState state) {
        List<StatementStat> stats = new ArrayList<>();
        int id = desc1Id;
        LogTag logTag = LogTag.STORAGE_BACKING_PROXIED;
        s1 = new ReadStatementStat(state, new Date(), logTag, id, new Duration(12, TimeUnit.MILLISECONDS));
        s2 = new ReadStatementStat(state, new Date(), logTag, id, new Duration(120, TimeUnit.MILLISECONDS));
        s3 = new ReadStatementStat(state, new Date(), logTag, id, new Duration(2, TimeUnit.MILLISECONDS));
        id = desc2Id;
        s4 = new ReadStatementStat(state, new Date(), logTag, id, new Duration(300, TimeUnit.MICROSECONDS));
        s5 = new ReadStatementStat(state, new Date(), logTag, id, new Duration(333, TimeUnit.MICROSECONDS));
        id = desc3Id;
        s6 = new WriteStatementStat(state, new Date(), logTag, id, new Duration(44, TimeUnit.NANOSECONDS), 3);
        // adding order should not matter
        stats.add(s1);
        stats.add(s3);
        stats.add(s2);
        stats.add(s4);
        stats.add(s6);
        stats.add(s5);
        return stats;
    }
}
