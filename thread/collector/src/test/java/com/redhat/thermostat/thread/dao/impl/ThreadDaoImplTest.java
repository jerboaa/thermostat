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

import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StatementExecutionException;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadHarvestingStatus;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public class ThreadDaoImplTest {

    @Test
    public void preparedQueryDescriptorsAreSane() {
        String expectedQueryThreadCaps = "QUERY vm-thread-capabilities WHERE 'agentId' = ?s AND 'vmId' = ?i LIMIT 1";
        assertEquals(expectedQueryThreadCaps, ThreadDaoImpl.QUERY_THREAD_CAPS);
        String expectedQueryLatestSummary = "QUERY vm-thread-summary WHERE 'agentId' = ?s AND 'vmId' = ?i SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(expectedQueryLatestSummary, ThreadDaoImpl.QUERY_LATEST_SUMMARY);
        String expectedQuerySummarySince = "QUERY vm-thread-summary WHERE 'agentId' = ?s AND 'vmId' = ?i AND 'timeStamp' > ?l SORT 'timeStamp' DSC";
        assertEquals(expectedQuerySummarySince, ThreadDaoImpl.QUERY_SUMMARY_SINCE);
        String expectedQueryLatestHarvestingStatus = "QUERY vm-thread-harvesting WHERE 'agentId' = ?s AND 'vmId' = ?i SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(expectedQueryLatestHarvestingStatus, ThreadDaoImpl.QUERY_LATEST_HARVESTING_STATUS);
        String expectedQueryThreadInfo = "QUERY vm-thread-info WHERE 'agentId' = ?s AND 'vmId' = ?i AND 'timeStamp' > ?l SORT 'timeStamp' DSC";
        assertEquals(expectedQueryThreadInfo, ThreadDaoImpl.QUERY_THREAD_INFO);
        String expectedQueryThreadLatestDeadlockInfo = "QUERY vm-deadlock-data WHERE 'agentId' = ?s AND 'vmId' = ?i SORT 'timeStamp' DSC LIMIT 1";
        assertEquals(expectedQueryThreadLatestDeadlockInfo, ThreadDaoImpl.QUERY_LATEST_DEADLOCK_INFO);
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
        when(ref.getId()).thenReturn(42);
        
        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");
        
        when(ref.getAgent()).thenReturn(agent);

        VMThreadCapabilities expected = new VMThreadCapabilities();
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
        verify(stmt).setInt(1, 42);
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
        when(ref.getId()).thenReturn(42);

        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");

        when(ref.getAgent()).thenReturn(agent);

        VMThreadCapabilities expected = new VMThreadCapabilities();
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
        verify(stmt).setInt(1, 42);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertEquals(null, caps);
    }

    @Test
    public void testSaveVMCapabilities() {
        Storage storage = mock(Storage.class);
        Replace replace = mock(Replace.class);
        when(storage.createReplace(any(Category.class))).thenReturn(replace);

        VMThreadCapabilities caps = mock(VMThreadCapabilities.class);
        when(caps.supportContentionMonitor()).thenReturn(true);
        when(caps.supportCPUTime()).thenReturn(true);
        when(caps.supportThreadAllocatedMemory()).thenReturn(true);
        when(caps.getVmId()).thenReturn(42);
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveCapabilities(caps);

        verify(storage).createReplace(ThreadDao.THREAD_CAPABILITIES);
        verify(replace).setPojo(caps);
        verify(replace).apply();
    }

    @Test
    public void testLoadLatestDeadLockStatus() throws DescriptorParsingException, StatementExecutionException {
        VmRef vm = mock(VmRef.class);
        when(vm.getId()).thenReturn(42);
        when(vm.getIdString()).thenReturn("42");

        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");
        when(vm.getAgent()).thenReturn(agent);

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
        verify(stmt).setInt(1, 42);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);
    }

    @Test
    public void testSaveDeadLockStatus() {
        Storage storage = mock(Storage.class);
        Add add = mock(Add.class);
        when(storage.createAdd(ThreadDaoImpl.DEADLOCK_INFO)).thenReturn(add);

        VmDeadLockData status = mock(VmDeadLockData.class);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveDeadLockStatus(status);

        verify(add).setPojo(status);
        verify(add).apply();
    }

    @Test
    public void testGetLatestHarvestingStatus() throws DescriptorParsingException, StatementExecutionException {
        VmRef vm = mock(VmRef.class);
        when(vm.getId()).thenReturn(42);
        when(vm.getIdString()).thenReturn("42");

        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");
        when(vm.getAgent()).thenReturn(agent);

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
        verify(stmt).setInt(1, 42);
        verify(stmt).executeQuery();
        verifyNoMoreInteractions(stmt);

        assertSame(status, result);
    }

    @Test
    public void testSetHarvestingStatus() {
        Storage storage = mock(Storage.class);
        Add add = mock(Add.class);
        when(storage.createAdd(ThreadDaoImpl.THREAD_HARVESTING_STATUS)).thenReturn(add);

        ThreadHarvestingStatus status = mock(ThreadHarvestingStatus.class);

        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveHarvestingStatus(status);

        verify(add).setPojo(status);
        verify(add).apply();
    }
}

