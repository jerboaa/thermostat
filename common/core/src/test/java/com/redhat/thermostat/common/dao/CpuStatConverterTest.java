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

import com.redhat.thermostat.common.model.CpuStat;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class CpuStatConverterTest {

    @Test
    public void testToChunk() {
        CpuStat stat = new CpuStat(10, 5.0, 10.0, 15.0);
        Chunk chunk = new CpuStatConverter().toChunk(stat);
        assertNotNull(chunk);
        assertEquals("cpu-stats", chunk.getCategory().getName());
        assertEquals((Long) 10L, chunk.get(Key.TIMESTAMP));
        assertEquals(5.0, chunk.get(CpuStatDAO.cpu5LoadKey), 0.001);
        assertEquals(10.0, chunk.get(CpuStatDAO.cpu10LoadKey), 0.001);
        assertEquals(15.0, chunk.get(CpuStatDAO.cpu15LoadKey), 0.001);
    }

    @Test
    public void testFromChunk() {
        Chunk chunk = new Chunk(CpuStatDAO.cpuStatCategory, false);
        chunk.put(Key.TIMESTAMP, 10L);
        chunk.put(CpuStatDAO.cpu5LoadKey, 5.0);
        chunk.put(CpuStatDAO.cpu10LoadKey, 10.0);
        chunk.put(CpuStatDAO.cpu15LoadKey, 15.0);
        CpuStat stat = new CpuStatConverter().fromChunk(chunk);
        assertNotNull(stat);
        assertEquals(10L, stat.getTimeStamp());
        assertEquals(5.0, stat.getLoad5(), 0.001);
        assertEquals(10.0, stat.getLoad10(), 0.001);
        assertEquals(15.0, stat.getLoad15(), 0.001);
    }
}
