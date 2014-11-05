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

package com.redhat.thermostat.client.cli.internal;

import static com.redhat.thermostat.testutils.Asserts.assertCommandIsRegistered;
import static com.redhat.thermostat.testutils.Asserts.assertCommandIsNotRegistered;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.utils.keyring.Keyring;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FrameworkUtil.class)
public class ActivatorTest {

    @Test
    public void testCommandsRegistered() throws Exception {
        // Need to mock FrameworkUtil to avoid NPE in commands' no-arg constructors
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle mockBundle = mock(Bundle.class);
        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(mockBundle);
        // When we call createFilter, we need a real return value
        when(FrameworkUtil.createFilter(anyString())).thenCallRealMethod();

        StubBundleContext ctx = new StubBundleContext();
        when(mockBundle.getBundleContext()).thenReturn(ctx);

        Keyring keyring = mock(Keyring.class);
        ctx.registerService(Keyring.class, keyring, null);
        CommonPaths paths = mock(CommonPaths.class);
        File userConfig = mock(File.class);
        when(userConfig.isFile()).thenReturn(false);
        when(paths.getUserClientConfigurationFile()).thenReturn(userConfig);
        ctx.registerService(CommonPaths.class, paths, null);
        
        Activator activator = new Activator();
        
        activator.start(ctx);
        
        assertCommandIsRegistered(ctx, "connect", ConnectCommand.class);
        assertCommandIsRegistered(ctx, "disconnect", DisconnectCommand.class);
        assertCommandIsRegistered(ctx, "list-vms", ListVMsCommand.class);
        assertCommandIsRegistered(ctx, "shell", ShellCommand.class);
        assertCommandIsRegistered(ctx, "vm-info", VMInfoCommand.class);
        assertCommandIsRegistered(ctx, "vm-stat", VMStatCommand.class);
        assertCommandIsRegistered(ctx, "list-agents", ListAgentsCommand.class);
        assertCommandIsRegistered(ctx, "agent-info", AgentInfoCommand.class);

        activator.stop(ctx);

        assertEquals(2, ctx.getAllServices().size());
    }

    @Test
    public void testConnectCommandUnregisteredWhenDepsDisappear() throws Exception {
        // Need to mock FrameworkUtil to avoid NPE in commands' no-arg constructors
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle mockBundle = mock(Bundle.class);
        when(FrameworkUtil.getBundle(any(Class.class))).thenReturn(mockBundle);
        // When we call createFilter, we need a real return value
        when(FrameworkUtil.createFilter(anyString())).thenCallRealMethod();

        StubBundleContext ctx = new StubBundleContext();
        when(mockBundle.getBundleContext()).thenReturn(ctx);
        
        Activator activator = new Activator();
        
        activator.start(ctx);
        
        assertCommandIsNotRegistered(ctx, "connect", ConnectCommand.class);

        Keyring keyring = mock(Keyring.class);
        ServiceRegistration keyringReg = ctx.registerService(Keyring.class, keyring, null);
        CommonPaths paths = mock(CommonPaths.class);
        File userConfig = mock(File.class);
        when(userConfig.isFile()).thenReturn(false);
        when(paths.getUserClientConfigurationFile()).thenReturn(userConfig);
        ctx.registerService(CommonPaths.class, paths, null);

        assertCommandIsRegistered(ctx, "connect", ConnectCommand.class);

        keyringReg.unregister();

        assertCommandIsNotRegistered(ctx, "connect", ConnectCommand.class);

        activator.stop(ctx);
    }

}

