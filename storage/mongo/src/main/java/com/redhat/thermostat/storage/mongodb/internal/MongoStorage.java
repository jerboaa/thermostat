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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.bson.Document;

import com.mongodb.MongoException;
import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.AbstractQuery.Sort;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.AggregateQuery;
import com.redhat.thermostat.storage.core.AggregateQuery.AggregateFunction;
import com.redhat.thermostat.storage.core.BackingStorage;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.DescriptorParsingException;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.PreparedStatement;
import com.redhat.thermostat.storage.core.PreparedStatementFactory;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.SaveFileListener;
import com.redhat.thermostat.storage.core.SaveFileListener.EventType;
import com.redhat.thermostat.storage.core.SchemaInfo;
import com.redhat.thermostat.storage.core.SchemaInfoInserter;
import com.redhat.thermostat.storage.core.Statement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.AggregateResult;
import com.redhat.thermostat.storage.model.DistinctResult;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;

/**
 * Implementation of the Storage interface that uses MongoDB to store the instrumentation data.
 *
 * In this implementation, each CATEGORY is given a distinct collection.
 */
public class MongoStorage implements BackingStorage, SchemaInfoInserter {
    
    private static final Logger logger = LoggingUtils.getLogger(MongoStorage.class);
    
    private class MongoDistinctQuery<T extends Pojo> extends AggregateQuery<T> {

        private final Category<T> category;
        
        private MongoDistinctQuery(MongoQuery<T> queryToAggregate, Category<T> category) {
            super(AggregateFunction.DISTINCT, queryToAggregate);
            this.category = category;
        }
        
        @Override
        public Cursor<T> execute() {
            return executeDistinctQuery(this, category, (MongoQuery<T>)queryToAggregate);
        }

        @Override
        public Statement<T> getRawDuplicate() {
            MongoQuery<T> query = (MongoQuery<T>) this.queryToAggregate;
            MongoDistinctQuery<T> aggQuery = new MongoDistinctQuery<>(query, category);
            // Distinct queries require this param. It's static so pass this
            // on to duplicates.
            aggQuery.setAggregateKey(getAggregateKey());
            return aggQuery;
        }
        
    }
    
    private class MongoCountQuery<T extends Pojo> extends AggregateQuery<T> {
        
        private final Category<T> category;
        
        private MongoCountQuery(MongoQuery<T> queryToAggregate, Category<T> category) {
            super(AggregateFunction.COUNT, queryToAggregate);
            this.category = category;
        }

        @Override
        public Cursor<T> execute() {
            return executeGetCount(category, (MongoQuery<T>)this.queryToAggregate);
        }

        @Override
        public Statement<T> getRawDuplicate() {
            MongoQuery<T> query = (MongoQuery<T>) this.queryToAggregate;
            MongoCountQuery<T> dupe = new MongoCountQuery<>(query, category);
            // Count aggregates have an optional key to aggregate. Optional for
            // backwards compat reasons. Be sure to copy it over if it was set.
            if (getAggregateKey() != null) {
                dupe.setAggregateKey(getAggregateKey());
            }
            return dupe;
        }
    }
    
    private static abstract class MongoSetter<T extends Pojo> {
        
        protected final Document values;
        protected final Category<T> category;
        
        private MongoSetter(Category<T> category) {
            this.category = category;
            this.values = new Document();
        }
        
        private Object convertPojo(Object value) {
            // convert pojo values to mongo DB objects if need be
            if (value instanceof Pojo) {
                Pojo pojo = (Pojo)value;
                MongoPojoConverter converter = new MongoPojoConverter();
                value = converter.convertPojoToMongo(pojo);
            } else if (value instanceof Pojo[]) {
                List<Document> pojos = new ArrayList<>();
                MongoPojoConverter converter = new MongoPojoConverter();
                Pojo[] list = (Pojo[])value;
                for (Pojo p: list) {
                    Document converted = converter.convertPojoToMongo(p);
                    pojos.add(converted);
                }
                value = pojos;
            }
            return value;
        }
        
        protected void set(String key, Object value) {
            // convert Pojo/list of Pojos to DBObject/list of DBObjects
            // if need be
            value = convertPojo(value);
            values.put(key, value);
        }
    }

    private class MongoAdd<T extends Pojo> extends MongoSetter<T>
            implements Add<T> {
        
        private MongoAdd(Category<T> category) {
            super(category);
        }
        
        @Override
        public int apply() {
            return addImpl(category, values);
        }

        @Override
        public void set(String key, Object value) {
            super.set(key, value);
        }

        @Override
        public Statement<T> getRawDuplicate() {
            return new MongoAdd<>(category);
        }
        
    }

