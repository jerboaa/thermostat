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

package com.redhat.thermostat.storage.mongodb.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.model.BasePojo;

public class MongoCursorTest {

    @Entity
    public static class TestClass extends BasePojo {
        
        public TestClass() {
            super(null);
        }
        
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
        when(dbCursor.batchSize(Cursor.DEFAULT_BATCH_SIZE)).thenReturn(dbCursor);
        when(dbCursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(dbCursor.next()).thenReturn(value1).thenReturn(value2).thenReturn(null);
        when(dbCursor.sort(any(DBObject.class))).thenReturn(dbCursor);
        when(dbCursor.limit(anyInt())).thenReturn(dbCursor);
        cursor = new MongoCursor<TestClass>(dbCursor, TestClass.class);
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
        try {
            cursor.next();
            fail("Cursor should throw a NoSuchElementException!");
        } catch (NoSuchElementException e) {
            // pass
        }
    }
    
    @Test
    public void testBatchSize() {
        DBCursor mongoCursor = mock(DBCursor.class);
        Cursor<TestClass> mC = new MongoCursor<>(mongoCursor, TestClass.class);
        try {
            mC.setBatchSize(-1);
            fail("expected IAE for batch size of -1");
        } catch (IllegalArgumentException e) {
            // pass
            assertEquals("Batch size must be > 0", e.getMessage());
        }
        try {
            mC.setBatchSize(0);
            fail("expected IAE for batch size of 0");
        } catch (IllegalArgumentException e) {
            // pass
            assertEquals("Batch size must be > 0", e.getMessage());
        }
        mC.setBatchSize(333);
        verify(mongoCursor).batchSize(333);
        Mockito.verifyNoMoreInteractions(mongoCursor);
    }

}

