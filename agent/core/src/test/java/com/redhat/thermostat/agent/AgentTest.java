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

package com.redhat.thermostat.agent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.agent.config.AgentStartupConfiguration;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.BackendInformation;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.utils.management.internal.MXBeanConnectionPoolImpl;
import com.redhat.thermostat.utils.management.internal.MXBeanConnectionPoolTracker;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class AgentTest {

    private AgentStartupConfiguration config;
    private BackendRegistry backendRegistry;
    private Backend backend;

    private Storage storage;
    private AgentInfoDAO agentInfoDao;
    private BackendInfoDAO backendInfoDao;
    private MXBeanConnectionPoolImpl pool;
    private MXBeanConnectionPoolTracker poolTracker;
    
    @Before
    public void setUp() throws Exception {
        config = mock(AgentStartupConfiguration.class);
        when(config.getStartTime()).thenReturn(123L);
        when(config.purge()).thenReturn(true);
        
        storage = mock(Storage.class);
        agentInfoDao = mock(AgentInfoDAO.class);
        backendInfoDao = mock(BackendInfoDAO.class);
        
        backend = mock(Backend.class);
        when(backend.getName()).thenReturn("testname");
        when(backend.getDescription()).thenReturn("testdesc");
        when(backend.getObserveNewJvm()).thenReturn(true);
        when(backend.activate()).thenReturn(true); // TODO: activate() should not return anything and throw exception in error case.
        when(backend.isActive()).thenReturn(true);

        backendRegistry = mock(BackendRegistry.class);
        pool = mock(MXBeanConnectionPoolImpl.class);
        when(pool.isStarted()).thenReturn(true);
        poolTracker = mock(MXBeanConnectionPoolTracker.class);
        when(poolTracker.getPoolWithTimeout()).thenReturn(pool);
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testAgentInit() throws Exception {
        Agent agent = new Agent(backendRegistry, config, storage, agentInfoDao, backendInfoDao, null, poolTracker);
        
        verify(backendRegistry).addActionListener(any(ActionListener.class));
    }
    
    @Test
    public void testStartAgent() throws Exception {
        
        // Start agent.
        UUID uuid = UUID.randomUUID();
        WriterID id = mock(WriterID.class);
        when(id.getWriterID()).thenReturn(uuid.toString());
        Agent agent = new Agent(backendRegistry, config, storage, agentInfoDao, backendInfoDao, id, poolTracker);
        
        agent.start();

        // Verify that backend registry is started
        verify(backendRegistry).start();
        ArgumentCaptor<AgentInformation> argument = ArgumentCaptor.forClass(AgentInformation.class);

        verify(agentInfoDao).addAgentInformation(argument.capture());
        assertEquals(123, argument.getValue().getStartTime());
        assertEquals(uuid.toString(), argument.getValue().getAgentId());
        
        verify(pool).start();
    }
    
    @Test
    public void testServiceAddedRemovedFromOSGi() throws Exception {
        ArgumentCaptor<ActionListener> backendListener = ArgumentCaptor.forClass(ActionListener.class);

        // Start agent.
        WriterID id = mock(WriterID.class);
        Agent agent = new Agent(backendRegistry, config, storage, agentInfoDao, backendInfoDao, id, poolTracker);
        verify(backendRegistry).addActionListener(backendListener.capture());
        
        agent.start();

        // Verify that backend registry is started
        verify(backendRegistry).start();
        ArgumentCaptor<AgentInformation> argument = ArgumentCaptor.forClass(AgentInformation.class);

        verify(agentInfoDao).addAgentInformation(argument.capture());
        assertEquals(123, argument.getValue().getStartTime());

        ActionListener listener = backendListener.getValue();
        
        // add a fake backend to see if it's registered corectly
        ActionEvent<ThermostatExtensionRegistry.Action> actionEvent =
                new ActionEvent<>(this, ThermostatExtensionRegistry.Action.SERVICE_ADDED);
        actionEvent.setPayload(backend);
        
        listener.actionPerformed(actionEvent);
        
        verify(backend).activate();
        
        assertTrue(agent.getBackendInfos().containsKey(backend));
        BackendInformation info = agent.getBackendInfos().get(backend);
        assertEquals("testname", info.getName());
        assertEquals("testdesc", info.getDescription());
        assertEquals(true, info.isObserveNewJvm());
        assertEquals(true, info.isActive());
        
        verify(backendInfoDao).addBackendInformation(info);

        actionEvent = new ActionEvent<>(this, ThermostatExtensionRegistry.Action.SERVICE_REMOVED);
        actionEvent.setPayload(backend);
        listener.actionPerformed(actionEvent);
        
        verify(backend).deactivate();

        assertFalse(agent.getBackendInfos().containsKey(backend));
        verify(backendInfoDao).removeBackendInformation(info);
    }
    
    @Test
    public void testStopAgentWithPurging() throws Exception {
                
        UUID uuid = UUID.randomUUID();
        WriterID id = mock(WriterID.class);
        when(id.getWriterID()).thenReturn(uuid.toString());
        Agent agent = new Agent(backendRegistry, config, storage, agentInfoDao, backendInfoDao, id, poolTracker);
        agent.start();
        
        // stop agent
        agent.stop();
        
        verify(backendRegistry).stop();

        ArgumentCaptor<AgentInformation> argument = ArgumentCaptor.forClass(AgentInformation.class);        
        verify(agentInfoDao, never()).updateAgentInformation(argument.capture());
        verify(storage, times(1)).purge(uuid.toString());
        
        verify(pool).shutdown();
    }
   
    @Test
    public void testStopAgentWithoutPurging() throws Exception {
        
        AgentStartupConfiguration config = mock(AgentStartupConfiguration.class);
        when(config.getStartTime()).thenReturn(123L);
        when(config.purge()).thenReturn(false);
        
        WriterID id = mock(WriterID.class);
        Agent agent = new Agent(backendRegistry, config, storage, agentInfoDao, backendInfoDao, id, poolTracker);
        agent.start();
        
        // stop agent
        agent.stop();
        
        verify(backendRegistry).stop();

        verify(agentInfoDao).updateAgentInformation(isA(AgentInformation.class));
        verify(storage, times(0)).purge(anyString());
        
        verify(pool).shutdown();
    }
}

