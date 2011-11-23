package com.redhat.thermostat.agent;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.redhat.thermostat.agent.config.Configuration;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.Constants;

/**
 * Represents the Agent running on a host.
 */
public class Agent {

    private final UUID id;
    private final BackendRegistry backendRegistry;
    private final Configuration config;

    private DB database;

    public Agent(BackendRegistry backendRegistry, Configuration config, DB db) {
        this(backendRegistry, UUID.randomUUID(), config, db);
    }

    public Agent(BackendRegistry registry, UUID agentId, Configuration config, DB db) {
        this.id = agentId;
        this.backendRegistry = registry;
        this.config = config;
        this.database = db;
        config.setCollection(database.getCollection(Constants.AGENT_CONFIG_COLLECTION_NAME));
    }

    private void loadConfiguredBackends() {
        // TODO Once Configuration has relevant methods for getting list of backend names and backend-specific parameters, iterate over that list,
        // activating as per configuration parameters and adding each to the registry
    }

    private void stopAllBackends() {
        // TODO Inverse of the above.  Stop each backend, remove from registry.
    }

    public void start() {
        loadConfiguredBackends();
        config.publish();
    }

    public void stop() {
        config.unpublish();
        stopAllBackends();
    }

    public UUID getId() {
        return id;
    }

}
