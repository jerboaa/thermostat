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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.model.VmCpuStat;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Cursor.SortDirection;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.test.MockQuery;

public class VmCpuStatDAOTest {

    private static final Long TIMESTAMP = 1234L;
    private static final Integer VM_ID = 321;
    private static final Double CPU_LOAD = 9.9;

    private VmCpuStat cpuStat;

    @Before
    public void setUp() {
        cpuStat = new VmCpuStat(TIMESTAMP, VM_ID, CPU_LOAD);
    }

    @Test
    public void testCategory() {
        assertEquals("vm-cpu-stats", VmCpuStatDAO.vmCpuStatCategory.getName());
        Collection<Key<?>> keys = VmCpuStatDAO.vmCpuStatCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId", true)));
        assertTrue(keys.contains(new Key<Long>("timeStamp", false)));
        assertTrue(keys.contains(new Key<Integer>("vmId", true)));
        assertTrue(keys.contains(new Key<Integer>("cpuLoad", false)));
        assertEquals(4, keys.size());
    }

    @Test
    public void testGetLatestCpuStatsBasic() {

        @SuppressWarnings("unchecked")
        Cursor<VmCpuStat> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(cpuStat);
        when(cursor.sort(any(Key.class), any(SortDirection.class))).thenReturn(cursor);

        Storage storage = mock(Storage.class);
        when(storage.createQuery()).then(new Answer<Query>() {
            @Override
            public Query answer(InvocationOnMock invocation) throws Throwable {
                return new MockQuery();
            }
        });
        when(storage.findAllPojos(any(Query.class), same(VmCpuStat.class))).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getAgent()).thenReturn(hostRef);
        when(vmRef.getId()).thenReturn(VM_ID);


        VmCpuStatDAO dao = new VmCpuStatDAOImpl(storage);
        List<VmCpuStat> vmCpuStats = dao.getLatestVmCpuStats(vmRef, Long.MIN_VALUE);

        ArgumentCaptor<MockQuery> arg = ArgumentCaptor.forClass(MockQuery.class);
        verify(storage).findAllPojos(arg.capture(), same(VmCpuStat.class));
        assertTrue(arg.getValue().hasWhereClause(Key.TIMESTAMP, Criteria.GREATER_THAN, Long.MIN_VALUE));

        assertEquals(1, vmCpuStats.size());
        VmCpuStat stat = vmCpuStats.get(0);
        assertEquals(TIMESTAMP, (Long) stat.getTimeStamp());
        assertEquals(CPU_LOAD, stat.getCpuLoad(), 0.001);
        assertEquals(VM_ID, (Integer) stat.getVmId());
    }

    @Test
    public void testPutVmCpuStat() {
        Storage storage = mock(Storage.class);
        VmCpuStat stat = new VmCpuStat(TIMESTAMP, VM_ID, CPU_LOAD);
        VmCpuStatDAO dao = new VmCpuStatDAOImpl(storage);
        dao.putVmCpuStat(stat);

        verify(storage).putPojo(VmCpuStatDAO.vmCpuStatCategory, false, stat);

    }
}
