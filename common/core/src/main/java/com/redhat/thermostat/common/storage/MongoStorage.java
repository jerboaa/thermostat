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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import com.redhat.thermostat.common.config.StartupConfiguration;
import com.redhat.thermostat.common.dao.Converter;
import com.redhat.thermostat.common.dao.VmMemoryStatConverter;
import com.redhat.thermostat.common.model.Pojo;
import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.storage.Connection.ConnectionListener;
import com.redhat.thermostat.common.storage.Connection.ConnectionStatus;

/**
 * Implementation of the Storage interface that uses MongoDB to store the instrumentation data.
 *
 * In this implementation, each CATEGORY is given a distinct collection.
 */
public class MongoStorage extends Storage {

    public static final String SET_MODIFIER = "$set";

    private MongoConnection conn;
    private DB db = null;
    private Map<String, DBCollection> collectionCache = new HashMap<String, DBCollection>();

    private UUID agentId = null;

    private Map<Class<?>, Converter<?>> converters;

    public MongoStorage(StartupConfiguration conf) {
        setupConverters();
        conn = new MongoConnection(conf);
        conn.addListener(new ConnectionListener() {
            @Override
            public void changed(ConnectionStatus newStatus) {
                switch (newStatus) {
                case DISCONNECTED:
                    db = null;
                case CONNECTED:
                    db = conn.getDB();
                default:
                    // ignore other status events
                }
            }
        });
    }

