package com.redhat.thermostat.common;

/**
 * A grab bag of constants. This could be cleaned up later, but lets throw
 * things here for now.
 */
public class Constants {

    public static final String LOGGING_CONFIG = "com.redhat.thermostat.level=ALL";

    public static final String AGENT_PROPERTIES_FILE = "config/agent.properties";

    public static final int EXIT_UNKNOWN_ERROR = 1;
    public static final int EXIT_UNABLE_TO_CONNECT_TO_DATABASE = 2;
    public static final int EXIT_UNABLE_TO_READ_PROPERTIES = 3;
    public static final int EXIT_CONFIGURATION_ERROR = 4;
    public static final int EXIT_BACKEND_LOAD_ERROR = 5;
    public static final int EXIT_BACKEND_START_ERROR = 6;

    public static final int SAMPLING_INTERVAL_UNKNOWN = -1;

    public static final String AGENT_ARGUMENT_LOCAL = "--local";
    public static final String AGENT_ARGUMENT_LOGLEVEL = "--loglevel";

    public static final String AGENT_PROPERTY_MONGOS_PORT = "mongos_port";
    public static final String AGENT_PROPERTY_MONGOD_PORT = "mongod_port";
    public static final String AGENT_PROPERTY_BACKENDS = "backends";
    public static final String AGENT_PROPERTY_BACKEND_ACTIVE = "active";

    public static final String AGENT_LOCAL_HOSTNAME = "localhost";

}
