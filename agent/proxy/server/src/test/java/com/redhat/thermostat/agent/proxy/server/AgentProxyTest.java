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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.redhat.thermostat.agent.ipc.client.ClientIPCService;
import com.redhat.thermostat.agent.proxy.server.AgentProxy.ControlCreator;

public class AgentProxyTest {
    
    private static final String JMX_URL = "service:jmx:rmi://myHost:1099/blah";
    
    private AgentProxyControlImpl control;
    private ClientIPCService ipcService;
    private ByteChannel channel;

    @Before
    public void setup() throws Exception {
        System.setProperty(AgentProxy.CONFIG_FILE_PROP, "/path/to/config/file");
        ControlCreator creator = mock(ControlCreator.class);
        control = mock(AgentProxyControlImpl.class);
        when(control.getConnectorAddress()).thenReturn(JMX_URL);
        when(control.isAttached()).thenReturn(true);
        when(creator.create(anyInt())).thenReturn(control);
        AgentProxy.setControlCreator(creator);
        
        ipcService = mock(ClientIPCService.class);
        channel = mock(ByteChannel.class);
        when(ipcService.connectToServer(AgentProxy.IPC_SERVER_NAME)).thenReturn(channel);
        AgentProxy.setIPCService(ipcService);
    }
    
    @After
    public void teardown() throws Exception {
        System.clearProperty(AgentProxy.CONFIG_FILE_PROP);
        AgentProxy.setControlCreator(new ControlCreator());
        AgentProxy.setIPCService(null);
    }
    
    @Test
    public void testMainSuccess() throws Exception {
        // Invoke main with PID of 8000
        AgentProxy.main(new String[] { "8000" });
        
        verify(control).attach();
        verify(control).getConnectorAddress();
        
        // Create a buffer with the expected data
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        JsonObject data = new JsonObject();
        data.addProperty(AgentProxy.JSON_PID, 8000);
        data.addProperty(AgentProxy.JSON_JMX_URL, JMX_URL);
        String jsonData = gson.toJson(data);
        ByteBuffer jsonBuf = ByteBuffer.wrap(jsonData.getBytes("UTF-8"));
        
        verify(channel).write(eq(jsonBuf));
        verify(channel).close();
        verify(control).detach();
    }
    
    @Test
    public void testMainAttachFails() throws Exception {
        // Simulate failure binding the login object
        doThrow(new IOException()).when(control).attach();
        when(control.isAttached()).thenReturn(false);
        
        try {
            // Invoke main with PID of 0
            AgentProxy.main(new String[] { "0" });
            fail("Expected IOException");
        } catch (IOException e) {
            // Should only call attach and close channel
            verify(control).attach();
            verify(control, never()).getConnectorAddress();
            
            verify(channel, never()).write(any(ByteBuffer.class));
            verify(channel).close();
            
            verify(control, never()).detach();
        }
    }
    
    @Test
    public void testMainGetAddressFails() throws Exception {
        // Simulate failure binding the login object
        doThrow(new IOException()).when(control).getConnectorAddress();
        
        try {
            // Invoke main with PID of 0
            AgentProxy.main(new String[] { "0" });
            fail("Expected IOException");
        } catch (IOException e) {
            verify(control).attach();
            verify(control).getConnectorAddress();

            // Should detach and close channel, but not send URL
            verify(channel, never()).write(any(ByteBuffer.class));
            verify(channel).close();
            
            verify(control).detach();
        }
    }
    
    @Test
    public void testMainSendAddressFails() throws Exception {
        // Simulate failure binding the login object
        doThrow(new IOException()).when(channel).write(any(ByteBuffer.class));
        
        try {
            // Invoke main with PID of 0
            AgentProxy.main(new String[] { "0" });
            fail("Expected IOException");
        } catch (IOException e) {
            verify(control).attach();
            verify(control).getConnectorAddress();
            
            // Should still detach and close channel
            verify(channel).write(any(ByteBuffer.class));
            verify(channel).close();
            
            verify(control).detach();
        }
    }
    
    @Test
    public void testMainDetachFails() throws Exception {
        // Simulate failure binding the login object
        doThrow(new IOException()).when(control).detach();
        
        // Invoke main with PID of 0
        AgentProxy.main(new String[] { "0" });
        
        // All should be called, should not be fatal
        verify(control).attach();
        verify(control).getConnectorAddress();

        verify(channel).write(any(ByteBuffer.class));
        verify(channel).close();

        verify(control).detach();
    }
    
    @Test
    public void testMainCloseFails() throws Exception {
        // Simulate failure binding the login object
        doThrow(new IOException()).when(channel).close();
        
        // Invoke main with PID of 0
        AgentProxy.main(new String[] { "0" });
        
        // All should be called, should not be fatal
        verify(control).attach();
        verify(control).getConnectorAddress();

        verify(channel).write(any(ByteBuffer.class));
        verify(channel).close();

        verify(control).detach();
    }

}

