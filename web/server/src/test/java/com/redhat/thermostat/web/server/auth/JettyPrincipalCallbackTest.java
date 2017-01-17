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

package com.redhat.thermostat.web.server.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.eclipse.jetty.jaas.JAASUserPrincipal;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.web.server.containers.ContainerVersion;
import com.redhat.thermostat.web.server.containers.ServletContainerInfo;

public class JettyPrincipalCallbackTest {
    
    private ContainerVersion jetty9Version;
    private ContainerVersion jetty8Version;
    private ContainerVersion unknownVersion;

    @Before
    public void setup() {
        jetty9Version = mock(ContainerVersion.class);
        when(jetty9Version.getMajor()).thenReturn(9);
        jetty8Version = mock(ContainerVersion.class);
        when(jetty8Version.getMajor()).thenReturn(8);
        unknownVersion = mock(ContainerVersion.class);
        when(unknownVersion.getMajor()).thenReturn(100);
    }
    
    @Test
    public void canGetUserPrincipalJetty9() {
        ServletContainerInfo info = mock(ServletContainerInfo.class);
        when(info.getContainerVersion()).thenReturn(jetty9Version);
        Subject subject = new Subject();
        String principalName = "foo";
        UserPrincipal userPrincipal = new UserPrincipal(principalName);
        Set<Principal> principals = subject.getPrincipals();
        principals.add(userPrincipal);
        LoginContext loginContext = mock(LoginContext.class);
        Principal principal = new JAASUserPrincipal(principalName, subject, loginContext);
        
        JettyPrincipalCallback callback = new JettyPrincipalCallback(info);
        UserPrincipal other = callback.getUserPrincipal(principal);
        assertSame(userPrincipal, other);
    }
    
    @Test
    public void canGetUserPrincipalJetty8() {
        ServletContainerInfo info = mock(ServletContainerInfo.class);
        when(info.getContainerVersion()).thenReturn(jetty8Version);
        Subject subject = new Subject();
        String principalName = "foo";
        UserPrincipal userPrincipal = new UserPrincipal(principalName);
        Set<Principal> principals = subject.getPrincipals();
        principals.add(userPrincipal);
        LoginContext loginContext = mock(LoginContext.class);
        // use the jetty 8 principal
        Principal principal = new org.eclipse.jetty.plus.jaas.JAASUserPrincipal(principalName, subject, loginContext);
        
        JettyPrincipalCallback callback = new JettyPrincipalCallback(info);
        UserPrincipal other = callback.getUserPrincipal(principal);
        assertSame(userPrincipal, other);
    }
    
    @Test
    public void failsOnUnknownPrincipal() {
        ServletContainerInfo info = mock(ServletContainerInfo.class);
        when(info.getContainerVersion()).thenReturn(unknownVersion);
        Principal principal = mock(Principal.class);
        JettyPrincipalCallback callback = new JettyPrincipalCallback(info);
        try {
            callback.getUserPrincipal(principal);
            fail("should have failed to retrieve user principal from non-jetty principal");
        } catch (IllegalStateException e) {
            // pass
        }
    }
    
    @Test
    public void testNoUserPrincipalInSetOfPrincipals() {
        ServletContainerInfo info = mock(ServletContainerInfo.class);
        when(info.getContainerVersion()).thenReturn(jetty9Version);
        Subject subject = new Subject();
        String principalName = "foo";
        LoginContext loginContext = mock(LoginContext.class);
        Principal principal = new JAASUserPrincipal(principalName, subject, loginContext);
        
        JettyPrincipalCallback callback = new JettyPrincipalCallback(info);
        try {
            callback.getUserPrincipal(principal);
            fail("should have thrown ISE, since no thermostat UserPrincipal is in the principal set");
        } catch (IllegalStateException e) {
            assertEquals("Number of thermostat user principals must be exactly 1!", e.getMessage());
        }
    }
    
    @Test
    public void testMoreThanOneUserPrincipalsInSetOfPrincipals() {
        ServletContainerInfo info = mock(ServletContainerInfo.class);
        when(info.getContainerVersion()).thenReturn(jetty9Version);
        Subject subject = new Subject();
        String principalName = "foo";
        UserPrincipal userPrincipal = new UserPrincipal(principalName);
        Set<Principal> principals = subject.getPrincipals();
        UserPrincipal userPrincipal2 = new UserPrincipal("other");
        principals.add(userPrincipal);
        principals.add(userPrincipal2);
        LoginContext loginContext = mock(LoginContext.class);
        Principal principal = new JAASUserPrincipal(principalName, subject, loginContext);
        
        JettyPrincipalCallback callback = new JettyPrincipalCallback(info);
        try {
            callback.getUserPrincipal(principal);
            fail("should have thrown ISE, since 2 thermostat UserPrincipals are in the principal set");
        } catch (IllegalStateException e) {
            assertEquals("Number of thermostat user principals must be exactly 1!", e.getMessage());
        }
    }
}
