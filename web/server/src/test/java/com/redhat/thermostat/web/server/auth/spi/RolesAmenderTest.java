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
import static org.mockito.Mockito.mock;

import java.net.URL;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.web.server.auth.BasicRole;
import com.redhat.thermostat.web.server.auth.RolePrincipal;
import com.redhat.thermostat.web.server.auth.spi.RolesAmender.RolesInfo;

public class RolesAmenderTest {

    private RolesAmender rolesAmender;
    private String validFile;
    private Set<Object> validUsers;
    
    @Before
    public void setup() {
        URL testFile = this.getClass().getResource("/test_roles.properties");
        validFile = testFile.getFile();
        Set<Object> users = new HashSet<>();
        users.add("user1");
        users.add("user2");
        users.add("someuser");
        users.add("someotheruser");
        validUsers = Collections.unmodifiableSet(users);
    }
    
    @After
    public void tearDown() {
        rolesAmender = null;
        validFile = null;
    }

    @Test
    public void testGetRolesDoesntLeak() {
        rolesAmender = new RolesAmender(validFile, validUsers);
        Set<BasicRole> roles = rolesAmender.getRoles("foo");
        assertEquals(roles, Collections.emptySet());
        BasicRole fooRole = mock(BasicRole.class);
        roles.add(fooRole);
        assertEquals(rolesAmender.getRoles("foo"), Collections.emptySet());
    }

    @Test
    public void canParseRoles() {
        rolesAmender = new RolesAmender(validFile, validUsers);
        Set<BasicRole> roles = rolesAmender.getRoles("user1");
        assertEquals(2, roles.size());
        Map<String, Boolean> userRolesMap = new HashMap<>();
        for (BasicRole role : roles) {
            // don't expect role nesting
            assertTrue(role instanceof RolePrincipal);
            assertFalse(role.members().hasMoreElements());
            userRolesMap.put(role.getName(), true);
        }
        assertTrue(userRolesMap.containsKey("my-role"));
        assertTrue(userRolesMap.containsKey("my-role2"));
        roles = rolesAmender.getRoles("user2");
        // new-role, role1, other-role
        assertEquals(3, roles.size());
        for (BasicRole role : roles) {
            assertTrue(role instanceof RolePrincipal);
            if (role.getName().equals("role1")) {
                // nested role
                int count = 0;
                @SuppressWarnings("rawtypes")
                Enumeration members = role.members();
                while (members.hasMoreElements()) {
                    count++;
                    assertEquals("other-role", ((Principal) members.nextElement()).getName());
                }
                assertEquals(1, count);
            }
            if (role.getName().equals("new-role")) {
                // not nested
                assertFalse(role.members().hasMoreElements());
            }
        }
        assertTrue(roles.contains(new RolePrincipal("other-role")));
        // role1 is not a user
        roles = rolesAmender.getRoles("role1");
        // expect empty
        assertEquals(0, roles.size());
    }
    
    @Test
    public void canParseRecursiveRolesWithMultipleMemberships() {
        rolesAmender = new RolesAmender(validFile, validUsers);
        Set<BasicRole> roles = rolesAmender.getRoles("user2");
        // new-role, role1, other-role
        assertEquals(3, roles.size());
        for (BasicRole role : roles) {
            assertTrue(role instanceof RolePrincipal);
            if (role.getName().equals("role1")) {
                // nested role
                int count = 0;
                @SuppressWarnings("rawtypes")
                Enumeration members = role.members();
                while (members.hasMoreElements()) {
                    count++;
                    assertEquals("other-role", ((Principal) members.nextElement()).getName());
                }
                assertEquals(1, count);
            }
            if (role.getName().equals("new-role")) {
                // not nested
                assertFalse(role.members().hasMoreElements());
            }
        }
        assertTrue(roles.contains(new RolePrincipal("other-role")));
        // role1 is not a user
        roles = rolesAmender.getRoles("role1");
        // expect empty
        assertEquals(0, roles.size());
        // test another user which is member of the recursive role
        roles = rolesAmender.getRoles("someuser");
        assertEquals(
                "Expected someuser to be a member of 'testing', 'role1' and 'other-role'",
                3, roles.size());
        assertTrue(roles.contains(new RolePrincipal("other-role")));
        assertTrue(roles.contains(new RolePrincipal("testing")));
        assertTrue(roles.contains(new RolePrincipal("role1")));
        // and yet another user which is part of a recursive role
        roles = rolesAmender.getRoles("someotheruser");
        assertEquals(
                "Expected someotheruser to be a member of 'role1' and 'other-role'",
                2, roles.size());
        assertTrue(roles.contains(new RolePrincipal("other-role")));
        assertTrue(roles.contains(new RolePrincipal("role1")));
    }
    
    @Test
    public void canParseRolesWithMoreUsersThanUsedInRoles() {
        Set<Object> users = new HashSet<>();
        users.add("user1");
        users.add("user2");
        users.add("someuser");
        users.add("someotheruser");
        users.add("user-with-no-roles");
        validUsers = Collections.unmodifiableSet(users);
        try {
            rolesAmender = new RolesAmender(validFile, validUsers);
        } catch (Exception e) {
            fail("should be able to parse roles");
        }
        Set<BasicRole> roles = rolesAmender.getRoles("user-with-no-roles");
        assertEquals(0, roles.size());
    }
    
    @Test
    public void parseFailsIfUserInRecursiveRole() {
        String brokenFile = this.getClass().getResource("/broken_test_roles.properties").getFile();
        try {
            rolesAmender = new RolesAmender(brokenFile, validUsers);
            fail("Should not parse");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
            assertEquals("Failed to parse roles", e.getMessage());
        }
    }
    
    @Test
    public void parseFailsIfNotAnyUserMemberOfRecursiveRole() {
        String brokenFile = this.getClass().getResource("/broken_test_roles2.properties").getFile();
        try {
            rolesAmender = new RolesAmender(brokenFile, validUsers);
            fail("Should not parse");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
            assertEquals("Failed to parse roles", e.getMessage());
        }
    }

    @Test
    public void testInit() {
        try {
            new RolesAmender(null);
            fail("THERMOSTAT_HOME not set, should have failed!");
        } catch (InvalidConfigurationException e) {
            // pass
            assertTrue(e.getMessage().contains("THERMOSTAT_HOME"));
        }
    }
    
    @Test
    public void testInitWithMissingFile() {
        try {
            new RolesAmender("file/which/does/not/exist", validUsers);
        } catch (InvalidConfigurationException e) {
            // pass
            assertEquals("Failed to load roles from properties", e.getMessage());
        }
    }
    
    @Test(expected = NullPointerException.class)
    public void testInitNull() {
        new RolesAmender("not-important", null);
    }
    
    @Test
    public void rolesInfoTest() {
        RolePrincipal p = new RolePrincipal("test-role");
        RolesInfo info = new RolesInfo(p);
        assertEquals("test-role", info.getRole().getName());
        assertEquals(0, info.getMemberUsers().size());
        Set<String> roleMembers = info.getMemberUsers();
        roleMembers.add("testuser");
        assertEquals(1, info.getMemberUsers().size());
        assertEquals(true, info.getMemberUsers().contains("testuser"));
        roleMembers.add("testuser");
        assertEquals(1, info.getMemberUsers().size());
    }
}

