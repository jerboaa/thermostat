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

package com.redhat.thermostat.web.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.model.Pojo;

public class CategorySerializationTest {
    
    private static class TestObj implements Pojo {
        // Dummy class for testing.
    }

    private Gson gson;
    
    @Before
    public void setup() {
        gson = new GsonBuilder().create();
    }
    
    @Test
    public void canSerializeDeserializeCategory() {
        Key<Boolean> barKey = new Key<>("bar-key");
        Category<TestObj> cat = new Category<>("foo-category", TestObj.class, barKey);
        String str = gson.toJson(cat, Category.class);
        @SuppressWarnings("unchecked")
        Category<TestObj> cat2 = (Category<TestObj>)gson.fromJson(str, Category.class);
        assertNotSame(cat, cat2);
        assertTrue(cat.equals(cat2));
        assertEquals(cat.hashCode(), cat2.hashCode());
        try {
            cat2.getKeys().add(new Key<>("testme"));
            fail("keys must be immutable after deserialization");
        } catch (UnsupportedOperationException e) {
            // pass
        }
        assertEquals(TestObj.class, cat2.getDataClass());
        assertEquals("foo-category", cat2.getName());
        assertEquals(barKey, cat2.getKey("bar-key"));
    }

    @Test
    public void canSerializeDeserializeCategoryWithIndexedKeys() {
        Key<Boolean> barKey = new Key<>("bar-key");
        Category<TestObj> cat = new Category<>("foo-category2", TestObj.class,
                Arrays.<Key<?>>asList(barKey), Arrays.<Key<?>>asList(barKey));
        String str = gson.toJson(cat, Category.class);
        @SuppressWarnings("unchecked")
        Category<TestObj> cat2 = (Category<TestObj>)gson.fromJson(str, Category.class);
        assertNotSame(cat, cat2);
        assertTrue(cat.equals(cat2));
        assertEquals(cat.hashCode(), cat2.hashCode());
        try {
            cat2.getKeys().add(new Key<>("testme"));
            fail("keys must be immutable after deserialization");
        } catch (UnsupportedOperationException e) {
            // pass
        }
        assertEquals(TestObj.class, cat2.getDataClass());
        assertEquals("foo-category2", cat2.getName());
        assertEquals(barKey, cat2.getKey("bar-key"));
        assertEquals(Arrays.asList(barKey), cat2.getIndexedKeys());
    }
}
