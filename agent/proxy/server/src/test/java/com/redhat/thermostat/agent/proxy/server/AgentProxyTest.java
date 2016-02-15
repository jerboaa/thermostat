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

package com.redhat.thermostat.agent.proxy.server;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.proxy.server.AgentProxy.ControlCreator;

public class AgentProxyTest {
    
    private static final String JMX_URL = "service:jmx:rmi://myHost:1099/blah";
    
    private AgentProxyControlImpl control;
    private PrintStream outStream;

    @Before
    public void setup() throws Exception {
        ControlCreator creator = mock(ControlCreator.class);
        control = mock(AgentProxyControlImpl.class);
        when(control.getConnectorAddress()).thenReturn(JMX_URL);
        outStream = mock(PrintStream.class);
        when(creator.create(0)).thenReturn(control);
        AgentProxy.setControlCreator(creator);
        AgentProxy.setOutStream(outStream);
    }
    
    @After
    public void teardown() throws Exception {
        AgentProxy.setControlCreator(new ControlCreator());
        AgentProxy.setOutStream(System.out);
    }
    
    @Test
    public void testMainSuccess() throws Exception {
        // Invoke main with PID of 0
        AgentProxy.main(new String[] { "0" });
        
        verify(control).attach();
        verify(control).getConnectorAddress();
        verify(control).detach();
        verify(outStream).println(JMX_URL);
    }
    
    @Test
    public void testMainAttachFails() throws Exception {
        // Simulate failure binding the login object
        doThrow(new IOException()).when(control).attach();
        
        // Invoke main with PID of 0
        AgentProxy.main(new String[] { "0" });
        
        verify(control).attach();
        verify(control, never()).getConnectorAddress();
        verify(control, never()).detach();
        verify(outStream, never()).println(JMX_URL);
    }
    
    @Test
    public void testMainGetAddressFails() throws Exception {
        // Simulate failure binding the login object
        doThrow(new IOException()).when(control).getConnectorAddress();
        
        // Invoke main with PID of 0
        AgentProxy.main(new String[] { "0" });
        
        verify(control).attach();
        verify(control).getConnectorAddress();
        
        // Should detach, but not print URL
        verify(control).detach();
        verify(outStream, never()).println(JMX_URL);
    }
    
    @Test
    public void testMainDetachFails() throws Exception {
        // Simulate failure binding the login object
        doThrow(new IOException()).when(control).detach();
        
        // Invoke main with PID of 0
        AgentProxy.main(new String[] { "0" });
        
        // All should be called
        verify(control).attach();
        verify(control).getConnectorAddress();
        verify(control).detach();
        verify(outStream).println(JMX_URL);
    }

}

