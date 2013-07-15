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

package com.redhat.thermostat.storage.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.model.TimeStampedPojo;

public class HostLatestPojoListGetterTest {
    private static final String AGENT_ID = "agentid";
    private static final String HOSTNAME = "host.example.com";
    private static final String CATEGORY_NAME = "hostcategory";
    // Make this one static so we don't get IllegalStateException from trying
    // to make category of same name while running tests in same classloader.
    private static final Category<TestPojo> cat =  new Category<>(CATEGORY_NAME, TestPojo.class);

    private static long t1 = 1;
    private static long t2 = 5;
    private static long t3 = 10;

    private static Double load5_1 = 2.0;
    private static Double load5_2 = 6.0;
    private static Double load5_3 = 11.0;

    private static Double load10_1 = 3.0;
    private static Double load10_2 = 7.0;
    private static Double load10_3 = 12.0;

    private static Double load15_1 = 4.0;
    private static Double load15_2 = 8.0;
    private static Double load15_3 = 13.0;

    private HostRef ref;
    private TestPojo result1, result2, result3;

    @Before
    public void setUp() {
        final double[] d1 = new double[] { load5_1, load10_1, load15_1 };
        final double[] d2 = new double[] { load5_2, load10_2, load15_2 };
        final double[] d3 = new double[] { load5_3, load10_3, load15_3 };
        ref = new HostRef(AGENT_ID, HOSTNAME);
        result1 = mock(TestPojo.class);
        when(result1.getTimeStamp()).thenReturn(t1);
        when(result1.getData()).thenReturn(d1);
        result2 = mock(TestPojo.class);
        when(result2.getTimeStamp()).thenReturn(t2);
        when(result2.getData()).thenReturn(d2);
        result3 = mock(TestPojo.class);
        when(result3.getTimeStamp()).thenReturn(t3);
        when(result3.getData()).thenReturn(d3);
    }

    @Test
    public void testBuildQuery() throws DescriptorParsingException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> query = (PreparedStatement<TestPojo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(query);

        HostLatestPojoListGetter<TestPojo> getter = new HostLatestPojoListGetter<>(storage, cat);
        query = getter.buildQuery(ref, 123);

        assertNotNull(query);
        verify(query).setString(0, ref.getAgentId());
        verify(query).setLong(1, 123L);
        verifyNoMoreInteractions(query);
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<TestPojo> anyDescriptor() {
        return (StatementDescriptor<TestPojo>) any(StatementDescriptor.class);
    }

    @Test
    public void testBuildQueryPopulatesUpdateTimes() throws DescriptorParsingException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> ignored = (PreparedStatement<TestPojo>) mock(PreparedStatement.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> query = (PreparedStatement<TestPojo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(ignored).thenReturn(query);

        HostLatestPojoListGetter<TestPojo> getter = new HostLatestPojoListGetter<>(storage, cat);
        ignored = getter.buildQuery(ref,Long.MIN_VALUE); // Ignore first return value.

        query = getter.buildQuery(ref, Long.MIN_VALUE);

        assertNotNull(query);
        verify(storage, times(2)).prepareStatement(anyDescriptor());
        verify(query).setString(0, ref.getAgentId());
        verify(query).setLong(1, Long.MIN_VALUE);
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetLatest() throws DescriptorParsingException, StatementExecutionException {
        @SuppressWarnings("unchecked")
        Cursor<TestPojo> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(result1).thenReturn(result2).thenReturn(null);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<TestPojo> query = (PreparedStatement<TestPojo>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(query);
        when(query.executeQuery()).thenReturn(cursor);

        HostLatestPojoListGetter<TestPojo> getter = new HostLatestPojoListGetter<>(storage, cat);

        List<TestPojo> stats = getter.getLatest(ref, Long.MIN_VALUE);

        assertNotNull(stats);
        assertEquals(2, stats.size());
        TestPojo stat1 = stats.get(0);
        assertEquals(t1, stat1.getTimeStamp());
        assertArrayEquals(new double[] {load5_1, load10_1, load15_1}, stat1.getData(), 0.001);
        TestPojo stat2 = stats.get(1);
        assertEquals(t2, stat2.getTimeStamp());
        assertArrayEquals(new double[] {load5_2, load10_2, load15_2}, stat2.getData(), 0.001);
    }

    @After
    public void tearDown() {
        ref = null;
        result1 = null;
        result2 = null;
        result3 = null;
    }
    
    private static interface TestPojo extends TimeStampedPojo {
        
        double[] getData();
        
    }

}

