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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;


public class HeapDAOTest {

    private HeapDAO dao;
    private Storage storage;
    private HeapInfo heapInfo;
    private InputStream dataStream;

    @Before
    public void setUp() {
        storage = mock(Storage.class);
        dao = new HeapDAOImpl(storage);
        HostRef host = new HostRef("987", "test-host");
        VmRef vm = new VmRef(host, 123, "test-vm");
        heapInfo = new HeapInfo(vm, 12345);
        byte[] data = new byte[] { 1, 2, 3 };
        dataStream = new ByteArrayInputStream(data);
        heapInfo.setHeapDump(dataStream);
    }

    @After
    public void tearDown() {
        dataStream = null;
        heapInfo = null;
        dao = null;
        storage = null;
    }

    @Test
    public void testCategory() {
        Category category = HeapDAO.heapInfoCategory;
        assertNotNull(category);
        assertEquals("vm-heap-info", category.getName());
        Collection<Key<?>> keys = category.getKeys();
        assertEquals(4, keys.size());
        assertTrue(keys.contains(new Key<>("agent-id", false)));
        assertTrue(keys.contains(new Key<>("vm-id", false)));
        assertTrue(keys.contains(new Key<>("timestamp", false)));
        assertTrue(keys.contains(new Key<>("heap-dump-id", false)));
    }

    @Test
    public void testPutHeapInfo() {
        dao.putHeapInfo(heapInfo);

        Chunk expectedChunk = new Chunk(HeapDAO.heapInfoCategory, false);
        expectedChunk.put(Key.AGENT_ID, "987");
        expectedChunk.put(Key.VM_ID, 123);
        expectedChunk.put(Key.TIMESTAMP, 12345L);
        expectedChunk.put(HeapDAO.heapDumpIdKey, "heapdump-987-123-12345");
        verify(storage).putChunk(expectedChunk);
        verify(storage).saveFile(eq("heapdump-987-123-12345"), same(dataStream));
    }

    @Test
    public void testPutHeapInfoWithoutDump() {
        heapInfo.setHeapDump(null);
        dao.putHeapInfo(heapInfo);

        Chunk expectedChunk = new Chunk(HeapDAO.heapInfoCategory, false);
        expectedChunk.put(Key.AGENT_ID, "987");
        expectedChunk.put(Key.VM_ID, 123);
        expectedChunk.put(Key.TIMESTAMP, 12345L);

        verify(storage).putChunk(expectedChunk);
        verify(storage, never()).saveFile(anyString(), any(InputStream.class));
    }
}
