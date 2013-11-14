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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class AgentProxyLoginModule implements LoginModule {
    
    private Subject subject;
    private CallbackHandler callbackHandler;
    private AgentProxyPrincipal principal;
    private boolean loggedIn;
    private boolean committed;
    private boolean debug;
    
    interface AgentProxyCallback extends Callback {
        
        void setTargetCredentials(UnixCredentials creds);
        
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        
        // Check for debug option
        debug = "true".equalsIgnoreCase((String) options.get("debug"));
    }

    @Override
    public boolean login() throws LoginException {
        loggedIn = false;
        
        // Get credentials of target process from callback
        UnixCredentials creds = getTargetCredentials();

        // Verify subject's credentials match those of target process
        checkCredentials(creds);

        // Add a custom principal to the subject to show we've authenticated
        principal = createPrincipal();
        if (debug) {
            System.out.println("\t\t[AgentProxyLoginModule]: " +
                    "created principal for user: " + principal.getName());
        }
        
        loggedIn = true;
        return true;
    }
    
    private UnixCredentials getTargetCredentials() throws LoginException {
        final CountDownLatch latch = new CountDownLatch(1);
        final UnixCredentials[] credsContainer = new UnixCredentials[1];
        
        try {
            callbackHandler.handle(new Callback[] { new AgentProxyCallback() {

                @Override
                public void setTargetCredentials(UnixCredentials creds) {
                    credsContainer[0] = creds;
                    latch.countDown();
                }

            }});

            latch.await();
            
            return credsContainer[0];
        } catch (IOException e) {
            throw new LoginException(e.getMessage());
        } catch (UnsupportedCallbackException e) {
            throw new LoginException(e.getMessage());
        } catch (InterruptedException e) {
            throw new LoginException("Interrupted");
        }
    }

    @Override
    public boolean commit() throws LoginException {
        committed = false;
        
        if (loggedIn) {
            subject.getPrincipals().add(principal);
            if (debug) {
                System.out.println("\t\t[AgentProxyLoginModule]: " +
                        "adding AgentProxyPrincipal to Subject");
            }
            committed = true;
        }
        return committed;
    }

    @Override
    public boolean abort() throws LoginException {
        if (debug) {
            System.out.println("\t\t[AgentProxyLoginModule]: " +
                    "aborted authentication attempt");
        }
        if (!loggedIn) {
            return false;
        }
        else if (loggedIn && !committed) {
            // Clean up state
            loggedIn = false;
            principal = null;
        }
        else {
            // Clean up state & remove principal
            logout();
        }
        
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        // Remove principal
        subject.getPrincipals().remove(principal);
        if (debug) {
            System.out.println("\t\t[AgentProxyLoginModule]: " +
                    "removed principal for user: " + principal.getName());
        }
        
        // Clean up state
        loggedIn = false;
        committed = false;
        principal = null;
        
        return true;
    }
    
    @SuppressWarnings("restriction")
    private void checkCredentials(UnixCredentials creds) throws LoginException {
        boolean uidOkay = false, gidOkay = false;
        
        // Check UID
        Set<com.sun.security.auth.UnixNumericUserPrincipal> userPrincipals = subject.getPrincipals(com.sun.security.auth.UnixNumericUserPrincipal.class);
        if (!userPrincipals.isEmpty()) {
            com.sun.security.auth.UnixNumericUserPrincipal userPrincipal = userPrincipals.iterator().next();
            if (debug) {
                System.out.println("UnixLoginModule UID: " + userPrincipal.longValue() + ", PID: " + creds.getPid() + ", Owner: " + creds.getUid());
            }
            if (userPrincipal.longValue() == creds.getUid() || userPrincipal.longValue() == 0) {
                uidOkay = true;
            }
        }
        
        // Check GID
        Set<com.sun.security.auth.UnixNumericGroupPrincipal> groupPrincipals = subject.getPrincipals(com.sun.security.auth.UnixNumericGroupPrincipal.class);
        for (com.sun.security.auth.UnixNumericGroupPrincipal groupPrincipal : groupPrincipals) {
            if (groupPrincipal.longValue() == creds.getGid() || groupPrincipal.longValue() == 0) {
                gidOkay = true;
            }
        }
        
        if (!uidOkay || !gidOkay) {
            throw new LoginException("Access Denied");
        }
    }
    
    @SuppressWarnings("restriction")
    private AgentProxyPrincipal createPrincipal() throws LoginException {
        Set<com.sun.security.auth.UnixPrincipal> userPrincipals = subject.getPrincipals(com.sun.security.auth.UnixPrincipal.class);
        if (userPrincipals.isEmpty()) {
            throw new LoginException("Unable to obtain user ID");
        }
        
        com.sun.security.auth.UnixPrincipal userPrincipal = userPrincipals.iterator().next();
        return new AgentProxyPrincipal(userPrincipal.getName());
    }
    
    /*
     * For testing purposes only.
     */
    AgentProxyPrincipal getPrincipal() {
        return principal;
    }
    
    /*
     * For testing purposes only.
     */
    boolean isLoggedIn() {
        return loggedIn;
    }
    
    /*
     * For testing purposes only.
     */
    boolean isCommitted() {
        return committed;
    }
    
}
