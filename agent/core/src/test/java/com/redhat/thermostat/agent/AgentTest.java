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

package com.redhat.thermostat.agent;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.config.AgentStartupConfiguration;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.BackendInfoDAO;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.model.AgentInformation;
import com.redhat.thermostat.common.model.BackendInformation;
import com.redhat.thermostat.common.storage.Storage;

public class AgentTest {

    private AgentStartupConfiguration config;
    private BackendRegistry backendRegistry;
    private Backend backend;

    private Storage storage;
    private DAOFactory daos;
    private AgentInfoDAO agentInfoDao;
    private BackendInfoDAO backendInfoDao;
    
    @Before
    public void setUp() {
        config = mock(AgentStartupConfiguration.class);
        when(config.getStartTime()).thenReturn(123L);
        when(config.purge()).thenReturn(true);
        
        storage = mock(Storage.class);
        agentInfoDao = mock(AgentInfoDAO.class);
        backendInfoDao = mock(BackendInfoDAO.class);
        daos = mock(DAOFactory.class);
        when(daos.getStorage()).thenReturn(storage);
        
        backend = mock(Backend.class);
        when(backend.getName()).thenReturn("testname");
        when(backend.getDescription()).thenReturn("testdesc");
        when(backend.getObserveNewJvm()).thenReturn(true);
        when(backend.activate()).thenReturn(true); // TODO: activate() should not return anything and throw exception in error case.
        Collection<Backend> backends = new ArrayList<Backend>();
        backends.add(backend);
        
        backendRegistry = mock(BackendRegistry.class);
        when(backendRegistry.getAll()).thenReturn(backends);
    }
    
    @Test
    public void testStartAgent() throws Exception {
        
        // Start agent.
        Agent agent = new Agent(backendRegistry, config, daos, agentInfoDao, backendInfoDao);
        agent.start();

        // Verify that backend has been activated and storage received the agent information.
        verify(backend).activate();
        ArgumentCaptor<AgentInformation> argument = ArgumentCaptor.forClass(AgentInformation.class);

        verify(agentInfoDao).addAgentInformation(argument.capture());
        assertEquals(123, argument.getValue().getStartTime());

        ArgumentCaptor<BackendInformation> backendsArg = ArgumentCaptor.forClass(BackendInformation.class);
        verify(backendInfoDao).addBackendInformation(backendsArg.capture());

        List<BackendInformation> backendInfos = backendsArg.getAllValues();
        assertEquals(1, backendInfos.size());
        BackendInformation backend0 = backendInfos.get(0);
        assertEquals("testname", backend0.getName());
        assertEquals("testdesc", backend0.getDescription());
        assertEquals(true, backend0.isObserveNewJvm());
        assertEquals(true, backend0.isActive());
        // TODO: We should probably also test getPIDs() and getConfiguration(), but it's not clear to me at this point
        // what those should really do (and it looks like they're not implemented yet).
    }
    
    @Test
    public void testStopAgentWithPurging() throws Exception {
        Agent agent = new Agent(backendRegistry, config, daos, agentInfoDao, backendInfoDao);
        agent.start();
        
        // stop agent
        agent.stop();
        
        verify(backend).deactivate();

        ArgumentCaptor<AgentInformation> argument = ArgumentCaptor.forClass(AgentInformation.class);        
        verify(agentInfoDao, never()).updateAgentInformation(argument.capture());
        verify(storage, times(1)).purge();
    }
    
    @Test
    public void testStopAgentWithoutPurging() throws Exception {
        
        AgentStartupConfiguration config = mock(AgentStartupConfiguration.class);
        when(config.getStartTime()).thenReturn(123L);
        when(config.purge()).thenReturn(false);
        
        Agent agent = new Agent(backendRegistry, config, daos, agentInfoDao, backendInfoDao);
        agent.start();
        
        // stop agent
        agent.stop();
        
        verify(backend).deactivate();

        verify(agentInfoDao).updateAgentInformation(isA(AgentInformation.class));
        verify(backendInfoDao, atLeast(1)).removeBackendInformation(isA(BackendInformation.class));
        verify(storage, times(0)).purge();
    }
}
