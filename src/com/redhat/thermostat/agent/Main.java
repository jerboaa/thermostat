package com.redhat.thermostat.agent;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.redhat.thermostat.agent.config.Configuration;
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

        Properties props = null;
        try {
            props = new Properties();
            props.load(new FileReader(Constants.AGENT_PROPERTIES_FILE));
        } catch (FileNotFoundException fnfe) {
            System.err.println("Unable to read properties file at " + Constants.AGENT_PROPERTIES_FILE);
            System.exit(Constants.EXIT_UNABLE_TO_READ_CONFIG);
        } catch (IOException e) {
            System.err.println("Unable to read properties file at " + Constants.AGENT_PROPERTIES_FILE);
            System.exit(Constants.EXIT_UNABLE_TO_READ_CONFIG);
        }

        Configuration config = new Configuration(args, props);

        logger.setLevel(config.getLogLevel());

        BackendRegistry backendRegistry = BackendRegistry.getInstance();

        Mongo mongo = null;
        DB db = null;
        try {
            MongoURI mongoURI = new MongoURI(config.getDatabaseURIAsString());
            mongo = new Mongo(mongoURI);
            db = mongo.getDB(Constants.THERMOSTAT_DB);
            logger.fine("connected");
        } catch (UnknownHostException uhe) {
            System.err.println("unknown host");
            uhe.printStackTrace();
            System.exit(Constants.EXIT_UNABLE_TO_CONNECT_TO_DATABASE);
        }

        Agent agent = new Agent(backendRegistry, config, db);
        config.setAgent(agent);
        agent.start();
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