    private class MongoReplace<T extends Pojo> extends MongoSetter<T>
            implements Replace<T> {
        
        private Document query;
        private final MongoExpressionParser parser;

        private MongoReplace(Category<T> category) {
            super(category);
            this.parser = new MongoExpressionParser();
        }
        
        @Override
        public int apply() {
            if (query == null) {
                String msg = "where expression must be set. " +
                             "Please call where() before apply().";
                throw new IllegalStateException(msg);
            }
            return (int)replaceImpl(category, values, query);
        }

        @Override
        public void where(Expression expression) {
            this.query = parser.parse(Objects.requireNonNull(expression));
        }

        @Override
        public void set(String key, Object value) {
            super.set(key, value);
        }

        @Override
        public Statement<T> getRawDuplicate() {
            return new MongoReplace<>(category);
        }
        
    }
    
    private class MongoUpdate<T extends Pojo> extends MongoSetter<T>
            implements Update<T> {

        private static final String SET_MODIFIER = "$set";

        private Document query;
        private final MongoExpressionParser parser;

        private MongoUpdate(Category<T> category) {
            super(category);
            this.parser = new MongoExpressionParser();
        }

        @Override
        public void where(Expression expr) {
            query = parser.parse(expr);
        }

        @Override
        public void set(String key, Object value) {
            super.set(key, value);
        }

        @Override
        public int apply() {
            Document setValues = new Document(SET_MODIFIER, values);
            return (int)updateImpl(category, setValues, query);
        }

        @Override
        public Statement<T> getRawDuplicate() {
            return new MongoUpdate<>(category);
        }
    }
    
    private class MongoRemove<T extends Pojo> implements Remove<T> {

        private final Category<T> category;
        private Document query;
        private final MongoExpressionParser parser;
        
        private MongoRemove(Category<T> category) {
            this(category, new MongoExpressionParser());
        }
        
        private MongoRemove(Category<T> category, MongoExpressionParser parser) {
            this.parser = parser;
            this.category = category;
        }

        @Override
        public void where(Expression expr) {
            query = parser.parse(expr);
        }
        
        @Override
        public int apply() {
            if (query == null) {
                query = new Document();
            }
            return (int)removePojo(category, query);
        }

        @Override
        public Statement<T> getRawDuplicate() {
            return new MongoRemove<>(category);
        }
        
    }

    private final MongoConnection conn;
    private final Map<String, MongoCollection<Document>> collectionCache = new HashMap<>();
    private final CountDownLatch connectedLatch;
    private MongoDatabase db = null;

    // For testing only
    MongoStorage(MongoDatabase db, CountDownLatch latch) {
        this.db = db;
        this.connectedLatch = latch;
        this.conn = null;
    }
    
    MongoStorage(MongoConnection connection) {
        this.conn = connection;
        connectedLatch = new CountDownLatch(1);
        
        // We register a connection listener in order for the mongo-java-driver
        // DB object to be valid once it's first used (that's usually in
        // registerCategory())
        conn.addListener(new ConnectionListener() {
            @Override
            public void changed(ConnectionStatus newStatus) {
                switch (newStatus) {
                case CONNECTED:
                    // Main success entry point
                    db = conn.getDatabase();
                    createSchemaInfo();
                    // This is important. See comment in registerCategory().
                    connectedLatch.countDown();
                    break;
                case FAILED_TO_CONNECT:
                    // Main connection-failure entry-point
                    connectedLatch.countDown();
                    break;
                case CONNECTING:
                    // no-op
                    break;
                case DISCONNECTED:
                    // mark the db object invalid
                    db = null;
                    break;
                }
            }
        });
    }
    
    public MongoStorage(String url, StorageCredentials creds, SSLConfiguration sslConf) {
        this(new MongoConnection(url, creds, sslConf));
    }
    
    @Override
    public Connection getConnection() {
        return conn;
    }

    @Override
    public <T extends Pojo> Add<T> createAdd(Category<T> into) {
        MongoAdd<T> add = new MongoAdd<>(into);
        return add;
    }

    @Override
    public <T extends Pojo> Replace<T> createReplace(Category<T> into) {
        MongoReplace<T> replace = new MongoReplace<>(into);
        return replace;
    }

