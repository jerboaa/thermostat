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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.common.model.VmMemoryStat.Space;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Cursor.SortDirection;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

public class VmMemoryStatDAOTest {

    private static final int VM_ID = 0xcafe;
    private static final String AGENT_ID = "agent";

    private Storage storage;
    private VmRef vmRef;

    private Chunk query;
    private Cursor cursor;

    @Before
    public void setUp() {
        

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn(AGENT_ID);

        vmRef = mock(VmRef.class);
        when(vmRef.getAgent()).thenReturn(hostRef);
        when(vmRef.getId()).thenReturn(VM_ID);

        storage = mock(Storage.class);

        cursor = mock(Cursor.class);
        when(storage.findAll(any(Chunk.class))).thenAnswer(new Answer<Cursor>() {
            @Override
            public Cursor answer(InvocationOnMock invocation) throws Throwable {
                query = (Chunk) invocation.getArguments()[0];
                return cursor;
            }

        });
        when(cursor.sort(any(Key.class), any(SortDirection.class))).thenReturn(cursor);
        when(cursor.limit(any(Integer.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);

    }

    @After
    public void tearDown() {
        query = null;
        vmRef = null;
        cursor = null;
        storage = null;
    }

    @Test
    public void testCategories() {
        Collection<Key<?>> keys;

        assertEquals("vm-memory-stats", VmMemoryStatDAO.vmMemoryStatsCategory.getName());
        keys = VmMemoryStatDAO.vmMemoryStatsCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agent-id", false)));
        assertTrue(keys.contains(new Key<Integer>("vm-id", false)));
        assertTrue(keys.contains(new Key<Long>("timestamp", false)));
        assertTrue(keys.contains(new Key<String>("eden.gen", false)));
        assertTrue(keys.contains(new Key<String>("eden.collector", false)));
        assertTrue(keys.contains(new Key<Long>("eden.capacity", false)));
        assertTrue(keys.contains(new Key<Long>("eden.max-capacity", false)));
        assertTrue(keys.contains(new Key<Long>("eden.used", false)));
        assertTrue(keys.contains(new Key<String>("s0.gen", false)));
        assertTrue(keys.contains(new Key<String>("s0.collector", false)));
        assertTrue(keys.contains(new Key<Long>("s0.capacity", false)));
        assertTrue(keys.contains(new Key<Long>("s0.max-capacity", false)));
        assertTrue(keys.contains(new Key<Long>("s0.used", false)));
        assertTrue(keys.contains(new Key<String>("s1.gen", false)));
        assertTrue(keys.contains(new Key<String>("s1.collector", false)));
        assertTrue(keys.contains(new Key<Long>("s1.capacity", false)));
        assertTrue(keys.contains(new Key<Long>("s1.max-capacity", false)));
        assertTrue(keys.contains(new Key<Long>("s1.used", false)));
        assertTrue(keys.contains(new Key<String>("old.gen", false)));
        assertTrue(keys.contains(new Key<String>("old.collector", false)));
        assertTrue(keys.contains(new Key<Long>("old.capacity", false)));
        assertTrue(keys.contains(new Key<Long>("old.max-capacity", false)));
        assertTrue(keys.contains(new Key<Long>("old.used", false)));
        assertTrue(keys.contains(new Key<String>("perm.gen", false)));
        assertTrue(keys.contains(new Key<String>("perm.collector", false)));
        assertTrue(keys.contains(new Key<Long>("perm.capacity", false)));
        assertTrue(keys.contains(new Key<Long>("perm.max-capacity", false)));
        assertTrue(keys.contains(new Key<Long>("perm.used", false)));
        assertEquals(28, keys.size());
    }

    @Test
    public void testGetLatest() {
        VmMemoryStatDAO impl = new VmMemoryStatDAOImpl(storage);
        impl.getLatestMemoryStat(vmRef);

        verifyQuery();
    }

    @Test
    public void testGetLatestSince() {
        VmMemoryStatDAO impl = new VmMemoryStatDAOImpl(storage);
        impl.getLatestVmMemoryStats(vmRef, 123);

        verifyQuery();

        assertEquals("this.timestamp > 123", query.get(Key.WHERE));
    }

    private void verifyQuery() {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Key> sortKey = ArgumentCaptor.forClass(Key.class);
        ArgumentCaptor<SortDirection> sortDirection = ArgumentCaptor.forClass(SortDirection.class);
        verify(cursor).sort(sortKey.capture(), sortDirection.capture());

        assertEquals(AGENT_ID, query.get(Key.AGENT_ID));
        assertEquals((Integer)VM_ID, query.get(Key.VM_ID));

        assertTrue(sortKey.getValue().equals(Key.TIMESTAMP));
        assertTrue(sortDirection.getValue().equals(SortDirection.DESCENDING));
    }

    @Test
    public void testGetLatestReturnsNullWhenStorageEmpty() {
        when(cursor.hasNext()).thenReturn(false);
        when(cursor.next()).thenReturn(null);

        Storage storage = mock(Storage.class);
        when(storage.findAll(any(Chunk.class))).thenReturn(cursor);

        VmMemoryStatDAO impl = new VmMemoryStatDAOImpl(storage);
        VmMemoryStat latest = impl.getLatestMemoryStat(vmRef);
        assertTrue(latest == null);
    }

    @Test
    public void testPutVmMemoryStat() {

        List<Generation> generations = new ArrayList<Generation>();

        int i = 0;
        for (String genName: new String[] { "new", "old", "perm" }) {
            Generation gen = new Generation();
            gen.name = genName;
            gen.collector = gen.name;
            generations.add(gen);
            List<Space> spaces = new ArrayList<Space>();
            gen.spaces = spaces;
            String[] spaceNames = null;
            if (genName.equals("new")) {
                spaceNames = new String[] { "eden", "s0", "s1" };
            } else if (genName.equals("old")) {
                spaceNames = new String[] { "old" };
            } else {
                spaceNames = new String[] { "perm" };
            }
            for (String spaceName: spaceNames) {
                Space space = new Space();
                space.name = spaceName;
                space.index = 0;
                space.used = i++;
                space.capacity = i++;
                space.maxCapacity = i++;
                spaces.add(space);
            }
        }
        VmMemoryStat stat = new VmMemoryStat(1, 2, generations);

        Storage storage = mock(Storage.class);
        VmMemoryStatDAO dao = new VmMemoryStatDAOImpl(storage);
        dao.putVmMemoryStat(stat);

        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).putChunk(arg.capture());
        Chunk chunk = arg.getValue();

        assertEquals(VmMemoryStatDAO.vmMemoryStatsCategory, chunk.getCategory());
        assertEquals((Long) 1l, chunk.get(new Key<Long>("timestamp", false)));
        assertEquals((Integer) 2, chunk.get(new Key<Integer>("vm-id", false)));
        assertEquals("new", chunk.get(new Key<String>("eden.gen", false)));
        assertEquals("new", chunk.get(new Key<String>("eden.collector", false)));
        assertEquals((Long) 0l, chunk.get(new Key<Long>("eden.used", false)));
        assertEquals((Long) 1l, chunk.get(new Key<Long>("eden.capacity", false)));
        assertEquals((Long) 2l, chunk.get(new Key<Long>("eden.max-capacity", false)));
        assertEquals("new", chunk.get(new Key<String>("s0.gen", false)));
        assertEquals("new", chunk.get(new Key<String>("s0.collector", false)));
        assertEquals((Long) 3l, chunk.get(new Key<Long>("s0.used", false)));
        assertEquals((Long) 4l, chunk.get(new Key<Long>("s0.capacity", false)));
        assertEquals((Long) 5l, chunk.get(new Key<Long>("s0.max-capacity", false)));
        assertEquals("new", chunk.get(new Key<String>("s1.gen", false)));
        assertEquals("new", chunk.get(new Key<String>("s1.collector", false)));
        assertEquals((Long) 6l, chunk.get(new Key<Long>("s1.used", false)));
        assertEquals((Long) 7l, chunk.get(new Key<Long>("s1.capacity", false)));
        assertEquals((Long) 8l, chunk.get(new Key<Long>("s1.max-capacity", false)));
        assertEquals("old", chunk.get(new Key<String>("old.gen", false)));
        assertEquals("old", chunk.get(new Key<String>("old.collector", false)));
        assertEquals((Long) 9l, chunk.get(new Key<Long>("old.used", false)));
        assertEquals((Long) 10l, chunk.get(new Key<Long>("old.capacity", false)));
        assertEquals((Long) 11l, chunk.get(new Key<Long>("old.max-capacity", false)));
        assertEquals("perm", chunk.get(new Key<String>("perm.gen", false)));
        assertEquals("perm", chunk.get(new Key<String>("perm.collector", false)));
        assertEquals((Long) 12l, chunk.get(new Key<Long>("perm.used", false)));
        assertEquals((Long) 13l, chunk.get(new Key<Long>("perm.capacity", false)));
        assertEquals((Long) 14l, chunk.get(new Key<Long>("perm.max-capacity", false)));
    }
}
