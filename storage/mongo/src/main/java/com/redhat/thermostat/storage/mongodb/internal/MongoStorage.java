/*
 * Copyright 2012-2014 Red Hat, Inc.
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
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
import com.redhat.thermostat.storage.core.Statement;
import com.redhat.thermostat.storage.core.StatementDescriptor;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.AggregateCount;
import com.redhat.thermostat.storage.model.AggregateResult;
import com.redhat.thermostat.storage.model.Pojo;
import com.redhat.thermostat.storage.query.Expression;

/**
 * Implementation of the Storage interface that uses MongoDB to store the instrumentation data.
 *
 * In this implementation, each CATEGORY is given a distinct collection.
 */
public class MongoStorage implements BackingStorage {
    
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
            return new MongoCountQuery<>(query, category);
        }
    }
    
    private static abstract class MongoSetter<T extends Pojo> {
        
        protected final DBObject values;
        protected final Category<T> category;
        
        private MongoSetter(Category<T> category) {
            this.category = category;
            this.values = new BasicDBObject();
        }
        
        private Object convertPojo(Object value) {
            // convert pojo values to mongo DB objects if need be
            if (value instanceof Pojo) {
                Pojo pojo = (Pojo)value;
                MongoPojoConverter converter = new MongoPojoConverter();
                value = converter.convertPojoToMongo(pojo);
            } else if (value instanceof Pojo[]) {
                List<DBObject> pojos = new ArrayList<>();
                MongoPojoConverter converter = new MongoPojoConverter();
                Pojo[] list = (Pojo[])value;
                for (Pojo p: list) {
                    DBObject converted = converter.convertPojoToMongo(p);
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
        
        private DBObject query;
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
            return replaceImpl(category, values, query);
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

        private DBObject query;
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
            DBObject setValues = new BasicDBObject(SET_MODIFIER, values);
            return updateImpl(category, setValues, query);
        }

        @Override
        public Statement<T> getRawDuplicate() {
            return new MongoUpdate<>(category);
        }
    }
    
    private class MongoRemove<T extends Pojo> implements Remove<T> {

        private final Category<T> category;
        private DBObject query;
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
            return removePojo(category, query);
        }

        @Override
        public Statement<T> getRawDuplicate() {
            return new MongoRemove<>(category);
        }
        
    }

    private final MongoConnection conn;
    private final Map<String, DBCollection> collectionCache = new HashMap<String, DBCollection>();
    private final CountDownLatch connectedLatch;
    private DB db = null;

    // For testing only
    MongoStorage(DB db, CountDownLatch latch) {
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
                    db = conn.getDB();
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
    
    public <T extends Pojo> Cursor<T> executeGetCount(Category<T> category, MongoQuery<T> queryToAggregate) {
        try {
            DBCollection coll = getCachedCollection(category);
            long count = 0L;
            DBObject query = queryToAggregate.getGeneratedQuery();
            if (coll != null) {
                count = coll.getCount(query);
            }
            AggregateCount result = new AggregateCount();
            result.setCount(count);
            return result.getCursor();
        } catch (MongoException me) {
            throw new StorageException(me);
        }
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

    private <T extends Pojo> int addImpl(final Category<T> cat, final DBObject values) {
        try {
            DBCollection coll = getCachedCollection(cat);
            assertContainsWriterID(values);
            WriteResult result = coll.insert(values);
            return numAffectedRecords(result);
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    private <T extends Pojo> int replaceImpl(final Category<T> cat, final DBObject values, final DBObject query) {
        try {
            DBCollection coll = getCachedCollection(cat);
            assertContainsWriterID(values);
            WriteResult result = coll.update(query, values, true, false);
            return numAffectedRecords(result);
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }
    
    private int numAffectedRecords(WriteResult result) {
        // response code corresponds to the number of records affected.
        int responseCode = result.getN();
        return responseCode;
    }

    private void assertContainsWriterID(final DBObject values) {
        if (values.get(Key.AGENT_ID.getName()) == null) {
            throw new AssertionError("agentId must be set");
        }
    }

    private <T extends Pojo> int updateImpl(Category<T> category, DBObject values, DBObject query) {
        try {
            DBCollection coll = getCachedCollection(category);
            WriteResult result = coll.update(query, values);
            return numAffectedRecords(result);
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    private int removePojo(Category<?> category, DBObject query) {
        try {
            DBCollection coll = getCachedCollection(category);
            WriteResult result = coll.remove(query);
            return numAffectedRecords(result);
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    private DBCollection getCachedCollection(Category<?> category) {
        String collName = category.getName();
        DBCollection coll = collectionCache.get(collName);
        if (coll == null && db.collectionExists(collName)) {
            throw new IllegalStateException("Categories need to be registered before being used");
        }
        return coll;
    }

    // TODO: This method is only temporary to enable tests, until we come up with a better design,
    // in particular, the collection should be stored in the category itself. It must not be called
    // from production code.
    void mapCategoryToDBCollection(Category<?> category, DBCollection coll) {
        collectionCache.put(category.getName(), coll);
    }


    @Override
    public void purge(String agentId) {
        try {
            BasicDBObject query = new BasicDBObject(Key.AGENT_ID.getName(), agentId);
            for (String collectionName : db.getCollectionNames()) {
                // Mongodb creates an internal collection called
                // "system.indexes". Don't delete anything in there as this
                // would throw MongoException later (error code 12050,
                // msg: "cannot delete from system namespace"
                if (collectionName.startsWith("system.")) {
                    continue;
                }
                DBCollection coll = db.getCollectionFromString(collectionName);
                coll.remove(query);
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

            DBCollection coll;
            if (! db.collectionExists(name)) {
                coll = db.createCollection(name, new BasicDBObject("capped", false));
                for (Key<?> key: category.getIndexedKeys()) {
                    coll.ensureIndex(key.getName());
                }
            } else {
                coll = db.getCollection(name);
            }
            collectionCache.put(name, coll);
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
            DBCollection coll = getCachedCollection(mongoQuery.getCategory());
            DBCursor dbCursor;
            if (mongoQuery.hasClauses()) {
                dbCursor = coll.find(mongoQuery.getGeneratedQuery());
            } else {
                dbCursor = coll.find();
            }
            dbCursor = applySortAndLimit(mongoQuery, dbCursor);
            return new MongoCursor<T>(dbCursor, resultClass);
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    private DBCursor applySortAndLimit(MongoQuery<?> query, DBCursor dbCursor) {
        BasicDBObject orderBy = new BasicDBObject();
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
    public void saveFile(String filename, InputStream data) {
        try {
            GridFS gridFS = new GridFS(db);
            GridFSInputFile inputFile = gridFS.createFile(data, filename);
            inputFile.save();
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    @Override
    public InputStream loadFile(String filename) {
        try {
            GridFS gridFS = new GridFS(db);
            GridFSDBFile file = gridFS.findOne(filename);
            if (file == null) {
                return null;
            } else {
                return file.getInputStream();
            }
        } catch (MongoException me) {
            throw new StorageException(me);
        }
    }

    @Override
    public void shutdown() {
        try {
            // Clean up any pending connections. mongo-java-driver issue 130
            // suggests that Mongo.close() helps with this ThreadLocal business
            // tomcat warns about. See also:
            // IcedTea BZ#1315 and https://jira.mongodb.org/browse/JAVA-130
            db.getMongo().close();
        } catch (Exception e) {
            // ignored
        }
    }

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
    public <T extends Pojo> Query<T> createAggregateQuery(
            AggregateFunction function, Category<T> category) {
        switch (function) {
        case COUNT:
            MongoQuery<T> query = (MongoQuery<T>)createQuery(category);
            return new MongoCountQuery<>(query, category);
        default:
            throw new IllegalStateException("function not supported: "
                    + function);
        }
    }

}

