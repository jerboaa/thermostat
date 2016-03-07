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

package com.redhat.thermostat.utils.management.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.Charset;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionException;
import com.redhat.thermostat.utils.management.internal.MXBeanConnectionPoolImpl.ConnectorCreator;
import com.redhat.thermostat.utils.management.internal.ProcessUserInfoBuilder.ProcessUserInfo;

public class MXBeanConnectionPoolImplTest {

    private File binDir;
    private AgentIPCService ipcService;
    private AgentProxyClient proxy;
    private MXBeanConnectionPoolImpl pool;
    private MXBeanConnectionImpl connection;
    private ConnectorCreator creator;
    private MXBeanConnector connector;
    private File ipcConfigFile;
    
    @Before
    public void setup() throws Exception {
        binDir = mock(File.class);
        ipcService = mock(AgentIPCService.class);
        connection = mock(MXBeanConnectionImpl.class);
        connector = mock(MXBeanConnector.class);
        creator = mock(ConnectorCreator.class);
        ipcConfigFile = mock(File.class);

        proxy = mock(AgentProxyClient.class);
        when(creator.createConnector(any(String.class))).thenReturn(connector);
        when(connector.connect()).thenReturn(connection);

        ProcessUserInfoBuilder builder = mock(ProcessUserInfoBuilder.class);
        ProcessUserInfo info = new ProcessUserInfo(8000, "Test");
        when(builder.build(8000)).thenReturn(info);

        pool = new MXBeanConnectionPoolImpl(creator, binDir, builder, ipcService, ipcConfigFile);
    }

    @Test
    public void testAcquire() throws Exception {
        final byte[] data = getJsonString(8000, "jmxUrl://hello");
        invokeCallbacksOnProxyCreation(data);

        MXBeanConnection result = pool.acquire(8000);
        
        verify(creator).createConnector("jmxUrl://hello");

        assertNotNull(result);
        assertEquals(result, connection);

        verify(connector).connect();
    }

    private void invokeCallbacksOnProxyCreation(final byte[] data) {
        when(creator.createAgentProxy(8000, "Test", binDir, ipcConfigFile)).thenAnswer(new Answer<AgentProxyClient>() {
            @Override
            public AgentProxyClient answer(InvocationOnMock invocation) throws Throwable {
                // Invoke callback
                pool.dataReceived(data);
                return proxy;
            }
        });
    }
    
    @Test
    public void testAcquireNoPid() throws Exception {
        final byte[] data = getJsonString(null, "jmxUrl://hello");
        invokeCallbacksOnProxyCreation(data);
        try {
            pool.acquire(8000);
            fail("Expected MXBeanConnectionException");
        } catch (MXBeanConnectionException e) {
            verify(creator, never()).createConnector("jmxUrl://hello");
            verify(connector, never()).connect();
        }
    }
    
    @Test
    public void testAcquireBadPid() throws Exception {
        final byte[] data = getJsonString(9000, "jmxUrl://hello");
        invokeCallbacksOnProxyCreation(data);
        try {
            pool.acquire(8000);
            fail("Expected MXBeanConnectionException");
        } catch (MXBeanConnectionException e) {
            verify(creator, never()).createConnector("jmxUrl://hello");
            verify(connector, never()).connect();
        }
    }
    
    @Test
    public void testAcquireNullPid() throws Exception {
        final byte[] data = getJsonString(null, "jmxUrl://hello", true);
        invokeCallbacksOnProxyCreation(data);
        try {
            pool.acquire(8000);
            fail("Expected MXBeanConnectionException");
        } catch (MXBeanConnectionException e) {
            verify(creator, never()).createConnector("jmxUrl://hello");
            verify(connector, never()).connect();
        }
    }
    
    @Test
    public void testAcquireNoJmxUrl() throws Exception {
        final byte[] data = getJsonString(8000, null);
        invokeCallbacksOnProxyCreation(data);
        try {
            pool.acquire(8000);
            fail("Expected MXBeanConnectionException");
        } catch (MXBeanConnectionException e) {
            verify(creator, never()).createConnector("jmxUrl://hello");
            verify(connector, never()).connect();
        }
    }
    
