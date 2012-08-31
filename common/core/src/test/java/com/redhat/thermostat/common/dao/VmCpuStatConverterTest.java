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

import com.redhat.thermostat.common.model.VmCpuStat;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class VmCpuStatConverterTest {

    final long TIMESTAMP = 12345l;
    final int VM_ID = 678;
    final double PROCESSOR_USAGE = 9.9;

    @Test
    public void testVmCpuStatToChunk() {

        VmCpuStat vmCpuStat = new VmCpuStat(TIMESTAMP, VM_ID, PROCESSOR_USAGE);
        Chunk chunk = new VmCpuStatConverter().toChunk(vmCpuStat);
        assertNotNull(chunk);
        assertEquals("vm-cpu-stats", chunk.getCategory().getName());
        assertEquals((Long)TIMESTAMP, chunk.get(Key.TIMESTAMP));
        assertEquals((Integer) VM_ID, chunk.get(new Key<Long>("vm-id", true)));
        assertEquals(PROCESSOR_USAGE, chunk.get(new Key<Double>("processor-usage", false)), 0.001);

    }

    @Test
    public void testChunkToVmCpuStat() {
        Chunk chunk = new Chunk(VmCpuStatDAO.vmCpuStatCategory, false);
        chunk.put(Key.TIMESTAMP, TIMESTAMP);
        chunk.put(Key.VM_ID, VM_ID);
        chunk.put(VmCpuStatDAO.vmCpuLoadKey, PROCESSOR_USAGE);

        VmCpuStat stat = new VmCpuStatConverter().fromChunk(chunk);
        assertNotNull(stat);
        assertEquals(TIMESTAMP, stat.getTimeStamp());
        assertEquals(VM_ID, stat.getVmId());
        assertEquals(PROCESSOR_USAGE, stat.getCpuLoad(), 0.001);

    }

}
