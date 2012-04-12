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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.config.AgentStartupConfiguration;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.storage.AgentInformation;
import com.redhat.thermostat.common.storage.BackendInformation;
import com.redhat.thermostat.common.storage.Storage;

public class AgentTest {

    @Test
    public void testStartAgent() throws Exception {
        // Setup class under test and test data (config, backendRegistry).
        AgentStartupConfiguration config = mock(AgentStartupConfiguration.class);
        when(config.getStartTime()).thenReturn(123L);

        Storage storage = mock(Storage.class);
        DAOFactory daos = mock(DAOFactory.class);
        when(daos.getStorage()).thenReturn(storage);

        Backend backend = mock(Backend.class);
        when(backend.getName()).thenReturn("testname");
        when(backend.getDescription()).thenReturn("testdesc");
        when(backend.getObserveNewJvm()).thenReturn(true);
        when(backend.activate()).thenReturn(true); // TODO: activate() should not return anything and throw exception in error case.
        Collection<Backend> backends = new ArrayList<Backend>();
        backends.add(backend);

        BackendRegistry backendRegistry = mock(BackendRegistry.class);
        when(backendRegistry.getAll()).thenReturn(backends);

        // Start agent.
        Agent agent = new Agent(backendRegistry, config, daos);
        agent.start();

        // Verify that backend has been activated and storage received the agent information.
        verify(backend).activate();
        ArgumentCaptor<AgentInformation> argument = ArgumentCaptor.forClass(AgentInformation.class);
        verify(storage).addAgentInformation(argument.capture());
        assertEquals(123, argument.getValue().getStartTime());
        List<BackendInformation> backendInfos = argument.getValue().getBackends();
        assertEquals(1, backendInfos.size());
        BackendInformation backend0 = backendInfos.get(0);
        assertEquals("testname", backend0.getName());
        assertEquals("testdesc", backend0.getDescription());
        assertEquals(true, backend0.isObserveNewJvm());
        // TODO: We should probably also test getPIDs() and getConfiguration(), but it's not clear to me at this point
        // what those should really do (and it looks like they're not implemented yet).
    }
}
