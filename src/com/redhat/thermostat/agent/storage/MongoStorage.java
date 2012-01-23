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

package com.redhat.thermostat.agent.storage;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bson.BSONObject;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.WriteConcern;
import com.redhat.thermostat.agent.config.StartupConfiguration;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Implementation of the Storage interface that uses MongoDB to store the instrumentation data.
 * 
 * In this implementation, each CATEGORY is given a distinct collection.
 */
public class MongoStorage extends Storage {

    public static final String KEY_AGENT_ID = "agent-id";

    private static final Logger logger = LoggingUtils.getLogger(MongoStorage.class);

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
    public void addAgentInformation(StartupConfiguration config, BackendRegistry registry) {
        DBCollection configCollection = db.getCollection(StorageConstants.CATEGORY_AGENT_CONFIG);
        DBObject toInsert = createConfigDBObject(config, registry);
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
            String[] entryParts = key.getName().split("\\.");
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

    private DBObject createConfigDBObject(StartupConfiguration config, BackendRegistry registry) {
        BasicDBObject result = getAgentDBObject();
        result.put(StorageConstants.KEY_AGENT_CONFIG_AGENT_START_TIME, config.getStartTime());
        BasicDBObject backends = new BasicDBObject();
        for (Backend backend : registry.getAll()) {
            backends.put(backend.getName(), createBackendConfigDBObject(backend));
        }
        result.put(StorageConstants.KEY_AGENT_CONFIG_BACKENDS, backends);
        return result;
    }

    private DBObject createBackendConfigDBObject(Backend backend) {
        BasicDBObject result = new BasicDBObject();
        Map<String, String> configMap = backend.getConfigurationMap();
        result.append(StorageConstants.KEY_AGENT_CONFIG_BACKEND_NAME, backend.getName());
        result.append(StorageConstants.KEY_AGENT_CONFIG_BACKEND_DESC, backend.getDescription());
        result.append(StorageConstants.KEY_AGENT_CONFIG_BACKEND_ACTIVE, createBackendActiveDBObject(backend));
        for (String configName : configMap.keySet()) {
            result.append(configName, configMap.get(configName));
        }
        return result;
    }

    private DBObject createBackendActiveDBObject(Backend backend) {
        BasicDBObject result = new BasicDBObject();
        result.append(StorageConstants.KEY_AGENT_CONFIG_BACKEND_NEW, backend.getObserveNewJvm());
        result.append(StorageConstants.KEY_AGENT_CONFIG_BACKEND_PIDS, new BasicDBList());
        // TODO check which processes are already being listened to.
        return result;
    }
}
