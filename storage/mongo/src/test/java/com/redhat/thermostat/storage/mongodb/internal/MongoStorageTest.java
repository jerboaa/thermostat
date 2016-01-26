/*
 * Copyright 2012-2016 Red Hat, Inc.
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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.mongodb.MongoClient;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.AggregateQuery;
import com.redhat.thermostat.storage.core.AggregateQuery.AggregateFunction;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.CategoryAdapter;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Entity;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Persist;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.SaveFileListener;
import com.redhat.thermostat.storage.core.SaveFileListener.EventType;
import com.redhat.thermostat.storage.core.SchemaInfo;
import com.redhat.thermostat.storage.core.Statement;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.BasePojo;
import com.redhat.thermostat.storage.model.DistinctResult;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;
import com.redhat.thermostat.storage.query.ExpressionFactory;

//There is a bug (resolved as wontfix) in powermock which results in
//java.lang.LinkageError if javax.management.* classes aren't ignored by
//Powermock. More here: http://code.google.com/p/powermock/issues/detail?id=277
//SSL tests need this and having that annotation on method level doesn't seem
//to solve the issue.
@PowerMockIgnore( {"javax.management.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({ MongoCollection.class, MongoDatabase.class, MongoStorage.class, MongoConnection.class })
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
    private static final Category<TestClass> emptyTestCategory = new Category<>("MongoEmptyCategory", TestClass.class);

    private String url;
    private StorageCredentials creds;
    private SSLConfiguration sslConf;
    private MongoClient m;
    private MongoDatabase db;
    private MongoCollection<Document> testCollection, emptyTestCollection, mockedCollection;
    private FindIterable<Document> cursor;
    private ExpressionFactory factory;

    private MongoStorage makeStorage() {
        MongoStorage storage = new MongoStorage(url, creds, sslConf);
        storage.mapCategoryToDBCollection(testCategory, testCollection);
        storage.mapCategoryToDBCollection(emptyTestCategory, emptyTestCollection);
        return storage;
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        url = "mongodb://127.0.0.1:27518";
        creds = mock(StorageCredentials.class);
        sslConf = mock(SSLConfiguration.class);
        db = PowerMockito.mock(MongoDatabase.class);
        m = PowerMockito.mock(MongoClient.class);
        mockedCollection = mock(MongoCollection.class);
        when(m.getDatabase(anyString())).thenReturn(db);
        when(db.getCollection("agent-config")).thenReturn(mockedCollection);

        Document value1 = new Document();
        value1.put("key1", "test1");
        value1.put("key2", "test2");
        Document value2 = new Document();
        value2.put("key3", "test3");
        value2.put("key4", "test4");

        cursor = mock(FindIterable.class);
        com.mongodb.client.MongoCursor<Document> mongoCursor = mock(com.mongodb.client.MongoCursor.class);
        
        when(cursor.iterator()).thenReturn(mongoCursor);
        when(mongoCursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mongoCursor.next()).thenReturn(value1).thenReturn(value2).thenReturn(null);

        testCollection = PowerMockito.mock(MongoCollection.class);
        when(testCollection.find(any(Document.class))).thenReturn(cursor);
        when(testCollection.find()).thenReturn(cursor);
        when(testCollection.count()).thenReturn(2L);
        
        // mock for remove
        DeleteResult result = mock(DeleteResult.class);
        when(testCollection.deleteOne(any(Document.class))).thenReturn(result);
        when(result.getDeletedCount()).thenReturn(1L);
        // mock for update
        UpdateResult updateResult = mock(UpdateResult.class);
        when(updateResult.getModifiedCount()).thenReturn(1L);
        when(testCollection.updateOne(any(Document.class), any(Document.class))).thenReturn(updateResult);
        // mock for replace
        UpdateResult replaceResult = mock(UpdateResult.class);
        when(replaceResult.getModifiedCount()).thenReturn(2L);
        when(testCollection.replaceOne(any(Document.class), any(Document.class))).thenReturn(replaceResult);
        
        emptyTestCollection = PowerMockito.mock(MongoCollection.class);
        when(emptyTestCollection.count()).thenReturn(0L);
        
        factory = new ExpressionFactory();
        
        MongoIterable<String> mongoIterable = mock(MongoIterable.class);
        com.mongodb.client.MongoCursor<String> it = mock(com.mongodb.client.MongoCursor.class);
        when(it.hasNext()).thenReturn(true).thenReturn(true);
        when(it.next()).thenReturn("testCollection").thenReturn("emptyTestCollection");
        when(mongoIterable.iterator()).thenReturn(it);
        when(db.listCollectionNames()).thenReturn(mongoIterable);
        when(db.getCollection("testCollection")).thenReturn(testCollection);
        when(db.getCollection("emptyTestCollection")).thenReturn(emptyTestCollection);
    }

    @After
    public void tearDown() {
        url = null;
        m = null;
        db = null;
        testCollection = null;
        emptyTestCollection = null;
        cursor = null;
    }
    
    @Test
    public void isBackingStorage() {
        MongoStorage storage = new MongoStorage(url, creds, sslConf);
        assertTrue(storage instanceof BackingStorage);
    }
    
    @Test
    public void verifyConnectEventSetsDb() {
        MongoConnection mockConnection = mock(MongoConnection.class);
        
        // This adds a listener which we capture
        new MongoStorage(mockConnection);
        ArgumentCaptor<ConnectionListener> captor = ArgumentCaptor.forClass(ConnectionListener.class);
        verify(mockConnection).addListener(captor.capture());
        ConnectionListener listener = captor.getValue();
        assertNotNull(listener);
        when(mockConnection.getDatabase()).thenReturn(db);
        @SuppressWarnings("unchecked")
        MongoIterable<String> mockIterable = mock(MongoIterable.class);
        when(db.listCollectionNames()).thenReturn(mockIterable);
        @SuppressWarnings("unchecked")
        com.mongodb.client.MongoCursor<String> mockCursor = mock(com.mongodb.client.MongoCursor.class);
        when(mockCursor.hasNext()).thenReturn(true).thenReturn(false);
        when(mockCursor.next()).thenReturn("foo-bar-collection");
        when(mockIterable.iterator()).thenReturn(mockCursor);
        listener.changed(ConnectionStatus.CONNECTED);
        verify(mockConnection).getDatabase();
        verify(db).createCollection(eq("schema-info"), any(CreateCollectionOptions.class));
    }
    
    @Test(expected=NullPointerException.class)
    public void verifyDisconnectedEventInvalidatesDb() {
        MongoConnection mockConnection = mock(MongoConnection.class);
        
        // This adds a listener which we capture
        MongoStorage storage = new MongoStorage(mockConnection);
        
        ArgumentCaptor<ConnectionListener> captor = ArgumentCaptor.forClass(ConnectionListener.class);
        verify(mockConnection).addListener(captor.capture());
        ConnectionListener listener = captor.getValue();
        assertNotNull(listener);
        
        // fire a connecting event for good measure. It should do nothing.
        listener.changed(ConnectionStatus.CONNECTING);
        
        listener.changed(ConnectionStatus.CONNECTED);
        verify(mockConnection).getDatabase();
        
        // This should set the db instance to null
        listener.changed(ConnectionStatus.DISCONNECTED);
        
        @SuppressWarnings("unchecked")
        Category<FakeDataClass> cat = mock(Category.class);
        when(cat.getDataClass()).thenReturn(FakeDataClass.class);
        // this throws NPE due to the null DB instance
        storage.registerCategory(cat);
    }
    
    @Test
    public void testRegisterCategory() throws Exception {
        MongoDatabase db = PowerMockito.mock(MongoDatabase.class);
        CountDownLatch latch = new CountDownLatch(1);
        MongoStorage storage = new MongoStorage(db, latch);
        latch.countDown();
        
        when(db.getCollection(SchemaInfo.CATEGORY.getName())).thenReturn(testCollection);
        storage.registerCategory(SchemaInfo.CATEGORY);
        verify(db, times(0)).createCollection(eq(SchemaInfo.CATEGORY.getName()), any(CreateCollectionOptions.class));
        verify(testCollection, times(0)).replaceOne(any(Document.class), any(Document.class));
        try {
            storage.registerCategory(SchemaInfo.CATEGORY);
            fail();
        } catch (IllegalStateException ex) {
            assertEquals("Category may only be associated with one backend.", ex.getMessage());
        }
        
        @SuppressWarnings("unchecked")
        MongoIterable<String> mockIterable = mock(MongoIterable.class);
        when(db.listCollectionNames()).thenReturn(mockIterable);
        @SuppressWarnings("unchecked")
        com.mongodb.client.MongoCursor<String> mockCursor = mock(com.mongodb.client.MongoCursor.class);
        when(mockIterable.iterator()).thenReturn(mockCursor);
        storage.registerCategory(HostInfoDAO.hostInfoCategory);
        Category<AggregateCount> countCat = new CategoryAdapter<HostInfo, AggregateCount>(HostInfoDAO.hostInfoCategory).getAdapted(AggregateCount.class);
        storage.registerCategory(countCat);
        
        ArgumentCaptor<Document> msgCaptor = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<Document> msgCaptor2 = ArgumentCaptor.forClass(Document.class);
        Document expectedObject1 = new Document(SchemaInfo.NAME.getName(), HostInfoDAO.hostInfoCategory.getName());
        
        verify(testCollection).replaceOne(msgCaptor.capture(), msgCaptor2.capture(), any(UpdateOptions.class));
        
        Document resultObject1 = msgCaptor.getValue();
        Document resultObject2 = msgCaptor2.getValue();
       
        assertEquals(expectedObject1, resultObject1);
        assertTrue(resultObject2.get(SchemaInfo.NAME.getName()) != null);
        assertTrue(resultObject2.get(Key.TIMESTAMP.getName()) != null);
        
        assertEquals(HostInfoDAO.hostInfoCategory.getName(), resultObject2.get(SchemaInfo.NAME.getName()));
        assertNotNull(resultObject2.get(Key.TIMESTAMP.getName()));
    }

    @Test
    public void verifyFindAllReturnsCursor() throws Exception {
        MongoStorage storage = makeStorage();
        Query<TestClass> query = storage.createQuery(testCategory);
        Cursor<TestClass> cursor = query.execute();
        assertNotNull(cursor);
    }

    @Test
    public void verifyFindAllCallsDBCollectionFind() throws Exception {
        MongoStorage storage = makeStorage();
        Query<TestClass> query = storage.createQuery(testCategory);
        ExpressionFactory factory = new ExpressionFactory();
        Expression expr = factory.equalTo(key1, "fluff");
        query.where(expr);
        query.execute();
        verify(testCollection).find(any(Document.class));
    }

    @Test
    public void verifyFindAllCallsDBCollectionFindWithCorrectQuery() throws Exception {
        MongoStorage storage = makeStorage();

        @SuppressWarnings("unchecked")
        MongoQuery<TestClass> query = mock(MongoQuery.class);
        when(query.hasClauses()).thenReturn(true);
        Document generatedQuery = mock(Document.class);
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
        Query<TestClass> query = storage.createQuery(testCategory);
        Cursor<TestClass> cursor = query.execute();

        verifyDefaultCursor(cursor);
    }

    @Test
    public void verifyFindAllWithSortAndLimit() throws Exception {
        MongoStorage storage = makeStorage();
        // TODO find a way to test this that isn't just testing MongoCursor
        // Because we mock the DBCollection, the contents of this query don't actually determine the result.
        Query<TestClass> query = storage.createQuery(testCategory);
        query.sort(key1, Query.SortDirection.ASCENDING);
        query.limit(3);

        Cursor<TestClass> cursor = query.execute();

        verifyDefaultCursor(cursor);
        ArgumentCaptor<Document> orderBy = ArgumentCaptor.forClass(Document.class);
        verify(this.cursor).sort(orderBy.capture());
        assertTrue(orderBy.getValue().get("key1") != null);
        assertEquals(1, orderBy.getValue().get("key1"));
        verify(this.cursor).limit(3);
    }

    @Test
    public void verifyFindAllFromCategoryCallsDBCollectionFindAll() throws Exception {
        MongoStorage storage = makeStorage();
        Query<TestClass> query = storage.createQuery(testCategory);
        query.execute();
        verify(testCollection).find();
    }

    @Test
    public void verifyFindAllFromCategoryReturnsCorrectCursor() throws Exception {
        MongoStorage storage = makeStorage();
        Query<TestClass> query = storage.createQuery(testCategory);
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
        try {
            cursor.next();
            fail("Cursor should throw a NoSuchElementException!");
        } catch (NoSuchElementException e) {
            // pass
        }
    }

    @Test (expected=NullPointerException.class)
    public void verifySaveFileNullListenerThrowsException() throws Exception{
        InputStream dataStream = new ByteArrayInputStream(new byte[0]);
        MongoStorage storage = makeStorage();
        storage.saveFile("test", dataStream, null);
    }

    @Test
    public void verifySaveFile() throws Exception {
        byte[] data = new byte[] { 1, 2, 3 };
        InputStream dataStream = new ByteArrayInputStream(data);

        SaveFileListener saveFileListener = mock(SaveFileListener.class);

        CountDownLatch latch = new CountDownLatch(0);
        MongoDatabase db = mock(MongoDatabase.class);
        final GridFSBucket mockBucket = mock(GridFSBucket.class);
        MongoStorage storage = new MongoStorage(db, latch) {
            
            @Override
            GridFSBucket createGridFSBucket() {
                return mockBucket;
            }
        };
        storage.saveFile("test", dataStream, saveFileListener);

        verify(saveFileListener).notify(EventType.SAVE_COMPLETE, null);
        verify(mockBucket).uploadFromStream("test", dataStream);
    }

    @Test
    public void verifyPutChunkUsesCorrectChunkAgent() throws Exception {
        MongoStorage storage = makeStorage();
        TestClass pojo = new TestClass();
        pojo.setAgentId("123");
        Add<TestClass> add = storage.createAdd(testCategory);
        add.set(Key.AGENT_ID.getName(), pojo.getAgentId());
        add.apply();
        ArgumentCaptor<Document> dbobj = ArgumentCaptor.forClass(Document.class);
        verify(testCollection).insertOne(dbobj.capture());
        Document val = dbobj.getValue();
        assertEquals("123", val.get("agentId"));
    }
    
    /*
     *  This test verifies if pojo types get converted correctly. This
     *  add isn't really using the correct type parameter. It should be using
     *  a pojo which has itself Pojo properties defined. For the sake of this
     *  test this "hack" should be sufficient.
     */
    @Test
    public void verifyPojoTypeGetsConverted() throws Exception {
        MongoStorage storage = makeStorage();
        TestClass pojo = new TestClass();
        pojo.setKey1("val1");
        pojo.setKey3("val3");
        Add<?> add = storage.createAdd(testCategory);
        add.set("agentId", "ignored");
        add.set("foo-pojo-key", pojo);
        add.apply();
        ArgumentCaptor<Document> dbobj = ArgumentCaptor.forClass(Document.class);
        verify(testCollection).insertOne(dbobj.capture());
        Document val = dbobj.getValue();
        assertTrue(val.get("foo-pojo-key") instanceof Document);
        Document pojoVal = (Document)val.get("foo-pojo-key");
        assertEquals("val1", pojoVal.get("key1"));
        assertEquals("val3", pojoVal.get("key3"));
    }
    
    @Test
    public void verifyPojoListGetsConverted() throws Exception {
        MongoStorage storage = makeStorage();
        TestClass pojo = new TestClass();
        pojo.setKey1("val1");
        pojo.setKey3("val3");
        TestClass[] list = new TestClass[] { pojo };
        Add<?> add = storage.createAdd(testCategory);
        add.set("agentId", "ignored");
        add.set("foo-pojo-key", list);
        add.apply();
        ArgumentCaptor<Document> dbobj = ArgumentCaptor.forClass(Document.class);
        verify(testCollection).insertOne(dbobj.capture());
        Document val = dbobj.getValue();
        assertTrue(val.get("foo-pojo-key") instanceof List);
        @SuppressWarnings("unchecked")
        List<Document> result = (List<Document>)val.get("foo-pojo-key");
        assertEquals(1, result.size());
        Document pojoVal = result.get(0);
        assertEquals("val1", pojoVal.get("key1"));
        assertEquals("val3", pojoVal.get("key3"));
    }
    
    @Test
    public void verifyAggregateDistinct() throws Exception {
        // setup
        @SuppressWarnings("unchecked")
        DistinctIterable<String> iterable = mock(DistinctIterable.class);
        @SuppressWarnings("unchecked")
        com.mongodb.client.MongoCursor<String> mockCursor = mock(com.mongodb.client.MongoCursor.class);
        when(iterable.iterator()).thenReturn(mockCursor);
        when(mockCursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mockCursor.next()).thenReturn("-1").thenReturn("200");
        when(testCollection.distinct(eq(key1.getName()), any(Document.class), eq(String.class))).thenReturn(iterable);
        
        MongoStorage storage = makeStorage();
        CategoryAdapter<TestClass, DistinctResult> adapter = new CategoryAdapter<>(testCategory);
        Category<DistinctResult> adaptedCategory = adapter.getAdapted(DistinctResult.class);
        AggregateQuery<DistinctResult> aggQuery = storage.createAggregateQuery(AggregateFunction.DISTINCT, adaptedCategory);
        aggQuery.setAggregateKey(key1);
        Cursor<DistinctResult> cursor = aggQuery.execute();
        assertTrue(cursor.hasNext());
        DistinctResult r = cursor.next();
        String[] expected = new String[] { "-1", "200" };
        assertArrayEquals(expected, r.getValues());
        assertEquals(key1, r.getKey());
        
        // do it again with a list of booleans
        @SuppressWarnings("unchecked")
        DistinctIterable<String> iterable2 = mock(DistinctIterable.class);
        @SuppressWarnings("unchecked")
        com.mongodb.client.MongoCursor<String> mockCursor2 = mock(com.mongodb.client.MongoCursor.class);
        when(iterable2.iterator()).thenReturn(mockCursor2);
        when(mockCursor2.hasNext()).thenReturn(true).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mockCursor2.next()).thenReturn("false").thenReturn("true").thenReturn("true");
        when(testCollection.distinct(eq(key2.getName()), any(Document.class), eq(String.class))).thenReturn(iterable2);
        aggQuery = storage.createAggregateQuery(AggregateFunction.DISTINCT, adaptedCategory);
        aggQuery.setAggregateKey(key2);
        cursor = aggQuery.execute();
        assertTrue(cursor.hasNext());
        r = cursor.next();
        expected = new String[] { "false", "true", "true" };
        assertArrayEquals(expected, r.getValues());
        assertEquals(key2, r.getKey());
    }
    
    /*
     * The statement descriptor subsystem uses getRawDuplicate. That duplicate
     * needs to keep the key parameter set for distinct aggregate queries.
     */
    @Test
    public void canDuplicateAggregateDistinct() throws Exception {
        MongoStorage storage = makeStorage();
        CategoryAdapter<TestClass, DistinctResult> adapter = new CategoryAdapter<>(testCategory);
        Category<DistinctResult> adaptedCategory = adapter.getAdapted(DistinctResult.class);
        AggregateQuery<DistinctResult> aggQuery = storage.createAggregateQuery(AggregateFunction.DISTINCT, adaptedCategory);
        assertNull(aggQuery.getAggregateKey());
        aggQuery.setAggregateKey(key1);
        
        Statement<DistinctResult> stmt = aggQuery.getRawDuplicate();
        AggregateQuery<DistinctResult> rawCopy = (AggregateQuery<DistinctResult>) stmt;
        assertEquals("Expected key to be copied for dupe", key1, rawCopy.getAggregateKey());
    }
    
    /*
     * The statement descriptor subsystem uses getRawDuplicate. That duplicate
     * needs to keep the key parameter if set for count aggregates.
     */
    @Test
    public void canDuplicateAggregateCountWithKey() throws Exception {
        MongoStorage storage = makeStorage();
        CategoryAdapter<TestClass, AggregateCount> adapter = new CategoryAdapter<>(testCategory);
        Category<AggregateCount> adaptedCategory = adapter.getAdapted(AggregateCount.class);
        AggregateQuery<AggregateCount> aggQuery = storage.createAggregateQuery(AggregateFunction.COUNT, adaptedCategory);
        
        // Produce duplicate without key set
        assertNull(aggQuery.getAggregateKey());
        Statement<AggregateCount> stmt = aggQuery.getRawDuplicate();
        assertTrue(stmt instanceof AggregateQuery);
        AggregateQuery<AggregateCount> rawCopy = (AggregateQuery<AggregateCount>) stmt;

        // Duplicate with key set
        aggQuery.setAggregateKey(key1);
        assertNotNull(aggQuery.getAggregateKey());
        stmt = aggQuery.getRawDuplicate();
        assertTrue(stmt instanceof AggregateQuery);
        rawCopy = (AggregateQuery<AggregateCount>) stmt;
        assertEquals("Expected key to be copied for dupe", key1, rawCopy.getAggregateKey());
    }
    
    private static void assertArrayEquals(String[] expected, String[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    @Test
    public void verifyPutChunkDoesNotUseGlobalAgent() throws Exception {
        MongoStorage storage = makeStorage();
        Add<TestClass> add = storage.createAdd(testCategory);
        try {
            add.apply();
            fail("We do not allow null agentId");
        } catch (AssertionError e) {
            // pass
        }
        Replace<TestClass> replace = storage.createReplace(testCategory);
        ExpressionFactory factory = new ExpressionFactory();
        Expression whereExp = factory.equalTo(Key.AGENT_ID, "foobar");
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
        final GridFSBucket mockBucket = mock(GridFSBucket.class);
        MongoStorage storage = new MongoStorage(mock(MongoDatabase.class), new CountDownLatch(0)) {
            
            @Override
            GridFSBucket createGridFSBucket() {
                return mockBucket;
            }
        };
        storage.loadFile("test");
        verify(mockBucket).openDownloadStreamByName("test");
    }

    @Test
    public void verifySimpleUpdate() {
        MongoStorage storage = makeStorage();
        Update<TestClass> update = storage.createUpdate(testCategory);
        Expression expr = factory.equalTo(Key.AGENT_ID, "test1");
        update.where(expr);
        update.set(key2.getName(), "test2");
        update.apply();

        ArgumentCaptor<Document> queryCaptor = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<Document> valueCaptor = ArgumentCaptor.forClass(Document.class);
        
        verify(testCollection).updateOne(queryCaptor.capture(), valueCaptor.capture());
        Document query = queryCaptor.getValue();
        assertTrue(query.get(Key.AGENT_ID.getName()) != null);
        assertEquals("test1", query.get(Key.AGENT_ID.getName()));

        Document set = valueCaptor.getValue();
        assertEquals(1, set.keySet().size());
        assertTrue(set.get("$set") != null);
        Document values = (Document) set.get("$set");
        assertEquals(1, values.keySet().size());
        assertTrue(values.get(key2.getName()) != null);
        assertEquals("test2", values.get(key2.getName()));
    }

    @Test
    public void verifyMultiFieldUpdate() {
        MongoStorage storage = makeStorage();
        Update<TestClass> update = storage.createUpdate(testCategory);
        Expression expr = factory.equalTo(Key.AGENT_ID, "test1");
        update.where(expr);
        update.set(key2.getName(), "test2");
        update.set(key3.getName(), "test3");
        update.apply();

        ArgumentCaptor<Document> queryCaptor = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<Document> valueCaptor = ArgumentCaptor.forClass(Document.class);
        
        verify(testCollection).updateOne(queryCaptor.capture(), valueCaptor.capture());
        Document query = queryCaptor.getValue();
        assertTrue(query.get(Key.AGENT_ID.getName()) != null);
        assertEquals("test1", query.get(Key.AGENT_ID.getName()));

        Document set = valueCaptor.getValue();
        assertTrue(set.get("$set") != null);
        Document values = (Document) set.get("$set");
        assertTrue(values.get("key2") != null);
        assertEquals("test2", values.get("key2"));
        assertTrue(values.get("key3") != null);
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
        when(testCollection.replaceOne(any(Bson.class), any(Document.class), any(UpdateOptions.class))).thenReturn(mock(UpdateResult.class));
        Replace<TestClass> replace = storage.createReplace(testCategory);
        ExpressionFactory factory = new ExpressionFactory();
        Expression first = factory.equalTo(key1, "test1");
        Expression second = factory.equalTo(key2, "test2");
        Expression and = factory.and(first, second);
        replace.where(and);
        replace.set("key1", pojo.getKey1());
        replace.set("key2", pojo.getKey2());
        replace.set("key3", pojo.getKey3());
        replace.set("key4", pojo.getKey4());
        replace.set("key5", pojo.getKey5());
        replace.set(Key.AGENT_ID.getName(), pojo.getAgentId());
        replace.apply();

        ArgumentCaptor<Document> queryCaptor = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<Document> valueCaptor = ArgumentCaptor.forClass(Document.class);
        verify(testCollection).replaceOne(queryCaptor.capture(), valueCaptor.capture(), any(UpdateOptions.class));

        Document query = queryCaptor.getValue();
        assertEquals("expected explicit and query", 1, query.keySet().size());
        Object andObj = query.get("$and");
        assertNotNull(andObj);
        assertTrue(andObj instanceof List);
        @SuppressWarnings("unchecked")
        List<Document> list = (List<Document>)andObj;
        assertEquals("expected two operands", 2, list.size());
        Document firstCond = list.get(0);
        Document secondCond = list.get(1);
        assertEquals("test1", firstCond.get("key1"));
        assertEquals("test2", secondCond.get("key2"));

        Document value = valueCaptor.getValue();
        assertEquals(6, value.keySet().size());
        assertEquals("test1", value.get("key1"));
        assertEquals("test2", value.get("key2"));
        assertEquals("test3", value.get("key3"));
        assertEquals("test4", value.get("key4"));
        assertEquals("test5", value.get("key5"));
        assertEquals("123", value.get("agentId"));
    }

    @Test
    public void verifyRemove() {
        MongoStorage storage = makeStorage();
        when(testCollection.deleteMany(any(Bson.class))).thenReturn(mock(DeleteResult.class));
        Remove<TestClass> remove = storage.createRemove(testCategory);
        Expression expr = factory.equalTo(Key.AGENT_ID, "test1");
        remove.where(expr);

        remove.apply();

        verify(testCollection).deleteMany(new Document(Key.AGENT_ID.getName(), "test1"));
    }

    @Test
    public void verifyRemoveWithoutWhere() {
        MongoStorage storage = makeStorage();
        when(testCollection.deleteMany(any(Bson.class))).thenReturn(mock(DeleteResult.class));
        Remove<TestClass> remove = storage.createRemove(testCategory);

        remove.apply();

        verify(testCollection).deleteMany(new Document());
    }

    @Test
    public void verifyDBPurge() throws Exception {
        MongoDatabase database = mock(MongoDatabase.class);
        @SuppressWarnings("unchecked")
        MongoIterable<String> stringIterable = mock(MongoIterable.class);
        @SuppressWarnings("unchecked")
        com.mongodb.client.MongoCursor<String> mockCursor = mock(com.mongodb.client.MongoCursor.class);
        when(mockCursor.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
        when(mockCursor.next()).thenReturn(testCategory.getName()).thenReturn(emptyTestCategory.getName());
        when(stringIterable.iterator()).thenReturn(mockCursor);
        when(database.listCollectionNames()).thenReturn(stringIterable);
        when(database.getCollection(eq(testCategory.getName()))).thenReturn(testCollection);
        when(database.getCollection(eq(emptyTestCategory.getName()))).thenReturn(emptyTestCollection);
        MongoStorage storage = new MongoStorage(database, new CountDownLatch(0));
        String agentId = "agentId123";
        Document query = new Document(Key.AGENT_ID.getName(), agentId);
        storage.purge(agentId);
        
        verify(testCollection, times(1)).deleteMany(query);
        verify(emptyTestCollection, times(1)).deleteMany(query);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void verifySchemaInfoNotExists() throws Exception {
        MongoDatabase db = PowerMockito.mock(MongoDatabase.class);
        MongoIterable<String> mockIterable = mock(MongoIterable.class);
        when(db.listCollectionNames()).thenReturn(mockIterable);
        when(mockIterable.iterator()).thenReturn(mock(com.mongodb.client.MongoCursor.class));
        CountDownLatch latch = new CountDownLatch(1);
        MongoStorage storage = new MongoStorage(db, latch);
        ArgumentCaptor<String> categoryNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CreateCollectionOptions> createOptions = ArgumentCaptor.forClass(CreateCollectionOptions.class);
        
        storage.createSchemaInfo();
        verify(db, times(1)).createCollection(categoryNameCaptor.capture(), createOptions.capture());
        assertEquals(SchemaInfo.CATEGORY.getName(), categoryNameCaptor.getValue());
        CreateCollectionOptions actual = createOptions.getValue();
        assertFalse(actual.isCapped());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void verifySchemaInfoExists() throws Exception {
        MongoDatabase db = PowerMockito.mock(MongoDatabase.class);
        MongoIterable<String> mockIterable = mock(MongoIterable.class);
        when(db.listCollectionNames()).thenReturn(mockIterable);
        com.mongodb.client.MongoCursor<String> cursor = mock(com.mongodb.client.MongoCursor.class);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(SchemaInfo.CATEGORY.getName());
        when(mockIterable.iterator()).thenReturn(cursor);
        CountDownLatch latch = new CountDownLatch(1);
        MongoStorage storage = new MongoStorage(db, latch);
        
        when(db.getCollection(eq(SchemaInfo.CATEGORY.getName()))).thenReturn(mock(MongoCollection.class));
        storage.createSchemaInfo();
        verify(db, times(0)).createCollection(any(String.class), any(CreateCollectionOptions.class));
    }
    
    @Test
    public void verifyInsertSchemaInfo() throws Exception {
        MongoDatabase db = PowerMockito.mock(MongoDatabase.class);
        CountDownLatch latch = new CountDownLatch(1);
        MongoStorage storage = new MongoStorage(db, latch);
        ArgumentCaptor<Document> basicDBCaptor1 = ArgumentCaptor.forClass(Document.class);
        ArgumentCaptor<Document> basicDBCaptor2 = ArgumentCaptor.forClass(Document.class);
        Document expected1 = new Document(SchemaInfo.NAME.getName(), testCategory.getName());
        
        when(db.getCollection(SchemaInfo.CATEGORY.getName())).thenReturn(testCollection);
        storage.insertSchemaInfo(testCategory);
        verify(testCollection).replaceOne(basicDBCaptor1.capture(), basicDBCaptor2.capture(), any(UpdateOptions.class));

        assertEquals(expected1, basicDBCaptor1.getValue());
        Document arg = (Document) basicDBCaptor2.getValue();
        
        assertTrue( arg.get(SchemaInfo.NAME.getName()) != null );
        assertEquals(testCategory.getName(), arg.get(SchemaInfo.NAME.getName()));
        assertTrue( arg.get(Key.TIMESTAMP.getName()) != null );
        assertNotNull(arg.get(Key.TIMESTAMP.getName()));
    }
    
    private static class FakeDataClass implements Pojo {};
}

