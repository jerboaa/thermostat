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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.proxy.server.AgentProxyLoginContext.ContextCreator;
import com.redhat.thermostat.agent.proxy.server.AgentProxyLoginModule.AgentProxyCallback;

public class AgentProxyLoginContextTest {
    
    private AgentProxyLoginContext context;
    private ContextCreator creator;
    private Subject user;
    private UnixCredentials creds;
    private LoginContext unixContext;
    private LoginContext ourContext;

    @Before
    public void setup() throws Exception {
        user = new Subject();
        creds = new UnixCredentials(9000, 9001, 0);
        creator = mock(ContextCreator.class);
        unixContext = mock(LoginContext.class);
        ourContext = mock(LoginContext.class);
        when(creator.createContext("UnixLogin", user)).thenReturn(unixContext);
        when(creator.createContext(eq("AgentProxyLogin"), same(user), any(CallbackHandler.class))).thenReturn(ourContext);
        context = new AgentProxyLoginContext(user, creds, creator);
    }
    
    @Test
    public void testCreate() throws Exception {
        verify(creator).createContext("UnixLogin", user);
        ArgumentCaptor<CallbackHandler> captor = ArgumentCaptor.forClass(CallbackHandler.class);
        verify(creator).createContext(eq("AgentProxyLogin"), same(user), captor.capture());
        CallbackHandler handler = captor.getValue();
        
        AgentProxyCallback callback = mock(AgentProxyCallback.class);
        handler.handle(new Callback[] { callback });
        verify(callback).setTargetCredentials(creds);
    }
    
    @Test
    public void testLogin() throws Exception {
        context.login();
        verify(unixContext).login();
        verify(ourContext).login();
    }
    
    @Test
    public void testLogout() throws Exception {
        context.logout();
        verify(ourContext).logout();
        verify(unixContext).logout();
    }
    
}

