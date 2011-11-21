package com.redhat.thermostat.agent;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.redhat.thermostat.backend.BackendRegistry;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StringUtils;

public final class Main {

    private Main() {
        throw new IllegalStateException("Should not be instantiated");
    }

    public static void main(String[] args) {
        try {
            LogManager.getLogManager().readConfiguration(
                    StringUtils.toInputStream(Constants.LOGGING_CONFIG));
        } catch (Exception e) {
            e.printStackTrace();
        }

        LoggingUtils.resetAndGetRootLogger();
        Logger logger = LoggingUtils.getLogger(Main.class);

        StartupConfiguration startupConfig = null;
        try {
            startupConfig = new StartupConfiguration(new FileReader(Constants.AGENT_CONFIG_FILE_LOCATION));
        } catch (FileNotFoundException fnfe) {
            System.err.println("unable to read config file at " + Constants.AGENT_CONFIG_FILE_LOCATION);
            System.exit(Constants.EXIT_UNABLE_TO_READ_CONFIG);
        }

        ArgumentParser argumentParser = new ArgumentParser(startupConfig, args);

        logger.setLevel(argumentParser.getLogLevel());

        BackendRegistry backendRegistry = BackendRegistry.getInstance();

        Mongo mongo = null;
        try {
            String uri = argumentParser.getConnectionURL();
            logger.fine("connecting to " + uri);
            mongo = new Mongo(new MongoURI(uri));
            DB db = mongo.getDB(Constants.THERMOSTAT_DB);
            logger.fine("connected");
            Agent agent = new Agent(backendRegistry, startupConfig);
            agent.setDatabase(db);
            agent.publish();
            logger.fine("agent published");

            try {
                System.in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

            agent.unpublish();
            logger.fine("agent unpublished");
        } catch (UnknownHostException uhe) {
            System.err.println("unknown host");
            uhe.printStackTrace();
            System.exit(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
        }

    }
}