    @Test
    public void testAcquireNullJmxUrl() throws Exception {
        final byte[] data = getJsonString(8000, null, true);
        invokeCallbacksOnProxyCreation(data);
        try {
            pool.acquire(8000);
            fail("Expected MXBeanConnectionException");
        } catch (MXBeanConnectionException e) {
            verify(creator, never()).createConnector("jmxUrl://hello");
            verify(connector, never()).connect();
        }
    }
    
    @Test
    public void testAcquireBadCast() throws Exception {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        JsonObject jsonData = new JsonObject();
        jsonData.addProperty(MXBeanConnectionPoolImpl.JSON_PID, "this is not an integer");
        jsonData.addProperty(MXBeanConnectionPoolImpl.JSON_JMX_URL, "jmxUrl://hello");
        
        String jsonString = gson.toJson(jsonData);
        final byte[] data = jsonString.getBytes(Charset.forName("UTF-8"));
        
        invokeCallbacksOnProxyCreation(data);
        try {
            pool.acquire(8000);
            fail("Expected MXBeanConnectionException");
        } catch (MXBeanConnectionException e) {
            verify(creator, never()).createConnector("jmxUrl://hello");
            verify(connector, never()).connect();
        }
    }
    
    @Test
    public void testAcquireBadJson() throws Exception {
        String jsonString = "not a json string";
        final byte[] data = jsonString.getBytes(Charset.forName("UTF-8"));
        
        invokeCallbacksOnProxyCreation(data);
        try {
            pool.acquire(8000);
            fail("Expected MXBeanConnectionException");
        } catch (MXBeanConnectionException e) {
            verify(creator, never()).createConnector("jmxUrl://hello");
            verify(connector, never()).connect();
        }
    }

    @Test
    public void testAcquireTwice() throws Exception {
        byte[] data = getJsonString(8000, "jmxUrl://hello");
        invokeCallbacksOnProxyCreation(data);
    
        MXBeanConnection connection1 = pool.acquire(8000);
    
        verify(connector).connect();
    
        MXBeanConnection connection2 = pool.acquire(8000);
    
        assertEquals(connection1, connection);
        assertEquals(connection2, connection);
    
        verifyNoMoreInteractions(connector);
    }

    @Test
    public void testRelease() throws Exception {
        byte[] data = getJsonString(8000, "jmxUrl://hello");
        invokeCallbacksOnProxyCreation(data);
    
        MXBeanConnection result = pool.acquire(8000);
    
        verify(connection, never()).close();
    
        pool.release(8000, result);
    
        verify(connection).close();
    }

    @Test
    public void testReleaseTwice() throws Exception {
        byte[] data = getJsonString(8000, "jmxUrl://hello");
        invokeCallbacksOnProxyCreation(data);
    
        // connection1 == connection1 == actualConnection
        MXBeanConnection connection1 = pool.acquire(8000);
        MXBeanConnection connection2 = pool.acquire(8000);
    
        pool.release(8000, connection1);
    
        verify(connection, never()).close();
    
        pool.release(8000, connection2);
    
        verify(connection).close();
    
    }

    private byte[] getJsonString(Integer pid, String jmxUrl) {
        return getJsonString(pid, jmxUrl, false);
    }
    
    private byte[] getJsonString(Integer pid, String jmxUrl, boolean serializeNulls) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        if (serializeNulls) {
            gsonBuilder.serializeNulls();
        }
        Gson gson = gsonBuilder.create();
        JsonObject jsonData = new JsonObject();

        jsonData.addProperty(MXBeanConnectionPoolImpl.JSON_PID, pid);
        jsonData.addProperty(MXBeanConnectionPoolImpl.JSON_JMX_URL, jmxUrl);

        String jsonString = gson.toJson(jsonData);
        return jsonString.getBytes(Charset.forName("UTF-8"));
    }

}

