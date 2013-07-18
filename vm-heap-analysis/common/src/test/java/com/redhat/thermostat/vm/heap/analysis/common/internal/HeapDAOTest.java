/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.heap.analysis.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HistogramRecord;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.sun.tools.hat.internal.model.JavaClass;
import com.sun.tools.hat.internal.model.JavaHeapObject;


public class HeapDAOTest {

    private HeapDAO dao;
    private Storage storage;
    private Add add;
    private HeapInfo heapInfo;
    private File heapDumpData;
    private ObjectHistogram histogram;
    private InputStream histogramData;

    private PreparedStatement<HeapInfo> stmt;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws IOException, DescriptorParsingException, StatementExecutionException {
        storage = mock(Storage.class);
        add = mock(Add.class);
        when(storage.createAdd(any(Category.class))).thenReturn(add);

        when(storage.getAgentId()).thenReturn("test");
        stmt = (PreparedStatement<HeapInfo>) mock(PreparedStatement.class); 
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);

        dao = new HeapDAOImpl(storage);
        
        heapInfo = new HeapInfo("vm1", 12345);
        byte[] data = new byte[] { 1, 2, 3 };
        heapDumpData = File.createTempFile("test", "test");
        FileOutputStream out = new FileOutputStream(heapDumpData);
        out.write(data);
        out.close();
        histogramData = createHistogramData();

        Cursor<HeapInfo> cursor = (Cursor<HeapInfo>) mock(Cursor.class);
        HeapInfo info1 = new HeapInfo("vm2", 12345L);
        info1.setAgentId("123");
        info1.setHeapId("testheap1");
        info1.setHeapDumpId("test1");
        info1.setHistogramId("histotest1");

        HeapInfo info2 = new HeapInfo("vm2", 23456L);
        info2.setAgentId("123");
        info2.setHeapId("testheap2");
        info2.setHeapDumpId("test2");
        info2.setHistogramId("histotest2");

        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(info1).thenReturn(info2).thenReturn(null);
        when(stmt.executeQuery()).thenReturn(cursor);

        // Setup for reading heapdump data.
        when(storage.loadFile("test-heap")).thenReturn(new ByteArrayInputStream(data));
        when(storage.loadFile("test-histo")).thenReturn(histogramData);

