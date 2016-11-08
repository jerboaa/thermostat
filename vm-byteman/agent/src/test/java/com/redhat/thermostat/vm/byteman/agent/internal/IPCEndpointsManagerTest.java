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

package com.redhat.thermostat.vm.byteman.agent.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;

public class IPCEndpointsManagerTest {
    
    private static final String SOME_VM_ID = "some-vm-id";
    private static final String SOME_AGENT_ID = "some-agent-id";
    private static final int SOME_VM_PID = 33388;
    private static final VmSocketIdentifier SOME_SOCK_ID = new VmSocketIdentifier(SOME_VM_ID, SOME_VM_PID, SOME_AGENT_ID);
    
    private IPCEndpointsManager ipcManager;
    private AgentIPCService ipcService;
    private VmBytemanDAO vmBytemanDao;
    private UserPrincipal owner;
    
    @Before
    public void setup() {
        vmBytemanDao = mock(VmBytemanDAO.class);
        ipcService = mock(AgentIPCService.class);
        owner = mock(UserPrincipal.class);
        ipcManager = new IPCEndpointsManager(ipcService);
    }

    @Test
    public void canStartIPCEndpoint() throws IOException {
        String name = doStartTest();
        assertTrue(ipcManager.isSocketTracked(name));
    }

    private String doStartTest() throws IOException {
        ThermostatIPCCallbacks callback = new BytemanMetricsReceiver(vmBytemanDao, SOME_SOCK_ID);
        ipcManager.startIPCEndpoint(SOME_SOCK_ID, callback, owner);
        String name = SOME_SOCK_ID.getName();
        verify(ipcService).createServer(eq(name), isA(BytemanMetricsReceiver.class), eq(owner));
        verify(ipcService).serverExists(name);
        return name;
    }
    
    @Test
    public void startStopIPCServiceCycle() throws IOException {
        String name = doStartTest();
        assertTrue(ipcManager.isSocketTracked(name));
        
        ipcManager.stopIPCEndpoint(SOME_SOCK_ID);
        verify(ipcService).destroyServer(eq(name));
        assertFalse(ipcManager.isSocketTracked(name));
    }
    
    @Test
    public void notStartedButStoppedIsNoOp() {
        String name = SOME_SOCK_ID.getName();
        assertFalse(ipcManager.isSocketTracked(name));
        
        ipcManager.stopIPCEndpoint(SOME_SOCK_ID);
        verifyNoMoreInteractions(ipcService);
        assertFalse(ipcManager.isSocketTracked(name));
    }
    
    @Test
    public void duplicateStartIsNoOpSecondTime() throws IOException {
        String name = doStartTest();
        assertTrue(ipcManager.isSocketTracked(name));
        
        // start it again, which should be a no-op
        ThermostatIPCCallbacks callback = new BytemanMetricsReceiver(vmBytemanDao, SOME_SOCK_ID);
        ipcManager.startIPCEndpoint(SOME_SOCK_ID, callback, owner);
        verifyNoMoreInteractions(ipcService);
    }
}
