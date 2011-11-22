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
import com.mongodb.MongoURI;
import com.mongodb.WriteConcern;
import com.redhat.thermostat.agent.Agent;
import com.redhat.thermostat.agent.Defaults;
import com.redhat.thermostat.common.Constants;

public final class Configuration {
    private Arguments arguments;
    private MongoURI uri;
    private Backends backends;
    private String hostname;
    private Agent agent;
    private DBCollection dbCollection = null;
    private boolean published = false;

    public Configuration(String[] args, Properties props) {
        arguments = new Arguments(args);
        if (arguments.getLocalMode()) {
            uri = new MongoURI(Constants.MONGO_URL + ":" + props.getProperty(Constants.AGENT_PROPERTY_MONGOD_PORT));
            hostname = Constants.AGENT_LOCAL_HOSTNAME;
        } else {
            uri = new MongoURI(props.getProperty(Constants.MONGO_URL) + ":" + props.getProperty(Constants.AGENT_PROPERTY_MONGOS_PORT));
            try {
                InetAddress addr = InetAddress.getLocalHost();
                hostname = addr.getCanonicalHostName();
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        backends = new Backends(props);
    }


    public void setCollection(DBCollection collection) {
        dbCollection = collection;
    }
    
    public Level getLogLevel() {
        return arguments.getLogLevel();
    }

    public MongoURI getMongoURI() {
        return uri;
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

    private class Arguments {
        private final boolean localMode;
        private final Level logLevel;

        public Arguments(String[] args) {
            boolean local = Defaults.local;
            Level level = Defaults.LOGGING_LEVEL;
            for (int index = 0; index < args.length; index++) {
                if (args[index].equals(Constants.AGENT_ARGUMENT_LOGLEVEL)) {
                    index++;
                    if (index < args.length) {
                        level = Level.parse(args[index].toUpperCase());
                    } else {
                        
                    }
                } else if (args[index].equals(Constants.AGENT_ARGUMENT_LOCAL)) {
                    local = true;
                }
            }
            logLevel = level;
            localMode = local;
        }

        public boolean getLocalMode() {
            return localMode;
        }

        public Level getLogLevel() {
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
