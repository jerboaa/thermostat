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

package com.redhat.thermostat.agent.proxy.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.proxy.server.AgentProxyControlImpl.VirtualMachineUtils;
import com.sun.tools.attach.VirtualMachine;

public class AgentProxyControlImplTest {
    
    private AgentProxyControlImpl control;
    private VirtualMachine vm;
    private VirtualMachineUtils vmUtils;
    
    @Before
    public void setup() throws Exception {
        vmUtils = mock(VirtualMachineUtils.class);
        vm = mock(VirtualMachine.class);
        
        // Mock VM properties
        Properties agentProps = mock(Properties.class);
        when(agentProps.getProperty("com.sun.management.jmxremote.localConnectorAddress"))
            .thenReturn(null).thenReturn("myJmxUrl");
        when(vm.getAgentProperties()).thenReturn(agentProps);
        Properties sysProps = mock(Properties.class);
        when(sysProps.getProperty("java.home")).thenReturn("/path/to/java/home");
        when(vm.getSystemProperties()).thenReturn(sysProps);
        
        when(vmUtils.attach(anyString())).thenReturn(vm);
        control = new AgentProxyControlImpl(0, vmUtils);
    }
    
    @Test
    public void testAttach() throws Exception {
        control.attach();
        
        verify(vmUtils).attach("0");
        verify(vm, times(2)).getAgentProperties();
        verify(vm).getSystemProperties();
        verify(vm).loadAgent("/path/to/java/home" + File.separator + "lib" + File.separator + "management-agent.jar");
    }

    @Test
    public void testIsAttached() throws Exception {
        assertFalse(control.isAttached());
        control.attach();
        assertTrue(control.isAttached());
    }
    
    @Test
    public void testGetAddress() throws Exception {
        control.attach();
        String addr = control.getConnectorAddress();
        assertEquals("myJmxUrl", addr);
    }
    
    @Test(expected=IOException.class)
    public void testGetAddressNotAttached() throws Exception {
        control.getConnectorAddress();
    }
    
    @Test
    public void testDetach() throws Exception {
        control.attach();
        control.detach();
        verify(vm).detach();
    }
    
    @Test
    public void testDetachNotAttached() throws Exception {
        control.detach();
        verify(vm, never()).detach();
    }
    
}

