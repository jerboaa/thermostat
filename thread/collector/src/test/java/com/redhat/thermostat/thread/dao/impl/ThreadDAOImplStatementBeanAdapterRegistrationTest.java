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

import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorMetadataFactory;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration;
import com.redhat.thermostat.storage.internal.dao.DAOImplStatementDescriptorRegistration;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ThreadDAOImplStatementBeanAdapterRegistrationTest {

    static class Triple<S, T, U> {
        final S first;
        final T second;
        final U third;

        public Triple(S first, T second, U third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }
    }
    
    @Test
    public void registersAllDescriptors() {
        ThreadDaoImplStatementDescriptorRegistration reg = new ThreadDaoImplStatementDescriptorRegistration();
        Set<String> descriptors = reg.getStatementDescriptors();
        assertEquals(20, descriptors.size());
        assertFalse("null statement not allowed", descriptors.contains(null));
    }
    
    /*
     * The web storage end-point uses service loader in order to determine the
     * list of trusted/known registrations. This test is to ensure service loading
     * works for this module's regs. E.g. renaming of the impl class without
     * changing META-INF/services/com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration
     */
    @Test
    public void serviceLoaderCanLoadRegistration() {
        Set<String> expectedClassNames = new HashSet<>();
        expectedClassNames.add(DAOImplStatementDescriptorRegistration.class.getName());
        expectedClassNames.add(ThreadDaoImplStatementDescriptorRegistration.class.getName());
        ServiceLoader<StatementDescriptorRegistration> loader = ServiceLoader.load(StatementDescriptorRegistration.class, ThreadDaoImplStatementDescriptorRegistration.class.getClassLoader());
        List<StatementDescriptorRegistration> registrations = new ArrayList<>(1);
        StatementDescriptorRegistration threadDaoReg = null;
        for (StatementDescriptorRegistration r: loader) {
            assertTrue(expectedClassNames.contains(r.getClass().getName()));
            if (r.getClass().getName().equals(ThreadDaoImplStatementDescriptorRegistration.class.getName())) {
                threadDaoReg = r;
            }
            registrations.add(r);
        }
        // storage-core + this module
        assertEquals(2, registrations.size());
        assertNotNull(threadDaoReg);
        assertEquals(20, threadDaoReg.getStatementDescriptors().size());
    }
    
    private Triple<String, String, PreparedParameter[]> setupForMetaDataTest() {
        PreparedParameter agentIdParam = mock(PreparedParameter.class);
        PreparedParameter vmIdParam = mock(PreparedParameter.class);
        String agentId = "agentId";
        String vmId = "vmId";
        when(agentIdParam.getValue()).thenReturn(agentId);
        when(vmIdParam.getValue()).thenReturn(vmId);
        PreparedParameter[] params = new PreparedParameter[] { agentIdParam,
                vmIdParam };
        return new Triple<String, String, PreparedParameter[]>(agentId, vmId,
                params);
    }

    private void assertThreadMetadata(
            Triple<String, String, PreparedParameter[]> triple,
            DescriptorMetadata data) {
        assertNotNull(data);
        assertEquals(triple.first, data.getAgentId());
        assertEquals(triple.second, data.getVmId());
    }
    
    @Test
    public void canGetMetadataForLatestDeadlockQuery() {
        Triple<String, String, PreparedParameter[]> triple = setupForMetaDataTest(); 
        
        StatementDescriptorMetadataFactory factory = new ThreadDaoImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(ThreadDaoImpl.QUERY_LATEST_DEADLOCK_INFO, triple.third);
        assertThreadMetadata(triple, data);
    }

    @Test
    public void canGetMetadataForLatestHarvestingStatusQuery() {
        Triple<String, String, PreparedParameter[]> triple = setupForMetaDataTest(); 
        
        StatementDescriptorMetadataFactory factory = new ThreadDaoImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(ThreadDaoImpl.QUERY_LATEST_HARVESTING_STATUS, triple.third);
        assertThreadMetadata(triple, data);
    }

    @Test
    public void canGetMetadataForThreadHeader() {
        Triple<String, String, PreparedParameter[]> triple = setupForMetaDataTest();

        StatementDescriptorMetadataFactory factory = new ThreadDaoImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(ThreadDaoImpl.QUERY_THREAD_HEADER, triple.third);
        assertThreadMetadata(triple, data);
    }

    @Test
    public void canGetMetadataAllThreadHeaders() {
        Triple<String, String, PreparedParameter[]> triple = setupForMetaDataTest();

        StatementDescriptorMetadataFactory factory = new ThreadDaoImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(ThreadDaoImpl.QUERY_ALL_THREAD_HEADERS, triple.third);
        assertThreadMetadata(triple, data);
    }

    @Test
    public void canGetMetadataLastThreadStateForThread() {
        PreparedParameter str1 = new PreparedParameter();
        str1.setType(String.class);
        str1.setValue("foo-agent");
        PreparedParameter str2 = new PreparedParameter();
        str2.setType(String.class);
        str2.setValue("something");
        PreparedParameter[] params = new PreparedParameter[] {
                str1, str2
        };
        StatementDescriptorMetadataFactory factory = new ThreadDaoImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(ThreadDaoImpl.QUERY_LATEST_THREAD_STATE_FOR_THREAD, params);
        assertNotNull(data);
        assertEquals("foo-agent", data.getAgentId());
        assertFalse(data.hasVmId());
        assertTrue(data.hasAgentId());
    }
    
    @Test
    public void canGetMetadataFirstThreadStateForThread() {
        PreparedParameter str1 = new PreparedParameter();
        str1.setType(String.class);
        str1.setValue("foo-agent");
        PreparedParameter str2 = new PreparedParameter();
        str2.setType(String.class);
        str2.setValue("something");
        PreparedParameter[] params = new PreparedParameter[] {
                str1, str2
        };
        StatementDescriptorMetadataFactory factory = new ThreadDaoImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(ThreadDaoImpl.QUERY_FIRST_THREAD_STATE_FOR_THREAD, params);
        assertNotNull(data);
        assertEquals("foo-agent", data.getAgentId());
        assertFalse(data.hasVmId());
        assertTrue(data.hasAgentId());
        assertNull(data.getVmId());
    }


    @Test
    public void canGetMetadataOldestThreadState() {
        Triple<String, String, PreparedParameter[]> triple = setupForMetaDataTest();

        StatementDescriptorMetadataFactory factory = new ThreadDaoImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(ThreadDaoImpl.QUERY_OLDEST_THREAD_STATE, triple.third);
        assertThreadMetadata(triple, data);
    }

    @Test
    public void canGetMetadataLatestThreadState() {
        Triple<String, String, PreparedParameter[]> triple = setupForMetaDataTest();

        StatementDescriptorMetadataFactory factory = new ThreadDaoImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(ThreadDaoImpl.QUERY_LATEST_THREAD_STATE, triple.third);
        assertThreadMetadata(triple, data);
    }

    @Test
    public void canGetMetadataThreadStatePerThread() {
        PreparedParameter str1 = new PreparedParameter();
        str1.setType(String.class);
        str1.setValue("foo");
        PreparedParameter long1 = new PreparedParameter();
        long1.setType(long.class);
        long1.setValue(1L);
        PreparedParameter long2 = new PreparedParameter();
        long2.setType(long.class);
        long2.setValue(2L);
        PreparedParameter[] params = new PreparedParameter[] {
                str1, long1, long2
        };
        StatementDescriptorMetadataFactory factory = new ThreadDaoImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(ThreadDaoImpl.QUERY_THREAD_STATE_PER_THREAD, params);
        assertNotNull(data);
        assertNull(data.getAgentId());
        assertNull(data.getVmId());
    }
    
    @Test
    public void canGetMetadataThreadLatestContentionSample() {
        PreparedParameter str1 = new PreparedParameter();
        str1.setType(String.class);
        str1.setValue("foo");
        PreparedParameter[] params = new PreparedParameter[] {
                str1
        };
        StatementDescriptorMetadataFactory factory = new ThreadDaoImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(ThreadDaoImpl.GET_LATEST_CONTENTION_SAMPLE, params);
        assertNotNull(data);
        assertFalse(data.hasAgentId());
        assertFalse(data.hasVmId());
        assertNull(data.getAgentId());
        assertNull(data.getVmId());
    }

    @Test
    public void unknownDescriptorThrowsException() {
        StatementDescriptorMetadataFactory factory = new ThreadDaoImplStatementDescriptorRegistration();
        try {
            factory.getDescriptorMetadata("QUERY foo-bar WHERE 'a' = 'b'", null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }
}

