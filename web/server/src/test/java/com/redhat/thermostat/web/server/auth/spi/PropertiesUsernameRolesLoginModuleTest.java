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

package com.redhat.thermostat.web.server.auth.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.web.server.auth.RolePrincipal;
import com.redhat.thermostat.web.server.auth.UserPrincipal;

public class PropertiesUsernameRolesLoginModuleTest {
    
    private Subject subject;
    private Map<String, Object> mockSharedState;
    private Map<String, Object> mockOptions;
    private CallbackHandler mockCallBack;
    
    @Before
    public void setup() {
        subject = new Subject();
        mockSharedState = new HashMap<>();
        mockOptions = new HashMap<>();
    }
    
    @After
    public void teardown() {
        subject = null;
        mockSharedState = null;
        mockOptions = null;
        mockCallBack = null;
    }

    @Test
    public void canInitialize() {
        LoginModule loginModule = new PropertiesUsernameRolesLoginModule();
        mockCallBack = new SimpleCallBackHandler("testUser", "testpassword".toCharArray());
        URL userFile = this.getClass().getResource("/properties_module_test_users.properties");
        URL rolesFile = this.getClass().getResource("/properties_module_test_roles.properties");
        mockOptions.put("users.properties", userFile.getFile());
        mockOptions.put("roles.properties", rolesFile.getFile());
        try {
            // this must not throw an exception
            loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
            // pass
        } catch (Exception e) {
            fail("Did not expect any exception here!");
        }
    }
    
    @Test
    public void failsToLoginWithInvalidCredentials() {
        LoginModule loginModule = new PropertiesUsernameRolesLoginModule();
        // testUser/testpassword not defined
        mockCallBack = new SimpleCallBackHandler("testUser", "testpassword".toCharArray());
        URL userFile = this.getClass().getResource("/properties_module_test_users.properties");
        URL rolesFile = this.getClass().getResource("/properties_module_test_roles.properties");
        mockOptions.put("users.properties", userFile.getFile());
        mockOptions.put("roles.properties", rolesFile.getFile());
        loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        try {
            loginModule.login();
            fail("testUser not defined in properties_module_test_users.properties");
        } catch (LoginException e) {
            // pass
            assertEquals("User 'testUser' not found", e.getMessage());
        }
        mockCallBack = new SimpleCallBackHandler("user1", "wrongpassword".toCharArray());
        loginModule = new PropertiesUsernameRolesLoginModule();
        loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        try {
            loginModule.login();
            fail("user1 provided wrong password!");
        } catch (LoginException e) {
            assertEquals("Login failed!", e.getMessage());
        }
    }
    
    @Test
    public void canLoginWithValidCredentials() {
        LoginModule loginModule = new PropertiesUsernameRolesLoginModule();
        mockCallBack = new SimpleCallBackHandler("user1", "somepassword".toCharArray());
        URL userFile = this.getClass().getResource("/properties_module_test_users.properties");
        URL rolesFile = this.getClass().getResource("/properties_module_test_roles.properties");
        mockOptions.put("users.properties", userFile.getFile());
        mockOptions.put("roles.properties", rolesFile.getFile());
        loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        try {
            boolean retval = loginModule.login();
            // pass
            assertTrue("Login should have returned true", retval);
        } catch (LoginException e) {
            fail("'user1' should be able to login");
        }
        mockCallBack = new SimpleCallBackHandler("user2", "password".toCharArray());
        loginModule = new PropertiesUsernameRolesLoginModule();
        loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        try {
            boolean retval = loginModule.login();
            // pass
            assertTrue("Login should have returned true", retval);
        } catch (LoginException e) {
            fail("'user2' should be able to login");
        }
    }
    
    @Test
    public void canCommitOnSuccessfulLogin() throws LoginException {
        LoginModule loginModule = new PropertiesUsernameRolesLoginModule();
        mockCallBack = new SimpleCallBackHandler("user1", "somepassword".toCharArray());
        URL userFile = this.getClass().getResource("/properties_module_test_users.properties");
        URL rolesFile = this.getClass().getResource("/properties_module_test_roles.properties");
        mockOptions.put("users.properties", userFile.getFile());
        mockOptions.put("roles.properties", rolesFile.getFile());
        loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        assertTrue(loginModule.login());
        try {
            boolean retval = loginModule.commit();
            // pass
            assertTrue("Commit should have returned true", retval);
        } catch (LoginException e) {
            fail("'user1' should have been able to commit");
        }
        // assert principals are added to subject
        Set<Principal> principals = subject.getPrincipals();
        // my-role, my-role2, Roles, user1
        assertEquals(4, principals.size());
        assertTrue(principals.contains(new RolePrincipal("my-role")));
        assertTrue(principals.contains(new RolePrincipal("my-role2")));
        assertTrue(principals.contains(new RolePrincipal("Roles")));
        assertTrue(principals.contains(new UserPrincipal("user1")));
    }
    