        // We dont check for AGENT_ID. That's enforced/added/checked by Storage

    }

    @SuppressWarnings("unchecked")
    private StatementDescriptor<HeapInfo> anyDescriptor() {
        return (StatementDescriptor<HeapInfo>) any(StatementDescriptor.class);
    }

    private InputStream createHistogramData() throws IOException {
        histogram = new ObjectHistogram();

        JavaClass cls1 = mock(JavaClass.class);
        JavaHeapObject obj1 = mock(JavaHeapObject.class);
        when(cls1.getName()).thenReturn("class1");
        when(obj1.getClazz()).thenReturn(cls1);
        when(obj1.getSize()).thenReturn(5);
        JavaHeapObject obj2 = mock(JavaHeapObject.class);
        when(obj2.getClazz()).thenReturn(cls1);
        when(obj2.getSize()).thenReturn(3);
        JavaClass cls2 = mock(JavaClass.class);
        JavaHeapObject obj3 = mock(JavaHeapObject.class);
        when(cls2.getName()).thenReturn("class2");
        when(obj3.getClazz()).thenReturn(cls2);
        when(obj3.getSize()).thenReturn(10);

        histogram.addThing(obj1);
        histogram.addThing(obj2);
        histogram.addThing(obj3);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(histogram);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    @After
    public void tearDown() {
        stmt = null;
        histogramData = null;
        histogram = null;
        heapDumpData.delete();
        heapDumpData = null;
        heapInfo = null;
        dao = null;
        storage = null;
        add = null;
    }
    
    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedQueryHeapInfo = "QUERY vm-heap-info WHERE 'heapId' = ?s LIMIT 1";
        assertEquals(expectedQueryHeapInfo, HeapDAOImpl.QUERY_HEAP_INFO);
        String expectedQueryAllHeaps = "QUERY vm-heap-info WHERE 'agentId' = ?s AND 'vmId' = ?s";
        assertEquals(expectedQueryAllHeaps, HeapDAOImpl.QUERY_ALL_HEAPS);
    }

    @Test
    public void testCategory() {
        Category<HeapInfo> category = HeapDAO.heapInfoCategory;
        assertNotNull(category);
        assertEquals("vm-heap-info", category.getName());
        Collection<Key<?>> keys = category.getKeys();
        assertEquals(6, keys.size());
        assertTrue(keys.contains(new Key<>("agentId", true)));
        assertTrue(keys.contains(new Key<>("vmId", true)));
        assertTrue(keys.contains(new Key<>("timeStamp", false)));
        assertTrue(keys.contains(new Key<>("heapId", false)));
        assertTrue(keys.contains(new Key<>("heapDumpId", false)));
        assertTrue(keys.contains(new Key<>("histogramId", false)));
    }

    @Test
    public void testPutHeapInfo() throws IOException {
        dao.putHeapInfo(heapInfo, heapDumpData, histogram);

        verify(storage).createAdd(HeapDAO.heapInfoCategory);
        verify(add).setPojo(heapInfo);
        verify(add).apply();

        ArgumentCaptor<InputStream> data = ArgumentCaptor.forClass(InputStream.class);
        verify(storage).saveFile(eq("heapdump-test-vm1-12345"), data.capture());
        InputStream in = data.getValue();
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(3, in.read());
        assertEquals(-1, in.read());
        assertEquals("test-vm1-12345", heapInfo.getHeapId());
        ArgumentCaptor<InputStream> histoStream = ArgumentCaptor.forClass(InputStream.class);
        verify(storage).saveFile(eq("histogram-test-vm1-12345"), histoStream.capture());
        InputStream histoActual = histoStream.getValue();
        int expected;
        int actual;
        do {
            expected = histogramData.read();
            actual = histoActual.read();
            assertEquals(expected, actual);
        } while (expected != -1 && actual != -1);
    }

    @Test
    public void testPutHeapInfoWithoutDump() throws IOException {
        dao.putHeapInfo(heapInfo, null, null);

        verify(storage).createAdd(HeapDAO.heapInfoCategory);
        verify(add).setPojo(heapInfo);
        verify(add).apply();

        verify(storage, never()).saveFile(anyString(), any(InputStream.class));
        assertEquals("test-vm1-12345", heapInfo.getHeapId());
    }

    @Test
    public void testGetAllHeapInfo() throws DescriptorParsingException, StatementExecutionException {
        // verify a connection key has been created before requesting the
        // heap dumps
        verify(storage).registerCategory(HeapDAO.heapInfoCategory);
        
        HostRef host = new HostRef("123", "test-host");
        VmRef vm = new VmRef(host, "vm2", 234, "test-vm");
        Collection<HeapInfo> heapInfos = dao.getAllHeapInfo(vm);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "123");
        verify(stmt).setString(1, "vm2");
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);
        
        HeapInfo info1 = new HeapInfo("vm2", 12345);
        info1.setHeapDumpId("test1");
        info1.setHeapId("testheap1");
        info1.setHistogramId("histotest1");
        
        HeapInfo info2 = new HeapInfo("vm2", 23456);
        info2.setHeapDumpId("test2");
        info2.setHeapId("testheap2");
        info2.setHistogramId("histotest2");
        
        assertEquals(2, heapInfos.size());
        assertTrue(heapInfos.contains(info1));
        assertTrue(heapInfos.contains(info2));
    }
    
    @Test
    public void testGetHeapInfo() throws DescriptorParsingException, StatementExecutionException {
        final String heapId = "testheap1";
        
        HeapInfo result = dao.getHeapInfo(heapId);
        
        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, heapId);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);
        
        HeapInfo info = new HeapInfo("vm2", 12345);
        info.setHeapDumpId("test1");
        info.setHeapId("testheap1");
        info.setHistogramId("histotest1");
        
        assertEquals(info, result);
    }

    @Test
    public void testGetHeapDump() throws IOException, DescriptorParsingException, StatementExecutionException {
        heapInfo.setHeapDumpId("test-heap");
        InputStream in = dao.getHeapDumpData(heapInfo);
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(3, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testGetHistogram() throws IOException {
        heapInfo.setHistogramId("test-histo");
        ObjectHistogram histo = dao.getHistogram(heapInfo);
        Collection<HistogramRecord> histoRecs = histo.getHistogram();
        assertEquals(2, histoRecs.size());
        assertTrue(histoRecs.contains(new HistogramRecord("class1", 2, 8)));
        assertTrue(histoRecs.contains(new HistogramRecord("class2", 1, 10)));
    }

    @Test
    public void testInvalidHeapId() throws IOException, StatementExecutionException {
        when(stmt.executeQuery()).thenThrow(new IllegalArgumentException("invalid ObjectId"));
        dao = new HeapDAOImpl(storage);
        heapInfo = dao.getHeapInfo("some-random-heap-id");
        assertTrue(heapInfo == null);
    }
}

