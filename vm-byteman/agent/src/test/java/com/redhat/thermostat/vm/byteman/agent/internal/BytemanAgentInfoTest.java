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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BytemanAgentInfoTest {
    
    private static final boolean SOME_ATTACH_FAILED = false;
    private static final boolean SOME_IS_OLD_ATTACH = false;
    private static final int SOME_VM_PID = 99292;
    private static final int SOME_LISTEN_PORT = 3321;
    private static final String SOME_LISTEN_HOST = "foo-bar";
    private static final String SOME_VM_ID = "some-vm-id";
    private static final String SOME_AGENT_ID = "some-agent-id";

    @Test
    public void testEqualsHashCode() {
        BytemanAgentInfo bytemanInfo = new BytemanAgentInfo(SOME_VM_PID,
                                                            SOME_LISTEN_PORT,
                                                            SOME_LISTEN_HOST,
                                                            SOME_VM_ID,
                                                            SOME_AGENT_ID,
                                                            SOME_ATTACH_FAILED,
                                                            SOME_IS_OLD_ATTACH);
        assertFalse(bytemanInfo.equals(null));
        assertFalse(bytemanInfo.equals(null)); // multiple calls
        BytemanAgentInfo info2 = new BytemanAgentInfo(SOME_VM_PID,
                                                      SOME_LISTEN_PORT,
                                                      SOME_LISTEN_HOST,
                                                      SOME_VM_ID,
                                                      SOME_AGENT_ID,
                                                      SOME_ATTACH_FAILED,
                                                      SOME_IS_OLD_ATTACH);
        assertTrue(bytemanInfo.equals(info2));
        assertTrue(info2.equals(bytemanInfo)); // reflexive property
        assertTrue(bytemanInfo.equals(info2)); // multiple calls
        
        assertEquals(info2.hashCode(), bytemanInfo.hashCode()); // equals => equals hashCode()
        assertEquals(info2.hashCode(), bytemanInfo.hashCode()); // multiple calls
    }
    
    @Test
    public void testNotEquals() {
        BytemanAgentInfo bytemanInfo = new BytemanAgentInfo(SOME_VM_PID,
                                                            SOME_LISTEN_PORT,
                                                            SOME_LISTEN_HOST,
                                                            SOME_VM_ID,
                                                            SOME_AGENT_ID,
                                                            SOME_ATTACH_FAILED,
                                                            SOME_IS_OLD_ATTACH);
        
        // vmPid different
        BytemanAgentInfo other = new BytemanAgentInfo(992,
                                                      SOME_LISTEN_PORT,
                                                      SOME_LISTEN_HOST,
                                                      SOME_VM_ID,
                                                      SOME_AGENT_ID,
                                                      SOME_ATTACH_FAILED,
                                                      SOME_IS_OLD_ATTACH);
        assertFalse(bytemanInfo.equals(other));
        assertFalse(other.equals(bytemanInfo));
        
        // listen port different
        other = new BytemanAgentInfo(SOME_VM_PID,
                                     5554,
                                     SOME_LISTEN_HOST,
                                     SOME_VM_ID,
                                     SOME_AGENT_ID,
                                     SOME_ATTACH_FAILED,
                                     SOME_IS_OLD_ATTACH);
        assertFalse(bytemanInfo.equals(other));
        assertFalse(other.equals(bytemanInfo));
        
        // listen host different
        other = new BytemanAgentInfo(SOME_VM_PID,
                                     SOME_LISTEN_PORT,
                                     null,
                                     SOME_VM_ID,
                                     SOME_AGENT_ID,
                                     SOME_ATTACH_FAILED,
                                     SOME_IS_OLD_ATTACH);
        assertFalse(bytemanInfo.equals(other));
        assertFalse(other.equals(bytemanInfo));
        
        // vmId different
        other = new BytemanAgentInfo(SOME_VM_PID,
                                     SOME_LISTEN_PORT,
                                     SOME_LISTEN_HOST,
                                     "other",
                                     SOME_AGENT_ID,
                                     SOME_ATTACH_FAILED,
                                     SOME_IS_OLD_ATTACH);
        assertFalse(bytemanInfo.equals(other));
        assertFalse(other.equals(bytemanInfo));
        
        // agentId different
        other = new BytemanAgentInfo(SOME_VM_PID,
                                     SOME_LISTEN_PORT,
                                     SOME_LISTEN_HOST,
                                     SOME_VM_ID,
                                     "foobar",
                                     SOME_ATTACH_FAILED,
                                     SOME_IS_OLD_ATTACH);
        assertFalse(bytemanInfo.equals(other));
        assertFalse(other.equals(bytemanInfo));
        
        // attachFailed different
        other = new BytemanAgentInfo(SOME_VM_PID,
                                     SOME_LISTEN_PORT,
                                     SOME_LISTEN_HOST,
                                     SOME_VM_ID,
                                     SOME_AGENT_ID,
                                     true,
                                     SOME_IS_OLD_ATTACH);
        assertFalse(bytemanInfo.equals(other));
        assertFalse(other.equals(bytemanInfo));
        
        // old attach different
        other = new BytemanAgentInfo(SOME_VM_PID,
                                     SOME_LISTEN_PORT,
                                     SOME_LISTEN_HOST,
                                     SOME_VM_ID,
                                     SOME_AGENT_ID,
                                     SOME_ATTACH_FAILED,
                                     true);
        assertFalse(bytemanInfo.equals(other));
        assertFalse(other.equals(bytemanInfo));
    }
}
