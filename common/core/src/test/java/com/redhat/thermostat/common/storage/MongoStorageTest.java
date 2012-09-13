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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

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
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.redhat.thermostat.common.config.StartupConfiguration;
import com.redhat.thermostat.common.model.Pojo;
import com.redhat.thermostat.common.storage.Query.Criteria;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ DBCollection.class, DB.class, Mongo.class, MongoStorage.class, MongoConnection.class })
public class MongoStorageTest {

    @Entity
    public static class TestClass implements Pojo {
        private String key1;
        private String key2;
        private String key3;
        private String key4;
        private String key5;
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
        @Persist
        public String getKey5() {
            return key5;
        }
        @Persist
        public void setKey5(String key5) {
            this.key5 = key5;
        }
    }

    private static final Key<String> key1 = new Key<>("key1", false);
    private static final Key<String> key2 = new Key<>("key2", false);
    private static final Key<String> key3 = new Key<>("key3", false);
    private static final Key<String> key4 = new Key<>("key4", false);
    private static final Key<String> key5 = new Key<>("key5", false);
    private static final Category testCategory = new Category("MongoStorageTest", key1, key2, key3, key4, key5);
    private static final Category emptyTestCategory = new Category("MongoEmptyCategory");

    private StartupConfiguration conf;
    private Chunk multiKeyQuery;
    private Mongo m;
    private DB db;
    private DBCollection testCollection, emptyTestCollection, mockedCollection;

    private MongoStorage makeStorage() {
        MongoStorage storage = new MongoStorage(conf);
        storage.mapCategoryToDBCollection(testCategory, testCollection);
        storage.mapCategoryToDBCollection(emptyTestCategory, emptyTestCollection);
        return storage;
    }

    @Before
    public void setUp() throws Exception {
        conf = mock(StartupConfiguration.class);
        when(conf.getDBConnectionString()).thenReturn("mongodb://127.0.0.1:27518");
        db = PowerMockito.mock(DB.class);
        m = PowerMockito.mock(Mongo.class);
        mockedCollection = mock(DBCollection.class);
        when(m.getDB(anyString())).thenReturn(db);
        when(db.getCollection("agent-config")).thenReturn(mockedCollection);
        when(db.collectionExists(anyString())).thenReturn(true);

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
        emptyTestCollection = PowerMockito.mock(DBCollection.class);
        when(emptyTestCollection.getCount()).thenReturn(0L);
        when(db.collectionExists(anyString())).thenReturn(false);
        when(db.createCollection(anyString(), any(DBObject.class))).thenReturn(testCollection);
    }

    @After
    public void tearDown() {
        conf = null;
        m = null;
        db = null;
        testCollection = null;
        emptyTestCollection = null;
        multiKeyQuery = null;
    }

