package com.redhat.thermostat.agent.storage;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.bson.BSONObject;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.WriteConcern;
import com.redhat.thermostat.agent.config.StartupConfiguration;

/**
 * Implementation of the Storage interface that uses MongoDB to store the instrumentation data.
 * 
 * In this implementation, each CATEGORY is given a distinct collection.
 */
public class MongoStorage extends Storage {

    public static final String KEY_AGENT_ID = "agent-id";

    private Mongo mongo = null;
    private DB db = null;

    private UUID agentId = null;

    @Override
    public void connect(String uri) throws UnknownHostException {
        connect(new MongoURI(uri));
    }

    public void connect(MongoURI uri) throws UnknownHostException {
        mongo = new Mongo(uri);
        db = mongo.getDB(StorageConstants.THERMOSTAT_DB_NAME);
    }

    @Override
    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    @Override
    public void addAgentInformation(StartupConfiguration config) {
        DBCollection configCollection = db.getCollection(StorageConstants.CATEGORY_AGENT_CONFIG);
        DBObject toInsert = config.toDBObject();
        /* cast required to disambiguate between putAll(BSONObject) and putAll(Map) */
        toInsert.putAll((BSONObject) getAgentDBObject());
        configCollection.insert(toInsert, WriteConcern.SAFE);
    }

    @Override
    public void removeAgentInformation() {
        DBCollection configCollection = db.getCollection(StorageConstants.CATEGORY_AGENT_CONFIG);
        BasicDBObject toRemove = getAgentDBObject();
        configCollection.remove(toRemove, WriteConcern.NORMAL);
    }

    @Override
    public String getBackendConfig(String backendName, String configurationKey) {
        DBCollection configCollection = db.getCollection(StorageConstants.CATEGORY_AGENT_CONFIG);
        BasicDBObject query = getAgentDBObject();
        query.put(StorageConstants.KEY_AGENT_CONFIG_BACKENDS + "." + backendName, new BasicDBObject("$exists", true));
        DBObject config = configCollection.findOne(query);
        Object value = config.get(configurationKey);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    private BasicDBObject getAgentDBObject() {
        return new BasicDBObject(KEY_AGENT_ID, agentId.toString());
    }

    @Override
    protected void addChunkImpl(Chunk chunk) {
        Category cat = chunk.getCategory();
        DBCollection coll = db.getCollection(cat.getName());
        BasicDBObject toInsert = getAgentDBObject();
        BasicDBObject toDelete = null;
        boolean replace = chunk.getReplace();
        Map<String, BasicDBObject> nestedParts = new HashMap<String, BasicDBObject>();
        Map<String, BasicDBObject> deleteNestedParts = null;
        if (replace) {
            toDelete = getAgentDBObject();
            deleteNestedParts = new HashMap<String, BasicDBObject>();
        }
        for (Iterator<com.redhat.thermostat.agent.storage.Key> iter = cat.getEntryIterator(); iter.hasNext();) {
            com.redhat.thermostat.agent.storage.Key key = iter.next();
            boolean isKey = key.isPartialCategoryKey();
            String[] entryParts = key.getName().split(".");
            if (entryParts.length == 2) {
                BasicDBObject nested = nestedParts.get(entryParts[0]);
                if (nested == null) {
                    nested = new BasicDBObject();
                    nestedParts.put(entryParts[0], nested);
                }
                nested.append(entryParts[1], chunk.get(key));
                if (replace && isKey) {
                    BasicDBObject deleteNested = deleteNestedParts.get(entryParts[0]);
                    if (deleteNested == null) {
                        deleteNested = new BasicDBObject();
                        deleteNestedParts.put(entryParts[0], deleteNested);
                    }
                    deleteNested.append(entryParts[1], deleteNested);
                }
            } else {
                String mongoKey = key.getName();
                String value = chunk.get(key);
                toInsert.append(mongoKey, value);
                if (replace && isKey) {
                    toDelete.append(mongoKey, value);
                }
            }
        }
        for (String mongoKey : nestedParts.keySet()) {
            toInsert.append(mongoKey, nestedParts.get(mongoKey));
        }
        if (replace) {
            for (String mongoKey : deleteNestedParts.keySet()) {
                toDelete.append(mongoKey, deleteNestedParts.get(mongoKey));
            }
            coll.remove(toDelete);
        }
        coll.insert(toInsert);
    }
}
