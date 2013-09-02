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

package com.redhat.thermostat.vm.gc.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.gc.common.VmGcStatDAO;
import com.redhat.thermostat.vm.gc.common.model.VmGcStat;

public class VmGcStatDAOTest {

    private static final String VM_ID = "VM321";
    private static final Long TIMESTAMP = 456L;
    private static final String COLLECTOR = "collector1";
    private static final Long RUN_COUNT = 10L;
    private static final Long WALL_TIME = 9L;

    @Test
    public void testCategory() {
        assertEquals("vm-gc-stats", VmGcStatDAO.vmGcStatCategory.getName());
        Collection<Key<?>> keys = VmGcStatDAO.vmGcStatCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<Integer>("vmId")));
        assertTrue(keys.contains(new Key<Long>("timeStamp")));
        assertTrue(keys.contains(new Key<String>("collectorName")));
        assertTrue(keys.contains(new Key<Long>("runCount")));
        assertTrue(keys.contains(new Key<Long>("wallTime")));
        assertEquals(6, keys.size());
    }

    @Test
    public void testGetLatestVmGcStatsBasic() throws DescriptorParsingException, StatementExecutionException {

        VmGcStat vmGcStat = new VmGcStat("foo-agent", VM_ID, TIMESTAMP, COLLECTOR, RUN_COUNT, WALL_TIME);

        @SuppressWarnings("unchecked")
        Cursor<VmGcStat> cursor = (Cursor<VmGcStat>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(vmGcStat);

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<VmGcStat> stmt = (PreparedStatement<VmGcStat>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getHostRef()).thenReturn(hostRef);
        when(vmRef.getVmId()).thenReturn("VM321");

        VmGcStatDAO dao = new VmGcStatDAOImpl(storage);
        List<VmGcStat> vmGcStats = dao.getLatestVmGcStats(vmRef, Long.MIN_VALUE);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "system");
        verify(stmt).setString(1, "VM321");
        verify(stmt).setLong(2, Long.MIN_VALUE);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(1, vmGcStats.size());
        VmGcStat stat = vmGcStats.get(0);
        assertEquals(TIMESTAMP, (Long) stat.getTimeStamp());
        assertEquals(VM_ID, stat.getVmId());
        assertEquals(COLLECTOR, stat.getCollectorName());
        assertEquals(RUN_COUNT, (Long) stat.getRunCount());
        assertEquals(WALL_TIME, (Long) stat.getWallTime());
    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<VmGcStat> anyDescriptor() {
        return (StatementDescriptor<VmGcStat>) any(StatementDescriptor.class);
    }

    @Test
    public void testPutVmGcStat() {
        Storage storage = mock(Storage.class);
        Add add = mock(Add.class);
        when(storage.createAdd(any(Category.class))).thenReturn(add);
        
        VmGcStat stat = new VmGcStat("foo-agent", VM_ID, TIMESTAMP, COLLECTOR, RUN_COUNT, WALL_TIME);
        VmGcStatDAO dao = new VmGcStatDAOImpl(storage);
        dao.putVmGcStat(stat);

        verify(storage).createAdd(VmGcStatDAO.vmGcStatCategory);
        verify(add).setPojo(stat);
        verify(add).apply();
    }
}

