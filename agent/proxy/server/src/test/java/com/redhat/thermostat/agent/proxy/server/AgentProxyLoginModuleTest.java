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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.agent.proxy.server.AgentProxyLoginModule.AgentProxyCallback;

public class AgentProxyLoginModuleTest {
    
    private AgentProxyLoginModule module;
    private CallbackHandler handler;
    private Subject subject;

    @Before
    public void setup() throws Exception {
        module = new AgentProxyLoginModule();
        subject = new Subject();
        handler = mock(CallbackHandler.class);
        final UnixCredentials creds = new UnixCredentials(9000, 9001, 0);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Callback[] callbacks = (Callback[]) invocation.getArguments()[0];
                for (Callback callback : callbacks) {
                    if (callback instanceof AgentProxyCallback) {
                        ((AgentProxyCallback) callback).setTargetCredentials(creds);
                    }
                }
                return null;
            }
        }).when(handler).handle(any(Callback[].class));
        module.initialize(subject, handler, new HashMap<String, Object>(), new HashMap<String, Object>());
    }
    
    @Test
    public void testLoginSuccess() throws Exception {
        addPrincipals();
        
        assertTrue(module.login());
        
        AgentProxyPrincipal principal = module.getPrincipal();
        assertNotNull(principal);
        assertEquals("TEST", principal.getName());
        assertTrue(module.isLoggedIn());
        assertFalse(module.isCommitted());
        assertTrue(subject.getPrincipals(AgentProxyPrincipal.class).isEmpty());
    }
    
    @SuppressWarnings("restriction")
    @Test
    public void testLoginBadUid() throws Exception {
        subject.getPrincipals().add(new com.sun.security.auth.UnixPrincipal("TEST"));
        subject.getPrincipals().add(new com.sun.security.auth.UnixNumericUserPrincipal(8000));
        subject.getPrincipals().add(new com.sun.security.auth.UnixNumericGroupPrincipal(9001, true));
        
        verifyFailedLogin();
    }

    @SuppressWarnings("restriction")
    @Test
    public void testLoginMissingUid() throws Exception {
        subject.getPrincipals().add(new com.sun.security.auth.UnixPrincipal("TEST"));
        subject.getPrincipals().add(new com.sun.security.auth.UnixNumericGroupPrincipal(9001, true));
        
        verifyFailedLogin();
    }
    
    @SuppressWarnings("restriction")
    @Test
    public void testLoginBadGid() throws Exception {
        subject.getPrincipals().add(new com.sun.security.auth.UnixPrincipal("TEST"));
        subject.getPrincipals().add(new com.sun.security.auth.UnixNumericUserPrincipal(9000));
        subject.getPrincipals().add(new com.sun.security.auth.UnixNumericGroupPrincipal(8001, true));
        
        verifyFailedLogin();
    }
    
    @SuppressWarnings("restriction")
    @Test
    public void testLoginMissingGid() throws Exception {
        subject.getPrincipals().add(new com.sun.security.auth.UnixPrincipal("TEST"));
        subject.getPrincipals().add(new com.sun.security.auth.UnixNumericUserPrincipal(9000));
        
        verifyFailedLogin();
    }
    
    @SuppressWarnings("restriction")
    @Test
    public void testLoginMissingUsername() throws Exception {
        subject.getPrincipals().add(new com.sun.security.auth.UnixNumericUserPrincipal(9000));
        subject.getPrincipals().add(new com.sun.security.auth.UnixNumericGroupPrincipal(9001, true));
        
        verifyFailedLogin();
    }
    
    @Test
    public void testCommitSuccess() throws Exception {
        addPrincipals();
        
        assertTrue(module.login());
        assertTrue(module.commit());
        
        assertTrue(module.isLoggedIn());
        assertTrue(module.isCommitted());
        Set<AgentProxyPrincipal> principals = subject.getPrincipals(AgentProxyPrincipal.class);
        assertFalse(principals.isEmpty());
        assertEquals(module.getPrincipal(), principals.iterator().next());
    }
    
    @Test
    public void testCommitNotLoggedIn() throws Exception {
        addPrincipals();
        
        assertFalse(module.commit());
        
        assertFalse(module.isLoggedIn());
        assertFalse(module.isCommitted());
        assertTrue(subject.getPrincipals(AgentProxyPrincipal.class).isEmpty());
    }
    
    @Test
    public void testAbortNotLoggedIn() throws Exception {
        addPrincipals();
        
        assertFalse(module.abort());
        
        verifyStateReset();
    }

    @Test
    public void testAbortNotCommitted() throws Exception {
        addPrincipals();
        
        assertTrue(module.login());
        assertTrue(module.abort());
        
        verifyStateReset();
    }
    
    @Test
    public void testAbortCommitted() throws Exception {
        addPrincipals();
        
        assertTrue(module.login());
        assertTrue(module.commit());
        assertTrue(module.abort());
        
        verifyStateReset();
    }
    
    @Test
    public void testLogout() throws Exception {
        addPrincipals();
        
        assertTrue(module.login());
        assertTrue(module.commit());
        assertTrue(module.logout());
        
        verifyStateReset();
    }

    @SuppressWarnings("restriction")
    private void addPrincipals() {
        subject.getPrincipals().add(new com.sun.security.auth.UnixPrincipal("TEST"));
        subject.getPrincipals().add(new com.sun.security.auth.UnixNumericUserPrincipal(9000));
        subject.getPrincipals().add(new com.sun.security.auth.UnixNumericGroupPrincipal(9001, true));
    }

    private void verifyFailedLogin() {
        try {
            module.login();
            fail("Expected LoginException");
        } catch (LoginException e) {
            assertFalse(module.isLoggedIn());
            assertNull(module.getPrincipal());
            assertFalse(module.isCommitted());
            assertTrue(subject.getPrincipals(AgentProxyPrincipal.class).isEmpty());
        }
    }

    @SuppressWarnings("restriction")
    private void verifyStateReset() {
        assertFalse(module.isLoggedIn());
        assertFalse(module.isCommitted());
        assertNull(module.getPrincipal());
        assertTrue(subject.getPrincipals(AgentProxyPrincipal.class).isEmpty());
        assertFalse(subject.getPrincipals(com.sun.security.auth.UnixPrincipal.class).isEmpty());
        assertFalse(subject.getPrincipals(com.sun.security.auth.UnixNumericUserPrincipal.class).isEmpty());
        assertFalse(subject.getPrincipals(com.sun.security.auth.UnixNumericGroupPrincipal.class).isEmpty());
    }

}
