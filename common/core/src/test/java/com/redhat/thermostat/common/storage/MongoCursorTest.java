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

package com.redhat.thermostat.common.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.common.model.Pojo;

public class MongoCursorTest {

    @Entity
    public static class TestClass implements Pojo {
        private String key1;
        private String key2;
        private String key3;
        private String key4;
        @Persist
        public String getKey1() {
            return key1;
        }
        @Persist
        public void setKey1(String key1) {
            this.key1 = key1;
        }
        @Persist
        public String getKey2() {
            return key2;
        }
        @Persist
        public void setKey2(String key2) {
            this.key2 = key2;
        }
        @Persist
        public String getKey3() {
            return key3;
        }
        @Persist
        public void setKey3(String key3) {
            this.key3 = key3;
        }
        @Persist
        public String getKey4() {
            return key4;
        }
        @Persist
        public void setKey4(String key4) {
            this.key4 = key4;
        }
    }

    private static final Key<String> key1 = new Key<>("key1", false);
    private static final Key<String> key2 = new Key<>("key2", false);
    private static final Key<String> key3 = new Key<>("key3", false);
    private static final Key<String> key4 = new Key<>("key4", false);

    private static final Category testCategory = new Category("MongoCursorTest", key1, key2, key3, key4);

    private DBCursor dbCursor;
    private Cursor<TestClass> cursor;

    @Before
    public void setUp() {
        
        BasicDBObject value1 = new BasicDBObject();
        value1.put("key1", "test1");
        value1.put("key2", "test2");
        BasicDBObject value2 = new BasicDBObject();
        value2.put("key3", "test3");
        value2.put("key4", "test4");

        dbCursor = mock(DBCursor.class);
        when(dbCursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(dbCursor.next()).thenReturn(value1).thenReturn(value2).thenReturn(null);
        when(dbCursor.sort(any(DBObject.class))).thenReturn(dbCursor);
        when(dbCursor.limit(anyInt())).thenReturn(dbCursor);
        cursor = new MongoCursor<TestClass>(dbCursor, testCategory, TestClass.class, null);

    }

    @After
    public void tearDown() {
        dbCursor = null;
        cursor = null;
    }

    @Test
    public void verifySimpleCursor() {

        assertTrue(cursor.hasNext());
        TestClass obj1 = cursor.next();
        assertEquals("test1", obj1.getKey1());
        assertEquals("test2", obj1.getKey2());

        assertTrue(cursor.hasNext());
        TestClass obj2 = cursor.next();
        assertEquals("test3", obj2.getKey3());
        assertEquals("test4", obj2.getKey4());

        assertFalse(cursor.hasNext());
        assertNull(cursor.next());
    }

    @Test
    public void verifyCursorSort() {
        ArgumentCaptor<DBObject> arg = ArgumentCaptor.forClass(DBObject.class);
        Cursor<TestClass> sorted = cursor.sort(key1, Cursor.SortDirection.ASCENDING);

        verify(dbCursor).sort(arg.capture());
        DBObject orderByDBObject = arg.getValue();
        assertEquals(1, orderByDBObject.keySet().size());
        assertEquals((Integer) Cursor.SortDirection.ASCENDING.getValue(), orderByDBObject.get(key1.getName()));
        // Verify that the sorted cursor is still return the same number of items. We leave the actual
        // sorting to Mongo and won't check it here.
        assertTrue(sorted.hasNext());
        sorted.next();
        assertTrue(sorted.hasNext());
        sorted.next();
        assertFalse(sorted.hasNext());
    }

    @Test
    public void verifyCursorLimit() {

        Cursor<TestClass> sorted = cursor.limit(1);

        verify(dbCursor).limit(1);

        // We cannot really test if the cursor really got limited, this is up to the mongo implementation.
        // In any case, we can verify that the returned cursor actually is 'active'.
        assertTrue(sorted.hasNext());
        sorted.next();
    }
}
