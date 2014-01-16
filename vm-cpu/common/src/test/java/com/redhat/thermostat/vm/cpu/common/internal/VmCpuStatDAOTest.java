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

package com.redhat.thermostat.vm.cpu.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.cpu.common.VmCpuStatDAO;
import com.redhat.thermostat.vm.cpu.common.model.VmCpuStat;

public class VmCpuStatDAOTest {

    private static final Long TIMESTAMP = 1234L;
    private static final String VM_ID = "321";
    private static final Double CPU_LOAD = 9.9;

    private VmCpuStat cpuStat;

    @Before
    public void setUp() {
        cpuStat = new VmCpuStat("foo-agent", TIMESTAMP, VM_ID, CPU_LOAD);
    }
    
    @Test
    public void verifyDescriptorsAreSane() {
        String addCpuStat = "ADD vm-cpu-stats SET 'agentId' = ?s , " +
                                                "'vmId' = ?s , " +
                                                "'timeStamp' = ?l , " +
                                                "'cpuLoad' = ?d";
        assertEquals(addCpuStat, VmCpuStatDAOImpl.DESC_ADD_VM_CPU_STAT);
    }

    @Test
    public void testCategory() {
        assertEquals("vm-cpu-stats", VmCpuStatDAO.vmCpuStatCategory.getName());
        Collection<Key<?>> keys = VmCpuStatDAO.vmCpuStatCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<Long>("timeStamp")));
        assertTrue(keys.contains(new Key<Integer>("vmId")));
        assertTrue(keys.contains(new Key<Integer>("cpuLoad")));
        assertEquals(4, keys.size());
    }

    @Test
    public void testGetLatestCpuStatsBasic() throws DescriptorParsingException, StatementExecutionException {

        @SuppressWarnings("unchecked")
        Cursor<VmCpuStat> cursor = (Cursor<VmCpuStat>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(cpuStat);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<VmCpuStat> stmt = (PreparedStatement<VmCpuStat>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getHostRef()).thenReturn(hostRef);
        when(vmRef.getVmId()).thenReturn(VM_ID);

        VmCpuStatDAO dao = new VmCpuStatDAOImpl(storage);
        List<VmCpuStat> vmCpuStats = dao.getLatestVmCpuStats(vmRef, Long.MIN_VALUE);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "system");
        verify(stmt).setString(1, VM_ID);
        verify(stmt).setLong(2, Long.MIN_VALUE);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(1, vmCpuStats.size());
        VmCpuStat stat = vmCpuStats.get(0);
        assertEquals(TIMESTAMP, (Long) stat.getTimeStamp());
        assertEquals(CPU_LOAD, stat.getCpuLoad(), 0.001);
        assertEquals(VM_ID, stat.getVmId());
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<VmCpuStat> anyDescriptor() {
        return (StatementDescriptor<VmCpuStat>) any(StatementDescriptor.class);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPutVmCpuStat() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        PreparedStatement<VmCpuStat> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);
        
        VmCpuStat stat = new VmCpuStat("foo-agent", TIMESTAMP, VM_ID, CPU_LOAD);
        VmCpuStatDAO dao = new VmCpuStatDAOImpl(storage);
        dao.putVmCpuStat(stat);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(VmCpuStatDAOImpl.DESC_ADD_VM_CPU_STAT, desc.getDescriptor());

        verify(add).setString(0, stat.getAgentId());
        verify(add).setString(1, stat.getVmId());
        verify(add).setLong(2, stat.getTimeStamp());
        verify(add).setDouble(3, stat.getCpuLoad());
        verify(add).execute();
        verifyNoMoreInteractions(add);
    }
}

