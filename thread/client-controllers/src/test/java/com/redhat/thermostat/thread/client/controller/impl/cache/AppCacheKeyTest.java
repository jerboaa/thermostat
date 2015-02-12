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

package com.redhat.thermostat.thread.client.controller.impl.cache;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppCacheKeyTest {
    @Test
    public void testEquals() throws Exception {

        AppCacheKey key1 = new AppCacheKey("test1", AppCacheKeyTest.class);
        AppCacheKey key2 = new AppCacheKey("test2", AppCacheKeyTest.class);

        assertFalse(key1.equals(key2));

        key2 = new AppCacheKey("test1", AppCacheKeyTest.class);
        assertEquals(key1, key2);

        key1 = new AppCacheKey("test1", AppCacheKey.class);
        key2 = new AppCacheKey("test1", AppCacheKeyTest.class);
        assertFalse(key1.equals(key2));
    }

    @Test
    public void testHashCode() throws Exception {
        AppCacheKey key1 = new AppCacheKey("test1", AppCacheKeyTest.class);
        AppCacheKey key2 = new AppCacheKey("test2", AppCacheKeyTest.class);

        assertFalse(key1.hashCode() == key2.hashCode());
        key2 = new AppCacheKey("test1", AppCacheKeyTest.class);
        assertEquals(key1.hashCode(), key2.hashCode());
    }

    @Test
    public void testInHashMap() {
        Map<AppCacheKey, String> table = new HashMap<>();
        AppCacheKey key1 = new AppCacheKey("test1", AppCacheKeyTest.class);
        table.put(key1, "1");

        AppCacheKey key2 = new AppCacheKey("test2", AppCacheKeyTest.class);
        table.put(key2, "1");

        assertTrue(table.containsKey(key1));
        assertTrue(table.containsKey(key2));

        assertEquals(table.get(key1), "1");
        assertEquals(table.get(key2), "1");

        key1 = new AppCacheKey("test1", AppCacheKey.class);
        table.put(key1, "1");

        key2 = new AppCacheKey("test1", AppCacheKeyTest.class);
        table.put(key2, "2");

        assertTrue(table.containsKey(key1));
        assertTrue(table.containsKey(key2));

        assertEquals(table.get(key1), "1");
        assertEquals(table.get(key2), "2");
    }
}
