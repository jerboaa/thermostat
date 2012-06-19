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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.model.HeapInfo;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Cursor;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;


public class HeapDAOTest {

    private HeapDAO dao;
    private Storage storage;
    private HeapInfo heapInfo;
    private InputStream heapDumpData;

    @Before
    public void setUp() {
        storage = mock(Storage.class);
        dao = new HeapDAOImpl(storage);
        HostRef host = new HostRef("987", "test-host");
        VmRef vm = new VmRef(host, 123, "test-vm");
        heapInfo = new HeapInfo(vm, 12345);
        byte[] data = new byte[] { 1, 2, 3 };
        heapDumpData = new ByteArrayInputStream(data);

        // Setup for reading data from DB.
        Chunk findAllQuery = new Chunk(HeapDAO.heapInfoCategory, false);
        findAllQuery.put(Key.AGENT_ID, "123");
        findAllQuery.put(Key.VM_ID, 234);
        Cursor cursor = mock(Cursor.class);
        Chunk info1 = new Chunk(HeapDAO.heapInfoCategory, false);
        info1.put(Key.AGENT_ID, "123");
        info1.put(Key.VM_ID, 234);
        info1.put(Key.TIMESTAMP, 12345l);
        info1.put(HeapDAO.heapDumpIdKey, "test1");
        Chunk info2 = new Chunk(HeapDAO.heapInfoCategory, false);
        info2.put(Key.AGENT_ID, "123");
        info2.put(Key.VM_ID, 234);
        info2.put(Key.TIMESTAMP, 23456l);
        info2.put(HeapDAO.heapDumpIdKey, "test2");
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(info1).thenReturn(info2).thenReturn(null);
        when(storage.findAll(findAllQuery)).thenReturn(cursor);

        // Setup for reading heapdump data.
        when(storage.loadFile("test")).thenReturn(heapDumpData);
    }

    @After
    public void tearDown() {
        heapDumpData = null;
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
        dao.putHeapInfo(heapInfo, heapDumpData);

        Chunk expectedChunk = new Chunk(HeapDAO.heapInfoCategory, false);
        expectedChunk.put(Key.AGENT_ID, "987");
        expectedChunk.put(Key.VM_ID, 123);
        expectedChunk.put(Key.TIMESTAMP, 12345L);
        expectedChunk.put(HeapDAO.heapDumpIdKey, "heapdump-987-123-12345");
        verify(storage).putChunk(expectedChunk);
        verify(storage).saveFile(eq("heapdump-987-123-12345"), same(heapDumpData));
    }

    @Test
    public void testPutHeapInfoWithoutDump() {
        dao.putHeapInfo(heapInfo, null);

        Chunk expectedChunk = new Chunk(HeapDAO.heapInfoCategory, false);
        expectedChunk.put(Key.AGENT_ID, "987");
        expectedChunk.put(Key.VM_ID, 123);
        expectedChunk.put(Key.TIMESTAMP, 12345L);

        verify(storage).putChunk(expectedChunk);
        verify(storage, never()).saveFile(anyString(), any(InputStream.class));
    }

    @Test
    public void testGetAllHeapInfo() {
        HostRef host = new HostRef("123", "test-host");
        VmRef vm = new VmRef(host, 234, "test-vm");
        Collection<HeapInfo> heapInfos = dao.getAllHeapInfo(vm);
        HeapInfo info1 = new HeapInfo(vm, 12345);
        info1.setHeapDumpId("test1");
        HeapInfo info2 = new HeapInfo(vm, 23456);
        info2.setHeapDumpId("test2");
        assertEquals(2, heapInfos.size());
        assertTrue(heapInfos.contains(info1));
        assertTrue(heapInfos.contains(info2));
    }

    @Test
    public void testGetHeapDump() throws IOException {
        heapInfo.setHeapDumpId("test");
        InputStream in = dao.getHeapDump(heapInfo);
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(3, in.read());
        assertEquals(-1, in.read());
    }
}
