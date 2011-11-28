package com.redhat.thermostat.agent;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mongodb.DB;
import com.redhat.thermostat.agent.config.Configuration;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Represents the Agent running on a host.
 */
public class Agent {

    private final UUID id;
    private final BackendRegistry backendRegistry;
    private final Configuration config;

    private static final Logger LOGGER = LoggingUtils.getLogger(Agent.class);

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

    private void startBackends() throws LaunchException {
        for (Backend be : backendRegistry.getAll()) {
            if (!be.activate()) {
                // When encountering issues during startup, we should not attempt to continue activating.
                stopBackends();
                throw new LaunchException("Could not activate backend: " + be.getName());
            }
        }
    }

    private void stopBackends() {
        for (Backend be : backendRegistry.getAll()) {
            if (!be.deactivate()) {
                // When encountering issues during shutdown, we should attempt to shut down remaining backends.
                LOGGER.log(Level.WARNING, "Issue while deactivating backend: " + be.getName());
            }
        }
    }

    public void start() throws LaunchException {
        startBackends();
        config.publish();
    }

    public void stop() {
        config.unpublish();
        stopBackends();
    }

    public UUID getId() {
        return id;
    }

}
