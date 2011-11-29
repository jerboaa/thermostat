package com.redhat.thermostat.agent;

import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;
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
        } catch (IOException e) {
            System.err.println("Unable to read properties file at " + Constants.AGENT_PROPERTIES_FILE);
            System.exit(Constants.EXIT_UNABLE_TO_READ_PROPERTIES);
        }

        Configuration config = null;
        try {
            config = new Configuration(startTimestamp, args, props);
        } catch (LaunchException e1) {
            System.exit(Constants.EXIT_CONFIGURATION_ERROR);
        }

        logger.setLevel(config.getLogLevel());

        BackendRegistry backendRegistry = null;
        try {
            backendRegistry = new BackendRegistry(config);
        } catch (BackendLoadException ble) {
            System.err.println("Could not get BackendRegistry instance.");
            ble.printStackTrace();
            System.exit(Constants.EXIT_BACKEND_LOAD_ERROR);
        }

        Storage storage = new MongoStorage();
        try {
            storage.connect(config.getDatabaseURIAsString());
            logger.fine("connected");
        } catch (UnknownHostException uhe) {
            System.err.println("unknown host");
            uhe.printStackTrace();
            System.exit(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
        }

        Agent agent = new Agent(backendRegistry, config, storage);
        config.setAgent(agent);
        config.setStorage(storage);
        try {
            agent.start();
        } catch (LaunchException le) {
            System.err.println("Agent could not start, probably because a configured backend could not be activated.");
            le.printStackTrace();
            System.exit(Constants.EXIT_BACKEND_START_ERROR);
        }
        logger.fine("agent published");

        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }

        agent.stop();
        logger.fine("agent unpublished");

    }
}
