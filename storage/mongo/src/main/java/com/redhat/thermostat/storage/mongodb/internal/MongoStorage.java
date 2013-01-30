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

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.redhat.thermostat.storage.config.StartupConfiguration;
import com.redhat.thermostat.storage.core.AbstractQuery.Sort;
import com.redhat.thermostat.storage.core.Add;
import com.redhat.thermostat.storage.core.BasePut;
import com.redhat.thermostat.storage.core.Category;
import com.redhat.thermostat.storage.core.Connection;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;
import com.redhat.thermostat.storage.core.Connection.ConnectionStatus;
import com.redhat.thermostat.storage.core.Cursor;
import com.redhat.thermostat.storage.core.Key;
import com.redhat.thermostat.storage.core.Query;
import com.redhat.thermostat.storage.core.Remove;
import com.redhat.thermostat.storage.core.Replace;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.Update;
import com.redhat.thermostat.storage.model.Pojo;

/**
 * Implementation of the Storage interface that uses MongoDB to store the instrumentation data.
 *
 * In this implementation, each CATEGORY is given a distinct collection.
 */
public class MongoStorage implements Storage {

    private class MongoAdd extends BasePut implements Add {

        @Override
        public void apply() {
            addImpl(getCategory(), getPojo());
        }
        
    }

    private class MongoReplace extends BasePut implements Replace {

        @Override
        public void apply() {
            replaceImpl(getCategory(), getPojo());
        }
        
    }

    private MongoConnection conn;
    private DB db = null;
    private Map<String, DBCollection> collectionCache = new HashMap<String, DBCollection>();
    private CountDownLatch connectedLatch;
    private UUID agentId;

    public MongoStorage(StartupConfiguration conf) {
        conn = new MongoConnection(conf);
        connectedLatch = new CountDownLatch(1);
        conn.addListener(new ConnectionListener() {
            @Override
            public void changed(ConnectionStatus newStatus) {
                switch (newStatus) {
                case DISCONNECTED:
                    db = null;
                case CONNECTED:
                    db = conn.getDB();
                    // This is important. See comment in registerCategory().
                    connectedLatch.countDown();
                default:
                    // ignore other status events
                }
            }
        });
    }

    @Override
    public Connection getConnection() {
        return conn;
    }

    @Override
    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    @Override
    public String getAgentId() {
        return agentId.toString();
    }

    @Override
    public Add createAdd(Category<?> into) {
        MongoAdd add = new MongoAdd();
        add.setCategory(into);
        return add;
    }

    @Override
    public Replace createReplace(Category<?> into) {
        MongoReplace replace = new MongoReplace();
        replace.setCategory(into);
        return replace;
    }

    private void addImpl(final Category<?> cat, final Pojo pojo) {
        DBCollection coll = getCachedCollection(cat);
        DBObject toInsert = preparePut(pojo);
        coll.insert(toInsert);
    }

    private void replaceImpl(final Category<?> cat, final Pojo pojo) {
        DBCollection coll = getCachedCollection(cat);
        DBObject toInsert = preparePut(pojo);

        DBObject query = new BasicDBObject();
        Collection<Key<?>> keys = cat.getKeys();
        for (Key<?> key : keys) {
            if (key.isPartialCategoryKey()) {
                String name = key.getName();
                query.put(name, toInsert.get(name));
            }
        }
        coll.update(query, toInsert, true, false);
    }

    private DBObject preparePut(final Pojo pojo) {
        MongoPojoConverter converter = new MongoPojoConverter();
        DBObject toInsert = converter.convertPojoToMongo(pojo);
        if (toInsert.get(Key.AGENT_ID.getName()) == null) {
            toInsert.put(Key.AGENT_ID.getName(), getAgentId());
        }
        return toInsert;
    }

    void updatePojo(MongoUpdate mongoUpdate) {
        Category<?> cat = mongoUpdate.getCategory();
        DBCollection coll = getCachedCollection(cat);
        DBObject query = mongoUpdate.getQuery();
        DBObject values = mongoUpdate.getValues();
        coll.update(query, values);
    }

    @Override
    public void removePojo(Remove remove) {
        assert (remove instanceof MongoRemove);
        MongoRemove mongoRemove = (MongoRemove) remove;
        DBObject query = mongoRemove.getQuery();
        Category<?> category = mongoRemove.getCategory();
        DBCollection coll = getCachedCollection(category);

        coll.remove(query);
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
    public void purge() {
        BasicDBObject query = new BasicDBObject(Key.AGENT_ID.getName(), getAgentId());
        for (DBCollection coll : collectionCache.values()) {
            coll.remove(query);
        }
    }
    
    @Override
    public void registerCategory(Category<?> category) {
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
        } else {
            coll = db.getCollection(name);
        }
        collectionCache.put(name, coll);
    }

    @Override
    public <T extends Pojo> Query<T> createQuery(Category<T> category) {
        return new MongoQuery<T>(this, category);
    }

    @Override
    public Update createUpdate(Category<?> category) {
        return new MongoUpdate(this, category);
    }

    @Override
    public Remove createRemove() {
        return new MongoRemove();
    }

    <T extends Pojo> Cursor<T> findAllPojos(MongoQuery<T> mongoQuery, Class<T> resultClass) {
        DBCollection coll = getCachedCollection(mongoQuery.getCategory());
        DBCursor dbCursor;
        if (mongoQuery.hasClauses()) {
            dbCursor = coll.find(mongoQuery.getGeneratedQuery());
        } else {
            dbCursor = coll.find();
        }
        dbCursor = applySortAndLimit(mongoQuery, dbCursor);
        return new MongoCursor<T>(dbCursor, resultClass);
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
    public long getCount(Category<?> category) {
        DBCollection coll = getCachedCollection(category);
        if (coll != null) {
            return coll.getCount();
        }
        return 0L;
    }

    @Override
    public void saveFile(String filename, InputStream data) {
        GridFS gridFS = new GridFS(db);
        GridFSInputFile inputFile = gridFS.createFile(data, filename);
        inputFile.save();
    }

    @Override
    public InputStream loadFile(String filename) {
        GridFS gridFS = new GridFS(db);
        GridFSDBFile file = gridFS.findOne(filename);
        if (file == null) {
            return null;
        } else {
            return file.getInputStream();
        }
    }

    @Override
    public void shutdown() {
        // Nothing to do here.
    }

}

