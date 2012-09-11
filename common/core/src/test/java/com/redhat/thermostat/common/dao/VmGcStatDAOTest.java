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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.model.VmGcStat;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.test.MockQuery;

public class VmGcStatDAOTest {

    private static final Integer VM_ID = 123;
    private static final Long TIMESTAMP = 456L;
    private static final String COLLECTOR = "collector1";
    private static final Long RUN_COUNT = 10L;
    private static final Long WALL_TIME = 9L;

    @Test
    public void testCategory() {
        assertEquals("vm-gc-stats", VmGcStatDAO.vmGcStatCategory.getName());
        Collection<Key<?>> keys = VmGcStatDAO.vmGcStatCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agentId", true)));
        assertTrue(keys.contains(new Key<Integer>("vmId", true)));
        assertTrue(keys.contains(new Key<Long>("timeStamp", false)));
        assertTrue(keys.contains(new Key<String>("collectorName", false)));
        assertTrue(keys.contains(new Key<Long>("runCount", false)));
        assertTrue(keys.contains(new Key<Long>("wallTime", false)));
        assertEquals(6, keys.size());
    }

    @Test
    public void testGetLatestVmGcStatsBasic() {

        Chunk chunk = new Chunk(VmGcStatDAO.vmGcStatCategory, false);
        chunk.put(Key.TIMESTAMP, TIMESTAMP);
        chunk.put(Key.VM_ID, VM_ID);
        chunk.put(VmGcStatDAO.collectorKey, COLLECTOR);
        chunk.put(VmGcStatDAO.runCountKey, RUN_COUNT);
        chunk.put(VmGcStatDAO.wallTimeKey, WALL_TIME);

        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(chunk);

        Storage storage = mock(Storage.class);
        when(storage.createQuery()).thenReturn(new MockQuery());
        when(storage.findAll(any(Query.class))).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getAgent()).thenReturn(hostRef);
        when(vmRef.getId()).thenReturn(321);


        VmGcStatDAO dao = new VmGcStatDAOImpl(storage);
        List<VmGcStat> vmGcStats = dao.getLatestVmGcStats(vmRef);

        ArgumentCaptor<MockQuery> arg = ArgumentCaptor.forClass(MockQuery.class);
        verify(storage).findAll(arg.capture());
        assertFalse(arg.getValue().hasWhereClauseFor(Key.TIMESTAMP));

        assertEquals(1, vmGcStats.size());
        VmGcStat stat = vmGcStats.get(0);
        assertEquals(TIMESTAMP, (Long) stat.getTimeStamp());
        assertEquals(VM_ID, (Integer) stat.getVmId());
        assertEquals(COLLECTOR, stat.getCollectorName());
        assertEquals(RUN_COUNT, (Long) stat.getRunCount());
        assertEquals(WALL_TIME, (Long) stat.getWallTime());
    }

    @Test
    public void testGetLatestVmGcStatsTwice() {

        Chunk chunk = new Chunk(VmGcStatDAO.vmGcStatCategory, false);
        chunk.put(Key.TIMESTAMP, TIMESTAMP);
        chunk.put(Key.VM_ID, VM_ID);
        chunk.put(VmGcStatDAO.collectorKey, COLLECTOR);
        chunk.put(VmGcStatDAO.runCountKey, RUN_COUNT);
        chunk.put(VmGcStatDAO.wallTimeKey, WALL_TIME);

        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(chunk);

        Storage storage = mock(Storage.class);

        when(storage.createQuery()).then(new Answer<MockQuery>() {
            @Override
            public MockQuery answer(InvocationOnMock invocation) throws Throwable {
                return new MockQuery();
            }
        });
        when(storage.findAll(any(Query.class))).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        VmRef vmRef = mock(VmRef.class);
        when(vmRef.getAgent()).thenReturn(hostRef);
        when(vmRef.getId()).thenReturn(321);

        VmGcStatDAO dao = new VmGcStatDAOImpl(storage);
        dao.getLatestVmGcStats(vmRef);

        dao.getLatestVmGcStats(vmRef);
        ArgumentCaptor<MockQuery> arg = ArgumentCaptor.forClass(MockQuery.class);
        verify(storage, times(2)).findAll(arg.capture());
        MockQuery query = arg.getValue();
        assertTrue(query.hasWhereClause(Key.TIMESTAMP, Criteria.GREATER_THAN, 456l));
    }

    @Test
    public void testPutVmGcStat() {
        Storage storage = mock(Storage.class);
        VmGcStat stat = new VmGcStat(VM_ID, TIMESTAMP, COLLECTOR, RUN_COUNT, WALL_TIME);
        VmGcStatDAO dao = new VmGcStatDAOImpl(storage);
        dao.putVmGcStat(stat);

        verify(storage).putPojo(VmGcStatDAO.vmGcStatCategory, false, stat);
    }
}