    private <T extends Pojo> Cursor<T> executeGetCount(Category<T> category, MongoQuery<T> queryToAggregate) {
        try {
            MongoCollection<Document> coll = getCachedCollection(category);
            long count = 0L;
            Document query = queryToAggregate.getGeneratedQuery();
            if (coll != null) {
                count = coll.count(query);
            }
            AggregateCount result = new AggregateCount();
            result.setCount(count);
            return result.getCursor();
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }
    
    private <T extends Pojo> Cursor<T> executeDistinctQuery(MongoDistinctQuery<T> aggQuery, Category<T> category, MongoQuery<T> queryToAggregate) {
        try {
            MongoCollection<Document> coll = getCachedCollection(category);
            Document query = queryToAggregate.getGeneratedQuery();
            String[] distinctValues;
            Key<?> aggregateKey = aggQuery.getAggregateKey();
            if (coll != null) {
                String key = aggregateKey.getName();
                DistinctIterable<String> iterable = coll.distinct(key, query, String.class);
                distinctValues = convertToStringList(iterable, key);
            } else {
                distinctValues = new String[0];
            }
            DistinctResult result = new DistinctResult();
            result.setKey(aggregateKey);
            result.setValues(distinctValues);
            return result.getCursor();
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }
    
    private String[] convertToStringList(Iterable<String> iterable, String keyName) {
        List<String> stringList = new ArrayList<>();
        Iterator<String> iter = iterable.iterator();
        while (iter.hasNext()) {
            String item  = iter.next();
            // Mongodb might give us null values.
            if (item != null) {
                stringList.add(item);
            }
        }
        return stringList.toArray(new String[0]);
    }

    private <T extends Pojo> int addImpl(final Category<T> cat, final Document values) {
        try {
            MongoCollection<Document> coll = getCachedCollection(cat);
            assertContainsWriterID(values);
            coll.insertOne(values);
            return 1;
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    private <T extends Pojo> long replaceImpl(final Category<T> cat, final Document values, final Document query) {
        try {
            MongoCollection<Document> coll = getCachedCollection(cat);
            assertContainsWriterID(values);
            UpdateResult result = coll.replaceOne(query, values, new UpdateOptions().upsert(true));
            return result.getModifiedCount();
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    private void assertContainsWriterID(final Document values) {
        if (values.get(Key.AGENT_ID.getName()) == null) {
            throw new AssertionError("agentId must be set");
        }
    }

    private <T extends Pojo> long updateImpl(Category<T> category, Document values, Document query) {
        try {
            MongoCollection<Document> coll = getCachedCollection(category);
            UpdateResult result = coll.updateOne(query, values);
            return result.getModifiedCount();
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    private long removePojo(Category<?> category, Document query) {
        try {
            MongoCollection<Document> coll = getCachedCollection(category);
            DeleteResult result = coll.deleteMany(query);
            return result.getDeletedCount();
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    private MongoCollection<Document> getCachedCollection(Category<?> category) {
        String collName = category.getName();
        MongoCollection<Document> coll = collectionCache.get(collName);
        if (coll == null && !collectionExists(collName)) {
            throw new IllegalStateException("Categories need to be registered before being used");
        }
        return coll;
    }

    // TODO: This method is only temporary to enable tests, until we come up with a better design,
    // in particular, the collection should be stored in the category itself. It must not be called
    // from production code.
    void mapCategoryToDBCollection(Category<?> category, MongoCollection<Document> coll) {
        collectionCache.put(category.getName(), coll);
    }


    @Override
    public void purge(String agentId) {
        try {
            Document query = new Document(Key.AGENT_ID.getName(), agentId);
            for (String collectionName : db.listCollectionNames()) {
                // Mongodb creates an internal collection called
                // "system.indexes". Don't delete anything in there as this
                // would throw MongoException later (error code 12050,
                // msg: "cannot delete from system namespace"
                if (collectionName.startsWith("system.")) {
                    continue;
                }
                MongoCollection<Document> coll = db.getCollection(collectionName);
                coll.deleteMany(query);
            }
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }
    
    @Override
    public void registerCategory(Category<?> category) {
        try {
            Class<?> dataClass = category.getDataClass();
            if (AggregateResult.class.isAssignableFrom(dataClass)) {
                // adapted aggregate category, no need to actually register
                return;
            }
            String name = category.getName();
            if (collectionCache.containsKey(name)) {
                throw new IllegalStateException("Category may only be associated with one backend.");
            }

            // The db field is only set once we've got a connection
            // established. Wait until we actually get notification
            // this has happened. Without this sychronization we might
            // get NPEs since the connection handshake might still be
            // ongoing.
            try {
                connectedLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            MongoCollection<Document> coll;
            Boolean isSchemaInfo = SchemaInfo.CATEGORY.getName().equals(category.getName());
            
            // Check if category is SchemaInfo, in this case it doesn't need to create this collection
            if ( !isSchemaInfo && !collectionExists(name)) {
                db.createCollection(name, new CreateCollectionOptions().capped(false));
                coll = db.getCollection(name);
                List<Key<?>> indexKeys = category.getIndexedKeys();
                // primarily on the first key then on subsequent keys
                if (indexKeys.size() >= 1) {
                    Document indexDoc = new Document(indexKeys.get(0).getName(), 1);
                    for (int i = 1; i < indexKeys.size(); i++) {
                        Key<?> key = indexKeys.get(i);
                        indexDoc.append(key.getName(), 1);
                    }
                    coll.createIndex(indexDoc);
                }
            } else {
                coll = db.getCollection(name);
            }
            collectionCache.put(name, coll);
            if(!isSchemaInfo) {
                insertSchemaInfo(category);
            }
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    @Override
    public <T extends Pojo> Query<T> createQuery(Category<T> category) {
        return new MongoQuery<T>(this, category);
    }

    @Override
    public <T extends Pojo> Update<T> createUpdate(Category<T> category) {
        return new MongoUpdate<>(category);
    }

    @Override
    public <T extends Pojo> Remove<T> createRemove(Category<T> category) {
        return new MongoRemove<>(category);
    }

    <T extends Pojo> Cursor<T> findAllPojos(MongoQuery<T> mongoQuery, Class<T> resultClass) {
        try {
            MongoCollection<Document> coll = getCachedCollection(mongoQuery.getCategory());
            FindIterable<Document> iterable;
            Document generatedQuery = mongoQuery.getGeneratedQuery();
            logger.fine("generated query is: " + generatedQuery);
            if (mongoQuery.hasClauses()) {
                iterable = coll.find(mongoQuery.getGeneratedQuery());
            } else {
                iterable = coll.find();
            }
            iterable.batchSize(Cursor.DEFAULT_BATCH_SIZE);
            iterable = applySortAndLimit(mongoQuery, iterable);
            Cursor<T> mongoCursor = new MongoCursor<T>(iterable, resultClass);
            return mongoCursor;
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    private FindIterable<Document> applySortAndLimit(MongoQuery<?> query, FindIterable<Document> dbCursor) {
        Document orderBy = new Document();
        List<Sort> sorts = query.getSorts();
        for (Sort sort : sorts) {
            orderBy.append(sort.getKey().getName(), sort.getDirection().getValue());
        }
        dbCursor.sort(orderBy);
        int limit = query.getLimit();
        if (limit > 0) {
            dbCursor.limit(limit);
        }
        return dbCursor;
    }

    @Override
    public void saveFile(String filename, InputStream data, SaveFileListener listener) {
        Objects.requireNonNull(listener);
        try {
            GridFSBucket gridFsBucket = createGridFSBucket();
            gridFsBucket.uploadFromStream(filename, data);
            listener.notify(EventType.SAVE_COMPLETE, null);
        } catch (MongoException me) {
            listener.notify(EventType.EXCEPTION_OCCURRED, new StorageException(me));
        }
    }

    // package-private for testing
    GridFSBucket createGridFSBucket() {
        return GridFSBuckets.create(db);
    }

    @Override
    public InputStream loadFile(String filename) {
        try {
            GridFSBucket gridFsBucket = createGridFSBucket();
            GridFSDownloadStream downloadStream = gridFsBucket.openDownloadStreamByName(filename);
            return downloadStream;
        } catch (MongoException me) {
            return null;
        }
    }

    @Override
    public void shutdown() {}

    /*
     *  QueuedStorage decorator uses this method and "wraps" the returned
     *  PreparedStatement so that it executes in a queued fashion.
     */
    @Override
    public <T extends Pojo> PreparedStatement<T> prepareStatement(StatementDescriptor<T> statementDesc)
            throws DescriptorParsingException {
        // FIXME: Use some kind of cache in order to avoid parsing of
        // descriptors each time this is called. At least if the descriptor
        // class is the same we should be able to do something here.
        return PreparedStatementFactory.getInstance(this, statementDesc);
    }

    @Override
    public <T extends Pojo> AggregateQuery<T> createAggregateQuery(
            AggregateFunction function, Category<T> category) {
        MongoQuery<T> query = (MongoQuery<T>)createQuery(category);
        switch (function) {
        case COUNT:
            return new MongoCountQuery<>(query, category);
        case DISTINCT:
            return new MongoDistinctQuery<>(query, category); 
        default:
            throw new IllegalStateException("function not supported: "
                    + function);
        }
    }

    @Override
    public void createSchemaInfo() {
        if (!collectionExists(SchemaInfo.CATEGORY.getName())) {
            db.createCollection(SchemaInfo.CATEGORY.getName(), new CreateCollectionOptions().capped(false));
        }
    }

    @Override
    public <T extends Pojo> void insertSchemaInfo(Category<T> category) {
        MongoCollection<Document> coll = db.getCollection(SchemaInfo.CATEGORY.getName());
        
        Document categoryInfo = new Document();
        categoryInfo.put(SchemaInfo.NAME.getName(), category.getName());
        categoryInfo.put(Key.TIMESTAMP.getName(), System.currentTimeMillis());
        Document query = new Document(SchemaInfo.NAME.getName(), category.getName());
        coll.replaceOne(query, categoryInfo, new UpdateOptions().upsert(true));
        
    }
    
    private boolean collectionExists(String name) {
        Iterator<String> it = db.listCollectionNames().iterator();
        while (it.hasNext()) {
            String collection = it.next();
            if (collection.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

}

