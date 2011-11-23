package com.redhat.thermostat.agent.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.redhat.thermostat.agent.Agent;
import com.redhat.thermostat.agent.Defaults;
import com.redhat.thermostat.common.Constants;

public final class Configuration {

    private Level logLevel;
    private boolean localMode;
    private int mongodPort;
    private int mongosPort;
    private String databaseURI;
    private String completeDatabaseURI;
    private Backends backends;

    private String hostname;

    private Agent agent;
    private boolean published = false;
    private DBCollection dbCollection = null;

    public Configuration(String[] args, Properties props) {
        initFromDefaults();
        initFromProperties(props);
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

    private void initFromProperties(Properties props) {
        if (props.getProperty(Constants.AGENT_PROPERTY_MONGOD_PORT) != null) {
            mongodPort = Integer.valueOf(props.getProperty(Constants.AGENT_PROPERTY_MONGOD_PORT));
        }
        if (props.getProperty(Constants.AGENT_PROPERTY_MONGOS_PORT) != null) {
            mongosPort = Integer.valueOf(props.getProperty(Constants.AGENT_PROPERTY_MONGOS_PORT));
        }
        backends = new Backends(props);
    }

    private void initFromArguments(String[] args) {
        Arguments arguments;

        arguments = new Arguments(args);
        if (arguments.isModeSpecified()) {
            localMode = arguments.getLocalMode();
        }
        if (arguments.isLogLevelSpecified()) {
            logLevel = arguments.getLogLevel();
        }
    }

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

    public DBObject toDBObject() {
        BasicDBObject result = new BasicDBObject();
        // TODO explicit exception if agent not yet set.
        result.put(Constants.AGENT_ID, agent.getId().toString());
        result.put(Constants.AGENT_CONFIG_KEY_HOST, hostname);
        result.put(Constants.AGENT_CONFIG_KEY_BACKENDS, backends.toDBObject());
        return result;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public void publish() {
        // TODO explicit exception if dbCollection has not yet been set.
        dbCollection.insert(toDBObject(), WriteConcern.SAFE);
        // TODO Start configuration-change-detection thread.
        published = true;
    }

    public void unpublish() {
        // TODO Stop configuration-change-detection thread.
        dbCollection.remove(new BasicDBObject(Constants.AGENT_ID, agent.getId().toString()), WriteConcern.NORMAL);
        published = false;
    }

    public String getBackendConfigValue(String backendName, String configurationKey) {
        String value = null;
        if (published) {
            value = getBackendConfigFromDatabase(backendName, configurationKey);
        } else {
            value = backends.getConfigValue(backendName, configurationKey);
        }
        return value;
    }

    private String getBackendConfigFromDatabase(String backendName, String configurationKey) {
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

        public Arguments(String[] args) {
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
                            System.err.println("warning: invalid argument for " + Constants.AGENT_ARGUMENT_LOGLEVEL);
                        }
                    } else {
                        System.err.println("warning: missing argument for " + Constants.AGENT_ARGUMENT_LOGLEVEL);
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

    /**
     * A wrapper around the backend-specific information.  Used mainly for convenience during startup; once running all config should happen via database.
     */
    private static class Backends {
        /* TODO Do we really need to do all this mapping?  These values should only be used at startup, maybe best to just have convenience wrappers
         * around the properties file...
         */
        /** {backend-name: { opt1: va1, opt2:val2, } } */
        private Map<String, Map<String, String>> info;

        public Backends(Properties props) {
            info = new HashMap<String, Map<String, String>>();
            initializeFromProperties(props);
        }

        private void initializeFromProperties(Properties props) {
            List<String> backendNames = Arrays.asList(props.getProperty(Constants.AGENT_PROPERTY_BACKENDS).trim().split(","));
            for (String backendName : backendNames) {
                // TODO Initialize Map<String, String> of properties for each.
            }
        }

        public String getConfigValue(String backendName, String configurationKey) {
            // TODO make this more robust, appropriate exceptions for invalid input values.
            Map<String, String> backendValues = info.get(backendName);
            return backendValues.get(configurationKey);
        }

        private void addBackend(String backendName) {
            if (!info.containsKey(backendName)) {
                info.put(backendName, new HashMap<String, String>());
            }
        }

        public Object toDBObject() {
            BasicDBObject result = new BasicDBObject();
            for (Entry<String, Map<String, String>> e : info.entrySet()) {
                BasicDBObject config = new BasicDBObject();
                config.putAll(e.getValue());
                result.put(e.getKey(), config);
            }
            return result;
        }

        public Set<String> getBackends() {
            return info.keySet();
        }

        public Set<String> getMatchingBackends(String key, String value) {
            // TODO perhaps extend this to regex?
            Set<String> matched = new HashSet<String>();
            for (Entry<String, Map<String, String>> e : info.entrySet()) {
                if (e.getValue().get(key) != null && e.getValue().get(key).equals(value)) {
                    matched.add(e.getKey());
                }
            }
            return matched;
        }

        public void addConfig(String backend, String key, String value) {
            addBackend(backend);
            info.get(backend).put(key, value);
        }

        public Map<String, String> getConfig(String backend) {
            return info.get(backend);
        }

    }
}
