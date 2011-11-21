package com.redhat.thermostat.agent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.Constants;

/**
 * Represents the Agent running on a host.
 */
public class Agent {

    private final UUID id;
    private final BackendRegistry backendRegistry;
    private final List<AgentStateListener> listeners;
    private final RuntimeConfiguration runtimeConfig;
    private final StartupConfiguration startupConfig;

    private AgentState currentState;
    private DB database;

    public Agent(BackendRegistry backendRegistry, StartupConfiguration startupConfiguration) {
        this(backendRegistry,
                UUID.randomUUID(),
                startupConfiguration,
                new RuntimeConfiguration());
    }

    public Agent(BackendRegistry registry, UUID agentId, StartupConfiguration startup, RuntimeConfiguration runtime) {
        this.id = agentId;
        this.backendRegistry = registry;
        this.listeners = new CopyOnWriteArrayList<AgentStateListener>();
        this.currentState = AgentState.DISCONNECTED;
        this.startupConfig = startup;
        this.runtimeConfig = runtime;
        updateConfig();
    }

    /**
     * Update the agent configuration from backends
     */
    private void updateConfig() {
        for (Backend backend : backendRegistry.getAll()) {
            String isActive = Boolean.toString(Arrays.asList(startupConfig.getBackendsToStart()).contains(backend.getName()));
            runtimeConfig.addConfig(backend.getName(), Constants.AGENT_CONFIG_KEY_BACKEND_ACTIVE, isActive);
            Map<String, String> settings = backend.getSettings();
            for (Entry<String, String> e : settings.entrySet()) {
                runtimeConfig.addConfig(backend.getName(), e.getKey(), e.getValue());
            }
        }
    }

    public void setDatabase(DB database) {
        this.database = database;
    }

    /**
     * Advertises the agent as active to the world.
     */
    public void publish() {
        DBCollection configCollection = database.getCollection(Constants.AGENT_CONFIG_COLLECTION_NAME);
        DBObject toInsert = runtimeConfig.toBson();
        toInsert.put(Constants.AGENT_ID, id.toString());
        configCollection.insert(toInsert, WriteConcern.SAFE);
        setState(AgentState.ACTIVE);
    }

    /**
     * Removes the agent info published to the world
     */
    public void unpublish() {
        DBCollection configCollection = database.getCollection(Constants.AGENT_CONFIG_COLLECTION_NAME);
        BasicDBObject toRemove = new BasicDBObject(Constants.AGENT_ID, id.toString());
        configCollection.remove(toRemove);
        setState(AgentState.DISCONNECTED);
    }

    public synchronized AgentState getState() {
        return currentState;
    }

    public synchronized void setState(AgentState newState) {
        if (currentState != newState) {
            currentState = newState;
            emitStateChanged();
        }
    }

    private void emitStateChanged() {
        for (AgentStateListener listener : listeners) {
            listener.stateChanged(this);
        }
    }

    public void addStateListener(AgentStateListener listener) {
        listeners.add(listener);
    }

    public void removeStateListener(AgentStateListener listener) {
        listeners.remove(listener);
    }

    public UUID getId() {
        return id;
    }

    public BackendRegistry getBackendRegistry() {
        return backendRegistry;
    }

}
