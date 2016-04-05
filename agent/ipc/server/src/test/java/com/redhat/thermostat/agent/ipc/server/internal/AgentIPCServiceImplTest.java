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

package com.redhat.thermostat.agent.ipc.server.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.server.ServerTransport;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.server.internal.AgentIPCServiceImpl.FileHelper;
import com.redhat.thermostat.testutils.StubBundleContext;

public class AgentIPCServiceImplTest {

    private static final String SERVER_NAME = "test";
    
    private ServerTransport transport;
    private IPCProperties props;
    private FileHelper helper;
    private IPCConfigurationWriter writer;
    private File propFile;
    private ServerIPCPropertiesBuilder propBuilder;
    private StubBundleContext context;
    
    @Before
    public void setUp() throws Exception {
        context = new StubBundleContext();
        transport = mock(ServerTransport.class);
        when(transport.getType()).thenReturn(IPCType.UNKNOWN);
        propFile = mock(File.class);
        propBuilder = mock(ServerIPCPropertiesBuilder.class);
        props = mock(IPCProperties.class);
        when(props.getType()).thenReturn(IPCType.UNKNOWN);
        when(propBuilder.getProperties(propFile)).thenReturn(props);
        helper = mock(FileHelper.class);
        writer = mock(IPCConfigurationWriter.class);
        when(helper.getConfigurationWriter(propFile)).thenReturn(writer);
    }

    @Test
    public void testCreateServer() throws Exception {
        context.registerService(ServerTransport.class.getName(), transport, null);
        AgentIPCServiceImpl service = createService();
        ThermostatIPCCallbacks callbacks = mock(ThermostatIPCCallbacks.class);
        
        assertFalse(service.isStarted());
        service.createServer(SERVER_NAME, callbacks);
        assertTrue(service.isStarted());
        
        verify(transport).start(props);
        verify(writer).write();
        verify(transport).createServer(SERVER_NAME, callbacks);
    }

    private AgentIPCServiceImpl createService() {
        return new AgentIPCServiceImpl(propBuilder, context, propFile, helper);
    }
    
    @Test
    public void testCreateServerNoTransport() throws Exception {
        AgentIPCServiceImpl service = createService();
        ThermostatIPCCallbacks callbacks = mock(ThermostatIPCCallbacks.class);
        
        try {
            service.createServer(SERVER_NAME, callbacks);
        } catch (IOException e) {
            verify(transport, never()).start(props);
            verify(transport, never()).createServer(SERVER_NAME, callbacks);
        }
    }
    
    @Test
    public void testCreateServerWrongTransport() throws Exception {
        ServerTransport badTransport = mock(ServerTransport.class);
        when(badTransport.getType()).thenReturn(IPCType.UNIX_SOCKET);
        context.registerService(ServerTransport.class.getName(), badTransport, null);
        
        AgentIPCServiceImpl service = createService();
        ThermostatIPCCallbacks callbacks = mock(ThermostatIPCCallbacks.class);
        
        try {
            service.createServer(SERVER_NAME, callbacks);
        } catch (IOException e) {
            verify(transport, never()).start(props);
            verify(transport, never()).createServer(SERVER_NAME, callbacks);
        }
    }
    
    @Test
    public void testCreateServerFileExists() throws Exception {
        context.registerService(ServerTransport.class.getName(), transport, null);
        AgentIPCServiceImpl service = createService();
        when(helper.configFileExists(propFile)).thenReturn(true);
        ThermostatIPCCallbacks callbacks = mock(ThermostatIPCCallbacks.class);
        
        assertFalse(service.isStarted());
        service.createServer(SERVER_NAME, callbacks);
        assertTrue(service.isStarted());
        
        verify(transport).start(props);
        verify(writer, never()).write();
        verify(transport).createServer(SERVER_NAME, callbacks);
    }
    
    @Test
    public void testCreateServerTwice() throws Exception {
        context.registerService(ServerTransport.class.getName(), transport, null);
        AgentIPCServiceImpl service = createService();
        ThermostatIPCCallbacks callbacks = mock(ThermostatIPCCallbacks.class);
        service.createServer(SERVER_NAME, callbacks);
        service.createServer(SERVER_NAME, callbacks);
        
        // Ensure initialization only occurs once (times(1) is implicit)
        verify(transport).start(props);
        verify(writer).write();
        verify(transport, times(2)).createServer(SERVER_NAME, callbacks);
    }

    @Test
    public void testServerExists() throws Exception {
        context.registerService(ServerTransport.class.getName(), transport, null);
        AgentIPCServiceImpl service = createService();
        assertFalse(service.isStarted());
        service.serverExists(SERVER_NAME);
        assertTrue(service.isStarted());
        
        verify(transport).start(props);
        verify(writer).write();
        verify(transport).serverExists(SERVER_NAME);
    }

    @Test
    public void testDestroyServer() throws Exception {
        context.registerService(ServerTransport.class.getName(), transport, null);
        AgentIPCServiceImpl service = createService();
        assertFalse(service.isStarted());
        service.destroyServer(SERVER_NAME);
        assertTrue(service.isStarted());
        
        verify(transport).start(props);
        verify(writer).write();
        verify(transport).destroyServer(SERVER_NAME);
    }

    @Test
    public void testShutdown() throws Exception {
        context.registerService(ServerTransport.class.getName(), transport, null);
        AgentIPCServiceImpl service = createService();
        // Trigger start service
        service.serverExists(SERVER_NAME);
        
        // Transport should be available before shutdown
        Map<IPCType, ServerTransport> transports = service.getTransports();
        assertEquals(1, transports.size());
        
        service.shutdown();
        // Should be empty after closed, indicating tracker closed
        transports = service.getTransports();
        assertTrue(transports.isEmpty());
        
        // Ensure transport is also shut down
        verify(transport).shutdown();
    }
    
    @Test
    public void testAddTransport() throws Exception {
        AgentIPCServiceImpl service = createService();
        // Should begin empty
        Map<IPCType, ServerTransport> transports = service.getTransports();
        assertTrue(transports.isEmpty());
        
        context.registerService(ServerTransport.class.getName(), transport, null);
        transports = service.getTransports();
        assertEquals(1, transports.size());
        assertEquals(transport, transports.get(IPCType.UNKNOWN));
    }
    
    @Test
    public void testRemovedTransport() throws Exception {
        ServiceRegistration reg = context.registerService(ServerTransport.class.getName(), transport, null);
        
        AgentIPCServiceImpl service = createService();
        
        // Should begin with transport
        Map<IPCType, ServerTransport> transports = service.getTransports();
        assertEquals(1, transports.size());
        assertEquals(transport, transports.get(IPCType.UNKNOWN));
        
        reg.unregister();
        transports = service.getTransports();
        assertTrue(transports.isEmpty());
    }

}
