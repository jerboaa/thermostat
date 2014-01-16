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

package com.redhat.thermostat.web.server.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.storage.query.BinaryLogicalExpression;
import com.redhat.thermostat.storage.query.BinarySetMembershipExpression;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.web.server.auth.FilterResult.ResultType;

public class VmUsernameFilterTest {
    
    @Test
    public void testReadAll() {
        Set<BasicRole> roles = new HashSet<>();
        RolePrincipal vmUsernameReadAll = new RolePrincipal(Roles.GRANT_VMS_READ_BY_USERNAME_ALL);
        roles.add(vmUsernameReadAll);
        
        VmUsernameFilter<?> filter = new VmUsernameFilter<>(roles);
        FilterResult result = filter.applyFilter(null, null, null);
        assertEquals(ResultType.ALL, result.getType());
        assertEquals(null, result.getFilterExpression());
    }
    
    @Test
    public void testReadAllWithParentExpression() {
        Set<BasicRole> roles = new HashSet<>();
        RolePrincipal vmUsernameReadAll = new RolePrincipal(Roles.GRANT_VMS_READ_BY_USERNAME_ALL);
        roles.add(vmUsernameReadAll);
        
        ExpressionFactory factory = new ExpressionFactory();
        Expression parentExpression = factory.equalTo(Key.AGENT_ID, "testKey");
        VmUsernameFilter<?> filter = new VmUsernameFilter<>(roles);
        FilterResult result = filter.applyFilter(null, null, parentExpression);
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertEquals(parentExpression, result.getFilterExpression());
    }
    
    @Test
    public void addsVmUsernameInQueryForVmInfo() {
        performVmInfoTest(new DescriptorMetadata());
    }
    
    @Test
    public void testMetadataNull() {
        performVmInfoTest(null);
    }
    
    private void performVmInfoTest(DescriptorMetadata metadata) {
        String testUsername = "fooBar";
        Set<BasicRole> roles = new HashSet<>();
        RolePrincipal vmUsernameRole = new RolePrincipal(VmUsernameFilter.VMS_BY_USERNAME_GRANT_ROLE_PREFIX + testUsername);
        roles.add(vmUsernameRole);
        
        StatementDescriptor<VmInfo> desc = new StatementDescriptor<>(VmInfoDAO.vmInfoCategory, "QUERY " + VmInfoDAO.vmInfoCategory.getName());
        
        Set<String> usernames = new HashSet<>();
        usernames.add(testUsername);
        Expression expected = new ExpressionFactory().in(VmInfoDAO.usernameKey, usernames, String.class);
        VmUsernameFilter<VmInfo> filter = new VmUsernameFilter<>(roles);
        FilterResult result = filter.applyFilter(desc, metadata, null);
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertNotNull(result.getFilterExpression());
        Expression actual = result.getFilterExpression();
        assertTrue(actual instanceof BinarySetMembershipExpression);
        assertEquals(expected, actual);
    }
    
    @Test
    public void addsVmUsernameInQueryForVmInfoAndParentExpression() {
        String testUsername = "fooBar";
        Set<BasicRole> roles = new HashSet<>();
        RolePrincipal vmUsernameRole = new RolePrincipal(VmUsernameFilter.VMS_BY_USERNAME_GRANT_ROLE_PREFIX + testUsername);
        roles.add(vmUsernameRole);
        
        DescriptorMetadata metadata = new DescriptorMetadata();
        StatementDescriptor<VmInfo> desc = new StatementDescriptor<>(VmInfoDAO.vmInfoCategory, "QUERY " + VmInfoDAO.vmInfoCategory.getName());
        
        Set<String> usernames = new HashSet<>();
        usernames.add(testUsername);
        ExpressionFactory factory = new ExpressionFactory();
        Expression parentExpression = factory.equalTo(Key.AGENT_ID, "testKey");
        Expression expectedIn = factory.in(VmInfoDAO.usernameKey, usernames, String.class);
        Expression expected = factory.and(parentExpression, expectedIn);
        VmUsernameFilter<VmInfo> filter = new VmUsernameFilter<>(roles);
        FilterResult result = filter.applyFilter(desc, metadata, parentExpression);
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertNotNull(result.getFilterExpression());
        Expression actual = result.getFilterExpression();
        assertTrue(actual instanceof BinaryLogicalExpression);
        assertEquals(expected, actual);
    }
    
    @Test
    public void byPassesFilterForUnrelatedQuery() {
        Set<BasicRole> roles = new HashSet<>();
        
        DescriptorMetadata metadata = new DescriptorMetadata();
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, "QUERY " + AgentInfoDAO.CATEGORY.getName());
        
        VmUsernameFilter<AgentInformation> filter = new VmUsernameFilter<>(roles);
        FilterResult result = filter.applyFilter(desc, metadata, null);
        assertEquals(ResultType.ALL, result.getType());
        assertNull(result.getFilterExpression());
    }
    
    @Test
    public void byPassesFilterForUnrelatedQueryAndParentExpression() {
        Set<BasicRole> roles = new HashSet<>();
        
        DescriptorMetadata metadata = new DescriptorMetadata();
        StatementDescriptor<AgentInformation> desc = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, "QUERY " + AgentInfoDAO.CATEGORY.getName());
        
        ExpressionFactory factory = new ExpressionFactory();
        Expression parentExpression = factory.equalTo(Key.AGENT_ID, "testKey");
        VmUsernameFilter<AgentInformation> filter = new VmUsernameFilter<>(roles);
        FilterResult result = filter.applyFilter(desc, metadata, parentExpression);
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertNotNull(result.getFilterExpression());
        assertEquals(parentExpression, result.getFilterExpression());
    }
    
}

