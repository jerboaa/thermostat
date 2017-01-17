/*
 * Copyright 2012-2017 Red Hat, Inc.
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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCPropertiesProvider;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.testutils.StubBundleContext;

public class ServerIPCPropertiesBuilderTest {
    
    private StubBundleContext context;
    private IPCPropertiesProvider provider;
    private IPCProperties ipcProps;
    private Properties jProps;
    private File propFile;

    @Before
    public void setUp() throws Exception {
        context = new StubBundleContext();
        ipcProps = mock(IPCProperties.class);
        provider = mock(IPCPropertiesProvider.class);
        jProps = mock(Properties.class);
        propFile = mock(File.class);
        when(provider.getType()).thenReturn(IPCType.UNIX_SOCKET);
        when(provider.create(jProps, propFile)).thenReturn(ipcProps);
    }
    
    @Test
    public void testAddProvider() throws Exception {
        ServerIPCPropertiesBuilder builder = new ServerIPCPropertiesBuilder(context);
        
        // Should begin empty
        Map<IPCType, IPCPropertiesProvider> providers = builder.getProviders();
        assertTrue(providers.isEmpty());
        
        context.registerService(IPCPropertiesProvider.class.getName(), provider, null);
        providers = builder.getProviders();
        assertEquals(1, providers.size());
        assertEquals(provider, providers.get(IPCType.UNIX_SOCKET));
    }
    
    @Test
    public void testRemovedProvider() throws Exception {
        ServiceRegistration reg = context.registerService(IPCPropertiesProvider.class.getName(), provider, null);
        
        ServerIPCPropertiesBuilder builder = new ServerIPCPropertiesBuilder(context);
        
        // Should begin with provider
        Map<IPCType, IPCPropertiesProvider> providers = builder.getProviders();
        assertEquals(1, providers.size());
        assertEquals(provider, providers.get(IPCType.UNIX_SOCKET));
        
        reg.unregister();
        providers = builder.getProviders();
        assertTrue(providers.isEmpty());
    }
    
    @Test
    public void testGetPropertiesForType() throws Exception {
        when(provider.getType()).thenReturn(IPCType.UNIX_SOCKET);
        context.registerService(IPCPropertiesProvider.class.getName(), provider, null);
        
        ServerIPCPropertiesBuilder builder = new ServerIPCPropertiesBuilder(context);
        IPCProperties result = builder.getPropertiesForType(IPCType.UNIX_SOCKET, jProps, propFile);
        assertEquals(ipcProps, result);
    }
    
    @Test
    public void testGetPropertiesForTypeNotFound() throws Exception {
        when(provider.getType()).thenReturn(IPCType.UNKNOWN);
        context.registerService(IPCPropertiesProvider.class.getName(), provider, null);
        
        ServerIPCPropertiesBuilder builder = new ServerIPCPropertiesBuilder(context);
        try {
            builder.getPropertiesForType(IPCType.UNIX_SOCKET, jProps, propFile);
            fail("Expected IOException");
        } catch (IOException e) {
            verify(provider, never()).create(any(Properties.class), any(File.class));
        }
    }
    
    @Test
    public void testTrackerClosed() throws Exception {
        context.registerService(IPCPropertiesProvider.class.getName(), provider, null);
        ServerIPCPropertiesBuilder builder = new ServerIPCPropertiesBuilder(context);
        
        // Should begin with provider
        Map<IPCType, IPCPropertiesProvider> providers = builder.getProviders();
        assertEquals(1, providers.size());
        assertEquals(provider, providers.get(IPCType.UNIX_SOCKET));
        
        // Should be empty after closed, indicating tracker closed
        builder.close();
        providers = builder.getProviders();
        assertTrue(providers.isEmpty());
    }
    
    @Test
    public void testIsClosed() throws Exception {
        ServerIPCPropertiesBuilder builder = new ServerIPCPropertiesBuilder(context);
        assertFalse(builder.isClosed());
        builder.close();
        assertTrue(builder.isClosed());
    }

}
