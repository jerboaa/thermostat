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

import com.redhat.thermostat.common.model.MemoryStat;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

public class MemoryStatDAOTest {


    private static long TIMESTAMP = 1;
    private static long TOTAL = 2;
    private static long FREE = 3;
    private static long BUFFERS = 4;
    private static long CACHED = 5;
    private static long SWAP_TOTAL = 6;
    private static long SWAP_FREE = 7;
    private static long COMMIT_LIMIT = 8;

    @Test
    public void testCategory() {
        assertEquals("memory-stats", MemoryStatDAO.memoryStatCategory.getName());
        Collection<Key<?>> keys = MemoryStatDAO.memoryStatCategory.getKeys();
        assertTrue(keys.contains(new Key<>("agent-id", false)));
        assertTrue(keys.contains(new Key<Long>("timestamp", false)));
        assertTrue(keys.contains(new Key<Long>("total", false)));
        assertTrue(keys.contains(new Key<Long>("free", false)));
        assertTrue(keys.contains(new Key<Long>("buffers", false)));
        assertTrue(keys.contains(new Key<Long>("cached", false)));
        assertTrue(keys.contains(new Key<Long>("swap-total", false)));
        assertTrue(keys.contains(new Key<Long>("swap-free", false)));
        assertTrue(keys.contains(new Key<Long>("commit-limit", false)));
        assertEquals(9, keys.size());

    }

    @Test
    public void testGetLatestMemoryStats() {

        Chunk chunk = new Chunk(MemoryStatDAO.memoryStatCategory, false);
        chunk.put(Key.TIMESTAMP, TIMESTAMP);
        chunk.put(MemoryStatDAO.memoryTotalKey, TOTAL);
        chunk.put(MemoryStatDAO.memoryFreeKey, FREE);
        chunk.put(MemoryStatDAO.memoryBuffersKey, BUFFERS);
        chunk.put(MemoryStatDAO.memoryCachedKey, CACHED);
        chunk.put(MemoryStatDAO.memorySwapTotalKey, SWAP_TOTAL);
        chunk.put(MemoryStatDAO.memorySwapFreeKey, SWAP_FREE);
        chunk.put(MemoryStatDAO.memoryCommitLimitKey, COMMIT_LIMIT);

        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(chunk);

        Storage storage = mock(Storage.class);
        when(storage.findAll(any(Chunk.class))).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        MemoryStatDAO dao = new MemoryStatDAOImpl(storage);
        List<MemoryStat> memoryStats = dao.getLatestMemoryStats(hostRef);

        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).findAll(arg.capture());
        assertNull(arg.getValue().get(new Key<String>("$where", false)));

        assertEquals(1, memoryStats.size());
        MemoryStat stat = memoryStats.get(0);

        assertEquals(TIMESTAMP, stat.getTimeStamp());
        assertEquals(TOTAL, stat.getTotal());
        assertEquals(FREE, stat.getFree());
        assertEquals(BUFFERS, stat.getBuffers());
        assertEquals(CACHED, stat.getCached());
        assertEquals(SWAP_TOTAL, stat.getSwapTotal());
        assertEquals(SWAP_FREE, stat.getSwapFree());
        assertEquals(COMMIT_LIMIT, stat.getCommitLimit());
    }

    @Test
    public void testGetLatestMemoryStatsTwice() {

        Chunk chunk = new Chunk(MemoryStatDAO.memoryStatCategory, false);
        chunk.put(Key.TIMESTAMP, TIMESTAMP);
        chunk.put(MemoryStatDAO.memoryTotalKey, TOTAL);
        chunk.put(MemoryStatDAO.memoryFreeKey, FREE);
        chunk.put(MemoryStatDAO.memoryBuffersKey, BUFFERS);
        chunk.put(MemoryStatDAO.memoryCachedKey, CACHED);
        chunk.put(MemoryStatDAO.memorySwapTotalKey, SWAP_TOTAL);
        chunk.put(MemoryStatDAO.memorySwapFreeKey, SWAP_FREE);
        chunk.put(MemoryStatDAO.memoryCommitLimitKey, COMMIT_LIMIT);

        Cursor cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(chunk);

        Storage storage = mock(Storage.class);
        when(storage.findAll(any(Chunk.class))).thenReturn(cursor);

        HostRef hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn("system");

        MemoryStatDAO dao = new MemoryStatDAOImpl(storage);
        dao.getLatestMemoryStats(hostRef);
        dao.getLatestMemoryStats(hostRef);

        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage, times(2)).findAll(arg.capture());
        assertEquals("this.timestamp > 1", arg.getValue().get(new Key<String>("$where", false)));
    }

    @Test
    public void testPutHostInfo() {
        Storage storage = mock(Storage.class);
        MemoryStat stat = new MemoryStat(TIMESTAMP, TOTAL, FREE, BUFFERS, CACHED, SWAP_TOTAL, SWAP_FREE, COMMIT_LIMIT);
        MemoryStatDAO dao = new MemoryStatDAOImpl(storage);
        dao.putMemoryStat(stat);

        ArgumentCaptor<Chunk> arg = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).putChunk(arg.capture());
        Chunk chunk = arg.getValue();

        assertEquals(MemoryStatDAO.memoryStatCategory, chunk.getCategory());
        assertEquals((Long) TIMESTAMP, chunk.get(Key.TIMESTAMP));
        assertEquals((Long) TOTAL, chunk.get(MemoryStatDAO.memoryTotalKey));
        assertEquals((Long) FREE, chunk.get(MemoryStatDAO.memoryFreeKey));
        assertEquals((Long) BUFFERS, chunk.get(MemoryStatDAO.memoryBuffersKey));
        assertEquals((Long) CACHED, chunk.get(MemoryStatDAO.memoryCachedKey));
        assertEquals((Long) SWAP_TOTAL, chunk.get(MemoryStatDAO.memorySwapTotalKey));
        assertEquals((Long) SWAP_FREE, chunk.get(MemoryStatDAO.memorySwapFreeKey));
        assertEquals((Long) COMMIT_LIMIT, chunk.get(MemoryStatDAO.memoryCommitLimitKey));
    }

    @Test
    public void testGetCount() {
        Storage storage = mock(Storage.class);
        when(storage.getCount(any(Category.class))).thenReturn(5L);
        MemoryStatDAO dao = new MemoryStatDAOImpl(storage);
        Long count = dao.getCount();
        assertEquals((Long) 5L, count);
    }
}
