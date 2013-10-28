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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.auth.DescriptorMetadata;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.BinaryLogicalExpression;
import com.redhat.thermostat.storage.query.BinarySetMembershipExpression;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;
import com.redhat.thermostat.web.server.auth.FilterResult.ResultType;

public class VmIdFilterTest {
    
    private static class FooPojo implements Pojo {
        // Dummy class for testing
    }
    
    private static final Category<FooPojo> TEST_NON_NULL_CATEGORY = new Category<>("foo-vmid-filter-test", FooPojo.class, Key.VM_ID);
    private static final Category<FooPojo> TEST_NULL_CATEGORY = new Category<>("foo-vmid-filter-test-null", FooPojo.class);
    /**
     * A query descriptor which will return a non-null Key for the "vmId" name.
     */
    private static final StatementDescriptor<FooPojo> TEST_DESC_NON_NULL_VM_ID = new StatementDescriptor<>(TEST_NON_NULL_CATEGORY, "QUERY foo");
    /**
     * A query descriptor which will return a null Key for the "vmId" name.
     */
    private static final StatementDescriptor<FooPojo> TEST_DESC_NULL_VM_ID = new StatementDescriptor<>(TEST_NULL_CATEGORY, "QUERY foo-null");

    @Test
    public void testReadAll() {
        Set<BasicRole> roles = new HashSet<>();
        RolePrincipal vmIdReadAll = new RolePrincipal(Roles.GRANT_VMS_READ_BY_VM_ID_ALL);
        roles.add(vmIdReadAll);
        
        VmIdFilter<?> filter = new VmIdFilter<>(roles);
        FilterResult result = filter.applyFilter(null, null, null);
        assertEquals(ResultType.ALL, result.getType());
        assertEquals(null, result.getFilterExpression());
    }
    
    @Test
    public void testReadAllAndParentExpression() {
        Set<BasicRole> roles = new HashSet<>();
        RolePrincipal vmIdReadAll = new RolePrincipal(Roles.GRANT_VMS_READ_BY_VM_ID_ALL);
        roles.add(vmIdReadAll);
        
        ExpressionFactory factory = new ExpressionFactory();
        Expression parentExpression = factory.equalTo(Key.AGENT_ID, "testKey");
        VmIdFilter<?> filter = new VmIdFilter<>(roles);
        FilterResult result = filter.applyFilter(null, null, parentExpression);
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertEquals(parentExpression, result.getFilterExpression());
    }
    
    @Test
    public void addsVmIdInQuery() {
        String vmId = UUID.randomUUID().toString();
        Set<BasicRole> roles = new HashSet<>();
        RolePrincipal vmIdRole = new RolePrincipal(VmIdFilter.VMS_BY_VM_ID_GRANT_ROLE_PREFIX + vmId);
        roles.add(vmIdRole);
        DescriptorMetadata metadata = new DescriptorMetadata();
        VmIdFilter<FooPojo> filter = new VmIdFilter<>(roles);
        FilterResult result = filter.applyFilter(TEST_DESC_NON_NULL_VM_ID, metadata, null);
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertNotNull(result.getFilterExpression());
        Expression actual = result.getFilterExpression();
        assertTrue(actual instanceof BinarySetMembershipExpression);
        Set<String> vmIdSet = new HashSet<>();
        vmIdSet.add(vmId);
        Expression expected = new ExpressionFactory().in(Key.VM_ID, vmIdSet, String.class);
        assertEquals(expected, actual);
    }
    
    @Test
    public void addsVmIdInQueryAndParentExpression() {
        String vmId = UUID.randomUUID().toString();
        Set<BasicRole> roles = new HashSet<>();
        RolePrincipal vmIdRole = new RolePrincipal(VmIdFilter.VMS_BY_VM_ID_GRANT_ROLE_PREFIX + vmId);
        roles.add(vmIdRole);
        DescriptorMetadata metadata = new DescriptorMetadata();
        VmIdFilter<FooPojo> filter = new VmIdFilter<>(roles);
        ExpressionFactory factory = new ExpressionFactory();
        Expression parentExpression = factory.equalTo(Key.AGENT_ID, "testKey");
        FilterResult result = filter.applyFilter(TEST_DESC_NON_NULL_VM_ID, metadata, parentExpression);
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertNotNull(result.getFilterExpression());
        Expression actual = result.getFilterExpression();
        assertTrue(actual instanceof BinaryLogicalExpression);
        Set<String> vmIdSet = new HashSet<>();
        vmIdSet.add(vmId);
        
        Expression expectedIn = factory.in(Key.VM_ID, vmIdSet, String.class);
        Expression expected = factory.and(parentExpression, expectedIn);
        assertEquals(expected, actual);
    }
    
