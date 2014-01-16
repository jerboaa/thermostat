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

package com.redhat.thermostat.web.server.auth.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.web.server.auth.BasicRole;
import com.redhat.thermostat.web.server.auth.RolePrincipal;
import com.redhat.thermostat.web.server.auth.UserPrincipal;

public class DelegateLoginModuleTest {

    private Subject subject;
    private Map<String, Object> mockSharedState;
    private Map<String, Object> mockOptions;
    private CallbackHandler mockCallBack;
    
    @Before
    public void setup() {
        subject = new Subject();
        mockSharedState = new HashMap<>();
        mockOptions = new HashMap<>();
        // DelegateLoginModule uses the name callback
        mockCallBack = new SimpleCallBackHandler("testUser", "doesn't matter".toCharArray());
        // sets jaas config so as to use StubDelegateLoginModule
        URL testConfig = DelegateLoginModuleTest.class
                .getResource("/delegate_login_module_test_jaas.conf");
        System.setProperty("java.security.auth.login.config", testConfig.getFile());
    }
    
    @After
    public void teardown() {
        subject = null;
        mockSharedState = null;
        mockOptions = null;
        mockCallBack = null;
    }
    
    @Test
    public void testBasicsSuccess() throws Exception {
        DelegateLoginModule delegateLogin = new DelegateLoginModule("Success");
        delegateLogin.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        assertTrue("Stub delegate in use is always successfull", delegateLogin.login());
        assertTrue("Stub delegate in use is always successfull", delegateLogin.commit());
        // SubSuccessDelegateLoginModule added one user principal
        // 'testUser' and one role principal 'testRole'.
        Subject subject = delegateLogin.getSubject();
        assertEquals(this.subject, subject);
        Set<Principal> principals = subject.getPrincipals();
        assertEquals(2, principals.size());
        Iterator<Principal> it = principals.iterator();
        while (it.hasNext()) {
            Principal p = it.next();
            if (p.getName().equals("testUser")) {
                // user principal
                assertTrue(p instanceof UserPrincipal);
                UserPrincipal uPrincipal = (UserPrincipal)p;
                assertNotNull("Should now be a thermostat UserPrincipal containing roles", uPrincipal.getRoles());
                assertEquals(1, uPrincipal.getRoles().size());
                Iterator<BasicRole> iter = uPrincipal.getRoles().iterator();
                while (iter.hasNext()) {
                    BasicRole role = iter.next();
                    assertEquals("testRole", role.getName());
                }
            }
            if (p.getName().equals("testRole")) {
                assertTrue("Should have been wrapped into a BasicRole", p instanceof BasicRole);
            }
        }
        // Test the same another way for good measure.
        assertTrue(principals.contains(new UserPrincipal("testUser")));
        assertTrue(principals.contains(new RolePrincipal("testRole")));
        
        // now logout. we expect principals to be cleared
        delegateLogin.logout();
        assertEquals(0, subject.getPrincipals().size());
    }
    
    @Test
    public void testBasicsFailure() throws Exception {
        DelegateLoginModule delegateLogin = new DelegateLoginModule("Failure");
        delegateLogin.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        // add some principal to the subject in order to make sure delegate
        // clears principals on login failure.
        Principal mock = new Principal() {

            @Override
            public String getName() {
                return "tester";
            }
            
        };
        subject.getPrincipals().add(mock);
        assertEquals(1, subject.getPrincipals().size());
        try {
            // this triggers abort() being called for the delegate.
            delegateLogin.login();
            fail("StubFailureDelegateLoginModule should have thrown LE");
        } catch (LoginException e) {
            // pass
        }
        assertEquals(0, subject.getPrincipals().size());
    }
    
    @Test
    public void testAbort() throws LoginException {
        // add some principal to the subject in order to make sure delegate
        // clears principals on login failure.
        Principal mock = new Principal() {

            @Override
            public String getName() {
                return "tester";
            }
            
        };
        subject.getPrincipals().add(mock);
        assertEquals(1, subject.getPrincipals().size());
        DelegateLoginModule delegateLogin = new DelegateLoginModule("Failure");
        delegateLogin.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        // simulate abort which should clear any principals from the subject
        delegateLogin.abort();
        assertEquals(0, subject.getPrincipals().size());
    }
}

