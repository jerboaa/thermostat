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

package com.redhat.thermostat.web.server.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.junit.Test;

public class RolePrincipalTest {
    
    @Test
    public void testEmptyRoles() {
        RolePrincipal principal = new RolePrincipal("RolesContainer");
        assertEquals(false, principal.members().hasMoreElements());
        assertEquals(false, principal.isMember(new RolePrincipal("not-there")));
        assertEquals(false, principal.isMember(new UserPrincipal("user also not there")));
        assertEquals("RolesContainer", principal.getName());
    }
    
    @Test
    public void canRemoveMembers() {
        RolePrincipal principal = new RolePrincipal("Testing");
        assertFalse(principal.removeMember(new RolePrincipal("other")));
        assertFalse(principal.removeMember(new UserPrincipal("testuser")));
        RolePrincipal firstRoleMember = new RolePrincipal("first");
        RolePrincipal secondRoleMember = new RolePrincipal("second");
        RolePrincipal thirdRoleMember = new RolePrincipal("third");
        principal.addMember(firstRoleMember);
        assertTrue("first member of Testing", principal.removeMember(firstRoleMember));
        principal.addMember(secondRoleMember);
        principal.addMember(thirdRoleMember);
        assertTrue("third member of Testing", principal.removeMember(thirdRoleMember));
        List<String> members = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Enumeration<Principal> roles = (Enumeration<Principal>) principal.members();
        while (roles.hasMoreElements()) {
            members.add(roles.nextElement().getName());
        }
        assertEquals(1, members.size());
        assertEquals("second", members.get(0));
    }
    
    @Test
    public void rolePrincipalDoesNotAllowNonGroupsToBeAdded() {
        RolePrincipal role = new RolePrincipal("test");
        try {
            role.addMember(new UserPrincipal("something"));
            fail("UserPrincipal not a Group, should not come here!");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }
    
    @Test( expected = NullPointerException.class )
    public void nullName() {
        new RolePrincipal(null);
    }
    
    @Test
    public void isMemberWorksForNestedGroups() {
        RolePrincipal principal = new RolePrincipal("Roles");
        RolePrincipal otherRole = new RolePrincipal("Nested 1");
        RolePrincipal thirdRole = new RolePrincipal("find-me");
        RolePrincipal notExistingRole = new RolePrincipal("notthere");
        otherRole.addMember(thirdRole);
        principal.addMember(otherRole);
        assertEquals("find-me member of Nested 1 which is member of Roles", true, principal.isMember(thirdRole));
        assertEquals(false, principal.isMember(notExistingRole));
        assertTrue(otherRole.removeMember(thirdRole));
        assertEquals("find-me no longer member of Nested 1 (which is still a member of Roles)", false, principal.isMember(thirdRole));
    }
    
    @Test
    public void testEquals() {
        UserPrincipal user = new UserPrincipal("test");
        RolePrincipal role = new RolePrincipal("test");
        assertFalse("Roles and users must not be equal", role.equals(user));
        assertTrue(role.equals(role));
    }
}
