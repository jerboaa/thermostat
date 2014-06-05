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

package com.redhat.thermostat.storage.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AggregateCountTest {
    
    @Test
    public void testEquals() {
        AggregateCount c = new AggregateCount();
        assertTrue("should be equal to self", c.equals(c));
        c.setCount(-1);
        assertTrue("should be equal to self", c.equals(c));
        AggregateCount d = new AggregateCount();
        d.setCount(-1);
        assertTrue("c + d have equal count", c.equals(d));
        d.setCount(10);
        assertFalse("c has count -1, d 10", c.equals(d));
        assertFalse("string is no agg-count", c.equals("foo"));
        assertTrue(new AggregateCount().equals(new AggregateCount()));
    }
    
    @Test
    public void testHashCode() {
        AggregateCount c = new AggregateCount();
        assertEquals(c.hashCode(), c.hashCode());
        c.setCount(-1);
        assertEquals(c.hashCode(), c.hashCode());
        AggregateCount d = new AggregateCount();
        d.setCount(-1);
        assertEquals(d.hashCode(), c.hashCode());
        d.setCount(100);
        assertFalse(d.hashCode() == c.hashCode());
        assertFalse("foo".hashCode() == d.hashCode());
        assertEquals(new AggregateCount().hashCode(), new AggregateCount().hashCode());
    }
}

