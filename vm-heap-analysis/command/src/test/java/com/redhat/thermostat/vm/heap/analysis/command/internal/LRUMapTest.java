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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class LRUMapTest {

    LRUMap<String, String> map;

    @Before
    public void setup() {
        map = new LRUMap<>(3);
    }

    @Test
    public void testDoesNotExceedMaximumSize() {
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        map.put("d", "4");
        assertThat(map.size(), is(3));
    }

    @Test
    public void testIsInitiallyEmpty() {
        assertThat(map.isEmpty(), is(true));
    }

    @Test
    public void testEvictsLeastRecentlyUsed() {
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        map.get("b");
        map.get("c");
        // "a" is now LRU entry

        map.put("d", "4");

        Map<String, String> expected = new HashMap<>();
        expected.put("b", "2");
        expected.put("c", "3");
        expected.put("d", "4");

        assertThat(map, is(equalTo(expected)));
    }

}
