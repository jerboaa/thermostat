/*
 * Copyright 2012 Red Hat, Inc.
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;

import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;

public class CategoryTest {

    @Test
    public void testGetKey() {
        Key<String> key1 = new Key<String>("key1", false);
        Category category = new Category("testGetKey", key1);
        assertEquals(key1, category.getKey("key1"));
    }

    @Test
    public void testGetNonExistingKey() {
        Key<String> key1 = new Key<String>("key1", false);
        Category category = new Category("testGetNonExistingKey", key1);
        assertNull(category.getKey("key2"));
    }

    @Test
    public void testGetKeys() {
        Key<String> key1 = new Key<String>("key1", false);
        Key<String> key2 = new Key<String>("key2", false);
        Key<String> key3 = new Key<String>("key3", false);
        Key<String> key4 = new Key<String>("key4", false);
        Category category = new Category("testGetKeys", key1, key2, key3, key4);
        assertEquals(4, category.getKeys().size());
        assertTrue(category.getKeys().contains(key1));
        assertTrue(category.getKeys().contains(key2));
        assertTrue(category.getKeys().contains(key3));
        assertTrue(category.getKeys().contains(key4));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void verifyThatKeysAreUnmodifiable() {
        Key<String> key1 = new Key<String>("key1", false);
        Key<String> key2 = new Key<String>("key2", false);
        Key<String> key3 = new Key<String>("key3", false);
        Category category = new Category("verifyThatKeysAreUnmodifiable", key1, key2, key3);

        Collection<Key<?>> keys = category.getKeys();

        keys.remove(key1);

    }
}
