package com.redhat.thermostat.agent.config;

import java.util.logging.Logger;

import com.redhat.thermostat.agent.storage.Storage;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class ConfigurationWatcher implements Runnable {

    private static final Logger logger = LoggingUtils.getLogger(ConfigurationWatcher.class);

    private Storage storage;
    private BackendRegistry backends;

    public ConfigurationWatcher(Storage storage, BackendRegistry backends) {
        this.storage = storage;
        this.backends = backends;
    }

    @Override
    public void run() {
        logger.fine("Watching for configuration changes.");
        while (!Thread.interrupted()) {
            checkConfigUpdates();
        }
        logger.fine("No longer watching for configuration changes.");
    }

    // TODO It would be best to develop this algorithm when we have a client that can initiate changes, so that it can be tested.
    private void checkConfigUpdates() {
        try { // THIS IS ONLY TEMPORARY.  Until we do implement this algorithm, we don't want this thread busy hogging CPU.
            Thread.sleep(2000);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
    }

}
