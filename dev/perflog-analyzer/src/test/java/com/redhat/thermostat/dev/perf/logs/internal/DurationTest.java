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

package com.redhat.thermostat.dev.perf.logs.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class DurationTest {

    @Test
    public void testEquals() {
        Duration one = new Duration(2, TimeUnit.DAYS);
        assertTrue(one.equals(one));
        Duration two = new Duration(2, TimeUnit.DAYS);
        assertEquals(one, two);
        Duration three = new Duration(3, TimeUnit.DAYS);
        // different value
        assertFalse(three.equals(two));
        Duration four = new Duration(2, TimeUnit.SECONDS);
        // different time unit
        assertFalse(four.equals(one));
        Duration nullDuration = null;
        assertEquals(nullDuration, nullDuration);
    }
    
    @Test
    public void testHashCode() {
        Duration one = new Duration(2, TimeUnit.DAYS);
        assertTrue(one.hashCode() == one.hashCode());
        Duration two = new Duration(2, TimeUnit.DAYS);
        assertTrue(one.hashCode() == two.hashCode());
        Duration three = new Duration(3, TimeUnit.DAYS);
        // different value
        assertTrue(three.hashCode() != two.hashCode());
        Duration four = new Duration(2, TimeUnit.SECONDS);
        // different time unit
        assertTrue(four.hashCode() != one.hashCode());
        Duration nullDuration = null;
        assertTrue(Objects.hashCode(nullDuration) == Objects.hashCode(nullDuration));
    }
}
