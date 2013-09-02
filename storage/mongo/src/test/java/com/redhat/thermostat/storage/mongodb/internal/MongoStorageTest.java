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

package com.redhat.thermostat.storage.mongodb.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.WriteResult;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;

//There is a bug (resolved as wontfix) in powermock which results in
//java.lang.LinkageError if javax.management.* classes aren't ignored by
//Powermock. More here: http://code.google.com/p/powermock/issues/detail?id=277
//SSL tests need this and having that annotation on method level doesn't seem
//to solve the issue.
@PowerMockIgnore( {"javax.management.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DBCollection.class, DB.class, Mongo.class, MongoStorage.class, MongoConnection.class })
public class MongoStorageTest {

    @Entity
    public static class TestClass extends BasePojo {
        
        public TestClass() {
            super(null);
        }
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

    private static final Key<String> key1 = new Key<>("key1");
    private static final Key<String> key2 = new Key<>("key2");
    private static final Key<String> key3 = new Key<>("key3");
    private static final Key<String> key4 = new Key<>("key4");
    private static final Key<String> key5 = new Key<>("key5");
    private static final Category<TestClass> testCategory = new Category<>("MongoStorageTest", TestClass.class, key1, key2, key3, key4, key5);
    private static final Category<TestClass> emptyTestCategory = new Category("MongoEmptyCategory", TestClass.class);

    private StartupConfiguration conf;
    private Mongo m;
    private DB db;
    private DBCollection testCollection, emptyTestCollection, mockedCollection;
    private DBCursor cursor;
    private ExpressionFactory factory;

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

        cursor = mock(DBCursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(value1).thenReturn(value2).thenReturn(null);

        testCollection = PowerMockito.mock(DBCollection.class);
        when(testCollection.find(any(DBObject.class))).thenReturn(cursor);
        when(testCollection.find()).thenReturn(cursor);
        when(testCollection.findOne(any(DBObject.class))).thenReturn(value1);
        when(testCollection.getCount()).thenReturn(2L);
        
        WriteResult mockWriteResult = mock(WriteResult.class);
        // fake that 1 record was affected for all db write ops.
        when(mockWriteResult.getN()).thenReturn(1);
        // mock for remove
        when(testCollection.remove(any(DBObject.class))).thenReturn(mockWriteResult);
        // mock for update
        when(testCollection.update(any(DBObject.class), any(DBObject.class))).thenReturn(mockWriteResult);
        // mock for replace
        when(testCollection.update(any(DBObject.class), any(DBObject.class), eq(true), eq(false))).thenReturn(mockWriteResult);
        // mock for add
        when(testCollection.insert(any(DBObject.class))).thenReturn(mockWriteResult);
        
        emptyTestCollection = PowerMockito.mock(DBCollection.class);
        when(emptyTestCollection.getCount()).thenReturn(0L);
        when(db.collectionExists(anyString())).thenReturn(false);
        when(db.createCollection(anyString(), any(DBObject.class))).thenReturn(testCollection);
        
        factory = new ExpressionFactory();
        
        Set<String> collectionNames = new LinkedHashSet<>();
        collectionNames.add("testCollection");
        collectionNames.add("emptyTestCollection");
        when(db.getCollectionNames()).thenReturn(collectionNames);
        when(db.getCollectionFromString("testCollection")).thenReturn(testCollection);
        when(db.getCollectionFromString("emptyTestCollection")).thenReturn(emptyTestCollection);
    }

    @After
    public void tearDown() {
        conf = null;
        m = null;
        db = null;
        testCollection = null;
        emptyTestCollection = null;
        cursor = null;
    }
    
    @Test
    public void testRegisterCategory() throws Exception {
        DB db = PowerMockito.mock(DB.class);
        CountDownLatch latch = new CountDownLatch(1);
        MongoStorage storage = new MongoStorage(db, latch);
        latch.countDown();
        storage.registerCategory(HostInfoDAO.hostInfoCategory);
        Category<AggregateCount> countCat = new CategoryAdapter<HostInfo, AggregateCount>(HostInfoDAO.hostInfoCategory).getAdapted(AggregateCount.class);
        storage.registerCategory(countCat);
        verify(db).collectionExists(eq(HostInfoDAO.hostInfoCategory.getName()));
    }

    @Test
    public void verifyFindAllReturnsCursor() throws Exception {
        MongoStorage storage = makeStorage();
        Query query = storage.createQuery(testCategory);
        Cursor<TestClass> cursor = query.execute();
        assertNotNull(cursor);
    }

    @Test
    public void verifyFindAllCallsDBCollectionFind() throws Exception {
        MongoStorage storage = makeStorage();
        Query query = storage.createQuery(testCategory);
        ExpressionFactory factory = new ExpressionFactory();
        Expression expr = factory.equalTo(key1, "fluff");
        query.where(expr);
        query.execute();
        verify(testCollection).find(any(DBObject.class));
    }

    @Test
    public void verifyFindAllCallsDBCollectionFindWithCorrectQuery() throws Exception {
        MongoStorage storage = makeStorage();

        MongoQuery query = mock(MongoQuery.class);
        when(query.hasClauses()).thenReturn(true);
        DBObject generatedQuery = mock(DBObject.class);
        when(query.getGeneratedQuery()).thenReturn(generatedQuery);
        when(query.getCategory()).thenReturn(testCategory);

        storage.findAllPojos(query, TestClass.class);

        verify(testCollection).find(same(generatedQuery));
    }

    @Test
    public void verifyFindAllReturnsCorrectCursor() throws Exception {
        MongoStorage storage = makeStorage();
        // TODO find a way to test this that isn't just testing MongoCursor
        // Because we mock the DBCollection, the contents of this query don't actually determine the result.
        Query query = storage.createQuery(testCategory);
        Cursor<TestClass> cursor = query.execute();

        verifyDefaultCursor(cursor);
    }

    @Test
    public void verifyFindAllWithSortAndLimit() throws Exception {
        MongoStorage storage = makeStorage();
        // TODO find a way to test this that isn't just testing MongoCursor
        // Because we mock the DBCollection, the contents of this query don't actually determine the result.
        Query query = storage.createQuery(testCategory);
        query.sort(key1, Query.SortDirection.ASCENDING);
        query.limit(3);

        Cursor<TestClass> cursor = query.execute();

        verifyDefaultCursor(cursor);
        ArgumentCaptor<DBObject> orderBy = ArgumentCaptor.forClass(DBObject.class);
        verify(this.cursor).sort(orderBy.capture());
        assertTrue(orderBy.getValue().containsField("key1"));
        assertEquals(1, orderBy.getValue().get("key1"));
        verify(this.cursor).limit(3);
    }

    @Test
    public void verifyFindAllFromCategoryCallsDBCollectionFindAll() throws Exception {
        MongoStorage storage = makeStorage();
        Query query = storage.createQuery(testCategory);
        query.execute();
        verify(testCollection).find();
    }

    @Test
    public void verifyFindAllFromCategoryReturnsCorrectCursor() throws Exception {
        MongoStorage storage = makeStorage();
        Query query = storage.createQuery(testCategory);
        Cursor<TestClass> cursor = query.execute();

        verifyDefaultCursor(cursor);
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
        MongoStorage storage = makeStorage();
        TestClass pojo = new TestClass();
        pojo.setAgentId("123");
        Add add = storage.createAdd(testCategory);
        add.setPojo(pojo);
        add.apply();
        ArgumentCaptor<DBObject> dbobj = ArgumentCaptor.forClass(DBObject.class);
        verify(testCollection).insert(dbobj.capture());
        DBObject val = dbobj.getValue();
        assertEquals("123", val.get("agentId"));
    }

    @Test
    public void verifyPutChunkDoesNotUseGlobalAgent() throws Exception {
        MongoStorage storage = makeStorage();
        TestClass pojo = new TestClass();
        Add add = storage.createAdd(testCategory);
        add.setPojo(pojo);
        try {
            add.apply();
            fail("We do not allow null agentId");
        } catch (AssertionError e) {
            // pass
        }
        Replace replace = storage.createReplace(testCategory);
        ExpressionFactory factory = new ExpressionFactory();
        Expression whereExp = factory.equalTo(Key.AGENT_ID, "foobar");
        replace.setPojo(pojo);
        replace.where(whereExp);
        try {
            replace.apply();
            fail("We do not allow null agentId");
        } catch (AssertionError e) {
            // pass
        }
    }

    @Test
    public void verifyLoadFile() throws Exception {
        InputStream stream = mock(InputStream.class);
        GridFSDBFile file = mock(GridFSDBFile.class);
        when(file.getInputStream()).thenReturn(stream);
        GridFS gridFS = mock(GridFS.class);
        when(gridFS.findOne("test")).thenReturn(file);
        PowerMockito.whenNew(GridFS.class).withArguments(any()).thenReturn(gridFS);
        MongoStorage storage = makeStorage();

        InputStream actual = storage.loadFile("test");
        assertSame(stream, actual);

        actual = storage.loadFile("doesnotexist");
        assertNull(actual);
    }

    @Test
    public void verifySimpleUpdate() {
        MongoStorage storage = makeStorage();
        Update update = storage.createUpdate(testCategory);
        Expression expr = factory.equalTo(Key.AGENT_ID, "test1");
        update.where(expr);
        update.set(key2, "test2");
        update.apply();

        ArgumentCaptor<DBObject> queryCaptor = ArgumentCaptor.forClass(DBObject.class);
        ArgumentCaptor<DBObject> valueCaptor = ArgumentCaptor.forClass(DBObject.class);
        
        verify(testCollection).update(queryCaptor.capture(), valueCaptor.capture());
        DBObject query = queryCaptor.getValue();
        assertTrue(query.containsField(Key.AGENT_ID.getName()));
        assertEquals("test1", query.get(Key.AGENT_ID.getName()));

        DBObject set = valueCaptor.getValue();
        assertEquals(1, set.keySet().size());
        assertTrue(set.containsField("$set"));
        DBObject values = (DBObject) set.get("$set");
        assertEquals(1, values.keySet().size());
        assertTrue(values.containsField(key2.getName()));
        assertEquals("test2", values.get(key2.getName()));
    }

    @Test
    public void verifyMultiFieldUpdate() {
        MongoStorage storage = makeStorage();
        Update update = storage.createUpdate(testCategory);
        Expression expr = factory.equalTo(Key.AGENT_ID, "test1");
        update.where(expr);
        update.set(key2, "test2");
        update.set(key3, "test3");
        update.apply();

        ArgumentCaptor<DBObject> queryCaptor = ArgumentCaptor.forClass(DBObject.class);
        ArgumentCaptor<DBObject> valueCaptor = ArgumentCaptor.forClass(DBObject.class);
        
        verify(testCollection).update(queryCaptor.capture(), valueCaptor.capture());
        DBObject query = queryCaptor.getValue();
        assertTrue(query.containsField(Key.AGENT_ID.getName()));
        assertEquals("test1", query.get(Key.AGENT_ID.getName()));

        DBObject set = valueCaptor.getValue();
        assertTrue(set.containsField("$set"));
        DBObject values = (DBObject) set.get("$set");
        assertTrue(values.containsField("key2"));
        assertEquals("test2", values.get("key2"));
        assertTrue(values.containsField("key3"));
        assertEquals("test3", values.get("key3"));
    }

    @Test
    public void verifyReplace() {
        TestClass pojo = new TestClass();
        pojo.setAgentId("123");
        pojo.setKey1("test1");
        pojo.setKey2("test2");
        pojo.setKey3("test3");
        pojo.setKey4("test4");
        pojo.setKey5("test5");

        MongoStorage storage = makeStorage();
        Replace replace = storage.createReplace(testCategory);
        ExpressionFactory factory = new ExpressionFactory();
        Expression first = factory.equalTo(key1, "test1");
        Expression second = factory.equalTo(key2, "test2");
        Expression and = factory.and(first, second);
        replace.where(and);
        replace.setPojo(pojo);
        replace.apply();

        ArgumentCaptor<DBObject> queryCaptor = ArgumentCaptor.forClass(DBObject.class);
        ArgumentCaptor<DBObject> valueCaptor = ArgumentCaptor.forClass(DBObject.class);
        verify(testCollection).update(queryCaptor.capture(), valueCaptor.capture(), eq(true), eq(false));

        DBObject query = queryCaptor.getValue();
        assertEquals("expected explicit and query", 1, query.keySet().size());
        Object andObj = query.get("$and");
        assertNotNull(andObj);
        assertTrue(andObj instanceof BasicDBList);
        BasicDBList list = (BasicDBList)andObj;
        assertEquals("expected two operands", 2, list.size());
        DBObject firstCond = (DBObject)list.get(0);
        DBObject secondCond = (DBObject)list.get(1);
        assertEquals("test1", firstCond.get("key1"));
        assertEquals("test2", secondCond.get("key2"));

        DBObject value = valueCaptor.getValue();
        assertEquals(6, value.keySet().size());
        assertEquals("test1", value.get("key1"));
        assertEquals("test2", value.get("key2"));
        assertEquals("test3", value.get("key3"));
        assertEquals("test4", value.get("key4"));
        assertEquals("test5", value.get("key5"));
        assertEquals("123", value.get("agentId"));
    }
    
    @Test
    public void verifyMongoCloseOnShutdown() throws Exception {
        Mongo mockMongo = mock(Mongo.class);
        when(db.getMongo()).thenReturn(mockMongo);
        MongoStorage storage = new MongoStorage(conf);
        setDbFieldInStorage(storage);
        storage.shutdown();
        verify(mockMongo).close();
    }

    @Test
    public void verifyDBPurge() throws Exception {
        MongoStorage storage = makeStorage();
        setDbFieldInStorage(storage);
        String agentId = "agentId123";
        BasicDBObject query = new BasicDBObject(Key.AGENT_ID.getName(), agentId);
        storage.purge(agentId);
        
        verify(testCollection, times(1)).remove(query);
        verify(emptyTestCollection, times(1)).remove(query);
    }

    private void setDbFieldInStorage(MongoStorage storage) throws Exception {
        // use a bit of reflection to set the db field
        Field dbField = storage.getClass().getDeclaredField("db");
        dbField.setAccessible(true);
        dbField.set(storage, db);
    }
}

