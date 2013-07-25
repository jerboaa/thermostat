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

package com.redhat.thermostat.storage.internal.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.junit.Test;

import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorMetadataFactory;
import com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration;

public class DAOImplStatementDescriptorRegistrationTest {

    @Test
    public void registersAllQueries() {
        DAOImplStatementDescriptorRegistration reg = new DAOImplStatementDescriptorRegistration();
        Set<String> descriptors = reg.getStatementDescriptors();
        assertEquals(9, descriptors.size());
        assertFalse(descriptors.contains(null));
    }
    
    /*
     * The web storage end-point uses service loader in order to determine the
     * list of trusted/known registrations. This test is to ensure service loading
     * works for this module's regs. E.g. renaming of the impl class without
     * changing META-INF/services/com.redhat.thermostat.storage.core.auth.StatementDescriptorRegistration
     */
    @Test
    public void serviceLoaderCanLoadRegistration() {
        ServiceLoader<StatementDescriptorRegistration> loader = ServiceLoader.load(StatementDescriptorRegistration.class, DAOImplStatementDescriptorRegistration.class.getClassLoader());
        List<StatementDescriptorRegistration> registrations = new ArrayList<>(1);
        for (StatementDescriptorRegistration r: loader) {
            registrations.add(r);
        }
        assertEquals(1, registrations.size());
        assertEquals(9, registrations.get(0).getStatementDescriptors().size());
    }
    
    @Test
    public void canGetMetadataForAgentAliveQuery() {
        StatementDescriptorMetadataFactory factory = new DAOImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(AgentInfoDAOImpl.QUERY_ALIVE_AGENTS, null);
        assertNotNull(data);
        assertFalse(data.hasAgentId());
        assertFalse(data.hasVmId());
    }
    
    @Test
    public void canGetMetadataForAgentInfoQuery() {
        PreparedParameter agentIdParam = mock(PreparedParameter.class);
        String agentId = "agentId";
        when(agentIdParam.getValue()).thenReturn(agentId);
        PreparedParameter[] params = new PreparedParameter[] {
                agentIdParam
        };
        
        StatementDescriptorMetadataFactory factory = new DAOImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(AgentInfoDAOImpl.QUERY_AGENT_INFO, params);
        assertNotNull(data);
        assertFalse(data.hasVmId());
        assertEquals(agentId, data.getAgentId());
    }
    
    @Test
    public void canGetMetadataForAllAgentsQuery() {
        StatementDescriptorMetadataFactory factory = new DAOImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(AgentInfoDAOImpl.QUERY_ALL_AGENTS, null);
        assertNotNull(data);
        assertFalse(data.hasAgentId());
        assertFalse(data.hasVmId());
    }
    
    @Test
    public void canGetMetadataForBackendInfoQuery() {
        PreparedParameter agentIdParam = mock(PreparedParameter.class);
        String agentId = "agentId";
        when(agentIdParam.getValue()).thenReturn(agentId);
        PreparedParameter[] params = new PreparedParameter[] {
                agentIdParam
        };
        
        StatementDescriptorMetadataFactory factory = new DAOImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(BackendInfoDAOImpl.QUERY_BACKEND_INFO, params);
        assertNotNull(data);
        assertTrue(data.hasAgentId());
        assertFalse(data.hasVmId());
        assertEquals(agentId, data.getAgentId());
    }
    
    @Test
    public void canGetMetadataForHostInfoQuery() {
        PreparedParameter agentIdParam = mock(PreparedParameter.class);
        String agentId = "agentId";
        when(agentIdParam.getValue()).thenReturn(agentId);
        PreparedParameter[] params = new PreparedParameter[] {
                agentIdParam
        };
        
        StatementDescriptorMetadataFactory factory = new DAOImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(HostInfoDAOImpl.QUERY_HOST_INFO, params);
        assertNotNull(data);
        assertTrue(data.hasAgentId());
        assertFalse(data.hasVmId());
        assertEquals(agentId, data.getAgentId());
    }
    
    @Test
    public void canGetMetadataForAllHostsQuery() {
        StatementDescriptorMetadataFactory factory = new DAOImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(HostInfoDAOImpl.QUERY_ALL_HOSTS, null);
        assertNotNull(data);
        assertFalse(data.hasAgentId());
        assertFalse(data.hasVmId());
    }
    
    @Test
    public void canGetMetadataForNetworkInfoQuery() {
        PreparedParameter agentIdParam = mock(PreparedParameter.class);
        String agentId = "agentId";
        when(agentIdParam.getValue()).thenReturn(agentId);
        PreparedParameter[] params = new PreparedParameter[] {
                agentIdParam
        };
        
        StatementDescriptorMetadataFactory factory = new DAOImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(NetworkInterfaceInfoDAOImpl.QUERY_NETWORK_INFO, params);
        assertNotNull(data);
        assertTrue(data.hasAgentId());
        assertFalse(data.hasVmId());
        assertEquals(agentId, data.getAgentId());
    }
    
    @Test
    public void canGetMetadataForVmInfoAllQuery() {
        PreparedParameter agentIdParam = mock(PreparedParameter.class);
        String agentId = "agentId";
        when(agentIdParam.getValue()).thenReturn(agentId);
        PreparedParameter[] params = new PreparedParameter[] {
                agentIdParam
        };
        
        StatementDescriptorMetadataFactory factory = new DAOImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(VmInfoDAOImpl.QUERY_ALL_VMS, params);
        assertNotNull(data);
        assertTrue(data.hasAgentId());
        assertFalse(data.hasVmId());
        assertEquals(agentId, data.getAgentId());
    }
    
    @Test
    public void canGetMetadataForSpecificVmInfoQuery() {
        PreparedParameter agentIdParam = mock(PreparedParameter.class);
        PreparedParameter vmIdParam = mock(PreparedParameter.class);
        String agentId = "agentId";
        String vmId = "vmId";
        when(agentIdParam.getValue()).thenReturn(agentId);
        when(vmIdParam.getValue()).thenReturn(vmId);
        PreparedParameter[] params = new PreparedParameter[] {
                agentIdParam, vmIdParam
        };
        
        StatementDescriptorMetadataFactory factory = new DAOImplStatementDescriptorRegistration();
        DescriptorMetadata data = factory.getDescriptorMetadata(VmInfoDAOImpl.QUERY_VM_INFO, params);
        assertNotNull(data);
        assertTrue(data.hasVmId());
        assertTrue(data.hasAgentId());
        assertEquals(agentId, data.getAgentId());
        assertEquals(vmId, data.getVmId());
    }
    
    @Test
    public void unknownDescriptorThrowsException() {
        StatementDescriptorMetadataFactory factory = new DAOImplStatementDescriptorRegistration();
        try {
            factory.getDescriptorMetadata("QUERY foo-bar WHERE 'a' = 'b'", null);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }
}
