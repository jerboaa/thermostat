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

class AgentProxyControlWrapper implements AgentProxyControl {
    
    private final Subject user;
    private final AgentProxyLoginContext context;
    private final AgentProxyControlImpl impl;
    private final ShutdownListener listener;
    private final RegistryUtils registryUtils;
    
    AgentProxyControlWrapper(Subject user, AgentProxyLoginContext context, AgentProxyControlImpl impl, 
            ShutdownListener listener, RegistryUtils registryUtils) {
        this.user = user;
        this.context = context;
        this.impl = impl;
        this.listener = listener;
        this.registryUtils = registryUtils;
    }

    @Override
    public void attach() throws RemoteException, SecurityException {
        impl.attach(user);
    }

    @Override
    public boolean isAttached() throws RemoteException, SecurityException {
        return impl.isAttached(user);
    }

    @Override
    public String getConnectorAddress() throws RemoteException, SecurityException {
        return impl.getConnectorAddress(user);
    }

    @Override
    public void detach() throws RemoteException, SecurityException {
        try {
            impl.detach(user);
        } finally {
            try {
                // Removes all Principals
                context.logout();
            } catch (LoginException e) {
                throw new RemoteException("Failed to log out", e);
            }
            // Unexport this object
            registryUtils.unexportObject(this);
            
            // Shutdown RMI server
            listener.shutdown();
        }
    }

}