    @Test
    public void testCreateConnectionKey() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        storage.getConnection().connect();
        Category category = new Category("testCreateConnectionKey");
        ConnectionKey connKey = storage.createConnectionKey(category);
        assertNotNull(connKey);
    }

    @Test (expected=IllegalArgumentException.class)
    public void verifyFindOnlyAcceptsMongoQuery() {
        MongoStorage storage = makeStorage();
        Query query = mock(Query.class);
        storage.find(query);
    }

    @Test (expected=IllegalArgumentException.class)
    public void verifyFindAllOnlyAcceptsMongoQuery() {
        MongoStorage storage = makeStorage();
        Query query = mock(Query.class);
        storage.findAllPojos(query, TestClass.class);
    }

    @Test
    public void verifyFindAllReturnsCursor() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        Query query = storage.createQuery().from(testCategory);
        Cursor<TestClass> cursor = storage.findAllPojos(query, TestClass.class);
        assertNotNull(cursor);
    }

    @Test
    public void verifyFindReturnsChunk() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        Query query = storage.createQuery().from(testCategory).where(key1, Criteria.EQUALS, "test1");
        Chunk result = storage.find(query);
        assertNotNull(result);
    }

    @Test
    public void verifyFindAllCallsDBCollectionFind() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        Query query = storage.createQuery().from(testCategory);
        storage.findAllPojos(query, TestClass.class);
        verify(testCollection).find(any(DBObject.class));
    }

    @Test
    public void verifyFindCallsDBCollectionFindOne() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        Query query = storage.createQuery().from(testCategory);
        storage.find(query);
        verify(testCollection).findOne(any(DBObject.class));
    }

    @Test
    public void verifyFindAllCallsDBCollectionFindWithCorrectQuery() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();

        MongoQuery query = mock(MongoQuery.class);
        DBObject generatedQuery = mock(DBObject.class);
        when(query.getGeneratedQuery()).thenReturn(generatedQuery);
        when(query.getCollectionName()).thenReturn(testCategory.getName());
        when(query.getCategory()).thenReturn(testCategory);
        ArgumentCaptor<DBObject> findArg = ArgumentCaptor.forClass(DBObject.class);

        storage.findAllPojos(query, TestClass.class);

        verify(testCollection).find(findArg.capture());
        assertSame(generatedQuery, findArg.getValue());
    }

    @Test
    public void verifyFindCallsDBCollectionFindOneWithCorrectQuery() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();

        MongoQuery query = mock(MongoQuery.class);
        DBObject generatedQuery = mock(DBObject.class);
        when(query.getGeneratedQuery()).thenReturn(generatedQuery);
        when(query.getCollectionName()).thenReturn(testCategory.getName());
        when(query.getCategory()).thenReturn(testCategory);

        ArgumentCaptor<DBObject> findArg = ArgumentCaptor.forClass(DBObject.class);

        storage.find(query);

        verify(testCollection).findOne(findArg.capture());
        assertSame(generatedQuery, findArg.getValue());
    }

    @Test
    public void verifyFindReturnsCorrectChunk() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        // TODO find a way to test this that isn't just testing mock and converters
        // Because we mock the DBCollection, the contents of this query don't actually determine the result.
        MongoQuery query = new MongoQuery().from(testCategory);

        Chunk result = storage.find(query);

        assertNotNull(result);
        assertArrayEquals(new Key<?>[]{key1, key2}, result.getKeys().toArray());
        assertEquals("test1", result.get(key1));
        assertEquals("test2", result.get(key2));
    }

    @Test
    public void verifyFindAllReturnsCorrectCursor() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        // TODO find a way to test this that isn't just testing MongoCursor
        // Because we mock the DBCollection, the contents of this query don't actually determine the result.
        MongoQuery query = new MongoQuery().from(testCategory);

        Cursor<TestClass> cursor = storage.findAllPojos(query, TestClass.class);

        verifyDefaultCursor(cursor);
    }

    @Test
    public void verifyFindAllFromCategoryCallsDBCollectionFindAll() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        storage.findAllPojosFromCategory(testCategory, TestClass.class);
        verify(testCollection).find();
    }

    @Test
    public void verifyFindAllFromCategoryReturnsCorrectCursor() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        Cursor<TestClass> cursor = storage.findAllPojosFromCategory(testCategory, TestClass.class);

        verifyDefaultCursor(cursor);
    }

    @Test
    public void verifyGetCount() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        long count = storage.getCount(testCategory);
        assertEquals(2, count);
    }

    @Test
    public void verifyGetCountForEmptyCategory() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        long count = storage.getCount(emptyTestCategory);
        assertEquals(0, count);
    }

    @Test
    public void verifyGetCountForNonexistentCategory() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        storage.getConnection().connect();
        long count = storage.getCount(new Category("NonExistent"));
        assertEquals(0, count);
    }

    private void verifyDefaultCursor(Cursor<TestClass> cursor) {
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
    public void verifySaveFile() throws Exception {
        GridFSInputFile gridFSFile = mock(GridFSInputFile.class);
        GridFS gridFS = mock(GridFS.class);
        when(gridFS.createFile(any(InputStream.class), anyString())).thenReturn(gridFSFile);
        PowerMockito.whenNew(GridFS.class).withArguments(any()).thenReturn(gridFS);
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        byte[] data = new byte[] { 1, 2, 3 };
        InputStream dataStream = new ByteArrayInputStream(data);
        storage.saveFile("test", dataStream);
        verify(gridFS).createFile(same(dataStream), eq("test"));
        verify(gridFSFile).save();
    }

    @Test
    public void verifyPutChunkUsesCorrectChunkAgent() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        Chunk chunk1 = new Chunk(testCategory, false);
        chunk1.put(Key.AGENT_ID, "123");
        storage.putChunk(chunk1);
        ArgumentCaptor<DBObject> dbobj = ArgumentCaptor.forClass(DBObject.class);
        verify(testCollection).insert(dbobj.capture());
        DBObject val = dbobj.getValue();
        assertEquals("123", val.get("agentId"));
    }

    @Test
    public void verifyPutChunkUsesCorrectGlobalAgent() throws Exception {
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();
        storage.setAgentId(new UUID(1, 2));
        Chunk chunk1 = new Chunk(testCategory, false);
        chunk1.put(Key.AGENT_ID, "123");
        storage.putChunk(chunk1);
        ArgumentCaptor<DBObject> dbobj = ArgumentCaptor.forClass(DBObject.class);
        verify(testCollection).insert(dbobj.capture());
        DBObject val = dbobj.getValue();
        assertEquals(new UUID(1, 2).toString(), val.get("agentId"));
    }

    @Test
    public void verifyLoadFile() throws Exception {
        InputStream stream = mock(InputStream.class);
        GridFSDBFile file = mock(GridFSDBFile.class);
        when(file.getInputStream()).thenReturn(stream);
        GridFS gridFS = mock(GridFS.class);
        when(gridFS.findOne("test")).thenReturn(file);
        PowerMockito.whenNew(GridFS.class).withArguments(any()).thenReturn(gridFS);
        PowerMockito.whenNew(Mongo.class).withParameterTypes(MongoURI.class).withArguments(any(MongoURI.class)).thenReturn(m);
        MongoStorage storage = makeStorage();

        InputStream actual = storage.loadFile("test");
        assertSame(stream, actual);

        actual = storage.loadFile("doesnotexist");
        assertNull(actual);
    }
}
