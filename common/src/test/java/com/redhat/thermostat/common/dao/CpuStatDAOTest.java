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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.model.CpuStat;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

public class CpuStatDAOTest {

    @Test
    public void testCategory() {
        assertEquals("cpu-stats", CpuStatDAO.cpuStatCategory.getName());
        Collection<Key<?>> keys = CpuStatDAO.cpuStatCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agent-id", false)));
        assertTrue(keys.contains(new Key<Long>("timestamp", false)));
        assertTrue(keys.contains(new Key<Double>("5load", false)));
        assertTrue(keys.contains(new Key<Double>("10load", false)));
        assertTrue(keys.contains(new Key<Double>("15load", false)));
        assertEquals(5, keys.size());
    }

    @Test
    public void testGetLatestCpuStats() {

        Cursor cursor = mock(Cursor.class);
        Storage storage = mock(Storage.class);
        HostRef hostRef = mock(HostRef.class);
        CpuStatDAO dao = new CpuStatDAOImpl(storage);

        Chunk chunk = new Chunk(CpuStatDAO.cpuStatCategory, false);
        chunk.put(Key.TIMESTAMP, 1234L);
        chunk.put(CpuStatDAO.cpu5LoadKey, 5.0);
        chunk.put(CpuStatDAO.cpu10LoadKey, 10.0);
        chunk.put(CpuStatDAO.cpu15LoadKey, 15.0);

        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(chunk);
        when(storage.findAll(any(Chunk.class))).thenReturn(cursor);
        when(hostRef.getAgentId()).thenReturn("system");

        List<CpuStat> cpuStats = dao.getLatestCpuStats(hostRef);

        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).findAll(arg.capture());
        assertNull(arg.getValue().get(new Key<String>("$where", false)));

        assertEquals(1, cpuStats.size());
        CpuStat stat = cpuStats.get(0);
        assertEquals(1234L, stat.getTimeStamp());
        assertEquals(5.0, stat.getLoad5(), 0.001);
        assertEquals(10.0, stat.getLoad10(), 0.001);
        assertEquals(15.0, stat.getLoad15(), 0.001);
    }

    @Test
    public void testGetLatestCpuStatsTwice() {

        Cursor cursor = mock(Cursor.class);
        Storage storage = mock(Storage.class);
        HostRef hostRef = mock(HostRef.class);

        CpuStatDAO dao = new CpuStatDAOImpl(storage);

        Chunk chunk = new Chunk(CpuStatDAO.cpuStatCategory, false);
        chunk.put(Key.TIMESTAMP, 1234L);
        chunk.put(CpuStatDAO.cpu5LoadKey, 5.0);
        chunk.put(CpuStatDAO.cpu10LoadKey, 10.0);
        chunk.put(CpuStatDAO.cpu15LoadKey, 15.0);

        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(chunk);
        when(storage.findAll(any(Chunk.class))).thenReturn(cursor);
        when(hostRef.getAgentId()).thenReturn("system");

        dao.getLatestCpuStats(hostRef);
        dao.getLatestCpuStats(hostRef);

        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage, times(2)).findAll(arg.capture());
        assertEquals("this.timestamp > 1234", arg.getValue().get(Key.WHERE));
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
