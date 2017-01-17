/*
 * Copyright 2012-2017 Red Hat, Inc.
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
import static org.mockito.Matchers.isA;
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

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CloseOnSave;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.SaveFileListener;
import com.redhat.thermostat.storage.core.SaveFileListener.EventType;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.HistogramRecord;
import com.redhat.thermostat.vm.heap.analysis.common.ObjectHistogram;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaClass;
import com.redhat.thermostat.vm.heap.analysis.hat.hprof.model.JavaHeapObject;

public class HeapDAOTest {

    private HeapDAO dao;
    private Storage storage;
    private HeapInfo heapInfo;
    private File heapDumpData;
    private ObjectHistogram histogram;
    private InputStream histogramData;

    @Before
    public void setUp() throws IOException, DescriptorParsingException, StatementExecutionException {
        storage = mock(Storage.class);

        dao = new HeapDAOImpl(storage);
        
        String writerId = "test";
        heapInfo = new HeapInfo(writerId, "vm1", 12345);
        byte[] data = new byte[] { 1, 2, 3 };
        heapDumpData = File.createTempFile("test", "test");
        FileOutputStream out = new FileOutputStream(heapDumpData);
        out.write(data);
        out.close();
        histogramData = createHistogramData();

        // Setup for reading heapdump data.
        when(storage.loadFile("test-heap")).thenReturn(new ByteArrayInputStream(data));
        when(storage.loadFile("test-histo")).thenReturn(histogramData);
    }

    private Cursor<HeapInfo> getMockCursor(String writerId) {
        @SuppressWarnings("unchecked")
        Cursor<HeapInfo> cursor = mock(Cursor.class);
        HeapInfo info1 = new HeapInfo(writerId, "vm2", 12345L);
        info1.setAgentId("123");
        info1.setHeapId("testheap1");
        info1.setHeapDumpId("test1");
        info1.setHistogramId("histotest1");

        HeapInfo info2 = new HeapInfo(writerId, "vm2", 23456L);
        info2.setAgentId("123");
        info2.setHeapId("testheap2");
        info2.setHeapDumpId("test2");
        info2.setHistogramId("histotest2");

        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(info1).thenReturn(info2).thenReturn(null);
        return cursor;
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
        histogramData = null;
        histogram = null;
        heapDumpData.delete();
        heapDumpData = null;
        heapInfo = null;
        dao = null;
        storage = null;
    }
    
    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedQueryHeapInfo = "QUERY vm-heap-info WHERE 'heapId' = ?s LIMIT 1";
        assertEquals(expectedQueryHeapInfo, HeapDAOImpl.QUERY_HEAP_INFO);
        String expectedQueryAllHeaps = "QUERY vm-heap-info WHERE 'agentId' = ?s AND 'vmId' = ?s";
        assertEquals(expectedQueryAllHeaps, HeapDAOImpl.QUERY_ALL_HEAPS_WITH_AGENT_AND_VM_IDS);
        
        String addHeapInfo = "ADD vm-heap-info SET 'agentId' = ?s , " +
                                                "'vmId' = ?s , " +
                                                "'timeStamp' = ?l , " +
                                                "'heapId' = ?s , " +
                                                "'heapDumpId' = ?s , " +
                                                "'histogramId' = ?s";
        assertEquals(addHeapInfo, HeapDAOImpl.DESC_ADD_VM_HEAP_INFO);
    }

    @Test
    public void testCategory() {
        Category<HeapInfo> category = HeapDAO.heapInfoCategory;
        assertNotNull(category);
        assertEquals("vm-heap-info", category.getName());
        Collection<Key<?>> keys = category.getKeys();
        assertEquals(6, keys.size());
        assertTrue(keys.contains(new Key<>("agentId")));
        assertTrue(keys.contains(new Key<>("vmId")));
        assertTrue(keys.contains(new Key<>("timeStamp")));
        assertTrue(keys.contains(new Key<>("heapId")));
        assertTrue(keys.contains(new Key<>("heapDumpId")));
        assertTrue(keys.contains(new Key<>("histogramId")));
    }
    
    @SuppressWarnings("unchecked")
    private void doAddHeapInfoVerifications(Storage storage,
            PreparedStatement<?> add, HeapInfo info)
            throws StatementExecutionException, DescriptorParsingException {
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(HeapDAOImpl.DESC_ADD_VM_HEAP_INFO, desc.getDescriptor());
        
        verify(add).setString(0, info.getAgentId());
        verify(add).setString(1, info.getVmId());
        verify(add).setLong(2, info.getTimeStamp());
        verify(add).setString(3, info.getHeapId());
        verify(add).setString(4, info.getHeapDumpId());
        verify(add).setString(5, info.getHistogramId());
        verify(add).execute();
        verifyNoMoreInteractions(add);
    }

    @Test
    public void testPutHeapInfo() throws IOException,
            StatementExecutionException, DescriptorParsingException {
        @SuppressWarnings("unchecked")
        PreparedStatement<HeapInfo> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(add);
        Runnable cleanup = mock(Runnable.class);
        dao.putHeapInfo(heapInfo, heapDumpData, histogram, cleanup);

        doAddHeapInfoVerifications(storage, add, heapInfo);

        ArgumentCaptor<InputStream> data = ArgumentCaptor.forClass(InputStream.class);
        ArgumentCaptor<SaveFileListener> saveListener = ArgumentCaptor.forClass(SaveFileListener.class);
        verify(storage).saveFile(eq("heapdump-test-vm1-12345"), data.capture(), saveListener.capture());
        InputStream in = data.getValue();
        assertEquals(1, in.read());
        assertEquals(2, in.read());
        assertEquals(3, in.read());
        assertEquals(-1, in.read());
        saveListener.getValue().notify(EventType.SAVE_COMPLETE, null);
        verify(cleanup).run();
        assertEquals("test-vm1-12345", heapInfo.getHeapId());
        ArgumentCaptor<InputStream> histoStream = ArgumentCaptor.forClass(InputStream.class);
        verify(storage).saveFile(eq("histogram-test-vm1-12345"), histoStream.capture(), isA(CloseOnSave.class));
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
    public void testPutHeapInfoWithoutDump() throws IOException,
            StatementExecutionException, DescriptorParsingException {
        @SuppressWarnings("unchecked")
        PreparedStatement<HeapInfo> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(add);
        
        dao.putHeapInfo(heapInfo, null, null, null);

        doAddHeapInfoVerifications(storage, add, heapInfo);

        verify(storage, never()).saveFile(anyString(), any(InputStream.class), isA(SaveFileListener.class));
        assertEquals("test-vm1-12345", heapInfo.getHeapId());
    }

    @Test
    public void testGetAllHeapInfo() throws DescriptorParsingException, StatementExecutionException {
        // verify a connection key has been created before requesting the
        // heap dumps
        verify(storage).registerCategory(HeapDAO.heapInfoCategory);
        
        HostRef host = new HostRef("123", "test-host");
        VmRef vm = new VmRef(host, "vm2", 234, "test-vm");
        
        @SuppressWarnings("unchecked")
        PreparedStatement<HeapInfo> stmt = mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        
        Cursor<HeapInfo> cursor = getMockCursor("foo-agent");
        when(stmt.executeQuery()).thenReturn(cursor);
        
        Collection<HeapInfo> heapInfos = dao.getAllHeapInfo(vm);

        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, "123");
        verify(stmt).setString(1, "vm2");
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);
        
        HeapInfo info1 = new HeapInfo("foo-agent", "vm2", 12345);
        info1.setHeapDumpId("test1");
        info1.setHeapId("testheap1");
        info1.setHistogramId("histotest1");
        
        HeapInfo info2 = new HeapInfo("foo-agent", "vm2", 23456);
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
        
        @SuppressWarnings("unchecked")
        PreparedStatement<HeapInfo> stmt = mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        
        Cursor<HeapInfo> cursor = getMockCursor("foo-agent");
        when(stmt.executeQuery()).thenReturn(cursor);
        
        HeapInfo result = dao.getHeapInfo(heapId);
        
        verify(storage).prepareStatement(anyDescriptor());
        verify(stmt).setString(0, heapId);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);
        
        HeapInfo info = new HeapInfo("foo-agent", "vm2", 12345);
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
    public void testInvalidHeapId() throws IOException,
            StatementExecutionException, DescriptorParsingException {
        @SuppressWarnings("unchecked")
        PreparedStatement<HeapInfo> stmt = mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor())).thenReturn(stmt);
        when(stmt.executeQuery()).thenThrow(new IllegalArgumentException("invalid ObjectId"));
        dao = new HeapDAOImpl(storage);
        heapInfo = dao.getHeapInfo("some-random-heap-id");
        assertTrue(heapInfo == null);
    }
}

