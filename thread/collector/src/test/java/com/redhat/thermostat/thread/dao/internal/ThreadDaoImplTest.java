/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.thread.dao.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.VmDeadLockData;

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

        String expectedQueryThreadLatestDeadlockInfo = "QUERY vm-deadlock-data WHERE 'agentId' = ?s AND 'vmId' = ?s SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(expectedQueryThreadLatestDeadlockInfo, ThreadDaoImpl.QUERY_LATEST_DEADLOCK_INFO);

        String aggregateCountAllDeadLocks = "QUERY-COUNT vm-deadlock-data";
        assertEquals(aggregateCountAllDeadLocks, ThreadDaoImpl.AGGREGATE_COUNT_ALL_DEADLOCKS);

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
    public void testGetDeadLockCount()
            throws DescriptorParsingException, StatementExecutionException {

        AggregateCount count = new AggregateCount();
        count.setCount(2);

        @SuppressWarnings("unchecked")
        Cursor<AggregateCount> c = (Cursor<AggregateCount>) mock(Cursor.class);
        when(c.hasNext()).thenReturn(true).thenReturn(false);
        when(c.next()).thenReturn(count).thenThrow(new NoSuchElementException());

        Storage storage = mock(Storage.class);
        @SuppressWarnings("unchecked")
        PreparedStatement<AggregateCount> stmt = (PreparedStatement<AggregateCount>) mock(PreparedStatement.class);
        @SuppressWarnings("unchecked")
        StatementDescriptor<AggregateCount> desc = any(StatementDescriptor.class);
        when(storage.prepareStatement(desc)).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(c);
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);

        assertEquals(2, dao.getDeadLockCount());
    }

}

