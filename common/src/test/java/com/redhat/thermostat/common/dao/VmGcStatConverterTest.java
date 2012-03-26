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
        final long RUN_COUNT = 10;
        final long WALL_TIME = 9;
        VmGcStat stat = new VmGcStat(123, 456, "collector1", RUN_COUNT, WALL_TIME);
        Chunk chunk = new VmGcStatConverter().vmGcStatToChunk(stat);
        assertNotNull(chunk);
        assertEquals("vm-gc-stats", chunk.getCategory().getName());
        assertEquals((Long)456L, chunk.get(new Key<Long>("timestamp", false)));
        assertEquals((Integer)123, chunk.get(new Key<Integer>("vm-id", false)));
        assertEquals("collector1", chunk.get(new Key<String>("collector", false)));
        assertEquals((Long)RUN_COUNT, chunk.get(new Key<Long>("runtime-count", false)));
        assertEquals((Long)WALL_TIME, chunk.get(new Key<Long>("wall-time", false)));

    }

    // TODO test conversion the other way too
}
