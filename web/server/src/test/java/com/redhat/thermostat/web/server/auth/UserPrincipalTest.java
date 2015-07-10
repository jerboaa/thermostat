/*
 * Copyright 2012-2015 Red Hat, Inc.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.web.server.auth.FilterResult.ResultType;

public class UserPrincipalTest {
    
    private static class FooPojo implements Pojo {
        // empty dummy pojo.
    }

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
    public void setRolesDefensiveCopy() {
        UserPrincipal p = new UserPrincipal("testuser");
        Set<BasicRole> roles = new HashSet<>();
        BasicRole fooRole = mock(BasicRole.class);
        roles.add(fooRole);
        p.setRoles(roles);
        BasicRole barRole = mock(BasicRole.class);
        roles.add(barRole);
        assertFalse(p.getRoles().contains(barRole));
    }

    @Test
    public void getRolesDoesntLeak() {
        UserPrincipal p = new UserPrincipal("testuser");
        Set<BasicRole> roles = p.getRoles();
        BasicRole fooRole = mock(BasicRole.class);
        roles.add(fooRole);
        assertFalse(p.getRoles().contains(fooRole));
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
    
    @Test
    public void readAllRoleBypassesReadFilters() {
        Set<BasicRole> roles = new HashSet<>();
        RolePrincipal readEverything = new RolePrincipal(Roles.GRANT_READ_ALL);
        roles.add(readEverything);
        SimplePrincipal testMe = new SimplePrincipal("test me");
        testMe.setRoles(roles);
        FilterResult result = testMe.getReadFilter(null);
        assertEquals(ResultType.ALL, result.getType());
        assertNull(result.getFilterExpression());
    }
    
    @Test
    public void testEntireFilterChainNoSpecificAgentIdVmId() {
        String agentId = "someAgentID";
        String vmId = "someVmID";
        String vmUsername = "someUser";
        RolePrincipal readAllHosts = new RolePrincipal(Roles.GRANT_HOSTS_READ_ALL);
        RolePrincipal readAgentId = new RolePrincipal(AgentIdFilter.AGENTS_BY_AGENT_ID_GRANT_ROLE_PREFIX + agentId);
        RolePrincipal readVmId = new RolePrincipal(VmIdFilter.VMS_BY_VM_ID_GRANT_ROLE_PREFIX + vmId);
        RolePrincipal readVmUsername = new RolePrincipal(VmUsernameFilter.VMS_BY_USERNAME_GRANT_ROLE_PREFIX + vmUsername);
        
        Set<BasicRole> roles = new HashSet<>();
        roles.add(readAllHosts);
        roles.add(readAgentId);
        roles.add(readVmUsername);
        roles.add(readVmId);
        
        assertFalse(roles.contains(AgentIdFilter.GRANT_AGENTS_READ_ALL));
        assertTrue(roles.contains(HostnameFilter.GRANT_HOSTS_READ_ALL));
        assertFalse(roles.contains(VmIdFilter.GRANT_VMS_BY_ID_READ_ALL));
        assertFalse(roles.contains(VmUsernameFilter.GRANT_VMS_USERNAME_READ_ALL));
        SimplePrincipal testMe = new SimplePrincipal("test me");
        testMe.setRoles(roles);
        StatementDescriptor<VmInfo> desc = new StatementDescriptor<>(VmInfoDAO.vmInfoCategory, "QUERY " + VmInfoDAO.vmInfoCategory.getName());
        
        // should pass through agentId -> hostname -> vmId -> vmUsername filters
        FilterResult result = testMe.getReadFilter(desc);
        
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertNotNull(result.getFilterExpression());
        Expression actual = result.getFilterExpression();
        ExpressionFactory factory = new ExpressionFactory();
        Set<String> agentIds = new HashSet<>();
        agentIds.add(agentId);
        Set<String> vmIds = new HashSet<>();
        vmIds.add(vmId);
        Expression agentInExpr = factory.in(Key.AGENT_ID, agentIds, String.class);
        Expression vmIdInExpr = factory.in(Key.VM_ID, vmIds, String.class);
        Set<String> vmUsernames = new HashSet<>();
        vmUsernames.add(vmUsername);
        Expression vmIdUsernameInExpr = factory.in(VmInfoDAO.usernameKey, vmUsernames, String.class);
        Expression expected = factory.and(factory.and(agentInExpr, vmIdInExpr), vmIdUsernameInExpr);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testEntireFilterChainSpecificAgentIdVmId() {
        String agentId = "someAgentID";
        String vmId = "someVmID";
        RolePrincipal readAgentId = new RolePrincipal(AgentIdFilter.AGENTS_BY_AGENT_ID_GRANT_ROLE_PREFIX + agentId);
        RolePrincipal readVmId = new RolePrincipal(VmIdFilter.VMS_BY_VM_ID_GRANT_ROLE_PREFIX + vmId);
        
        Set<BasicRole> roles = new HashSet<>();
        roles.add(HostnameFilter.GRANT_HOSTS_READ_ALL);
        roles.add(readAgentId);
        roles.add(VmUsernameFilter.GRANT_VMS_USERNAME_READ_ALL);
        roles.add(readVmId);
        
        assertFalse(roles.contains(AgentIdFilter.GRANT_AGENTS_READ_ALL));
        assertTrue(roles.contains(HostnameFilter.GRANT_HOSTS_READ_ALL));
        assertFalse(roles.contains(VmIdFilter.GRANT_VMS_BY_ID_READ_ALL));
        assertTrue(roles.contains(VmUsernameFilter.GRANT_VMS_USERNAME_READ_ALL));
        SimplePrincipal testMe = new SimplePrincipal("test me");
        testMe.setRoles(roles);
        
        // want for the agent/vm id key to be present in category
        Key<?>[] keys = new Key[] {
             Key.AGENT_ID, Key.VM_ID
        };
        Category<FooPojo> agentAndVmIdCat = new Category<>("agentAndVmIdCat", FooPojo.class, keys);
        StatementDescriptor<FooPojo> desc = new StatementDescriptor<>(agentAndVmIdCat, "QUERY agentAndVmIdCat");
        
        // should pass through agentId -> hostname -> vmId -> vmUsername filters
        FilterResult result = testMe.getReadFilter(desc);
        
        // should return all, since ACL allows specific agentId/vmIds
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        Expression expected = getExpectedAgentIdVmIdExpression(agentId, vmId);
        assertEquals(expected, result.getFilterExpression());
    }
    
    private Expression getExpectedAgentIdVmIdExpression(String agentId,
            String vmId) {
        ExpressionFactory factory = new ExpressionFactory();
        Set<String> agentIdSet = new HashSet<>();
        agentIdSet.add(agentId);
        Expression agentIdExp = factory.in(Key.AGENT_ID, agentIdSet, String.class);
        Set<String> vmIdSet = new HashSet<>();
        vmIdSet.add(vmId);
        Expression vmIdExp = factory.in(Key.VM_ID, vmIdSet, String.class);
        return factory.and(agentIdExp, vmIdExp);
    }

    @Test
    public void testEntireFilterChainSpecificAgentIdVmIdPlusHostname() {
        String agentId = "someAgentID";
        String vmId = "someVmID";
        String hostname = "somehost.example.com";
        RolePrincipal readAgentId = new RolePrincipal(AgentIdFilter.AGENTS_BY_AGENT_ID_GRANT_ROLE_PREFIX + agentId);
        RolePrincipal readVmId = new RolePrincipal(VmIdFilter.VMS_BY_VM_ID_GRANT_ROLE_PREFIX + vmId);
        RolePrincipal readHostname = new RolePrincipal(HostnameFilter.HOSTS_BY_HOSTNAME_GRANT_ROLE_PREFIX + hostname);
        
        Set<BasicRole> roles = new HashSet<>();
        roles.add(readHostname);
        roles.add(readAgentId);
        roles.add(VmUsernameFilter.GRANT_VMS_USERNAME_READ_ALL);
        roles.add(readVmId);
        
        assertFalse(roles.contains(AgentIdFilter.GRANT_AGENTS_READ_ALL));
        assertFalse(roles.contains(HostnameFilter.GRANT_HOSTS_READ_ALL));
        assertFalse(roles.contains(VmIdFilter.GRANT_VMS_BY_ID_READ_ALL));
        assertTrue(roles.contains(VmUsernameFilter.GRANT_VMS_USERNAME_READ_ALL));
        SimplePrincipal testMe = new SimplePrincipal("test me");
        testMe.setRoles(roles);
        StatementDescriptor<HostInfo> desc = new StatementDescriptor<>(HostInfoDAO.hostInfoCategory, "QUERY " + HostInfoDAO.hostInfoCategory.getName());
        
        // should pass through agentId -> hostname -> vmId -> vmUsername filters
        FilterResult result = testMe.getReadFilter(desc);
        
        // should return query expression in order to allow only specific
        // hostname and specific agent id. Since the category does not
        // include vmId, nothing is added by the vm id filter.
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertNotNull(result.getFilterExpression());
        Expression actual = result.getFilterExpression();
        ExpressionFactory factory = new ExpressionFactory();
        Set<String> agentIdSet = new HashSet<>();
        agentIdSet.add(agentId);
        Expression agentIdExp = factory.in(Key.AGENT_ID, agentIdSet, String.class);
        Set<String> hostnames = new HashSet<>();
        hostnames.add(hostname);
        Expression hostnameExp = factory.in(HostInfoDAO.hostNameKey, hostnames, String.class);
        Expression expected = factory.and(agentIdExp, hostnameExp);
        assertEquals(expected, actual);
    }
    
    @Test
    public void testEntireFilterChainSpecificAgentIdVmIdPlusVmUsername() {
        String agentId = "someAgentID";
        String vmId = "someVmID";
        String vmUserame = "someOwningUser";
        RolePrincipal readAgentId = new RolePrincipal(AgentIdFilter.AGENTS_BY_AGENT_ID_GRANT_ROLE_PREFIX + agentId);
        RolePrincipal readVmId = new RolePrincipal(VmIdFilter.VMS_BY_VM_ID_GRANT_ROLE_PREFIX + vmId);
        RolePrincipal readVmUsername = new RolePrincipal(VmUsernameFilter.VMS_BY_USERNAME_GRANT_ROLE_PREFIX + vmUserame);
        
        Set<BasicRole> roles = new HashSet<>();
        roles.add(readVmUsername);
        roles.add(readAgentId);
        roles.add(HostnameFilter.GRANT_HOSTS_READ_ALL);
        roles.add(readVmId);
        
        assertFalse(roles.contains(AgentIdFilter.GRANT_AGENTS_READ_ALL));
        assertTrue(roles.contains(HostnameFilter.GRANT_HOSTS_READ_ALL));
        assertFalse(roles.contains(VmIdFilter.GRANT_VMS_BY_ID_READ_ALL));
        assertFalse(roles.contains(VmUsernameFilter.GRANT_VMS_USERNAME_READ_ALL));
        SimplePrincipal testMe = new SimplePrincipal("test me");
        testMe.setRoles(roles);
        StatementDescriptor<VmInfo> desc = new StatementDescriptor<>(VmInfoDAO.vmInfoCategory, "QUERY " + VmInfoDAO.vmInfoCategory.getName());
        
        // should pass through agentId -> hostname -> vmId -> vmUsername filters
        FilterResult result = testMe.getReadFilter(desc);
        
        // should return query expression in order to allow only specific
        // owning vm username.
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertNotNull(result.getFilterExpression());
        Expression actual = result.getFilterExpression();
        ExpressionFactory factory = new ExpressionFactory();
        Set<String> usernames = new HashSet<>();
        usernames.add(vmUserame);
        Expression userNameExp = factory.in(VmInfoDAO.usernameKey, usernames, String.class);
        Set<String> agentIdSet = new HashSet<>();
        agentIdSet.add(agentId);
        Expression agentIdExp = factory.in(Key.AGENT_ID, agentIdSet, String.class);
        Set<String> vmIdSet = new HashSet<>();
        vmIdSet.add(vmId);
        Expression vmIdExp = factory.in(Key.VM_ID, vmIdSet, String.class);
        Expression lhs = factory.and(agentIdExp, vmIdExp);
        Expression expected = factory.and(lhs, userNameExp);
        assertEquals(expected, actual);
    }
    
    
    @SuppressWarnings("serial")
    private static class SimplePrincipal extends UserPrincipal {
        
        public SimplePrincipal(String name) {
            super(name);
        }
    }
}

