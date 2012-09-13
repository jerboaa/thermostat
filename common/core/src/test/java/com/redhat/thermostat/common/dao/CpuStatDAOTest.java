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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.common.model.CpuStat;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Cursor.SortDirection;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.utils.ArrayUtils;
import com.redhat.thermostat.test.MockQuery;

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
        MockQuery query = new MockQuery();
        HostRef hostRef = mock(HostRef.class);
        CpuStatDAO dao = new CpuStatDAOImpl(storage);

        Double LOAD = 5.0;
        List<Double> loadList = Arrays.asList(LOAD);
        CpuStat cpuStat = new CpuStat(1234L, loadList);

        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(cpuStat);
        when(cursor.sort(any(Key.class), any(SortDirection.class))).thenReturn(cursor);

        when(storage.createQuery()).thenReturn(query);
        when(storage.findAllPojos(query, CpuStat.class)).thenReturn(cursor);
        when(hostRef.getAgentId()).thenReturn("system");

        List<CpuStat> cpuStats = dao.getLatestCpuStats(hostRef);

        assertFalse(query.hasWhereClauseFor(Key.TIMESTAMP));

        assertEquals(1, cpuStats.size());
        CpuStat stat = cpuStats.get(0);
        assertEquals(1234L, stat.getTimeStamp());
        assertArrayEquals(new double[] { LOAD }, ArrayUtils.toPrimitiveDoubleArray(stat.getPerProcessorUsage()), 0.001);

    }

    @Test
    public void testGetLatestCpuStatsTwice() {

        @SuppressWarnings("unchecked")
        Cursor<CpuStat> cursor = mock(Cursor.class);
        Storage storage = mock(Storage.class);
        MockQuery query = new MockQuery();
        HostRef hostRef = mock(HostRef.class);

        CpuStatDAO dao = new CpuStatDAOImpl(storage);

        CpuStat cpuStat = new CpuStat(1234L, Arrays.asList(5.0));

        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(cpuStat);
        when(cursor.sort(any(Key.class), any(SortDirection.class))).thenReturn(cursor);

        when(storage.createQuery()).thenReturn(query);
        when(storage.findAllPojos(any(Query.class), same(CpuStat.class))).thenReturn(cursor);
        when(hostRef.getAgentId()).thenReturn("system");

        dao.getLatestCpuStats(hostRef);
        dao.getLatestCpuStats(hostRef);

        verify(storage, times(2)).findAllPojos(query, CpuStat.class);

        query.hasWhereClauseFor(Key.TIMESTAMP);
    }

    @Test
    public void testPutCpuStat() {
        Storage storage = mock(Storage.class);
        CpuStat stat = new CpuStat(1,  ArrayUtils.toDoubleList(new double[] {5.0, 10.0, 15.0}));
        CpuStatDAO dao = new CpuStatDAOImpl(storage);
        dao.putCpuStat(stat);

        verify(storage).putPojo(CpuStatDAO.cpuStatCategory, false, stat);
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
