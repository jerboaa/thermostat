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

import static org.junit.Assert.*;

import org.junit.Test;

import com.redhat.thermostat.common.model.MemoryStat;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class MemoryStatConverterTest {

    @Test
    public void testMemoryStatToChunk() {
        MemoryStat stat = new MemoryStat(0, 1, 2, 3, 4, 5, 6, 7);

        Chunk chunk = new MemoryStatConverter().memoryStatToChunk(stat);

        assertEquals((Long) 0l, chunk.get(Key.TIMESTAMP));
        assertEquals((Long) 1l, chunk.get(new Key<Long>("total", false)));
        assertEquals((Long) 2l, chunk.get(new Key<Long>("free", false)));
        assertEquals((Long) 3l, chunk.get(new Key<Long>("buffers", false)));
        assertEquals((Long) 4l, chunk.get(new Key<Long>("cached", false)));
        assertEquals((Long) 5l, chunk.get(new Key<Long>("swap-total", false)));
        assertEquals((Long) 6l, chunk.get(new Key<Long>("swap-free", false)));
        assertEquals((Long) 7l, chunk.get(new Key<Long>("commit-limit", false)));
    }

    @Test
    public void testChunkToMemoryStat() {
        long TIMESTAMP = 1;
        long TOTAL = 2;
        long FREE = 3;
        long BUFFERS = 4;
        long CACHED = 5;
        long SWAP_TOTAL = 6;
        long SWAP_FREE = 7;
        long COMMIT_LIMIT = 8;

        Chunk chunk = new Chunk(MemoryStatDAO.memoryStatCategory, false);
        chunk.put(Key.TIMESTAMP, TIMESTAMP);
        chunk.put(MemoryStatDAO.memoryTotalKey, TOTAL);
        chunk.put(MemoryStatDAO.memoryFreeKey, FREE);
        chunk.put(MemoryStatDAO.memoryBuffersKey, BUFFERS);
        chunk.put(MemoryStatDAO.memoryCachedKey, CACHED);
        chunk.put(MemoryStatDAO.memorySwapTotalKey, SWAP_TOTAL);
        chunk.put(MemoryStatDAO.memorySwapFreeKey, SWAP_FREE);
        chunk.put(MemoryStatDAO.memoryCommitLimitKey, COMMIT_LIMIT);

        MemoryStat stat = new MemoryStatConverter().chunkToMemoryStat(chunk);

        assertEquals(TIMESTAMP, stat.getTimeStamp());
        assertEquals(TOTAL, stat.getTotal());
        assertEquals(FREE, stat.getFree());
        assertEquals(BUFFERS, stat.getBuffers());
        assertEquals(CACHED, stat.getCached());
        assertEquals(SWAP_TOTAL, stat.getSwapTotal());
        assertEquals(SWAP_FREE, stat.getSwapFree());
        assertEquals(COMMIT_LIMIT, stat.getCommitLimit());
    }
}