    private void setupConverters() {
        converters = new HashMap<>();
        converters.put(VmMemoryStat.class, new VmMemoryStatConverter());
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

    private BasicDBObject getAgentQueryKeyFromGlobalAgent() {
        if (agentId != null) {
            return new BasicDBObject(Key.AGENT_ID.getName(), agentId.toString());
        } else {
            return null;
        }
    }

    private BasicDBObject getAgentQueryKeyFromChunkOrGlobalAgent(Chunk chunk) {
        BasicDBObject queryKey = getAgentQueryKeyFromGlobalAgent();
        if (queryKey != null) {
            return queryKey;
        } else if (chunk.get(Key.AGENT_ID) != null) {
            return new BasicDBObject(Key.AGENT_ID.getName(), chunk.get(Key.AGENT_ID));
        } else {
            return null;
        }
    }

    // TODO: Make this private, and change the testcase to test putPojo() instead.
    void putChunk(Chunk chunk) {
        Category cat = chunk.getCategory();
        DBCollection coll = getCachedCollection(cat.getName());
        BasicDBObject toInsert = getAgentQueryKeyFromChunkOrGlobalAgent(chunk);
        BasicDBObject replaceKey = null;
        boolean replace = chunk.getReplace();
        Map<String, BasicDBObject> nestedParts = new HashMap<String, BasicDBObject>();
        Map<String, BasicDBObject> replaceKeyNestedParts = null;
        if (replace) {
            replaceKey = getAgentQueryKeyFromChunkOrGlobalAgent(chunk);
            replaceKeyNestedParts = new HashMap<String, BasicDBObject>();
        }
        for (Key<?> key : cat.getKeys()) {
            boolean isKey = key.isPartialCategoryKey();
            String[] entryParts = key.getName().split("\\.");
            if (entryParts.length == 2) {
                BasicDBObject nested = nestedParts.get(entryParts[0]);
                if (nested == null) {
                    if (isKey) {
                        throwMissingKey(key.getName(), chunk);
                    }
                    nested = new BasicDBObject();
                    nestedParts.put(entryParts[0], nested);
                }
                nested.append(entryParts[1], chunk.get(key));
                if (replace && isKey) {
                    BasicDBObject replaceKeyNested = replaceKeyNestedParts.get(entryParts[0]);
                    if (replaceKeyNested == null) {
                        replaceKeyNested = new BasicDBObject();
                        replaceKeyNestedParts.put(entryParts[0], replaceKeyNested);
                    }
                    replaceKeyNested.append(entryParts[1], replaceKeyNested);
                }
            } else {
                /* we dont modify agent id, and it's already used as key in updateKey */
                if (!key.equals(Key.AGENT_ID)) {
                    String mongoKey = key.getName();
                    Object value = chunk.get(key);
                    if ((value == null) && isKey) {
                        throwMissingKey(key.getName(), chunk);
                    }
                    toInsert.append(mongoKey, value);
                    if (replace && isKey) {
                        replaceKey.append(mongoKey, value);
                    }
                }
            }
        }
        for (Entry<String, BasicDBObject> entry: nestedParts.entrySet()) {
            toInsert.append(entry.getKey(), entry.getValue());
        }
        if (replace) {
            for (Entry<String, BasicDBObject> entry: replaceKeyNestedParts.entrySet()) {
                replaceKey.append(entry.getKey(), entry.getValue());
            }
            coll.update(replaceKey, toInsert, true, false);
        } else {
            coll.insert(toInsert);
        }
    }

    @Override
    public void updatePojo(Update update) {
        assert update instanceof MongoUpdate;
        MongoUpdate mongoUpdate = (MongoUpdate) update;
        Chunk chunk = mongoUpdate.getChunk();
        updateChunk(chunk);
    }

    void updateChunk(Chunk chunk) {
        Category cat = chunk.getCategory();
        DBCollection coll = getCachedCollection(cat.getName());
        BasicDBObject toUpdate = new BasicDBObject();
        BasicDBObject updateKey = getAgentQueryKeyFromChunkOrGlobalAgent(chunk);
        BasicDBObject setObj = null;
        Map<String, BasicDBObject> nestedParts = new HashMap<String, BasicDBObject>();
        Map<String, BasicDBObject> updateKeyNestedParts = new HashMap<String, BasicDBObject>();
        for (Key<?> key : cat.getKeys()) {
            boolean isKey = key.isPartialCategoryKey();
            String[] entryParts = key.getName().split("\\.");
            if (entryParts.length == 2) {
                BasicDBObject nested = nestedParts.get(entryParts[0]);
                if (nested == null) {
                    if (isKey) {
                        throwMissingKey(key.getName(), chunk);
                    }
                } else {
                    if (isKey) {
                        BasicDBObject updateKeyNested = updateKeyNestedParts.get(entryParts[0]);
                        if (updateKeyNested == null) {
                            updateKeyNested = new BasicDBObject();
                            updateKeyNestedParts.put(entryParts[0], updateKeyNested);
                        }
                        updateKeyNested.append(entryParts[1], updateKeyNested);
                    } else {
                        if (setObj == null) {
                            setObj = new BasicDBObject();
                            nested.append(SET_MODIFIER, setObj);
                        }
                        setObj.append(entryParts[1], chunk.get(key));
                    }
                }
            } else {
                String mongoKey = key.getName();
                /* we dont modify agent id, and it's already used as key in updateKey */
                if (!key.equals(Key.AGENT_ID)) {
                    Object value = chunk.get(key);
                    if (value == null) {
                        if (isKey) {
                            throwMissingKey(key.getName(), chunk);
                        }
                    } else {
                        if (isKey) {
                            updateKey.append(mongoKey, value);
                        } else {
                            if (setObj == null) {
                                setObj = new BasicDBObject();
                                toUpdate.append(SET_MODIFIER, setObj);
                            }
                            setObj.append(mongoKey, value);
                        }
                    }
                }
            }
        }
        for (Entry<String, BasicDBObject> entry: nestedParts.entrySet()) {
            toUpdate.append(entry.getKey(), entry.getValue());
        }
        for (Entry<String, BasicDBObject> entry: updateKeyNestedParts.entrySet()) {
            updateKey.append(entry.getKey(), entry.getValue());
        }
        coll.update(updateKey, toUpdate);
    }

    private void throwMissingKey(String keyName, Chunk chunk) {
        throw new IllegalArgumentException("Attempt to insert chunk with incomplete partial key.  Missing: '" + keyName + "' in " + chunk);
    }

    @Override
    public void removeChunk(Chunk query) {
        Category category = query.getCategory();
        DBCollection coll = getCachedCollection(category.getName());

        BasicDBObject toRemove = getAgentQueryKeyFromChunkOrGlobalAgent(query);
        for (Key<?> key : category.getKeys()) {
            if (key.isPartialCategoryKey()) {
                toRemove.put(key.getName(), query.get(key));
            }
        }

        coll.remove(toRemove);
    }

    private DBCollection getCachedCollection(String collName) {
        DBCollection coll = collectionCache.get(collName);
        if (coll == null && db.collectionExists(collName)) {
            coll = db.getCollection(collName);
            if (coll != null) {
                collectionCache.put(collName, coll);
            }
        }
        return coll;
    }

    // TODO: This method is only temporary to enable tests, until we come up with a better design,
    // in particular, the collection should be stored in the category itself. It must not be called
    // from production code.
    void mapCategoryToDBCollection(Category category, DBCollection coll) {
        collectionCache.put(category.getName(), coll);
    }


    @Override
    public void purge() {
        BasicDBObject deleteKey = getAgentQueryKeyFromGlobalAgent();
        for (DBCollection coll : collectionCache.values()) {
            coll.remove(deleteKey);
        }
    }
    
    @Override
    public ConnectionKey createConnectionKey(Category category) {
        // TODO: There is probably some better place to do this, perhaps related to the inner class
        // idea mentioned below.
        if (!db.collectionExists(category.getName())) {
            db.createCollection(category.getName(), new BasicDBObject("capped", false));
        }
        // TODO: We want to return an instance of an inner class here that carries the actual connection
        // and replace the collectionCache. For now this is good enough though.
        return new ConnectionKey(){};
    }

    @Override
    public Query createQuery() {
        return new MongoQuery();
    }

    @Override
    public Update createUpdate() {
        return new MongoUpdate();
    }

    @Override
    public <T extends Pojo> Cursor<T> findAllPojos(Query query, Class<T> resultClass) {
        MongoQuery mongoQuery =  checkAndCastQuery(query);
        DBCollection coll = getCachedCollection(mongoQuery.getCollectionName());
        DBCursor dbCursor = coll.find(mongoQuery.getGeneratedQuery());
        return new MongoCursor<T>(dbCursor, mongoQuery.getCategory(), resultClass, converters);
    }

    @Override
    public <T extends Pojo> T findPojo(Query query, Class<T> resultClass) {
        Chunk resultChunk = find(query);
        if (resultChunk == null) {
            return null;
        }
        return ChunkToPojoConverter.convertChunkToPojo(resultChunk, resultClass, converters);
    }

    // TODO: Make this private, and change the testcase to test putPojo() instead.
    Chunk find(Query query) {
        MongoQuery mongoQuery = checkAndCastQuery(query);
        DBCollection coll = getCachedCollection(mongoQuery.getCollectionName());
        DBObject dbResult = coll.findOne(mongoQuery.getGeneratedQuery());
        ChunkConverter converter = new ChunkConverter();
        return dbResult == null ? null : converter.dbObjectToChunk(dbResult, mongoQuery.getCategory());
    }

    private MongoQuery checkAndCastQuery(Query query) {
        if (!(query instanceof MongoQuery)) {
            throw new IllegalArgumentException("MongoStorage can only handle MongoQuery");
        }

        return (MongoQuery) query;

    }
    
    @Override
    public <T extends Pojo> Cursor<T> findAllPojosFromCategory(Category category, Class<T> resultClass) {
        DBCollection coll = getCachedCollection(category.getName());
        DBCursor dbCursor = coll.find();
        return new MongoCursor<T>(dbCursor, category, resultClass, converters);
    }

    @Override
    public long getCount(Category category) {
        DBCollection coll = getCachedCollection(category.getName());
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
    public void putPojo(Category category, boolean replace, Pojo pojo) {
        Chunk chunk = convertPojoToChunk(category, replace, pojo);
        putChunk(chunk);
    }

    private Chunk convertPojoToChunk(Category category, boolean replace,
            Pojo pojo) {
        Converter customConverter = converters.get(pojo.getClass());
        Chunk chunk;
        if (customConverter != null) {
            chunk = customConverter.toChunk(pojo);
        } else {
            chunk = new ChunkAdapter(pojo, category, replace);
        }
        return chunk;
    }

}
