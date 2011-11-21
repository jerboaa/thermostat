package com.redhat.thermostat.agent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.redhat.thermostat.common.Constants;

/**
 * The current run time configuration of the agent.
 */
public class RuntimeConfiguration {

    private String agentName;
    private Backends backendConfig;

    public RuntimeConfiguration() {
        agentName = "agent " + new Random().nextLong();
        backendConfig = new Backends();
    }

    public static RuntimeConfiguration fromBson(DBObject remote) {
        RuntimeConfiguration config = new RuntimeConfiguration();
        BasicDBObject other = (BasicDBObject) remote;
        // TODO deal with missing/bad keys/values
        config.agentName = other.getString(Constants.AGENT_CONFIG_KEY_AGENT_NAME);
        BasicDBObject remoteBackends = (BasicDBObject) other.get(Constants.AGENT_CONFIG_KEY_BACKENDS);
        for (Entry<String, Object> remoteBackendEntry : remoteBackends.entrySet()) {
            String backendName = remoteBackendEntry.getKey();
            BasicDBObject backendConfig = (BasicDBObject) remoteBackendEntry.getValue();
            for (Entry<String, Object> e : backendConfig.entrySet()) {
                config.backendConfig.addConfig(backendName, e.getKey(), (String) e.getValue());
            }
        }
        return config;
    }

    public DBObject toBson() {
        BasicDBObject result = new BasicDBObject();
        result.put(Constants.AGENT_CONFIG_KEY_AGENT_NAME, agentName);
        result.put(Constants.AGENT_CONFIG_KEY_BACKENDS, backendConfig.toDBObject());
        return result;
    }

    public void setAvailableBackends(String[] backends) {
        for (String backend : backends) {
            backendConfig.addBackend(backend);
        }
    }

    public String[] getAllBackends() {
        return backendConfig.getBackends().toArray(new String[0]);
    }

    public String[] getActiveBackends() {
        return backendConfig.getMatchingBackends(Constants.AGENT_CONFIG_KEY_BACKEND_ACTIVE, Boolean.TRUE.toString()).toArray(new String[0]);
    }

    public void addConfig(String backend, String key, String value) {
        backendConfig.addConfig(backend, key, value);
    }

    public String getConfig(String backendName, String configKey) {
        return backendConfig.getConfig(backendName).get(configKey);
    }

    /**
     * A wrapper around the backend-specific information
     */
    private static class Backends {
        /** {backend-name: { opt1: va1, opt2:val2, } } */
        private Map<String, Map<String, String>> info;

        public Backends() {
            info = new HashMap<String, Map<String, String>>();
        }

        public void addBackend(String backendName) {
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
