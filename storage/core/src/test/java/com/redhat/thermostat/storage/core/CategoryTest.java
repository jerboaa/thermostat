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

package com.redhat.thermostat.storage.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.Test;

import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.Pojo;

public class CategoryTest {

    private static class TestObj implements Pojo {
        // Dummy class for testing.
    }

    @Test
    public void testGetKey() {
        Key<String> key1 = new Key<String>("key1");
        Category<TestObj> category = new Category<>("testGetKey", TestObj.class, key1);
        assertEquals(key1, category.getKey("key1"));
    }

    @Test
    public void testGetNonExistingKey() {
        Key<String> key1 = new Key<String>("key1");
        Category<TestObj> category = new Category<>("testGetNonExistingKey", TestObj.class, key1);
        assertNull(category.getKey("key2"));
    }

    @Test
    public void testGetKeys() {
        Key<String> key1 = new Key<String>("key1");
        Key<String> key2 = new Key<String>("key2");
        Key<String> key3 = new Key<String>("key3");
        Key<String> key4 = new Key<String>("key4");
        Category<TestObj> category = new Category<>("testGetKeys", TestObj.class, key1, key2, key3, key4);
        assertEquals(4, category.getKeys().size());
        assertTrue(category.getKeys().contains(key1));
        assertTrue(category.getKeys().contains(key2));
        assertTrue(category.getKeys().contains(key3));
        assertTrue(category.getKeys().contains(key4));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void verifyThatKeysAreUnmodifiable() {
        Key<String> key1 = new Key<String>("key1");
        Key<String> key2 = new Key<String>("key2");
        Key<String> key3 = new Key<String>("key3");
        Category<TestObj> category = new Category<>("verifyThatKeysAreUnmodifiable", TestObj.class, key1, key2, key3);

        Collection<Key<?>> keys = category.getKeys();

        keys.remove(key1);
    }
    
    @Test
    public void testEquals() {
        Key<String> key1 = new Key<String>("key1");
        Key<String> key2 = new Key<String>("key2");
        Key<String> key3 = new Key<String>("key3");
        Category<TestObj> category = new Category<>("testEquals", TestObj.class, key1, key2, key3);
        assertTrue(category.equals(category));
        assertFalse(category.equals(HostInfoDAO.hostInfoCategory));
    }
    
    @Test
    public void testHashCode() {
        Key<String> key1 = new Key<String>("key1");
        Category<TestObj> category = new Category<>("testHashCode", TestObj.class, key1);
        Map<String, Key<?>> keys = new HashMap<>();
        keys.put(key1.getName(), key1);
        int expectedHash = Objects.hash("testHashCode", keys, TestObj.class);
        assertEquals(expectedHash, category.hashCode());
    }
    
    /**
     * If a Category instance gets serialized we only set the dataClassName.
     * However, getting the dataClass from the name must still work.
     */
    @Test
    public void testGetDataClassByName() {
        Category<TestObj> category = new Category<>("testGetDataClassByName", null);
        // set dataClassName via reflection
        try {
            Field dataClassName = Category.class.getDeclaredField("dataClassName");
            dataClassName.setAccessible(true);
            dataClassName.set(category, TestObj.class.getName());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // now we should be able to get the dataclass itself
        Class<TestObj> dataClass = category.getDataClass();
        assertNotNull(dataClass);
        assertEquals(TestObj.class, dataClass);
    }
    
    @Test
    public void testHashCodeWithDataClassNotSet() {
        Category<TestObj> category = new Category<>("testHashCodeWithDataClassNotSet", null);
        // set dataClassName via reflection
        try {
            Field dataClassName = Category.class.getDeclaredField("dataClassName");
            dataClassName.setAccessible(true);
            dataClassName.set(category, TestObj.class.getName());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        // hash code must not change if dataclass gets set internally
        int firstHashCode = category.hashCode();
        Class<TestObj> dataClass = category.getDataClass();
        // hashCode should have initialized dataClass
        assertNotNull(dataClass);
        assertEquals(TestObj.class, dataClass);
        assertEquals(firstHashCode, category.hashCode());
    }
    
    @Test
    public void getKeysNull() {
        Category<TestObj> cat = new Category<>();
        // this must not throw NPE
        Collection<Key<?>> keys = cat.getKeys();
        assertTrue(keys.isEmpty());
        try {
            keys.add(new Key<>());
            fail("empty keys must be immutable");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }
    
    @Test
    public void getKeyByNameNull() {
        Category<TestObj> cat = new Category<>();
        // This must not throw a NPE
        Key<?> key = cat.getKey("foo-key-not-there");
        assertNull(key);
    }
}

