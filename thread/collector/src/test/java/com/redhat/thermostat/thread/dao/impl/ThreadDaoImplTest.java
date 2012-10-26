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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.storage.Key;
import com.redhat.thermostat.common.storage.Query.Criteria;
import com.redhat.thermostat.common.storage.Storage;
import com.redhat.thermostat.test.MockQuery;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.VMThreadCapabilities;

public class ThreadDaoImplTest {

    @Test
    public void testCreateConnectionKey() {
        Storage storage = mock(Storage.class);
        
        @SuppressWarnings("unused")
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        
        verify(storage).registerCategory(ThreadDao.THREAD_CAPABILITIES);
        verify(storage).registerCategory(ThreadDao.THREAD_INFO);
        verify(storage).registerCategory(ThreadDao.THREAD_SUMMARY);
    }
    
    @Test
    public void testLoadVMCapabilities() {
        MockQuery query = new MockQuery();
        Storage storage = mock(Storage.class);
        when(storage.createQuery()).thenReturn(query);
        VmRef ref = mock(VmRef.class);
        when(ref.getId()).thenReturn(42);
        
        HostRef agent = mock(HostRef.class);
        when(agent.getAgentId()).thenReturn("0xcafe");
        
        when(ref.getAgent()).thenReturn(agent);

        VMThreadCapabilities expected = new VMThreadCapabilities();
        expected.setSupportedFeaturesList(new String[] { ThreadDao.CPU_TIME, ThreadDao.THREAD_ALLOCATED_MEMORY });
        when(storage.findPojo(query, VMThreadCapabilities.class)).thenReturn(expected);
        
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        VMThreadCapabilities caps = dao.loadCapabilities(ref);

        assertTrue(query.hasWhereClause(Key.VM_ID, Criteria.EQUALS, 42));
        assertTrue(query.hasWhereClause(Key.AGENT_ID, Criteria.EQUALS, "0xcafe"));
        
        assertFalse(caps.supportContentionMonitor());
        assertTrue(caps.supportCPUTime());
        assertTrue(caps.supportThreadAllocatedMemory());
    }
    
    @Test
    public void testSaveVMCapabilities() {
        Storage storage = mock(Storage.class);
        
        VMThreadCapabilities caps = mock(VMThreadCapabilities.class);
        when(caps.supportContentionMonitor()).thenReturn(true);
        when(caps.supportCPUTime()).thenReturn(true);
        when(caps.supportThreadAllocatedMemory()).thenReturn(true);
        when(caps.getVmId()).thenReturn(42);
        ThreadDaoImpl dao = new ThreadDaoImpl(storage);
        dao.saveCapabilities(caps);

        verify(storage).putPojo(ThreadDao.THREAD_CAPABILITIES, true, caps);

    }
}
