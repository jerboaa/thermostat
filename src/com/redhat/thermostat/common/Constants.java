package com.redhat.thermostat.common;

/**
 * A grab bag of constants. This could be cleaned up later, but lets throw
 * things here for now.
 */
public class Constants {

    public static final String LOGGING_CONFIG = "com.redhat.thermostat.level=ALL";

    public static final String AGENT_CONFIG_FILE_LOCATION = "config/agent.properties";

    public static final int EXIT_UNKNOWN_ERROR = 1;
    public static final int EXIT_UNABLE_TO_CONNECT_TO_DATABASE = 2;
    public static final int EXIT_UNABLE_TO_READ_CONFIG = 3;

    public static final String THERMOSTAT_DB = "thermostat";
    public static final String MONGO_DEFAULT_URL = "mongodb://127.0.0.1";

    public static final String AGENT_CONFIG_COLLECTION_NAME = "agent-configs";
    public static final String AGENT_ID = "agent-id";

    public static final int SAMPLING_INTERVAL_UNKNOWN = -1;

    public static final String AGENT_CONFIG_KEY_AGENT_NAME = "agent-name";
    public static final String AGENT_CONFIG_KEY_BACKENDS = "backends";
    public static final String AGENT_CONFIG_KEY_BACKEND_ACTIVE = "active";

}
