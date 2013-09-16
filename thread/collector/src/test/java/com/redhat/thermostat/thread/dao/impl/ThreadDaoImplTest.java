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

package com.redhat.thermostat.thread.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;

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
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public class ThreadDaoImplTest {

    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedQueryThreadCaps = "QUERY vm-thread-capabilities WHERE 'agentId' = ?s AND 'vmId' = ?s LIMIT 1";
        assertEquals(expectedQueryThreadCaps, ThreadDaoImpl.QUERY_THREAD_CAPS);
        String expectedQueryLatestSummary = "QUERY vm-thread-summary WHERE 'agentId' = ?s AND 'vmId' = ?s SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(expectedQueryLatestSummary, ThreadDaoImpl.QUERY_LATEST_SUMMARY);
        String expectedQuerySummarySince = "QUERY vm-thread-summary WHERE 'agentId' = ?s AND 'vmId' = ?s AND 'timeStamp' > ?l SORT 'timeStamp' DSC";
        assertEquals(expectedQuerySummarySince, ThreadDaoImpl.QUERY_SUMMARY_SINCE);
        String expectedQueryLatestHarvestingStatus = "QUERY vm-thread-harvesting WHERE 'agentId' = ?s AND 'vmId' = ?s SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(expectedQueryLatestHarvestingStatus, ThreadDaoImpl.QUERY_LATEST_HARVESTING_STATUS);
        String expectedQueryThreadInfo = "QUERY vm-thread-info WHERE 'agentId' = ?s AND 'vmId' = ?s AND 'timeStamp' > ?l SORT 'timeStamp' DSC";
        assertEquals(expectedQueryThreadInfo, ThreadDaoImpl.QUERY_THREAD_INFO);
        String expectedQueryThreadLatestDeadlockInfo = "QUERY vm-deadlock-data WHERE 'agentId' = ?s AND 'vmId' = ?s SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(expectedQueryThreadLatestDeadlockInfo, ThreadDaoImpl.QUERY_LATEST_DEADLOCK_INFO);
        String addThreadSummary = "ADD vm-thread-summary SET 'agentId' = ?s , " +
                                            "'vmId' = ?s , " +
                                            "'currentLiveThreads' = ?l , " +
                                            "'currentDaemonThreads' = ?l , " +
                                            "'timeStamp' = ?l";
        assertEquals(addThreadSummary, ThreadDaoImpl.DESC_ADD_THREAD_SUMMARY);
        String addThreadHarvesting = "ADD vm-thread-harvesting SET 'agentId' = ?s , " +
                                                    "'vmId' = ?s , " +
                                                    "'timeStamp' = ?l , " +
                                                    "'harvesting' = ?b";
        assertEquals(addThreadHarvesting, ThreadDaoImpl.DESC_ADD_THREAD_HARVESTING_STATUS);
        String addThreadInfo = "ADD vm-thread-info SET 'agentId' = ?s , " +
                                    "'vmId' = ?s , " +
                                    "'threadName' = ?s , " +
                                    "'threadId' = ?l , " +
                                    "'threadState' = ?s , " +
                                    "'allocatedBytes' = ?l , " +
                                    "'timeStamp' = ?l , " +
                                    "'threadCpuTime' = ?l , " +
                                    "'threadUserTime' = ?l , " +
                                    "'threadBlockedCount' = ?l , " +
                                    "'threadWaitCount' = ?l";
        assertEquals(addThreadInfo, ThreadDaoImpl.DESC_ADD_THREAD_INFO);
        String addDeadlockData = "ADD vm-deadlock-data SET 'agentId' = ?s , " +
                                    "'vmId' = ?s , " +
                                    "'timeStamp' = ?l , " +
                                    "'deadLockDescription' = ?s";
        assertEquals(addDeadlockData, ThreadDaoImpl.DESC_ADD_THREAD_DEADLOCK_DATA);
        String replaceThreadCaps = "REPLACE vm-thread-capabilities SET 'agentId' = ?s , "+
                                        "'vmId' = ?s , " +
                                        "'supportedFeaturesList' = ?s[" +
                                    " WHERE 'agentId' = ?s AND 'vmId' = ?s";
        assertEquals(replaceThreadCaps, ThreadDaoImpl.DESC_REPLACE_THREAD_CAPS);
    }
    
    @Test
    public void testThreadDaoCategoryRegistration() {
        Storage storage = mock(Storage.class);
        
        @SuppressWarnings("unused")
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        
        verify(storage).registerCategory(ThreadDao.THREAD_CAPABILITIES);
        verify(storage).registerCategory(ThreadDao.THREAD_HARVESTING_STATUS);
        verify(storage).registerCategory(ThreadDao.THREAD_INFO);
        verify(storage).registerCategory(ThreadDao.THREAD_SUMMARY);
    }
    
    @Test
    public void testLoadVMCapabilities() throws DescriptorParsingException, StatementExecutionException {
        @SuppressWarnings("unchecked")
        PreparedStatement<VMThreadCapabilities> stmt = (PreparedStatement<VMThreadCapabilities>) mock(PreparedStatement.class);
        Storage storage = mock(Storage.class);
        when(storage.prepareStatement(anyDescriptor(VMThreadCapabilities.class))).thenReturn(stmt);
        VmRef ref = mock(VmRef.class);
        when(ref.getVmId()).thenReturn("VM42");
        
        String agentId = "0xcafe";
        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn(agentId);
        
        when(ref.getHostRef()).thenReturn(agent);

        VMThreadCapabilities expected = new VMThreadCapabilities(agentId);
        expected.setSupportedFeaturesList(new String[] { ThreadDao.CPU_TIME, ThreadDao.THREAD_ALLOCATED_MEMORY });
        @SuppressWarnings("unchecked")
        Cursor<VMThreadCapabilities> cursor = (Cursor<VMThreadCapabilities>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(expected).thenReturn(null);
        when(stmt.executeQuery()).thenReturn(cursor);
        
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        VMThreadCapabilities caps = dao.loadCapabilities(ref);

        verify(storage).prepareStatement(anyDescriptor(VMThreadCapabilities.class));
        verify(stmt).setString(0, "0xcafe");
        verify(stmt).setString(1, "VM42");
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertFalse(caps.supportContentionMonitor());
        assertTrue(caps.supportCPUTime());
        assertTrue(caps.supportThreadAllocatedMemory());
    }

    @SuppressWarnings("unchecked")
    private <T extends Pojo> StatementDescriptor<T> anyDescriptor(Class<T> type) {
        return (StatementDescriptor<T>) any(StatementDescriptor.class);
    }

    @Test
    public void testLoadVMCapabilitiesWithoutAnyDataInStorage() throws DescriptorParsingException, StatementExecutionException {
        @SuppressWarnings("unchecked")
        PreparedStatement<VMThreadCapabilities> stmt = (PreparedStatement<VMThreadCapabilities>) mock(PreparedStatement.class);
        Storage storage = mock(Storage.class);
        when(storage.prepareStatement(anyDescriptor(VMThreadCapabilities.class))).thenReturn(stmt);
        VmRef ref = mock(VmRef.class);
        when(ref.getVmId()).thenReturn("VM42");

        String agentId = "0xcafe";
        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn(agentId);

        when(ref.getHostRef()).thenReturn(agent);

        VMThreadCapabilities expected = new VMThreadCapabilities(agentId);
        expected.setSupportedFeaturesList(new String[] { ThreadDao.CPU_TIME, ThreadDao.THREAD_ALLOCATED_MEMORY });
        @SuppressWarnings("unchecked")
        Cursor<VMThreadCapabilities> cursor = (Cursor<VMThreadCapabilities>) mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(false);
        when(cursor.next()).thenThrow(new NoSuchElementException());
        when(stmt.executeQuery()).thenReturn(cursor);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        VMThreadCapabilities caps = dao.loadCapabilities(ref);

        verify(storage).prepareStatement(anyDescriptor(VMThreadCapabilities.class));
        verify(stmt).setString(0, "0xcafe");
        verify(stmt).setString(1, "VM42");
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(null, caps);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSaveVMCapabilities() throws DescriptorParsingException, StatementExecutionException {
        String agentId = "fooAgent";
        Storage storage = mock(Storage.class);
        PreparedStatement<AgentInformation> replace = mock(PreparedStatement.class);
        when(storage.prepareStatement(any(StatementDescriptor.class))).thenReturn(replace);
        
        String vmId = "VM42";
        VMThreadCapabilities caps = new VMThreadCapabilities(agentId);
        String[] capsFeatures = new String[] {
                ThreadDao.CONTENTION_MONITOR,
                ThreadDao.CPU_TIME,
                ThreadDao.THREAD_ALLOCATED_MEMORY,
        };
        caps.setSupportedFeaturesList(capsFeatures);
        assertTrue(caps.supportContentionMonitor());
        assertTrue(caps.supportCPUTime());
        assertTrue(caps.supportThreadAllocatedMemory());
        caps.setVmId(vmId);
        
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveCapabilities(caps);
        
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<StatementDescriptor> captor = ArgumentCaptor.forClass(StatementDescriptor.class);
        
        verify(storage).prepareStatement(captor.capture());
        StatementDescriptor<?> desc = captor.getValue();
        assertEquals(ThreadDaoImpl.DESC_REPLACE_THREAD_CAPS, desc.getDescriptor());
        
        verify(replace).setString(0, caps.getAgentId());
        verify(replace).setString(1, caps.getVmId());
        verify(replace).setStringList(2, caps.getSupportedFeaturesList());
        verify(replace).setString(3, caps.getAgentId());
        verify(replace).setString(4, caps.getVmId());
        verify(replace).execute();
        verifyNoMoreInteractions(replace);
    }

    @Test
    public void testLoadLatestDeadLockStatus() throws DescriptorParsingException, StatementExecutionException {
        VmRef vm = mock(VmRef.class);
        when(vm.getVmId()).thenReturn("VM42");

        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");
        when(vm.getHostRef()).thenReturn(agent);

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
        VmDeadLockData result = dao.loadLatestDeadLockStatus(vm);

        assertSame(data, result);

        verify(storage).prepareStatement(anyDescriptor(VmDeadLockData.class));
        verify(stmt).setString(0, "0xcafe");
        verify(stmt).setString(1, "VM42");
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
        VmRef vm = mock(VmRef.class);
        when(vm.getVmId()).thenReturn("VM42");

        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");
        when(vm.getHostRef()).thenReturn(agent);

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
        ThreadHarvestingStatus result = dao.getLatestHarvestingStatus(vm);

        verify(storage).prepareStatement(anyDescriptor(ThreadHarvestingStatus.class));
        verify(stmt).setString(0, "0xcafe");
        verify(stmt).setString(1, "VM42");
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
}

