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

package com.redhat.thermostat.vm.byteman.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class VmSocketIdentifierTest {

    private static final String SOME_VM_ID = "some-vm-id"; // longer than 4 chars
    private static final String SOME_AGENT_ID = "some-agent-id"; // longer than 8 chars
    private static final int SOME_PID = 9999;
    
    @Test(expected = NullPointerException.class)
    public void testNullNotAcceptedAgentId() {
        new VmSocketIdentifier("foo", 30, null);
    }
    
    @Test(expected = NullPointerException.class)
    public void testNullNotAcceptedVmId() {
        new VmSocketIdentifier(null, 30, "agent-id");
    }
    
    @Test
    public void getNameIsFixedAtXChars() {
        String vmId = UUID.randomUUID().toString();
        String agentId = UUID.randomUUID().toString();
        VmSocketIdentifier id = new VmSocketIdentifier(vmId, 20, agentId);
        String actual = id.getName();
        assertEquals("8 chars agentId + 4 chars vmId + 6 chars pid + 2 underscores", 20, actual.length());
        VmSocketIdentifier id2 = new VmSocketIdentifier(vmId, 230032, UUID.randomUUID().toString());
        String actual2 = id2.getName();
        assertEquals(20, actual2.length());
    }
    
    @Test
    public void getNameIsFixedAtXCharsKnownIDs() {
        VmSocketIdentifier id = new VmSocketIdentifier(SOME_VM_ID, 20, SOME_AGENT_ID);
        String actual = id.getName();
        assertEquals("8 chars agentId + 4 chars vmId + 6 chars pid + 2 underscores", 20, actual.length());
        assertEquals("some-age_some_000020", actual);
    }
    
    @Test
    public void testEqualsHashCode() {
        VmSocketIdentifier id1 = new VmSocketIdentifier(SOME_VM_ID, SOME_PID, SOME_AGENT_ID);
        VmSocketIdentifier id2 = new VmSocketIdentifier(SOME_VM_ID, SOME_PID, SOME_AGENT_ID);
        assertFalse(id1.equals(null));
        assertFalse(id2.equals(null));
        assertFalse(id2.equals(null)); // multiple invocation
        VmSocketIdentifier other = new VmSocketIdentifier("foo", 333, "bar");
        assertFalse(id1.equals(other));
        assertFalse(id2.equals(other));
        assertTrue(id2.equals(id1));
        assertTrue(id2.equals(id1)); // multiple invocation
        assertTrue(id1.equals(id2)); // reflexive property
        assertNotSame(id1, id2);
        // be sure equal objects have equal hash code
        assertEquals(id2.hashCode(), id1.hashCode());
        assertEquals(id2.hashCode(), id1.hashCode()); // multiple invocation
    }
    
    
    @Test
    public void shortAgentIdGetName() {
        String agentId = "bar"; // less than agent part length of 8
        String id = new VmSocketIdentifier(SOME_VM_ID, SOME_PID, agentId).getName();
        assertEquals("bar_some_009999", id);
    }
    
    @Test
    public void shortVmIdGetName() {
        String vmId = "foo"; // less than vmId part length of 4
        String id = new VmSocketIdentifier(vmId, SOME_PID, SOME_AGENT_ID).getName();
        assertEquals("some-age_foo_009999", id);
    }
}
