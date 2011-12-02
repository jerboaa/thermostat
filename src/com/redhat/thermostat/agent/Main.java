package com.redhat.thermostat.agent;

import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.config.Configuration;
import com.redhat.thermostat.backend.BackendLoadException;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StringUtils;

public final class Main {

    private Main() {
        throw new IllegalStateException("Should not be instantiated");
    }

    public static void main(String[] args) {
        long startTimestamp = System.currentTimeMillis();

        try {
            LogManager.getLogManager().readConfiguration(
                    StringUtils.toInputStream(Constants.LOGGING_CONFIG));
        } catch (Exception e) {
            e.printStackTrace();
        }

        LoggingUtils.resetAndGetRootLogger();
        Logger logger = LoggingUtils.getLogger(Main.class);

        Properties props = null;
        try {
            props = new Properties();
            props.load(new FileReader(Constants.AGENT_PROPERTIES_FILE));
        } catch (IOException ioe) {
            logger.log(Level.SEVERE,
                    "Unable to read properties file at " + Constants.AGENT_PROPERTIES_FILE,
                    ioe);
            System.exit(Constants.EXIT_UNABLE_TO_READ_PROPERTIES);
        }

        Configuration config = null;
        try {
            config = new Configuration(startTimestamp, args, props);
        } catch (LaunchException le) {
            logger.log(Level.SEVERE,
                    "Unable to instantiate startup configuration.",
                    le);
            System.exit(Constants.EXIT_CONFIGURATION_ERROR);
        }

        logger.setLevel(config.getLogLevel());

        Storage storage = new MongoStorage();
        try {
            storage.connect(config.getDatabaseURIAsString());
            logger.fine("connected");
        } catch (UnknownHostException uhe) {
            logger.log(Level.SEVERE, "Could not initialize storage layer.", uhe);
            System.exit(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
        }
        config.setStorage(storage);

        BackendRegistry backendRegistry = null;
        try {
            backendRegistry = new BackendRegistry(config, storage);
        } catch (BackendLoadException ble) {
            logger.log(Level.SEVERE, "Could not get BackendRegistry instance.", ble);
            System.exit(Constants.EXIT_BACKEND_LOAD_ERROR);
        }

        Agent agent = new Agent(backendRegistry, config, storage);
        config.setAgent(agent);
        storage.setAgentId(agent.getId());
        try {
            agent.start();
        } catch (LaunchException le) {
            logger.log(Level.SEVERE,
                    "Agent could not start, probably because a configured backend could not be activated.",
                    le);
            System.exit(Constants.EXIT_BACKEND_START_ERROR);
        }
        logger.fine("Agent started.");

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        agent.stop();
        logger.fine("Agent stopped.");
    }
}
