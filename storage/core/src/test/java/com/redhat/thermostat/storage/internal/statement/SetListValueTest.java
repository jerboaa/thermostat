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

package com.redhat.thermostat.storage.internal.statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SetListValueTest {

    @Test
    public void testEqualsHashCodeEmpty() {
        SetListValue value = new SetListValue();
        assertTrue(value.equals(value));
        SetListValue value2 = new SetListValue();
        assertTrue(value.equals(value2));
        assertEquals(value.hashCode(), value2.hashCode());
    }
    
    @Test
    public void testEqualsHashCodeNonEmpty() {
        TerminalNode keyNode = new TerminalNode(null);
        keyNode.setValue("foo");
        TerminalNode valNode = new TerminalNode(null);
        valNode.setValue("bar-val");
        SetListValue value = new SetListValue();
        value.setKey(keyNode);
        value.setValue(valNode);
        assertTrue(value.equals(value));
        SetListValue value2 = new SetListValue();
        TerminalNode keyNode2 = new TerminalNode(null);
        keyNode2.setValue("foo");
        TerminalNode valNode2 = new TerminalNode(null);
        valNode2.setValue("bar-val");
        value2.setKey(keyNode2);
        value2.setValue(valNode2);
        assertEquals(valNode, valNode2);
        assertEquals(keyNode, keyNode2);
        assertTrue(value.equals(value2));
        assertEquals(value.hashCode(), value2.hashCode());
        assertFalse(value.equals("something-else"));
    }
}

