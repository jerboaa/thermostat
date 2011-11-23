package com.redhat.thermostat.agent;

import java.util.logging.Level;

public final class Defaults {

	private Defaults() {
	    /* Should not be instantiated */
	}

    public static final Level LOGGING_LEVEL = Level.WARNING;
    public static final String DATABASE_URI = "mongodb://127.0.0.1";
    public static final int MONGOS_PORT = 27517;
    public static final int MONGOD_PORT = 27518;
    public static final boolean LOCAL_MODE = false; // Default behaviour is to connect to cluster.

}
