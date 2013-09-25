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

package com.redhat.thermostat.storage.internal.statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.redhat.thermostat.storage.core.IllegalPatchException;
import com.redhat.thermostat.storage.core.PreparedParameter;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.LiteralExpression;

public class TerminalNodeTest {

    @Test
    public void canPatchWithCorrectType() {
        TerminalNode node = new TerminalNode(null);
        UnfinishedValueNode unfinished = new UnfinishedValueNode();
        unfinished.setLHS(false);
        unfinished.setParameterIndex(0);
        unfinished.setType(String.class);
        unfinished.setArrayType(false);
        
        node.setValue(unfinished);
        
        PreparedParameter p = new PreparedParameter();
        p.setType(String.class);
        p.setArrayType(false);
        p.setValue("foo-bar");
        
        PatchedWhereExpression expn = null;
        try {
            expn = node.patch(new PreparedParameter[] { p });
            // pass
        } catch (IllegalPatchException e) {
            fail(e.getMessage());
        }
        assertNotNull(expn);
        LiteralExpression<?> literal = (LiteralExpression<?>)expn.getExpression();
        assertEquals("foo-bar", literal.getValue());
    }
    
    @Test
    public void canPatchWithPojoType() {
        TerminalNode node = new TerminalNode(null);
        UnfinishedValueNode unfinished = new UnfinishedValueNode();
        unfinished.setLHS(false);
        unfinished.setParameterIndex(0);
        unfinished.setType(Pojo.class);
        unfinished.setArrayType(false);
        
        node.setValue(unfinished);
        
        PreparedParameter p = new PreparedParameter();
        p.setType(AgentInformation.class);
        p.setArrayType(false);
        AgentInformation info = new AgentInformation("foo-bar");
        p.setValue(info);
        
        PatchedWhereExpression expn = null;
        try {
            expn = node.patch(new PreparedParameter[] { p });
            // pass
        } catch (IllegalPatchException e) {
            fail(e.getMessage());
        }
        assertNotNull(expn);
        LiteralExpression<?> literal = (LiteralExpression<?>)expn.getExpression();
        assertEquals(info, literal.getValue());
    }
    
    @Test
    public void rejectPatchingWithIncorrectListType() {
        TerminalNode node = new TerminalNode(null);
        UnfinishedValueNode unfinished = new UnfinishedValueNode();
        unfinished.setLHS(false);
        unfinished.setParameterIndex(0);
        unfinished.setType(String.class);
        unfinished.setArrayType(false);
        
        node.setValue(unfinished);
        
        PreparedParameter p = new PreparedParameter();
        p.setType(String.class);
        p.setArrayType(true);
        p.setValue(new String[] { "foo-bar" });
        
        try {
            node.patch(new PreparedParameter[] { p });
            fail("Should not be able to patch string with string list.");
        } catch (IllegalPatchException e) {
            // pass
            assertTrue(e.getMessage().contains("invalid type when attempting to patch."));
        }
    }
    
    @Test
    public void rejectPatchingWithIncorrectPojoListType() {
        TerminalNode node = new TerminalNode(null);
        UnfinishedValueNode unfinished = new UnfinishedValueNode();
        unfinished.setLHS(false);
        unfinished.setParameterIndex(0);
        unfinished.setType(Pojo.class);
        unfinished.setArrayType(false);
        
        node.setValue(unfinished);
        
        PreparedParameter p = new PreparedParameter();
        p.setType(AgentInformation.class);
        p.setArrayType(true);
        AgentInformation info = new AgentInformation("testing");
        p.setValue(new AgentInformation[] { info });
        
        try {
            node.patch(new PreparedParameter[] { p });
            fail("Should not be able to patch string with string list.");
        } catch (IllegalPatchException e) {
            // pass
            assertTrue(e.getMessage().contains("invalid type when attempting to patch."));
        }
    }
    
    @Test
    public void rejectPatchingWithIncorrectType() {
        TerminalNode node = new TerminalNode(null);
        UnfinishedValueNode unfinished = new UnfinishedValueNode();
        unfinished.setLHS(false);
        unfinished.setParameterIndex(0);
        unfinished.setType(Integer.class);
        unfinished.setArrayType(false);
        
        node.setValue(unfinished);
        
        PreparedParameter p = new PreparedParameter();
        p.setType(String.class);
        p.setArrayType(true);
        p.setValue(new String[] { "foo-bar" });
        
        try {
            node.patch(new PreparedParameter[] { p });
            fail("Should not be able to patch integer with string list.");
        } catch (IllegalPatchException e) {
            // pass
            assertTrue(e.getMessage().contains("invalid type when attempting to patch."));
        }
    }
}
