package com.redhat.thermostat.agent;

import java.util.logging.Level;

public final class Defaults {

	/* Should not be instantiated */
	private Defaults() {
	}

    public static final Level LOGGING_LEVEL = Level.WARNING;
    public static final boolean local = false; // Default behaviour is to connect to cluster.

}