    @Test
    public void addsVmIdInQuery2() {
        String vmId = UUID.randomUUID().toString();
        String vmId2 = UUID.randomUUID().toString();
        Set<BasicRole> roles = new HashSet<>();
        RolePrincipal vm1Role = new RolePrincipal(VmIdFilter.VMS_BY_VM_ID_GRANT_ROLE_PREFIX + vmId);
        RolePrincipal vm2Role = new RolePrincipal(VmIdFilter.VMS_BY_VM_ID_GRANT_ROLE_PREFIX + vmId2);
        roles.add(vm1Role);
        roles.add(vm2Role);
        DescriptorMetadata metadata = new DescriptorMetadata();
        VmIdFilter<FooPojo> filter = new VmIdFilter<>(roles);
        FilterResult result = filter.applyFilter(TEST_DESC_NON_NULL_VM_ID, metadata, null);
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertNotNull(result.getFilterExpression());
        Expression actual = result.getFilterExpression();
        assertTrue(actual instanceof BinarySetMembershipExpression);
        Set<String> vmIdSet = new HashSet<>();
        vmIdSet.add(vmId);
        vmIdSet.add(vmId2);
        Expression expected = new ExpressionFactory().in(Key.VM_ID, vmIdSet, String.class);
        assertEquals(expected, actual);
    }
    
    @Test
    public void returnsEmptyIfVmIdDoesNotMatch() {
        String vmId = UUID.randomUUID().toString();
        Set<BasicRole> roles = new HashSet<>();
        RolePrincipal vmIdRole = new RolePrincipal(VmIdFilter.VMS_BY_VM_ID_GRANT_ROLE_PREFIX + vmId);
        roles.add(vmIdRole);
        String wrongVmId = "something other than vmId";
        // assert precondition
        assertFalse(vmId.equals(wrongVmId));
        
        DescriptorMetadata metadata = new DescriptorMetadata(null, wrongVmId);
        assertTrue(metadata.hasVmId());
        VmIdFilter<FooPojo> filter = new VmIdFilter<>(roles);
        FilterResult result = filter.applyFilter(TEST_DESC_NON_NULL_VM_ID, metadata, null);
        assertEquals(ResultType.EMPTY, result.getType());
        assertNull(result.getFilterExpression());
    }
    
    @Test
    public void returnsAllForUnrelatedQuery() {
        Set<BasicRole> roles = new HashSet<>();
        
        DescriptorMetadata metadata = new DescriptorMetadata();
        assertFalse(metadata.hasAgentId());
        assertFalse(metadata.hasVmId());
        VmIdFilter<FooPojo> filter = new VmIdFilter<>(roles);
        // want to have a null retval of vmId
        FilterResult result = filter.applyFilter(TEST_DESC_NULL_VM_ID, metadata, null);
        assertEquals(ResultType.ALL, result.getType());
        assertNull(result.getFilterExpression());
    }
    
    @Test
    public void returnsParentExpressionForUnrelatedQuery() {
        Set<BasicRole> roles = new HashSet<>();
        
        ExpressionFactory factory = new ExpressionFactory();
        Expression parentExpression = factory.equalTo(Key.AGENT_ID, "testKey");
        DescriptorMetadata metadata = new DescriptorMetadata();
        assertFalse(metadata.hasAgentId());
        assertFalse(metadata.hasVmId());
        VmIdFilter<FooPojo> filter = new VmIdFilter<>(roles);
        // want to have a null retval of vmId
        FilterResult result = filter.applyFilter(TEST_DESC_NULL_VM_ID, metadata, parentExpression);
        assertEquals(ResultType.QUERY_EXPRESSION, result.getType());
        assertNotNull(result.getFilterExpression());
        assertEquals(parentExpression, result.getFilterExpression());
    }
}
