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

import java.util.Collection;

import org.junit.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.redhat.thermostat.common.HostInfo;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;

public class HostInfoDAOTest {

    @Test
    public void testCategory() {
        assertEquals("host-info", HostInfoDAO.hostInfoCategory.getName());
        Collection<Key<?>> keys = HostInfoDAO.hostInfoCategory.getKeys();
        assertTrue(keys.contains(new Key<String>("hostname", true)));
        assertTrue(keys.contains(new Key<String>("os_name", false)));
        assertTrue(keys.contains(new Key<String>("os_kernel", false)));
        assertTrue(keys.contains(new Key<String>("cpu_model", false)));
        assertTrue(keys.contains(new Key<Integer>("cpu_num", false)));
        assertTrue(keys.contains(new Key<Long>("memory_total", false)));
        assertEquals(6, keys.size());
    }

    @Test
    public void testGetHostInfo() {
        final String HOST_NAME = "a host name";
        final String OS_NAME = "some os";
        final String OS_KERNEL = "some kernel";
        final String CPU_MODEL = "some cpu that runs fast";
        final int CPU_NUM = -1;
        final long MEMORY_TOTAL = 0xCAFEBABEl;

        Chunk chunk = new Chunk(HostInfoDAO.hostInfoCategory, false);
        chunk.put(HostInfoDAO.hostNameKey, HOST_NAME);
        chunk.put(HostInfoDAO.osNameKey, OS_NAME);
        chunk.put(HostInfoDAO.osKernelKey, OS_KERNEL);
        chunk.put(HostInfoDAO.cpuModelKey, CPU_MODEL);
        chunk.put(HostInfoDAO.cpuCountKey, CPU_NUM);
        chunk.put(HostInfoDAO.hostMemoryTotalKey, MEMORY_TOTAL);

        Storage storage = mock(Storage.class);
        when(storage.find(any(Chunk.class))).thenReturn(chunk);

        HostInfo info = new HostInfoDAOImpl(storage, new HostRef("some uid", HOST_NAME)).getHostInfo();
        assertNotNull(info);
        assertEquals(HOST_NAME, info.getHostname());
        assertEquals(OS_NAME, info.getOsName());
        assertEquals(OS_KERNEL, info.getOsKernel());
        assertEquals(CPU_MODEL, info.getCpuModel());
        assertEquals(CPU_NUM, info.getCpuCount());
        assertEquals(MEMORY_TOTAL, info.getTotalMemory());
    }
}
