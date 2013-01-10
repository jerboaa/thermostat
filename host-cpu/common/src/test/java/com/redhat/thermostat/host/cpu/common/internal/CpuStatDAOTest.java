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

package com.redhat.thermostat.host.cpu.common.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.host.cpu.common.CpuStatDAO;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Query.Criteria;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.model.CpuStat;

public class CpuStatDAOTest {

    @Test
    public void testCategory() {
        assertEquals("cpu-stats", CpuStatDAO.cpuStatCategory.getName());
        Collection<Key<?>> keys = CpuStatDAO.cpuStatCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId", true)));
        assertTrue(keys.contains(new Key<Long>("timeStamp", false)));
        assertTrue(keys.contains(new Key<Double>("perProcessorUsage", false)));

        assertEquals(3, keys.size());
    }

    @Test
    public void testGetLatestCpuStats() {

        @SuppressWarnings("unchecked")
        Cursor<CpuStat> cursor = mock(Cursor.class);
        Storage storage = mock(Storage.class);
        Query query = mock(Query.class);
        HostRef hostRef = mock(HostRef.class);
        CpuStatDAO dao = new CpuStatDAOImpl(storage);

        Double LOAD = 5.0;
        CpuStat cpuStat = new CpuStat(1234L, new double[] { LOAD });

        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(cpuStat);

        when(storage.createQuery(any(Category.class), any(Class.class))).thenReturn(query);
        when(query.execute()).thenReturn(cursor);
        when(hostRef.getAgentId()).thenReturn("system");

        List<CpuStat> cpuStats = dao.getLatestCpuStats(hostRef, Long.MIN_VALUE);

        verify(query).where(Key.TIMESTAMP, Criteria.GREATER_THAN, Long.MIN_VALUE);
        verify(query).where(Key.AGENT_ID, Criteria.EQUALS, "system");
        verify(query).sort(Key.TIMESTAMP, Query.SortDirection.DESCENDING);
        verify(query).execute();
        verifyNoMoreInteractions(query);

        assertEquals(1, cpuStats.size());
        CpuStat stat = cpuStats.get(0);
        assertEquals(1234L, stat.getTimeStamp());
        assertArrayEquals(new double[] { LOAD }, stat.getPerProcessorUsage(), 0.001);

    }

    @Test
    public void testGetLatestCpuStatsTwice() {

        @SuppressWarnings("unchecked")
        Cursor<CpuStat> cursor = mock(Cursor.class);
        Storage storage = mock(Storage.class);
        Query query = mock(Query.class);
        HostRef hostRef = mock(HostRef.class);

        CpuStatDAO dao = new CpuStatDAOImpl(storage);

        CpuStat cpuStat = new CpuStat(1234L, new double[] { 5.0 });

        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(cpuStat);

        when(storage.createQuery(CpuStatDAO.cpuStatCategory, CpuStat.class)).thenReturn(query);
        when(query.execute()).thenReturn(cursor);
        when(hostRef.getAgentId()).thenReturn("system");

        dao.getLatestCpuStats(hostRef, Long.MIN_VALUE);
        dao.getLatestCpuStats(hostRef, Long.MIN_VALUE);

        verify(query, times(2)).execute();
        verify(query, atLeastOnce()).where(same(Key.AGENT_ID), same(Criteria.EQUALS), any());
        verify(query, atLeastOnce()).where(same(Key.TIMESTAMP), any(Criteria.class), any());
    }

    @Test
    public void testPutCpuStat() {
        Storage storage = mock(Storage.class);
        Add add = mock(Add.class);
        when(storage.createAdd(any(Category.class))).thenReturn(add);
        
        CpuStat stat = new CpuStat(1,  new double[] {5.0, 10.0, 15.0});
        CpuStatDAO dao = new CpuStatDAOImpl(storage);
        dao.putCpuStat(stat);

        verify(storage).createAdd(CpuStatDAO.cpuStatCategory);
        verify(add).setPojo(stat);
        verify(add).apply();
    }

    @Test
    public void testGetCount() {
        Storage storage = mock(Storage.class);
        when(storage.getCount(any(Category.class))).thenReturn(5L);
        CpuStatDAO dao = new CpuStatDAOImpl(storage);
        Long count = dao.getCount();
        assertEquals((Long) 5L, count);
    }
}
