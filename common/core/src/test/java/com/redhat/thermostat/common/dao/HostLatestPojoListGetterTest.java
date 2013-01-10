/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.common.dao;

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

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.CpuStat;

public class HostLatestPojoListGetterTest {
    private static final String AGENT_ID = "agentid";
    private static final String HOSTNAME = "host.example.com";
    private static final String CATEGORY_NAME = "hostcategory";
    // Make this one static so we don't get IllegalStateException from trying
    // to make category of same name while running tests in same classloader.
    private static final Category cat =  new Category(CATEGORY_NAME);

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
    private CpuStat result1, result2, result3;

    @Before
    public void setUp() {
        ref = new HostRef(AGENT_ID, HOSTNAME);
        result1 = new CpuStat(t1, new double[] { load5_1, load10_1, load15_1 } );
        result2 = new CpuStat(t2, new double[] { load5_2, load10_2, load15_2 } );
        result3 = new CpuStat(t3, new double[] { load5_3, load10_3, load15_3 } );
    }

    @Test
    public void testBuildQuery() {
        Storage storage = mock(Storage.class);
        Query query = mock(Query.class);
        when (storage.createQuery(any(Category.class), any(Class.class))).thenReturn(query);

        HostLatestPojoListGetter<CpuStat> getter = new HostLatestPojoListGetter<>(storage, cat, CpuStat.class);
        query = getter.buildQuery(ref, 123);

        assertNotNull(query);
        verify(storage).createQuery(cat, CpuStat.class);
        verify(query).where(Key.TIMESTAMP, Criteria.GREATER_THAN, 123l);
        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, AGENT_ID);
        verify(query).sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING);
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testBuildQueryPopulatesUpdateTimes() {
        Storage storage = mock(Storage.class);
        Query ignored = mock(Query.class);
        Query query = mock(Query.class);
        when(storage.createQuery(any(Category.class), any(Class.class))).thenReturn(ignored).thenReturn(query);

        HostLatestPojoListGetter<CpuStat> getter = new HostLatestPojoListGetter<>(storage, cat, CpuStat.class);
        ignored = getter.buildQuery(ref,Long.MIN_VALUE); // Ignore first return value.

        query = getter.buildQuery(ref, Long.MIN_VALUE);

        assertNotNull(query);
        verify(storage, times(2)).createQuery(cat, CpuStat.class);
        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, AGENT_ID);
        verify(query).where(Key.TIMESTAMP, Criteria.GREATER_THAN, Long.MIN_VALUE);
        verify(query).sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING);
        verifyNoMoreInteractions(query);
    }

    @Test
    public void testGetLatest() {
        @SuppressWarnings("unchecked")
        Cursor<CpuStat> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(result1).thenReturn(result2).thenReturn(null);

        Storage storage = mock(Storage.class);
        Query query = mock(Query.class);
        when(storage.createQuery(any(Category.class), any(Class.class))).thenReturn(query);
        when(query.execute()).thenReturn(cursor);

        HostLatestPojoListGetter<CpuStat> getter = new HostLatestPojoListGetter<>(storage, cat, CpuStat.class);

        List<CpuStat> stats = getter.getLatest(ref, Long.MIN_VALUE);

        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, AGENT_ID);
        verify(query).where(Key.TIMESTAMP, Criteria.GREATER_THAN, Long.MIN_VALUE);

        assertNotNull(stats);
        assertEquals(2, stats.size());
        CpuStat stat1 = stats.get(0);
        assertEquals(t1, stat1.getTimeStamp());
        assertArrayEquals(new double[] {load5_1, load10_1, load15_1}, stat1.getPerProcessorUsage(), 0.001);
        CpuStat stat2 = stats.get(1);
        assertEquals(t2, stat2.getTimeStamp());
        assertArrayEquals(new double[] {load5_2, load10_2, load15_2}, stat2.getPerProcessorUsage(), 0.001);
    }

    @After
    public void tearDown() {
        ref = null;
        result1 = null;
        result2 = null;
        result3 = null;
    }
}
