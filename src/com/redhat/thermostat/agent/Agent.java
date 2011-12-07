package com.redhat.thermostat.agent;

import java.util.UUID;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.config.ConfigurationWatcher;
import com.redhat.thermostat.agent.config.StartupConfiguration;
import com.redhat.thermostat.agent.storage.Storage;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Represents the Agent running on a host.
 */
public class Agent {

    private static final Logger logger = LoggingUtils.getLogger(Agent.class);

    private final UUID id;
    private final BackendRegistry backendRegistry;
    private final StartupConfiguration config;

    private Storage storage;
    private Thread configWatcherThread = null;

    public Agent(BackendRegistry backendRegistry, StartupConfiguration config, Storage storage) {
        this(backendRegistry, UUID.randomUUID(), config, storage);
    }

    public Agent(BackendRegistry registry, UUID agentId, StartupConfiguration config, Storage storage) {
        this.id = agentId;
        this.backendRegistry = registry;
        this.config = config;
        this.storage = storage;
    }

    private void startBackends() throws LaunchException {
        for (Backend be : backendRegistry.getAll()) {
            logger.fine("Attempting to start backend: " + be.getName());
            if (!be.activate()) {
                logger.warning("Issue while starting backend: " + be.getName());
                // When encountering issues during startup, we should not attempt to continue activating.
                stopBackends();
                throw new LaunchException("Could not activate backend: " + be.getName());
            }
        }
    }

    private void stopBackends() {
        for (Backend be : backendRegistry.getAll()) {
            logger.fine("Attempting to stop backend: " +be.getName());
            if (!be.deactivate()) {
                // When encountering issues during shutdown, we should attempt to shut down remaining backends.
                logger.warning("Issue while deactivating backend: " + be.getName());
            }
        }
    }

    public synchronized void start() throws LaunchException {
        if (configWatcherThread == null) {
            startBackends();
            storage.addAgentInformation(config);
            configWatcherThread = new Thread(new ConfigurationWatcher(storage), "Configuration Watcher");
            configWatcherThread.start();
        } else {
            logger.warning("Attempt to start agent when already started.");
        }
    }

    public synchronized void stop() {
        if (configWatcherThread != null) {
            configWatcherThread.interrupt(); // This thread checks for its own interrupted state and ends if interrupted.
            while (configWatcherThread.isAlive()) {
                try {
                    configWatcherThread.join();
                } catch (InterruptedException e) {
                    logger.fine("Interrupted while waiting for ConfigurationWatcher to die.");
                }
            }
            configWatcherThread = null;
            storage.removeAgentInformation();
            stopBackends();
        } else {
            logger.warning("Attempt to stop agent which is not active");
        }
    }

    public UUID getId() {
        return id;
    }

}
