/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.thread.dao.impl;

import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.ThreadHeader;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.thread.model.VmDeadLockData;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ThreadDaoImplTest {

    private static final String AGENT_ID = "0xcafe";
    private static final String VM_ID = "VM42";

    private VmRef vmRef;
    private HostRef hostRef;

    @Before
    public void setUp() {
        hostRef = mock(HostRef.class);
        when(hostRef.getAgentId()).thenReturn(AGENT_ID);

        vmRef = mock(VmRef.class);
        when(vmRef.getHostRef()).thenReturn(hostRef);
        when(vmRef.getVmId()).thenReturn(VM_ID);
    }

    @Test
    public void preparedQueryDescriptorsAreSane() {

        String expectedQueryLatestHarvestingStatus = "QUERY vm-thread-harvesting WHERE 'agentId' = ?s AND 'vmId' = ?s SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(expectedQueryLatestHarvestingStatus, ThreadDaoImpl.QUERY_LATEST_HARVESTING_STATUS);

        String expectedQueryThreadHeader = "QUERY vm-thread-header WHERE 'agentId' = ?s AND 'vmId' = ?s AND 'threadName' = ?s AND 'threadId' = ?l LIMIT 1";
        assertEquals(expectedQueryThreadHeader, ThreadDaoImpl.QUERY_THREAD_HEADER);

        String expectedQueryAllThreadHeaders = "QUERY vm-thread-header WHERE 'agentId' = ?s AND 'vmId' = ?s SORT 'timeStamp' DSC";
        assertEquals(expectedQueryAllThreadHeaders, ThreadDaoImpl.QUERY_ALL_THREAD_HEADERS);

        String expectedQueryLatestThreadStateForThread = "QUERY vm-thread-state WHERE 'agentId' = ?s AND 'referenceID' = ?s SORT 'probeEndTime' DSC LIMIT 1";
        assertEquals(expectedQueryLatestThreadStateForThread, ThreadDaoImpl.QUERY_LATEST_THREAD_STATE_FOR_THREAD);

        String expectedQueryOldestThreadState = "QUERY vm-thread-state WHERE 'agentId' = ?s AND 'vmId' = ?s SORT 'probeStartTime' ASC LIMIT 1";
        assertEquals(expectedQueryOldestThreadState, ThreadDaoImpl.QUERY_OLDEST_THREAD_STATE);

        String expectedQueryThreadLatestDeadlockInfo = "QUERY vm-deadlock-data WHERE 'agentId' = ?s AND 'vmId' = ?s SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(expectedQueryThreadLatestDeadlockInfo, ThreadDaoImpl.QUERY_LATEST_DEADLOCK_INFO);

        String addThreadHarvesting = "ADD vm-thread-harvesting SET 'agentId' = ?s , " +
                                                    "'vmId' = ?s , " +
                                                    "'timeStamp' = ?l , " +
                                                    "'harvesting' = ?b";
        assertEquals(addThreadHarvesting, ThreadDaoImpl.DESC_ADD_THREAD_HARVESTING_STATUS);

        String addThreadInfo = "ADD vm-thread-header SET 'agentId' = ?s , " +
                                    "'vmId' = ?s , " +
                                    "'threadName' = ?s , " +
                                    "'threadId' = ?l , " +
                                    "'timeStamp' = ?l , " +
                                    "'referenceID' = ?s";
        assertEquals(addThreadInfo, ThreadDaoImpl.ADD_THREAD_HEADER);
        String addDeadlockData = "ADD vm-deadlock-data SET 'agentId' = ?s , " +
                                    "'vmId' = ?s , " +
                                    "'timeStamp' = ?l , " +
                                    "'deadLockDescription' = ?s";
        assertEquals(addDeadlockData, ThreadDaoImpl.DESC_ADD_THREAD_DEADLOCK_DATA);

        String addThreadState = "ADD vm-thread-state SET 'agentId' = ?s , " +
                                "'vmId' = ?s , 'state' = ?s , " +
                                "'probeStartTime' = ?l , 'probeEndTime' = ?l , " +
                                "'referenceID' = ?s";
        assertEquals(addThreadState, ThreadDaoImpl.ADD_THREAD_STATE);

        String getThreadStatesForVM = "QUERY vm-thread-state WHERE " +
                                      "'referenceID' = ?s AND " +
                                      "'probeEndTime' >= ?l AND " +
                                      "'probeStartTime' <= ?l SORT " +
                                      "'probeStartTime' ASC";
        assertEquals(getThreadStatesForVM, ThreadDaoImpl.QUERY_THREAD_STATE_PER_THREAD);

        String addContentionSample = "ADD thread-contention-sample SET " +
                "'agentId' = ?s , 'vmId' = ?s , 'blockedCount' = ?l , " +
                "'blockedTime' = ?l , 'waitedCount' = ?l , " +
                "'waitedTime' = ?l , 'referenceID' = ?s , 'timeStamp' = ?l";
        assertEquals(addContentionSample, ThreadDaoImpl.ADD_CONTENTION_SAMPLE);

        String getLatestContentionSample = "QUERY thread-contention-sample " +
                "WHERE 'referenceID' = ?s SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(getLatestContentionSample, ThreadDaoImpl.GET_LATEST_CONTENTION_SAMPLE);

        String getFirstThreadState = "QUERY vm-thread-state WHERE " +
                "'agentId' = ?s AND 'referenceID' = ?s SORT 'probeStartTime' " +
                "ASC LIMIT 1";
        assertEquals(getFirstThreadState, ThreadDaoImpl.QUERY_FIRST_THREAD_STATE_FOR_THREAD);
    }
    
    @Test
    public void testThreadDaoCategoryRegistration() {
        Storage storage = mock(Storage.class);
        
        @SuppressWarnings("unused")
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        
        verify(storage).registerCategory(ThreadDao.THREAD_HARVESTING_STATUS);
    }

    @SuppressWarnings("unchecked")
    private <T extends Pojo> StatementDescriptor<T> anyDescriptor(Class<T> type) {
        return (StatementDescriptor<T>) any(StatementDescriptor.class);
    }

    @Test
    public void testLoadLatestDeadLockStatusWithNoData() throws Exception {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<VmDeadLockData> stmt = (PreparedStatement<VmDeadLockData>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor(VmDeadLockData.class))).thenReturn(stmt);
        @SuppressWarnings("unchecked")
        Cursor<VmDeadLockData> cursor = (Cursor<VmDeadLockData>) mock(Cursor.class);

        when(cursor.hasNext()).thenReturn(false);
        when(cursor.next()).thenThrow(new IllegalStateException("must not do this"));
        when(stmt.executeQuery()).thenReturn(cursor);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        VmDeadLockData result = dao.loadLatestDeadLockStatus(vmRef);

        assertNull(result);
    }

    @Test
    public void testLoadLatestDeadLockStatus() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<VmDeadLockData> stmt = (PreparedStatement<VmDeadLockData>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor(VmDeadLockData.class))).thenReturn(stmt);
        @SuppressWarnings("unchecked")
        Cursor<VmDeadLockData> cursor = (Cursor<VmDeadLockData>) mock(Cursor.class);
        VmDeadLockData data = mock(VmDeadLockData.class);

        when(cursor.hasNext()).thenReturn(true);
        when(cursor.next()).thenReturn(data);
        when(stmt.executeQuery()).thenReturn(cursor);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        VmDeadLockData result = dao.loadLatestDeadLockStatus(vmRef);

        assertSame(data, result);

        verify(storage).prepareStatement(anyDescriptor(VmDeadLockData.class));
        verify(stmt).setString(0, AGENT_ID);
        verify(stmt).setString(1, VM_ID);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSaveDeadLockStatus() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        PreparedStatement<VmDeadLockData> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);

        VmDeadLockData status = mock(VmDeadLockData.class);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveDeadLockStatus(status);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<VmDeadLockData> desc = captor.getValue();
        assertEquals(ThreadDaoImpl.DESC_ADD_THREAD_DEADLOCK_DATA, desc.getDescriptor());

        verify(add).setString(0, status.getAgentId());
        verify(add).setString(1, status.getVmId());
        verify(add).setLong(2, status.getTimeStamp());
        verify(add).setString(3, status.getDeadLockDescription());
        verify(add).execute();
        Mockito.verifyNoMoreInteractions(add);
    }

    @Test
    public void testGetLatestHarvestingStatus()
            throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<ThreadHarvestingStatus> stmt = (PreparedStatement<ThreadHarvestingStatus>) mock(PreparedStatement.class);
        when(storage.prepareStatement(anyDescriptor(ThreadHarvestingStatus.class))).thenReturn(stmt);
        @SuppressWarnings("unchecked")
        Cursor<ThreadHarvestingStatus> cursor = (Cursor<ThreadHarvestingStatus>) mock(Cursor.class);
        ThreadHarvestingStatus status = mock(ThreadHarvestingStatus.class);

        when(cursor.hasNext()).thenReturn(true);
        when(cursor.next()).thenReturn(status);
        when(stmt.executeQuery()).thenReturn(cursor);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        ThreadHarvestingStatus result = dao.getLatestHarvestingStatus(vmRef);

        verify(storage).prepareStatement(anyDescriptor(ThreadHarvestingStatus.class));
        verify(stmt).setString(0, AGENT_ID);
        verify(stmt).setString(1, VM_ID);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertSame(status, result);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddHarvestingStatus() throws DescriptorParsingException, StatementExecutionException {
        Storage storage = mock(Storage.class);
        PreparedStatement<ThreadHarvestingStatus> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);

        ThreadHarvestingStatus status = mock(ThreadHarvestingStatus.class);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveHarvestingStatus(status);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<VmDeadLockData> desc = captor.getValue();
        assertEquals(ThreadDaoImpl.DESC_ADD_THREAD_HARVESTING_STATUS, desc.getDescriptor());

        verify(add).setString(0, status.getAgentId());
        verify(add).setString(1, status.getVmId());
        verify(add).setLong(2, status.getTimeStamp());
        verify(add).setBoolean(3, status.isHarvesting());
        verify(add).execute();
        verifyNoMoreInteractions(add);
    }

    @Test
    public void testSaveThread() throws DescriptorParsingException, StatementExecutionException {
        final String THREAD_NAME = "name of a thread";
        final long THREAD_ID = 0xcafebabe;
        final long TIMESTAMP = 0xdeadbeef;
        final String REF_ID = "42";

        Storage storage = mock(Storage.class);
        PreparedStatement<ThreadHeader> add = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(add);

        // not using mocks because ThreadHeader is really a data holder (no logic)
        ThreadHeader header = new ThreadHeader();
        header.setAgentId(AGENT_ID);
        header.setVmId(VM_ID);
        header.setThreadName(THREAD_NAME);
        header.setThreadId(THREAD_ID);
        header.setTimeStamp(TIMESTAMP);
        header.setReferenceID(REF_ID);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveThread(header);

        verify(add).setString(0, AGENT_ID);
        verify(add).setString(1, VM_ID);
        verify(add).setString(2, THREAD_NAME);
        verify(add).setLong(3, THREAD_ID);
        verify(add).setLong(4, TIMESTAMP);
        verify(add).setString(5, REF_ID);

        verify(add).execute();

        verifyNoMoreInteractions(add);
    }

    @Test
    public void testGetThreadNoData() throws DescriptorParsingException, StatementExecutionException {
        final String THREAD_NAME = "name of a thread";
        final long THREAD_ID = 0xcafebabe;
        final long TIMESTAMP = 0xdeadbeef;
        final String REF_ID = "42";

        Cursor<ThreadHeader> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(false);

        Storage storage = mock(Storage.class);
        PreparedStatement<ThreadHeader> get = mock(PreparedStatement.class);
        when(get.executeQuery()).thenReturn(cursor);

        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(get);

        ThreadHeader header = new ThreadHeader();
        header.setAgentId(AGENT_ID);
        header.setVmId(VM_ID);
        header.setThreadName(THREAD_NAME);
        header.setThreadId(THREAD_ID);
        header.setTimeStamp(TIMESTAMP);
        header.setReferenceID(REF_ID);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        ThreadHeader result = dao.getThread(header);
        assertNull(result);

        verify(get).setString(0, AGENT_ID);
        verify(get).setString(1, VM_ID);
        verify(get).setString(2, THREAD_NAME);
        verify(get).setLong(3, THREAD_ID);

        verify(get).executeQuery();

        verifyNoMoreInteractions(get);
    }

    @Test
    public void testGetThreadWithData() throws DescriptorParsingException, StatementExecutionException {
        final String THREAD_NAME = "name of a thread";
        final long THREAD_ID = 0xcafebabe;
        final long TIMESTAMP = 0xdeadbeef;
        final String REF_ID = "42";

        ThreadHeader header = new ThreadHeader();
        header.setAgentId(AGENT_ID);
        header.setVmId(VM_ID);
        header.setThreadName(THREAD_NAME);
        header.setThreadId(THREAD_ID);
        header.setTimeStamp(TIMESTAMP);
        header.setReferenceID(REF_ID);

        Cursor<ThreadHeader> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(header).thenReturn(null);

        Storage storage = mock(Storage.class);
        PreparedStatement<ThreadHeader> get = mock(PreparedStatement.class);
        when(get.executeQuery()).thenReturn(cursor);

        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(get);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        ThreadHeader result = dao.getThread(header);
        assertNotNull(result);
        assertEquals(header, result);

        verify(get).setString(0, AGENT_ID);
        verify(get).setString(1, VM_ID);
        verify(get).setString(2, THREAD_NAME);
        verify(get).setLong(3, THREAD_ID);

        verify(get).executeQuery();

        verifyNoMoreInteractions(get);
    }

    @Test
    public void testGetThreads() throws DescriptorParsingException, StatementExecutionException {

        ThreadHeader header0 = mock(ThreadHeader.class);
        ThreadHeader header1 = mock(ThreadHeader.class);
        ThreadHeader header2 = mock(ThreadHeader.class);

        Cursor<ThreadHeader> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(header0).thenReturn(header1).thenReturn(header2).thenReturn(null);

        Storage storage = mock(Storage.class);
        PreparedStatement<ThreadHeader> get = mock(PreparedStatement.class);
        when(get.executeQuery()).thenReturn(cursor);

        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(get);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        List<ThreadHeader> result = dao.getThreads(vmRef);
        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertEquals(result.get(0), header0);
        assertEquals(result.get(1), header1);
        assertEquals(result.get(2), header2);

        verify(get).setString(0, AGENT_ID);
        verify(get).setString(1, VM_ID);

        verify(get).executeQuery();

        verifyNoMoreInteractions(get);
    }

    @Test
    public void testAddThreadState() throws Exception {
        final String VM_ID = "vm42";
        final String AGENT_ID = "agent42";
        final String REF_ID = "42";
        final String STATE = "runnable";
        final long START_TIME = 1l;
        final long STOP_TIME = 1l;

        ThreadHeader header = new ThreadHeader(AGENT_ID);
        header.setVmId(VM_ID);
        header.setReferenceID(REF_ID);

        ThreadState template = mock(ThreadState.class);
        when(template.getHeader()).thenReturn(header);
        when(template.getState()).thenReturn(STATE);
        when(template.getProbeStartTime()).thenReturn(START_TIME);
        when(template.getProbeEndTime()).thenReturn(STOP_TIME);

        Storage storage = mock(Storage.class);
        PreparedStatement<ThreadState> set = mock(PreparedStatement.class);

        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(set);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.addThreadState(template);

        verify(set).setString(0, AGENT_ID);
        verify(set).setString(1, VM_ID);
        verify(set).setString(2, STATE);
        verify(set).setLong(3, START_TIME);
        verify(set).setLong(4, STOP_TIME);
        verify(set).setString(5, REF_ID);

        verify(set).execute();
    }

    @Test
    public void testUpdateThreadState() throws Exception {
        final String REF_ID = "42";
        final long START_TIME = 0l;
        final long STOP_TIME = 1l;

        ThreadHeader header = new ThreadHeader(AGENT_ID);
        header.setVmId(VM_ID);
        header.setReferenceID(REF_ID);

        ThreadState template = mock(ThreadState.class);
        when(template.getHeader()).thenReturn(header);
        when(template.getProbeStartTime()).thenReturn(START_TIME);
        when(template.getProbeEndTime()).thenReturn(STOP_TIME);

        Storage storage = mock(Storage.class);
        PreparedStatement<ThreadState> update = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(update);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.updateThreadState(template);

        verify(update).setLong(0, STOP_TIME);
        verify(update).setString(1, REF_ID);
        verify(update).setLong(2, START_TIME);

        verify(update).execute();
    }

    @Test
    public void testGetThreadStateTotalTimeRange() throws Exception {

        Range<Long> oldest = new Range(0l, 1l);
        ThreadState oldestData = mock(ThreadState.class);
        when(oldestData.getRange()).thenReturn(oldest);

        Range<Long> latest = new Range(2l, 3l);
        ThreadState latestData = mock(ThreadState.class);
        when(latestData.getRange()).thenReturn(latest);

        Cursor<ThreadState> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(oldestData).thenReturn(latestData);

        Storage storage = mock(Storage.class);
        PreparedStatement<ThreadState> get = mock(PreparedStatement.class);
        when(get.executeQuery()).thenReturn(cursor);

        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(get);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        Range<Long> result = dao.getThreadStateTotalTimeRange(vmRef);
        assertNotNull(result);

        verify(get, times(2)).setString(0, AGENT_ID);
        verify(get, times(2)).setString(1, VM_ID);

        verify(get, times(2)).executeQuery();

        verifyNoMoreInteractions(get);
    }

    @Test
    public void testGetThreadStateTotalTimeRangeNoData() throws Exception {

        Cursor<ThreadState> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(false);

        Storage storage = mock(Storage.class);
        PreparedStatement<ThreadState> get = mock(PreparedStatement.class);
        when(get.executeQuery()).thenReturn(cursor);

        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(get);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        Range<Long> result = dao.getThreadStateTotalTimeRange(vmRef);
        assertNull(result);

        verify(get).setString(0, AGENT_ID);
        verify(get).setString(1, VM_ID);

        verify(get).executeQuery();

        verifyNoMoreInteractions(get);
    }
}

