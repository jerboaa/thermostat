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

package com.redhat.thermostat.thread.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.thread.collector.VMThreadCapabilities;
import com.redhat.thermostat.thread.dao.ThreadDao;

public class ThreadDaoImplTest {

    @Test
    public void testCreateConnectionKey() {
        Storage storage = mock(Storage.class);
        
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        verify(storage).createConnectionKey(ThreadDao.THREAD_CAPABILITIES);
        verify(storage).createConnectionKey(ThreadDao.THREAD_INFO);
        verify(storage).createConnectionKey(ThreadDao.THREAD_SUMMARY);
    }
    
    @Test
    public void testLoadVMCapabilities() {
        Storage storage = mock(Storage.class);
        VmRef ref = mock(VmRef.class);
        when(ref.getId()).thenReturn(42);
        
        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");
        
        when(ref.getAgent()).thenReturn(agent);
        
        Chunk answer = mock(Chunk.class);
        when(answer.get(ThreadDao.CONTENTION_MONITOR_KEY)).thenReturn(false);
        when(answer.get(ThreadDao.CPU_TIME_KEY)).thenReturn(true);
        when(answer.get(ThreadDao.THREAD_ALLOCATED_MEMORY_KEY)).thenReturn(true);
        
        ArgumentCaptor<Chunk> queryCaptor = ArgumentCaptor.forClass(Chunk.class);
        when(storage.find(queryCaptor.capture())).thenReturn(answer);
        
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        VMThreadCapabilities caps = dao.loadCapabilities(ref);

        Chunk query = queryCaptor.getValue();
        assertEquals(42, (int) query.get(Key.VM_ID));
        assertEquals("0xcafe", query.get(Key.AGENT_ID));
        
        assertFalse(caps.supportContentionMonitor());
        assertTrue(caps.supportCPUTime());
        assertTrue(caps.supportThreadAllocatedMemory());
    }
    
    @Test
    public void testSaveVMCapabilities() {
        Storage storage = mock(Storage.class);
        
        VmRef ref = mock(VmRef.class);
        when(ref.getId()).thenReturn(42);
        
        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");

        when(ref.getAgent()).thenReturn(agent);

        Chunk answer = mock(Chunk.class);
        when(answer.get(ThreadDao.CONTENTION_MONITOR_KEY)).thenReturn(false);
        when(answer.get(ThreadDao.CPU_TIME_KEY)).thenReturn(true);
        
        VMThreadCapabilities caps = mock(VMThreadCapabilities.class);
        when(caps.supportContentionMonitor()).thenReturn(true);
        when(caps.supportCPUTime()).thenReturn(true);
        when(caps.supportThreadAllocatedMemory()).thenReturn(true);
        
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveCapabilities(ref, caps);

        ArgumentCaptor<Chunk> queryCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(storage).putChunk(queryCaptor.capture());

        Chunk query = queryCaptor.getValue();
        assertEquals(42, (int) query.get(Key.VM_ID));
        assertEquals("0xcafe", query.get(Key.AGENT_ID));
        assertTrue((boolean) query.get(ThreadDao.CONTENTION_MONITOR_KEY));
        assertTrue((boolean) query.get(ThreadDao.CPU_TIME_KEY));
        assertTrue((boolean) query.get(ThreadDao.THREAD_ALLOCATED_MEMORY_KEY));
    }
}
