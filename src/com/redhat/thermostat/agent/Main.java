package com.redhat.thermostat.agent;

import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.config.StartupConfiguration;
import com.redhat.thermostat.backend.BackendLoadException;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.utils.LoggingUtils;

public final class Main {

    private Main() {
        throw new IllegalStateException("Should not be instantiated");
    }

    public static void main(String[] args) {
        long startTimestamp = System.currentTimeMillis();

        List<String> argsAsList = new ArrayList<String>(Arrays.asList(args));
        while (argsAsList.contains(Constants.AGENT_ARGUMENT_DEVEL)) {
            argsAsList.remove(Constants.AGENT_ARGUMENT_DEVEL);
            LoggingUtils.useDevelConsole();
        }

        LoggingUtils.setGlobalLogLevel(Level.ALL);
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

        StartupConfiguration config = null;
        try {
            config = new StartupConfiguration(startTimestamp, argsAsList.toArray(new String[0]), props);
        } catch (LaunchException le) {
            logger.log(Level.SEVERE,
                    "Unable to instantiate startup configuration.",
                    le);
            System.exit(Constants.EXIT_CONFIGURATION_ERROR);
        }

        LoggingUtils.setGlobalLogLevel(config.getLogLevel());
        Storage storage = new MongoStorage();
        try {
            storage.connect(config.getDatabaseURIAsString());
            logger.fine("Storage configured with database URI.");
        } catch (UnknownHostException uhe) {
            logger.log(Level.SEVERE, "Could not initialize storage layer.", uhe);
            System.exit(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
        }

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
            logger.fine("Starting agent.");
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
