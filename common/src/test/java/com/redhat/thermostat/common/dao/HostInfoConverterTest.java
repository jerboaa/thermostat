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

import org.junit.Test;

import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;

public class HostInfoConverterTest {

    @Test
    public void testHostInfoToChunk() {
        HostInfo info = new HostInfo("a-host", "an-os", "a-kernel", "a-cpu", 9, 99);

        Chunk chunk = new HostInfoConverter().hostInfoToChunk(info);

        assertEquals("host-info", chunk.getCategory().getName());
        assertEquals("a-host", chunk.get(new Key<String>("hostname", true)));
        assertEquals("an-os", chunk.get(new Key<String>("os_name", false)));
        assertEquals("a-kernel", chunk.get(new Key<String>("os_kernel", false)));
        assertEquals("a-cpu", chunk.get(new Key<String>("cpu_model", false)));
        assertEquals((Integer)9, chunk.get(new Key<Integer>("cpu_num", false)));
        assertEquals((Long) 99L, chunk.get(new Key<Long>("memory_total", false)));
    }
}
