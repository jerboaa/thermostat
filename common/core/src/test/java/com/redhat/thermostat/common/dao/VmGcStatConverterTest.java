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

import com.redhat.thermostat.common.model.VmGcStat;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class VmGcStatConverterTest {

    @Test
    public void testVmGcStatToChunk() {
        final Integer VM_ID = 123;
        final Long TIMESTAMP = 456L;
        final String COLLECTOR = "collector1";
        final Long RUN_COUNT = 10L;
        final Long WALL_TIME = 9L;

        VmGcStat stat = new VmGcStat(VM_ID, TIMESTAMP, COLLECTOR, RUN_COUNT, WALL_TIME);
        Chunk chunk = new VmGcStatConverter().toChunk(stat);

        assertNotNull(chunk);
        assertEquals("vm-gc-stats", chunk.getCategory().getName());
        assertEquals(TIMESTAMP, chunk.get(new Key<Long>("timeStamp", false)));
        assertEquals(VM_ID, chunk.get(new Key<Integer>("vmId", true)));
        assertEquals(COLLECTOR, chunk.get(new Key<String>("collectorName", false)));
        assertEquals(RUN_COUNT, chunk.get(new Key<Long>("runCount", false)));
        assertEquals(WALL_TIME, chunk.get(new Key<Long>("wallTime", false)));
    }

    @Test
    public void testChunkToVmGcStat() {
        final Integer VM_ID = 123;
        final Long TIMESTAMP = 456L;
        final String COLLECTOR = "collector1";
        final Long RUN_COUNT = 10L;
        final Long WALL_TIME = 9L;

        Chunk chunk = new Chunk(VmGcStatDAO.vmGcStatCategory, false);
        chunk.put(Key.TIMESTAMP, TIMESTAMP);
        chunk.put(Key.VM_ID, VM_ID);
        chunk.put(VmGcStatDAO.collectorKey, COLLECTOR);
        chunk.put(VmGcStatDAO.runCountKey, RUN_COUNT);
        chunk.put(VmGcStatDAO.wallTimeKey, WALL_TIME);
        VmGcStat stat = new VmGcStatConverter().fromChunk(chunk);

        assertNotNull(stat);
        assertEquals(TIMESTAMP, (Long) stat.getTimeStamp());
        assertEquals(VM_ID, (Integer) stat.getVmId());
        assertEquals(COLLECTOR, stat.getCollectorName());
        assertEquals(RUN_COUNT, (Long) stat.getRunCount());
        assertEquals(WALL_TIME, (Long) stat.getWallTime());
    }
}
