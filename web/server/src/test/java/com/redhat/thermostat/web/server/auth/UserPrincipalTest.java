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
import static org.mockito.Mockito.mock;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class UserPrincipalTest {

    @Test(expected = NullPointerException.class)
    public void testConstructor() {
        new UserPrincipal(null);
    }
    
    @Test
    public void getName() {
        UserPrincipal p = new UserPrincipal("testing");
        assertEquals("testing", p.getName());
    }
    
    @Test
    public void canSetRoles() {
        UserPrincipal p = new UserPrincipal("superuser");
        try {
            p.setRoles(null);
            fail("null roles not allowed");
        } catch (NullPointerException e) {
            // pass
        }
        Set<BasicRole> roles = new HashSet<>();
        BasicRole role = mock(BasicRole.class);
        roles.add(role);
        p.setRoles(roles);
        assertEquals(1, p.getRoles().size());
    }
    
    @Test
    public void testEquals() {
        UserPrincipal p = new UserPrincipal("testuser");
        assertTrue(p.equals(p));
        SimplePrincipal p2 = new SimplePrincipal("testuser");
        assertTrue(p2.equals(p));
        assertTrue(p.equals(p2));
        SimplePrincipal p3 = new SimplePrincipal("Tester");
        assertFalse(p2.equals(p3));
        assertFalse(p.equals(p3));
        Principal principal = new Principal() {

            @Override
            public String getName() {
                return "testuser";
            }
            
        };
        assertTrue(p.equals(principal));
    }
    
    @SuppressWarnings("serial")
    private static class SimplePrincipal extends UserPrincipal {
        
        public SimplePrincipal(String name) {
            super(name);
        }
    }
}
