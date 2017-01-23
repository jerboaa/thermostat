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

package com.redhat.thermostat.agent.internal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import com.redhat.thermostat.common.portability.UserNameUtil;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.agent.VmBlacklist;
import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.NativeLibraryResolver;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.utils.management.internal.MXBeanConnectionPoolControl;
import com.redhat.thermostat.utils.management.internal.MXBeanConnectionPoolImpl;

public class ActivatorTest {
    
    private StubBundleContext context;
    private ServiceRegistration ipcReg;

    @Before
    public void setup() {
        CommonPaths paths = mock(CommonPaths.class);
    	when(paths.getSystemNativeLibsRoot()).thenReturn(new File("target"));
        when(paths.getUserAgentAuthConfigFile()).thenReturn(new File("not.exist.does.not.matter"));
    	NativeLibraryResolver.setCommonPaths(paths);
    
        context = new StubBundleContext();
        context.registerService(CommonPaths.class.getName(), paths, null);

        // required by MXBeanConnectionPoolImpl
        UserNameUtil userNameUtil = mock(UserNameUtil.class);
        context.registerService(UserNameUtil.class.getName(), userNameUtil, null);

        AgentIPCService ipcService = mock(AgentIPCService.class);
        ipcReg = context.registerService(AgentIPCService.class.getName(), ipcService, null);
    }

    @Test
    public void verifyServiceIsRegistered() throws Exception {
        Activator activator = new Activator();
        activator.start(context);

        assertTrue(context.isServiceRegistered(MXBeanConnectionPool.class.getName(), MXBeanConnectionPoolImpl.class));
        assertTrue(context.isServiceRegistered(MXBeanConnectionPoolControl.class.getName(), MXBeanConnectionPoolImpl.class));
        assertTrue(context.isServiceRegistered(VmBlacklist.class.getName(), VmBlacklistImpl.class));
    }

    @Test
    public void verifyPoolShutdown() throws Exception {
        Activator activator = new Activator();
        activator.start(context);
        
        MXBeanConnectionPoolImpl pool = mock(MXBeanConnectionPoolImpl.class);
        when(pool.isStarted()).thenReturn(true);
        activator.setPool(pool);

        // Remove tracked service
        ipcReg.unregister();
        
        verify(pool).shutdown();
    }
    
    @Test
    public void verifyPoolShutdownNotStarted() throws Exception {
        Activator activator = new Activator();
        activator.start(context);
        
        MXBeanConnectionPoolImpl pool = mock(MXBeanConnectionPoolImpl.class);
        activator.setPool(pool);

        // Remove tracked service
        ipcReg.unregister();
        
        verify(pool, never()).shutdown();
    }
}

