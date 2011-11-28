package com.redhat.thermostat.backend;

public class BackendLoadException extends Exception {

    private static final long serialVersionUID = 4057881401012295723L;

    public BackendLoadException(String message) {
        super(message);
    }

    public BackendLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
