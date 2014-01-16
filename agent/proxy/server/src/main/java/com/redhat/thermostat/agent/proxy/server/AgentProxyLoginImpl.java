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

import java.rmi.RemoteException;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;

import com.redhat.thermostat.agent.proxy.common.AgentProxyControl;
import com.redhat.thermostat.agent.proxy.common.AgentProxyLogin;

class AgentProxyLoginImpl implements AgentProxyLogin {
    
    private final UnixCredentials creds;
    private final AgentProxyControlImpl impl;
    private final ShutdownListener listener;
    private final LoginContextCreator contextCreator;
    private final RegistryUtils registryUtils;
    
    AgentProxyLoginImpl(UnixCredentials creds, int pid, ShutdownListener listener) throws RemoteException {
        this(creds, pid, listener, new LoginContextCreator(), new RegistryUtils());
    }
    
    AgentProxyLoginImpl(UnixCredentials creds, int pid, ShutdownListener listener, 
            LoginContextCreator contextCreator, RegistryUtils registryUtils) throws RemoteException {
        this.creds = creds;
        this.impl = new AgentProxyControlImpl(pid);
        this.listener = listener;
        this.contextCreator = contextCreator;
        this.registryUtils = registryUtils;
    }

    @Override
    public AgentProxyControl login() throws RemoteException, SecurityException {
        Subject user = new Subject();
        try {
            AgentProxyLoginContext context = contextCreator.createContext(user, creds);
            context.login();
            
            AgentProxyControl control = new AgentProxyControlWrapper(user, context, impl, 
                    listener, registryUtils);
            AgentProxyControl stub = (AgentProxyControl) registryUtils.exportObject(control);
            return stub;
        } catch (LoginException e) {
            throw new RemoteException("Failed to login", e);
        }
    }
    
    static class LoginContextCreator {
        AgentProxyLoginContext createContext(Subject user, UnixCredentials creds) throws LoginException {
            return new AgentProxyLoginContext(user, creds);
        }
    }
    
}

