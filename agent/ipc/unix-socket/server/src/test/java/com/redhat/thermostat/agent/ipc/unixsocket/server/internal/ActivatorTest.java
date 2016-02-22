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

package com.redhat.thermostat.agent.ipc.unixsocket.server.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCPropertiesBuilder;
import com.redhat.thermostat.agent.ipc.common.internal.IPCPropertiesProvider;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.UnixSocketIPCPropertiesProvider;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.Activator.IPCServiceHelper;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.testutils.StubBundleContext;

public class ActivatorTest {
    
    private Activator activator;
    private CommonPaths paths;
    private IPCPropertiesBuilder builder;
    private TestSelector selector;
    private AgentIPCServiceImpl service;
    private File configFile;
    
    @Before
    public void setUp() throws Exception {
        paths = mock(CommonPaths.class);
        builder = mock(IPCPropertiesBuilder.class);
        configFile = mock(File.class);
        when(paths.getUserIPCConfigurationFile()).thenReturn(configFile);
        IPCProperties props = mock(IPCProperties.class);
        when(props.getType()).thenReturn(IPCType.UNIX_SOCKET);
        when(builder.getProperties(configFile)).thenReturn(props);
        
        SelectorProvider provider = mock(SelectorProvider.class);
        selector = new TestSelector(provider);
        when(provider.openSelector()).thenReturn(selector);
        
        IPCServiceHelper helper = mock(IPCServiceHelper.class);
        service = mock(AgentIPCServiceImpl.class);
        when(helper.configFileExists(configFile)).thenReturn(true);
        when(helper.createService(eq(selector), eq(props))).thenReturn(service);
        
        activator = new Activator(provider, helper);
    }

    @Test
    public void verifyServiceIsRegisteredSocketType() throws Exception {
        StubBundleContext context = new StubBundleContext();
        
        context.registerService(CommonPaths.class.getName(), paths, null);
        context.registerService(IPCPropertiesBuilder.class.getName(), builder, null);

        activator.start(context);

        assertTrue(context.isServiceRegistered(IPCPropertiesProvider.class.getName(), UnixSocketIPCPropertiesProvider.class));
        assertEquals(4, context.getAllServices().size());
        ServiceReference ref = context.getServiceReference(AgentIPCService.class);
        assertEquals(service, context.getService(ref));

        verify(service).start();
    }
    
    @Test
    public void verifyServiceIsNotRegisteredWrongSocketType() throws Exception {
        StubBundleContext context = new StubBundleContext();
        IPCProperties props = mock(IPCProperties.class);
        when(props.getType()).thenReturn(null);
        when(builder.getProperties(configFile)).thenReturn(props);
        
        context.registerService(CommonPaths.class.getName(), paths, null);
        context.registerService(IPCPropertiesBuilder.class.getName(), builder, null);

        activator.start(context);

        // Should register properties provider, but not IPC service
        assertEquals(3, context.getAllServices().size());
        ServiceReference ref = context.getServiceReference(AgentIPCService.class);
        assertNull(ref);

        verify(service, never()).start();
    }
    
    @Test
    public void verifyServiceIsUnregistered() throws Exception {
        StubBundleContext context = new StubBundleContext();
        
        ServiceRegistration pathsReg = context.registerService(CommonPaths.class.getName(), paths, null);
        ServiceRegistration builderReg = context.registerService(IPCPropertiesBuilder.class.getName(), builder, null);

        activator.start(context);
        
        pathsReg.unregister();
        builderReg.unregister();

        assertEquals(1, context.getAllServices().size());
        verify(service).shutdown();
    }
    
    @Test
    public void verifySelectorClosed() throws Exception {
        StubBundleContext context = new StubBundleContext();

        activator.start(context);
        activator.stop(context);
        
        assertTrue(selector.isCloseCalled());
    }
    
    // Can't mock properly because close() is final
    private static class TestSelector extends AbstractSelector {
        
        private boolean closeCalled;

        protected TestSelector(SelectorProvider provider) {
            super(provider);
            closeCalled = false;
        }
        
        @Override
        protected void implCloseSelector() throws IOException {
            this.closeCalled = true;
        }
        
        boolean isCloseCalled() {
            return closeCalled;
        }

        @Override
        protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
            return null;
        }

        @Override
        public Set<SelectionKey> keys() {
            return Collections.emptySet();
        }

        @Override
        public Set<SelectionKey> selectedKeys() {
            return Collections.emptySet();
        }

        @Override
        public int selectNow() throws IOException {
            return select(0);
        }

        @Override
        public int select(long timeout) throws IOException {
            return 0;
        }

        @Override
        public int select() throws IOException {
            return select(0);
        }

        @Override
        public Selector wakeup() {
            return this;
        }
        
    }

}
