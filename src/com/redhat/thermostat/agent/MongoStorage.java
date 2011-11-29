package com.redhat.thermostat.agent;

import java.net.UnknownHostException;
import java.util.UUID;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.WriteConcern;
import com.redhat.thermostat.agent.config.Configuration;

public class MongoStorage implements Storage {

    private Mongo mongo = null;
    private DB db = null;

    private UUID agentId = null;

    @Override
    public void connect(String uri) throws UnknownHostException {
        connect(new MongoURI(uri));
    }

    public void connect(MongoURI uri) throws UnknownHostException {
        mongo = new Mongo(uri);
        db = mongo.getDB(StorageConstants.THERMOSTAT_DB);
    }

    @Override
    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    @Override
    public void addAgentInformation(Configuration config) {
        DBCollection configCollection = db.getCollection(StorageConstants.COLLECTION_AGENT_CONFIG);
        DBObject toInsert = config.toDBObject();
        toInsert.put(StorageConstants.KEY_AGENT_CONFIG_AGENT_ID, agentId.toString());
        configCollection.insert(toInsert, WriteConcern.SAFE);
    }

    @Override
    public void removeAgentInformation() {
        DBCollection configCollection = db.getCollection(StorageConstants.COLLECTION_AGENT_CONFIG);
        BasicDBObject toRemove = new BasicDBObject(StorageConstants.KEY_AGENT_CONFIG_AGENT_ID, agentId.toString());
        configCollection.remove(toRemove, WriteConcern.NORMAL);
    }

    @Override
    public String getBackendConfig(String backendName, String configurationKey) {
        DBCollection configCollection = db.getCollection(StorageConstants.COLLECTION_AGENT_CONFIG);
        BasicDBObject query = new BasicDBObject();
        query.put(StorageConstants.KEY_AGENT_CONFIG_AGENT_ID, agentId.toString());
        query.put(StorageConstants.KEY_AGENT_CONFIG_BACKENDS + "." + backendName, new BasicDBObject("$exists", true));
        DBObject config = configCollection.findOne(query);
        Object value = config.get(configurationKey);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

}
