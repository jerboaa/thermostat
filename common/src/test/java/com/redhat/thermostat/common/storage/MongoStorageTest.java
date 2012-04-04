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
import static org.mockito.Matchers.anyString;
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
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DBCollection.class, DB.class})
public class MongoStorageTest {

    private static final Key<String> key1 = new Key<>("key1", false);
    private static final Key<String> key2 = new Key<>("key2", false);
    private static final Key<String> key3 = new Key<>("key3", false);
    private static final Key<String> key4 = new Key<>("key4", false);
    private static final Key<String> key5 = new Key<>("key5", false);
    private static final Category testCategory = new Category("MongoStorageTest", key1, key2, key3, key4, key5);
    private static final Category emptyTestCategory = new Category("MongoEmptyCategory");

    private Chunk multiKeyQuery;

    private MongoStorage storage;
    private DB db;
    private DBCollection testCollection, emptyTestCollection;

    @Before
    public void setUp() {
        storage = new MongoStorage(null);
        db = PowerMockito.mock(DB.class);
        storage.connect(db);

        BasicDBObject value1 = new BasicDBObject();
        value1.put("key1", "test1");
        value1.put("key2", "test2");
        BasicDBObject value2 = new BasicDBObject();
        value2.put("key3", "test3");
        value2.put("key4", "test4");

        multiKeyQuery = new Chunk(testCategory, false);
        multiKeyQuery.put(key5, "test1");
        multiKeyQuery.put(key4, "test2");
        multiKeyQuery.put(key3, "test3");
        multiKeyQuery.put(key2, "test4");
        multiKeyQuery.put(key1, "test5");

        DBCursor cursor = mock(DBCursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(value1).thenReturn(value2).thenReturn(null);

        testCollection = PowerMockito.mock(DBCollection.class);
        when(testCollection.find(any(DBObject.class))).thenReturn(cursor);
        when(testCollection.find()).thenReturn(cursor);
        when(testCollection.findOne(any(DBObject.class))).thenReturn(value1);
        when(testCollection.getCount()).thenReturn(2L);
        storage.mapCategoryToDBCollection(testCategory, testCollection);
        emptyTestCollection = PowerMockito.mock(DBCollection.class);
        when(emptyTestCollection.getCount()).thenReturn(0L);
        storage.mapCategoryToDBCollection(emptyTestCategory, emptyTestCollection);
        when(db.collectionExists(anyString())).thenReturn(false);
        when(db.createCollection(anyString(), any(DBObject.class))).thenReturn(testCollection);
    }

    @After
    public void tearDown() {
        storage = null;
        testCollection = null;
        multiKeyQuery = null;
    }

    @Test
    public void testCreateConnectionKey() {
        MongoStorage mongoStorage = new MongoStorage(null);
        mongoStorage.connect(db);
        Category category = new Category("testCreateConnectionKey");
        ConnectionKey connKey = mongoStorage.createConnectionKey(category);
        assertNotNull(connKey);
    }

    @Test 
    public void verifyFindAllReturnsCursor() {
        Chunk query = new Chunk(testCategory, false);
        Cursor cursor = storage.findAll(query);
        assertNotNull(cursor);
    }

    @Test
    public void verifyFindReturnsChunk() {
        Chunk query = new Chunk(testCategory, false);
        query.put(key1, "test1");
        Chunk result = storage.find(query);
        assertNotNull(result);
    }

    @Test
    public void verifyFindAllCallsDBCollectionFind() {
        Chunk query = new Chunk(testCategory, false);
        storage.findAll(query);
        verify(testCollection).find(any(DBObject.class));
    }

    @Test
    public void verifyFindCallsDBCollectionFindOne() {
        Chunk query = new Chunk(testCategory, false);
        storage.find(query);
        verify(testCollection).findOne(any(DBObject.class));
    }

    @Test
    public void verifyFindAllCallsDBCollectionFindWithCorrectQuery() {

        Chunk query = new Chunk(testCategory, false);
        query.put(key1, "test");
        storage.findAll(query);

        ArgumentCaptor<DBObject> findArg = ArgumentCaptor.forClass(DBObject.class);
        verify(testCollection).find(findArg.capture());

        DBObject arg = findArg.getValue();
        assertEquals(1, arg.keySet().size());
        assertTrue(arg.keySet().contains("key1"));
        assertEquals("test", arg.get("key1"));
    }

    @Test
    public void verifyFindCallsDBCollectionFindOneWithCorrectQuery() {

        Chunk query = new Chunk(testCategory, false);
        query.put(key1, "test");
        storage.find(query);

        ArgumentCaptor<DBObject> findArg = ArgumentCaptor.forClass(DBObject.class);
        verify(testCollection).findOne(findArg.capture());

        DBObject arg = findArg.getValue();
        assertEquals(1, arg.keySet().size());
        assertTrue(arg.keySet().contains("key1"));
        assertEquals("test", arg.get("key1"));
    }

    @Test
    public void verifyFindAllWithMultiKeys() {

        storage.findAll(multiKeyQuery);

        ArgumentCaptor<DBObject> findArg = ArgumentCaptor.forClass(DBObject.class);
        verify(testCollection).find(findArg.capture());

        DBObject arg = findArg.getValue();
        assertArrayEquals(new String[]{ "key5", "key4", "key3", "key2", "key1" }, arg.keySet().toArray());
    }

    @Test
    public void verifyFindWithMultiKeys() {

        storage.find(multiKeyQuery);

        ArgumentCaptor<DBObject> findArg = ArgumentCaptor.forClass(DBObject.class);
        verify(testCollection).findOne(findArg.capture());

        DBObject arg = findArg.getValue();
        assertArrayEquals(new String[]{ "key5", "key4", "key3", "key2", "key1" }, arg.keySet().toArray());
    }

    @Test
    public void verifyFindReturnsCorrectChunk() {

        // TODO find a way to test this that isn't just testing mock and converters
        Chunk query = new Chunk(testCategory, false);
        // Because we mock the DBCollection, the contents of this query don't actually determine the result.
        query.put(key5, "test1");

        Chunk result = storage.find(query);

        assertNotNull(result);
        assertArrayEquals(new Key<?>[]{key1, key2}, result.getKeys().toArray());
        assertEquals("test1", result.get(key1));
        assertEquals("test2", result.get(key2));
    }

    @Test
    public void verifyFindAllReturnsCorrectCursor() {

        // TODO find a way to test this that isn't just testing MongoCursor
        Chunk query = new Chunk(testCategory, false);
        // Because we mock the DBCollection, the contents of this query don't actually determine the result.
        query.put(key5, "test1");

        Cursor cursor = storage.findAll(query);

        verifyDefaultCursor(cursor);
    }

    @Test
    public void verifyFindAllFromCategoryCallsDBCollectionFindAll() {
        storage.findAllFromCategory(testCategory);
        verify(testCollection).find();
    }

    @Test
    public void verifyFindAllFromCategoryReturnsCorrectCursor() {
        Cursor cursor = storage.findAllFromCategory(testCategory);

        verifyDefaultCursor(cursor);
    }

    @Test
    public void verifyGetCount() {
        long count = storage.getCount(testCategory);
        assertEquals(2, count);
    }

    @Test
    public void verifyGetCountForEmptyCategory() {
        long count = storage.getCount(emptyTestCategory);
        assertEquals(0, count);
    }

    @Test
    public void verifyGetCountForNonexistentCategory() {
        long count = storage.getCount(new Category("NonExistent"));
        assertEquals(0, count);
    }

    private void verifyDefaultCursor(Cursor cursor) {
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
