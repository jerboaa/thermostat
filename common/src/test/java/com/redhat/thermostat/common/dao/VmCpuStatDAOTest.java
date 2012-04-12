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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.model.VmCpuStat;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

public class VmCpuStatDAOTest {

    private static final Long TIMESTAMP = 1234L;
    private static final String AGENT_ID = "test-agent-id";
    private static final Integer VM_ID = 321;
    private static final Double CPU_LOAD = 9.9;

    private Chunk chunk;

    @Before
    public void setUp() {
        chunk = new Chunk(VmCpuStatDAO.vmCpuStatCategory, false);
        chunk.put(Key.TIMESTAMP, TIMESTAMP);
        chunk.put(Key.AGENT_ID, AGENT_ID);
        chunk.put(Key.VM_ID, VM_ID);
        chunk.put(VmCpuStatDAO.vmCpuLoadKey, CPU_LOAD);
    }

    @Test
    public void testCategory() {
        assertEquals("vm-cpu-stats", VmCpuStatDAO.vmCpuStatCategory.getName());
        Collection<Key<?>> keys = VmCpuStatDAO.vmCpuStatCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agent-id", false)));
        assertTrue(keys.contains(new Key<Long>("timestamp", false)));
        assertTrue(keys.contains(new Key<Integer>("vm-id", false)));
        assertTrue(keys.contains(new Key<Integer>("processor-usage", false)));
        assertEquals(4, keys.size());
    }

    @Test
    public void testGetLatestCpuStatsBasic() {

        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(chunk);

        Storage storage = mock(Storage.class);
        when(storage.findAll(any(Chunk.class))).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getAgent()).thenReturn(hostRef);
        when(vmRef.getId()).thenReturn(VM_ID);


        VmCpuStatDAO dao = new VmCpuStatDAOImpl(storage);
        List<VmCpuStat> vmCpuStats = dao.getLatestVmCpuStats(vmRef);

        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).findAll(arg.capture());
        assertNull(arg.getValue().get(new Key<String>("$where", false)));

        assertEquals(1, vmCpuStats.size());
        VmCpuStat stat = vmCpuStats.get(0);
        assertEquals(TIMESTAMP, (Long) stat.getTimeStamp());
        assertEquals(CPU_LOAD, stat.getCpuLoad(), 0.001);
        assertEquals(VM_ID, (Integer) stat.getVmId());
    }

    @Test
    public void testGetLatestCpuStatsTwice() {

        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(chunk);

        Storage storage = mock(Storage.class);
        when(storage.findAll(any(Chunk.class))).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getAgent()).thenReturn(hostRef);
        when(vmRef.getId()).thenReturn(321);

        VmCpuStatDAO dao = new VmCpuStatDAOImpl(storage);
        dao.getLatestVmCpuStats(vmRef);

        dao.getLatestVmCpuStats(vmRef);
        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage, times(2)).findAll(arg.capture());
        assertEquals("this.timestamp > 1234", arg.getValue().get(new Key<String>("$where", false)));
    }

    @Test
    public void testPutVmCpuStat() {
        Storage storage = mock(Storage.class);
        VmCpuStat stat = new VmCpuStat(TIMESTAMP, VM_ID, CPU_LOAD);
        VmCpuStatDAO dao = new VmCpuStatDAOImpl(storage);
        dao.putVmCpuStat(stat);

        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).putChunk(arg.capture());
        Chunk chunk = arg.getValue();

        assertEquals(VmCpuStatDAO.vmCpuStatCategory, chunk.getCategory());
        assertEquals(TIMESTAMP, chunk.get(Key.TIMESTAMP));
        assertEquals(VM_ID, chunk.get(Key.VM_ID));
        assertEquals(CPU_LOAD, chunk.get(VmCpuStatDAO.vmCpuLoadKey));
    }
}
