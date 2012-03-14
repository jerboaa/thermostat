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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DBCollection.class)
public class MongoStorageTest {

    private static final Key<String> key1 = new Key<>("key1", false);
    private static final Key<String> key2 = new Key<>("key2", false);
    private static final Key<String> key3 = new Key<>("key3", false);
    private static final Key<String> key4 = new Key<>("key4", false);
    private static final Key<String> key5 = new Key<>("key5", false);
    private static final Category testCategory = new Category("MongoStorageTest", key1, key2, key3, key4, key5);

    private MongoStorage storage;
    private DBCollection testCollection;

    @Before
    public void setUp() {
        storage = new MongoStorage();

        BasicDBObject value1 = new BasicDBObject();
        value1.put("key1", "test1");
        value1.put("key2", "test2");
        BasicDBObject value2 = new BasicDBObject();
        value2.put("key3", "test3");
        value2.put("key4", "test4");

        DBCursor cursor = mock(DBCursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(value1).thenReturn(value2).thenReturn(null);

        testCollection = PowerMockito.mock(DBCollection.class);
        when(testCollection.find(any(DBObject.class))).thenReturn(cursor);
        storage.mapCategoryToDBCollection(testCategory, testCollection);
    }

    @After
    public void tearDown() {
        storage = null;
        testCollection = null;
    }

    @Test
    public void testCreateConnectionKey() {
        MongoStorage mongoStorage = new MongoStorage();
        Category category = new Category("testCreateConnectionKey");
        ConnectionKey connKey = mongoStorage.createConnectionKey(category);
        assertNotNull(connKey);
    }

    @Test 
    public void verifyFindReturnsCursor() {
        Chunk query = new Chunk(testCategory, false);
        Cursor cursor = storage.find(query);
        assertNotNull(cursor);
    }

    @Test
    public void verifyFindCallsDBCollectionFind() {

        Chunk query = new Chunk(testCategory, false);
        storage.find(query);
        verify(testCollection).find(any(DBObject.class));

    }

    @Test
    public void verifyFindCallsDBCollectionFindWithCorrectQuery() {

        Chunk query = new Chunk(testCategory, false);
        query.put(key1, "test");
        storage.find(query);

        ArgumentCaptor<DBObject> findArg = ArgumentCaptor.forClass(DBObject.class);
        verify(testCollection).find(findArg.capture());

        DBObject arg = findArg.getValue();
        assertEquals(1, arg.keySet().size());
        assertTrue(arg.keySet().contains("key1"));
        assertEquals("test", arg.get("key1"));
    }

    @Test
    public void verifyFindFindWithMultiKeys() {

        Chunk query = new Chunk(testCategory, false);
        query.put(key5, "test1");
        query.put(key4, "test2");
        query.put(key3, "test3");
        query.put(key2, "test4");
        query.put(key1, "test5");
        storage.find(query);

        ArgumentCaptor<DBObject> findArg = ArgumentCaptor.forClass(DBObject.class);
        verify(testCollection).find(findArg.capture());

        DBObject arg = findArg.getValue();
        assertArrayEquals(new String[]{ "key5", "key4", "key3", "key2", "key1" }, arg.keySet().toArray());
    }

    @Test
    public void verifyFindReturnsCorrectCursor() {

        Chunk query = new Chunk(testCategory, false);
        query.put(key5, "test1");

        Cursor cursor = storage.find(query);

        assertTrue(cursor.hasNext());
        Chunk chunk1 = cursor.next();
        assertArrayEquals(new Key<?>[]{key1, key2}, chunk1.getKeys().toArray());
        assertEquals("test1", chunk1.get(key1));
        assertEquals("test2", chunk1.get(key2));

        assertTrue(cursor.hasNext());
        Chunk chunk2 = cursor.next();
        assertArrayEquals(new Key<?>[]{key3, key4}, chunk2.getKeys().toArray());
        assertEquals("test3", chunk2.get(key3));
        assertEquals("test4", chunk2.get(key4));

        assertFalse(cursor.hasNext());
        assertNull(cursor.next());
    }
}
