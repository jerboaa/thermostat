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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

public class LogTagStatsFilterDecoratorTest {

    // should not matter
    private static final LogTag LOG_TAG = LogTag.STORAGE_BACKING_PROXIED;
    private LogTagStatsFilterDecorator<TestLineStat, TestLineStats> filter;
    
    @Before
    public void setup() {
        filter = new LogTagStatsFilterDecorator<>(new MatchAllStatsFilter(), LOG_TAG);
    }
    
    @Test
    public void verifyBucketName() {
        assertEquals(MatchAllStatsFilter.BUCKETNAME + "-" + LOG_TAG.toString(), filter.getBucketName());
    }
    
    @Test
    public void verifyFilterMatches() {
        LineStat stat = mock(LineStat.class);
        assertTrue("pre: should match all stats", new MatchAllStatsFilter().matches(stat));
        when(stat.getLogTag()).thenReturn(LogTag.STORAGE_BACKING_PROXIED);
        assertTrue(filter.matches(stat));
        LineStat statLogTagMismatch = mock(LineStat.class);
        when(statLogTagMismatch.getLogTag()).thenReturn(LogTag.STORAGE_FRONT_END);
        assertFalse(filter.matches(statLogTagMismatch));
        assertFalse(filter.matches(mock(LineStat.class)));
    }
    
    @Test
    public void verifyStatsClass() {
        assertEquals(TestLineStats.class, filter.getStatsClass());
    }
    
    private static class MatchAllStatsFilter implements LineStatsFilter<TestLineStat, TestLineStats> {

        private static final String BUCKETNAME = "base-name";
        
        @Override
        public boolean matches(LineStat stat) {
            return true;
        }

        @Override
        public String getBucketName() {
            return BUCKETNAME;
        }

        @Override
        public Class<TestLineStats> getStatsClass() {
            return TestLineStats.class;
        }
        
    }
    
    private static class TestLineStat implements LineStat {

        @Override
        public LogTag getLogTag() {
            return null;
        }

        @Override
        public Date getTimeStamp() {
            return new Date();
        }
        
    };
    
    private static class TestLineStats extends BasicStats<TestLineStat> {

        @Override
        public void printSummary(PrintStream out) {
            // no-op
        }
        
    }
}
