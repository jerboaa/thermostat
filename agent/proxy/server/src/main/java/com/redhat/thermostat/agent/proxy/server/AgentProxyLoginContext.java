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

import java.io.IOException;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.redhat.thermostat.agent.proxy.server.AgentProxyLoginModule.AgentProxyCallback;

/*
 * Wraps both of our LoginModules.
 */
class AgentProxyLoginContext {
    
    private static final String UNIX_LOGIN_MODULE = "UnixLogin";
    private static final String AGENT_PROXY_LOGIN_MODULE = "AgentProxyLogin";
    
    private final LoginContext unixContext;
    private final LoginContext context;
    
    AgentProxyLoginContext(Subject user, UnixCredentials creds) throws LoginException {
        this(user, creds, new ContextCreator());
    }
    
    AgentProxyLoginContext(Subject user, final UnixCredentials creds, ContextCreator creator) throws LoginException {
        unixContext = creator.createContext(UNIX_LOGIN_MODULE, user);
        context = creator.createContext(AGENT_PROXY_LOGIN_MODULE, user, new CallbackHandler() {
            
            @Override
            public void handle(Callback[] callbacks) throws IOException,
                    UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof AgentProxyCallback) {
                        ((AgentProxyCallback) callback).setTargetCredentials(creds);
                    }
                }
            }
        });
    }
    
    void login() throws LoginException {
        unixContext.login();
        context.login();
    }
    
    void logout() throws LoginException {
        context.logout();
        unixContext.logout();
    }
    
    static class ContextCreator {
        LoginContext createContext(String name, Subject subject) throws LoginException {
            return new LoginContext(name, subject);
        }
        
        LoginContext createContext(String name, Subject subject, CallbackHandler handler) throws LoginException {
            return new LoginContext(name, subject, handler);
        }
    }

}
