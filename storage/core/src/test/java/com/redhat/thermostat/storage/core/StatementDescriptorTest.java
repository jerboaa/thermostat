/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.storage.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.VmInfo;

public class StatementDescriptorTest {

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testToString() {
        String desc = "QUERY agent-config WHERE 'agentId' = ?s";
        Category cat = mock(Category.class);
        StatementDescriptor stmtDesc = new StatementDescriptor(cat, desc);
        // toString is used extensively for logging.
        assertEquals(desc, stmtDesc.toString());
    }
    
    @Test
    public void testHashCode() {
        String strDesc = "QUERY agent-config";
        StatementDescriptor<AgentInformation> desc1 = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, strDesc);
        CategoryAdapter<AgentInformation, AggregateCount> adapter = new CategoryAdapter<>(AgentInfoDAO.CATEGORY);
        Category<AggregateCount> aggregateCat = adapter.getAdapted(AggregateCount.class);
        StatementDescriptor<AggregateCount> desc2 = new StatementDescriptor<>(aggregateCat, strDesc);
        assertTrue("regular and aggregate category should have different hashcode",
                desc1.hashCode() != desc2.hashCode());
        
        System.out.println("desc1 = " + desc1.hashCode());
        assertEquals("same descriptors should have same hash code", desc1.hashCode(), desc1.hashCode());
        assertEquals("same descriptors should have same hash code", desc2.hashCode(), desc2.hashCode());
        StatementDescriptor<AgentInformation> desc1Dupl = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, strDesc);
        assertEquals("same descriptors should have same hash code", desc1.hashCode(), desc1Dupl.hashCode());
        
        strDesc = "QUERY " + VmInfoDAO.vmInfoCategory.getName();
        StatementDescriptor<VmInfo> desc3 = new StatementDescriptor<>(VmInfoDAO.vmInfoCategory, strDesc);
        assertTrue("*very* different descriptors should have different hash code",
                desc1.hashCode() != desc3.hashCode());
        
        strDesc = "QUERY " + VmInfoDAO.vmInfoCategory.getName() + " WHERE 'a' = ?s";
        StatementDescriptor<VmInfo> desc4 = new StatementDescriptor<>(VmInfoDAO.vmInfoCategory, strDesc);
        assertTrue("different descriptors, but same category should have different hash code",
                desc3.hashCode() != desc4.hashCode());
        
        strDesc = "QUERY agent-config";
        // doesn't really make sense (would not parse), but this is a hash code test
        StatementDescriptor<VmInfo> desc5 = new StatementDescriptor<>(VmInfoDAO.vmInfoCategory, strDesc);
        assertTrue("same string desc, but different category should be different hash code",
                desc5.hashCode() != desc4.hashCode());
    }
    
    @Test
    public void testEquals() {
        String strDesc = "QUERY agent-config";
        StatementDescriptor<AgentInformation> desc1 = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, strDesc);
        CategoryAdapter<AgentInformation, AggregateCount> adapter = new CategoryAdapter<>(AgentInfoDAO.CATEGORY);
        Category<AggregateCount> aggregateCat = adapter.getAdapted(AggregateCount.class);
        StatementDescriptor<AggregateCount> desc2 = new StatementDescriptor<>(aggregateCat, strDesc);
        assertFalse("regular and aggregate category should not be equal",
                desc1.equals(desc2));
        assertFalse("null not equal non-null desc", desc1.equals(null));
        assertTrue("self-equals test", desc1.equals(desc1));
        
        assertEquals("same descriptors should be equal", desc1, desc1);
        assertEquals("same descriptors should be equal", desc2, desc2);
        StatementDescriptor<AgentInformation> desc1Dupl = new StatementDescriptor<>(AgentInfoDAO.CATEGORY, strDesc);
        assertEquals("same descriptors should be equal", desc1, desc1Dupl);
        
        strDesc = "QUERY " + VmInfoDAO.vmInfoCategory.getName();
        StatementDescriptor<VmInfo> desc3 = new StatementDescriptor<>(VmInfoDAO.vmInfoCategory, strDesc);
        assertFalse("*very* different descriptors should not be equal",
                desc1.equals(desc3));
        
        strDesc = "QUERY " + VmInfoDAO.vmInfoCategory.getName() + " WHERE 'a' = ?s";
        StatementDescriptor<VmInfo> desc4 = new StatementDescriptor<>(VmInfoDAO.vmInfoCategory, strDesc);
        assertFalse("different descriptors, but same category should not be equal",
                desc3.equals(desc4));
        
        strDesc = "QUERY agent-config";
        // doesn't really make sense (would not parse), but will do for this test
        StatementDescriptor<VmInfo> desc5 = new StatementDescriptor<>(VmInfoDAO.vmInfoCategory, strDesc);
        assertFalse("same string desc, but different category should not be equal",
                desc5.equals(desc4));
    }
    
}

