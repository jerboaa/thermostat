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

package com.redhat.thermostat.web.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class SharedStateIdTest {
    
    @Test
    public void testEquals() {
        UUID uuid = UUID.randomUUID();
        SharedStateId id = new SharedStateId(3200, uuid);
        SharedStateId id2 = new SharedStateId(3200, UUID.randomUUID());
        SharedStateId id3 = new SharedStateId(300, uuid);
        SharedStateId equals = new SharedStateId(3200, uuid);
        
        assertFalse(id.equals(null));
        assertTrue(id.equals(equals));
        assertFalse("Different uuid", id.equals(id2));
        assertFalse("Different id val", id.equals(id3));
        assertFalse("UUID and id val different", id3.equals(id2));
        assertTrue(id.equals(id));
    }

    @Test
    public void testHashCode() {
        UUID uuid = UUID.randomUUID();
        SharedStateId id = new SharedStateId(3200, uuid);
        SharedStateId id2 = new SharedStateId(3200, UUID.randomUUID());
        SharedStateId id3 = new SharedStateId(300, uuid);
        SharedStateId equals = new SharedStateId(3200, uuid);
        
        assertTrue(id.hashCode() == equals.hashCode());
        assertTrue("Different uuid", id.hashCode() != id2.hashCode());
        assertTrue("Different id val", id.hashCode() != id3.hashCode());
        assertTrue("UUID and id val different", id3.hashCode() != id2.hashCode());
        assertTrue(id.hashCode() == id.hashCode());
    }
    
    @Test
    public void testBasic() {
        UUID uuid = UUID.randomUUID();
        SharedStateId id = new SharedStateId(3200, uuid);
        assertEquals(uuid, id.getServerToken());
        assertEquals(3200, id.getId());
    }
}