    @Test
    public void testRecursiveRoles() throws LoginException {
        LoginModule loginModule = new PropertiesUsernameRolesLoginModule();
        mockCallBack = new SimpleCallBackHandler("user2", "password".toCharArray());
        URL userFile = this.getClass().getResource("/properties_module_test_users.properties");
        URL rolesFile = this.getClass().getResource("/properties_module_test_roles.properties");
        mockOptions.put("users.properties", userFile.getFile());
        mockOptions.put("roles.properties", rolesFile.getFile());
        loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        assertTrue(loginModule.login());
        try {
            boolean retval = loginModule.commit();
            // pass
            assertTrue("Commit should have returned true", retval);
        } catch (LoginException e) {
            fail("'user2' should have been able to commit");
        }
        // assert principals are added to subject
        Set<Principal> principals = subject.getPrincipals();
        // new-role, role1, other-role, Roles, user2
        assertEquals(5, principals.size());
        assertTrue(principals.contains(new RolePrincipal("new-role")));
        assertTrue(principals.contains(new RolePrincipal("role1")));
        assertTrue(principals.contains(new RolePrincipal("Roles")));
        // via recursive role 'role1'
        assertTrue(principals.contains(new RolePrincipal("other-role")));
        assertTrue(principals.contains(new UserPrincipal("user2")));
    }
    
    @Test
    public void testRecursiveRolesMultiple() throws LoginException {
        LoginModule loginModule = new PropertiesUsernameRolesLoginModule();
        mockCallBack = new SimpleCallBackHandler("user3", "password".toCharArray());
        URL userFile = this.getClass().getResource("/properties_module_test_users.properties");
        URL rolesFile = this.getClass().getResource("/properties_module_test_roles.properties");
        mockOptions.put("users.properties", userFile.getFile());
        mockOptions.put("roles.properties", rolesFile.getFile());
        loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        assertTrue(loginModule.login());
        try {
            boolean retval = loginModule.commit();
            // pass
            assertTrue("Commit should have returned true", retval);
        } catch (LoginException e) {
            fail("'user3' should have been able to commit");
        }
        // assert principals are added to subject
        Set<Principal> principals = subject.getPrincipals();
        // other-role, role1, Roles, user3
        assertEquals(4, principals.size());
        assertTrue(principals.contains(new RolePrincipal("role1")));
        assertTrue(principals.contains(new RolePrincipal("Roles")));
        // via recursive role 'role1'
        assertTrue(principals.contains(new RolePrincipal("other-role")));
        assertTrue(principals.contains(new UserPrincipal("user3")));
        loginModule = new PropertiesUsernameRolesLoginModule();
        mockCallBack = new SimpleCallBackHandler("user2", "password".toCharArray());
        subject = new Subject();
        loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        assertTrue(loginModule.login());
        try {
            boolean retval = loginModule.commit();
            // pass
            assertTrue("Commit should have returned true", retval);
        } catch (LoginException e) {
            fail("'user2' should have been able to commit");
        }
        // assert principals are added to subject
        principals = subject.getPrincipals();
        // new-role, other-role, role1, Roles, user2
        assertEquals(5, principals.size());
        assertTrue(principals.contains(new RolePrincipal("role1")));
        assertTrue(principals.contains(new RolePrincipal("Roles")));
        // via recursive role 'role1'
        assertTrue(principals.contains(new RolePrincipal("other-role")));
        assertTrue(principals.contains(new UserPrincipal("user2")));
        assertTrue(principals.contains(new RolePrincipal("new-role")));
    }
    
    @Test
    public void cannotCommitWithoutLoggingIn() throws LoginException {
        LoginModule loginModule = new PropertiesUsernameRolesLoginModule();
        assertFalse(loginModule.commit());
        
        loginModule = new PropertiesUsernameRolesLoginModule();
        mockCallBack = new SimpleCallBackHandler("user1", "somepassword".toCharArray());
        URL userFile = this.getClass().getResource("/properties_module_test_users.properties");
        URL rolesFile = this.getClass().getResource("/properties_module_test_roles.properties");
        mockOptions.put("users.properties", userFile.getFile());
        mockOptions.put("roles.properties", rolesFile.getFile());
        loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        assertFalse(loginModule.commit());
        assertEquals(0, subject.getPrincipals().size());
    }
    
    @Test
    public void testLoginCommitAbort() throws LoginException {
        LoginModule loginModule = new PropertiesUsernameRolesLoginModule();
        mockCallBack = new SimpleCallBackHandler("user1", "somepassword".toCharArray());
        URL userFile = this.getClass().getResource("/properties_module_test_users.properties");
        URL rolesFile = this.getClass().getResource("/properties_module_test_roles.properties");
        mockOptions.put("users.properties", userFile.getFile());
        mockOptions.put("roles.properties", rolesFile.getFile());
        loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        assertTrue(loginModule.login());
        assertTrue(loginModule.commit());
        // assert principals are added to subject after login
        Set<Principal> principals = subject.getPrincipals();
        // my-role, my-role2, Roles, user1
        assertEquals(4, principals.size());
        assertTrue(loginModule.abort());
        // make sure principals are cleared
        assertEquals(0, principals.size());
    }
    
    @Test
    public void testLoginCommitLogout() throws LoginException {
        LoginModule loginModule = new PropertiesUsernameRolesLoginModule();
        mockCallBack = new SimpleCallBackHandler("user1", "somepassword".toCharArray());
        URL userFile = this.getClass().getResource("/properties_module_test_users.properties");
        URL rolesFile = this.getClass().getResource("/properties_module_test_roles.properties");
        mockOptions.put("users.properties", userFile.getFile());
        mockOptions.put("roles.properties", rolesFile.getFile());
        loginModule.initialize(subject, mockCallBack, mockSharedState, mockOptions);
        assertTrue(loginModule.login());
        assertTrue(loginModule.commit());
        // assert principals are added to subject after login
        Set<Principal> principals = subject.getPrincipals();
        // my-role, my-role2, Roles, user1
        assertEquals(4, principals.size());
        assertTrue(loginModule.logout());
        // make sure principals are cleared
        assertEquals(0, principals.size());
    }
}

