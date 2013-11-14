/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.security.auth.Subject;

import org.junit.Before;
import org.junit.Test;

public class AgentProxyControlWrapperTest {
    
    private AgentProxyControlWrapper control;
    private AgentProxyControlImpl impl;
    private Subject user;
    private AgentProxyLoginContext context;
    private ShutdownListener listener;
    private RegistryUtils registryUtils;
    
    @Before
    public void setup() throws Exception {
        user = new Subject();
        context = mock(AgentProxyLoginContext.class);
        impl = mock(AgentProxyControlImpl.class);
        listener = mock(ShutdownListener.class);
        registryUtils = mock(RegistryUtils.class);
        control = new AgentProxyControlWrapper(user, context, impl, listener, registryUtils);
    }
    
    @Test
    public void testAttach() throws Exception {
        control.attach();
        verify(impl).attach(user);
    }
    
    @Test
    public void testIsAttached() throws Exception {
        control.isAttached();
        verify(impl).isAttached(user);
    }
    
    @Test
    public void testGetAddress() throws Exception {
        control.getConnectorAddress();
        verify(impl).getConnectorAddress(user);
    }
    
    @Test
    public void testDetach() throws Exception {
        control.detach();
        
        verify(impl).detach(user);
        verify(context).logout();
        verify(listener).shutdown();
        verify(registryUtils).unexportObject(control);
    }
    
}
