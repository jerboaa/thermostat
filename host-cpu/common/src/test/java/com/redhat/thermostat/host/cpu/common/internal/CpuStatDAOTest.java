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

package com.redhat.thermostat.host.cpu.common.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.host.cpu.common.CpuStatDAO;
import com.redhat.thermostat.host.cpu.common.model.CpuStat;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;

public class CpuStatDAOTest {

    @Test
    public void testCategory() {
        assertEquals("cpu-stats", CpuStatDAO.cpuStatCategory.getName());
        Collection<Key<?>> keys = CpuStatDAO.cpuStatCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<Long>("timeStamp")));
        assertTrue(keys.contains(new Key<Double>("perProcessorUsage")));

        assertEquals(3, keys.size());
    }

    @Test
    public void testGetLatestCpuStats() throws DescriptorParsingException, StatementExecutionException {

        @SuppressWarnings("unchecked")
        Cursor<CpuStat> cursor = (Cursor<CpuStat>) mock(Cursor.class);
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<CpuStat> stmt = (PreparedStatement<CpuStat>) mock(PreparedStatement.class);
        HostRef hostRef = mock(HostRef.class);
        CpuStatDAO dao = new CpuStatDAOImpl(storage);

        Double LOAD = 5.0;
        CpuStat cpuStat = new CpuStat("foo", 1234L, new double[] { LOAD });

        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(cpuStat);

        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);
        when(hostRef.getAgentId()).thenReturn("system");

        List<CpuStat> cpuStats = dao.getLatestCpuStats(hostRef, Long.MIN_VALUE);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "system");
        verify(stmt).setLong(1, Long.MIN_VALUE);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(1, cpuStats.size());
        CpuStat stat = cpuStats.get(0);
        assertEquals(1234L, stat.getTimeStamp());
        assertArrayEquals(new double[] { LOAD }, stat.getPerProcessorUsage(), 0.001);

    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<CpuStat> anyDescriptor() {
        return (StatementDescriptor<CpuStat>) any(StatementDescriptor.class);
    }

    @Test
    public void testGetLatestCpuStatsTwice() throws DescriptorParsingException, StatementExecutionException {
        @SuppressWarnings("unchecked")
        Cursor<CpuStat> cursor = (Cursor<CpuStat>) mock(Cursor.class);
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<CpuStat> stmt = (PreparedStatement<CpuStat>) mock(PreparedStatement.class);
        HostRef hostRef = mock(HostRef.class);

        CpuStatDAO dao = new CpuStatDAOImpl(storage);

        CpuStat cpuStat = new CpuStat("foo", 1234L, new double[] { 5.0 });

        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(cpuStat);

        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);
        when(hostRef.getAgentId()).thenReturn("system");

        dao.getLatestCpuStats(hostRef, Long.MIN_VALUE);
        dao.getLatestCpuStats(hostRef, Long.MIN_VALUE);

        verify(storage, atLeastOnce()).prepareStatement(anyDescriptor());
        verify(stmt, times(2)).setString(0, "system");
        verify(stmt, times(2)).setLong(1, Long.MIN_VALUE);
        verify(stmt, times(2)).executeQuery();
        verifyNoMoreInteractions(stmt);
    }

    @Test
    public void testPutCpuStat() {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        Add<CpuStat> add = mock(Add.class);
        when(storage.createAdd(eq(CpuStatDAO.cpuStatCategory))).thenReturn(add);
        
        CpuStat stat = new CpuStat("foo", 1,  new double[] {5.0, 10.0, 15.0});
        CpuStatDAO dao = new CpuStatDAOImpl(storage);
        dao.putCpuStat(stat);

        verify(storage).createAdd(CpuStatDAO.cpuStatCategory);
        verify(add).setPojo(stat);
        verify(add).apply();
    }
}

