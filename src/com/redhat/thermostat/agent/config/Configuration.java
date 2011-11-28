package com.redhat.thermostat.agent.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.redhat.thermostat.agent.Agent;
import com.redhat.thermostat.agent.Defaults;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.LaunchException;

public final class Configuration {

    /* FIXME
     * 
     * This class needs some love.  It mixes up startup configuration with runtime configuration,
     * while each is handled in very different ways.  It probably should be split into separate
     * classes, but it makes very little sense to do that before we have a Storage abstraction
     * hiding implementation details (ie Mongo API stuff).
     */
    private Properties props;

    private Level logLevel;
    private boolean localMode;
    private int mongodPort;
    private int mongosPort;
    private String databaseURI;
    private String completeDatabaseURI;

    private String hostname;

    private Agent agent;
    private DBCollection dbCollection = null;

    public Configuration(String[] args, Properties props) throws LaunchException {
        this.props = props;

        initFromDefaults();
        initFromProperties();
        initFromArguments(args);

        if (localMode) {
            completeDatabaseURI = databaseURI + ":" + mongodPort;
            hostname = Constants.AGENT_LOCAL_HOSTNAME;
        } else {
            completeDatabaseURI = databaseURI + ":" + mongosPort;
            try {
                InetAddress addr = InetAddress.getLocalHost();
                hostname = addr.getCanonicalHostName();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    private void initFromDefaults() {
        logLevel = Defaults.LOGGING_LEVEL;
        localMode = Defaults.LOCAL_MODE;
        mongodPort = Defaults.MONGOD_PORT;
        mongosPort = Defaults.MONGOS_PORT;
        databaseURI = Defaults.DATABASE_URI;
    }

    private void initFromProperties() {
        if (props.getProperty(Constants.AGENT_PROPERTY_MONGOD_PORT) != null) {
            mongodPort = Integer.valueOf(props.getProperty(Constants.AGENT_PROPERTY_MONGOD_PORT));
        }
        if (props.getProperty(Constants.AGENT_PROPERTY_MONGOS_PORT) != null) {
            mongosPort = Integer.valueOf(props.getProperty(Constants.AGENT_PROPERTY_MONGOS_PORT));
        }
    }

    private void initFromArguments(String[] args) throws LaunchException {
        Arguments arguments;

        arguments = new Arguments(args);
        if (arguments.isModeSpecified()) {
            localMode = arguments.getLocalMode();
        }
        if (arguments.isLogLevelSpecified()) {
            logLevel = arguments.getLogLevel();
        }
    }

    // TODO hide Mongo stuff behind Storage facade
    public void setCollection(DBCollection collection) {
        dbCollection = collection;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public String getDatabaseURIAsString() {
        return completeDatabaseURI;
    }

    public String getHostname() {
        return hostname;
    }

    // TODO all of this should be assembled somewhere behind the Storage facade, once it exists.
    public DBObject toDBObject() {
        BasicDBObject result = new BasicDBObject();
        // TODO explicit exception if agent not yet set.
        result.put(Constants.AGENT_ID, agent.getId().toString());
        result.put(Constants.AGENT_CONFIG_KEY_HOST, hostname);
        // TODO create nested backend config parts
        return result;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public void publish() {
        // TODO Hide Mongo stuff behind Storage facade.
        dbCollection.insert(toDBObject(), WriteConcern.SAFE);
        // TODO Start configuration-change-detection thread.
    }

    public void unpublish() {
        // TODO Stop configuration-change-detection thread.
        // TODO hide Mongo stuff behind storage facade.
        dbCollection.remove(new BasicDBObject(Constants.AGENT_ID, agent.getId().toString()), WriteConcern.NORMAL);
    }

    public List<String> getStartupBackendClassNames() {
        String fullPropertyString = props.getProperty(Constants.AGENT_PROPERTY_BACKENDS);
        if ((fullPropertyString == null) // Avoid NPE
                || (fullPropertyString.length() == 0)) { /* If we do the split() on this empty string,
                                                          * it will produce an array of size 1 containing
                                                          * the empty string, which we do not want.
                                                          */
            return new ArrayList<String>();
        } else {
            return Arrays.asList(fullPropertyString.trim().split(","));
        }
    }

    public Map<String, String> getStartupBackendConfigMap(String backendName) {
        String prefix = backendName + ".";
        Map<String, String> configMap = new HashMap<String, String>();
        for (Entry<Object, Object> e : props.entrySet()) {
            String key = (String) e.getKey();
            if (key.startsWith(prefix)) {
                String value = (String) e.getValue();
                String mapKey = key.substring(prefix.length());
                configMap.put(mapKey, value);
            }
        }
        return configMap;
    }

    /**
     * 
     * @param backendName
     * @param configurationKey
     * @return
     */
    public String getBackendConfigValue(String backendName, String configurationKey) {
        // TODO hide Mongo stuff behind Storage facade.
        DBObject config = dbCollection.findOne(new BasicDBObject(Constants.AGENT_ID, agent.getId().toString()));
        // TODO get the appropriate value from this agent's configuration.
        return null;
    }

    /**
     * Exposes the command line arguments in a more object-oriented style.
     * <p>
     * Please check that an option was specified (using the various is*Specified() methods) before using its value.
     */
    private static class Arguments {
        private final boolean localMode;
        private final boolean modeSpecified;
        private final Level logLevel;
        private final boolean logLevelSpecified;

        public Arguments(String[] args) throws LaunchException {
            boolean local = false;
            boolean explicitMode = false;
            Level level = null;
            boolean explicitLogLevel = false;
            for (int index = 0; index < args.length; index++) {
                if (args[index].equals(Constants.AGENT_ARGUMENT_LOGLEVEL)) {
                    index++;
                    if (index < args.length) {
                        try {
                            level = Level.parse(args[index].toUpperCase());
                            explicitLogLevel = true;
                        } catch (IllegalArgumentException iae) {
                            String message = "Invalid argument for " + Constants.AGENT_ARGUMENT_LOGLEVEL;
                            System.err.println("ERROR: " + message);
                            throw new LaunchException(message, iae);
                        }
                    } else {
                        String message = "Missing argument for " + Constants.AGENT_ARGUMENT_LOGLEVEL;
                        System.err.println("ERROR: " + message);
                        throw new LaunchException(message);
                    }
                } else if (args[index].equals(Constants.AGENT_ARGUMENT_LOCAL)) {
                    explicitMode = true;
                    local = true;
                }
            }
            logLevel = level;
            logLevelSpecified = explicitLogLevel;
            localMode = local;
            modeSpecified = explicitMode;
        }

        public boolean isModeSpecified() {
            return modeSpecified;
        }

        public boolean getLocalMode() {
            if (!isModeSpecified()) {
                throw new IllegalStateException("local mode is not valid");
            }
            return localMode;
        }

        public boolean isLogLevelSpecified() {
            return logLevelSpecified;
        }

        public Level getLogLevel() {
            if (!isLogLevelSpecified()) {
                throw new IllegalStateException("log level not explicitly specified");
            }
            return logLevel;
        }
    }
}
