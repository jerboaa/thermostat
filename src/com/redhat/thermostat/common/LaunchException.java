package com.redhat.thermostat.common;
/**
 * Exception that should be thrown when a condition is detected that prevents proper startup
 * of program.
 *
 */
public class LaunchException extends Exception {

    private static final long serialVersionUID = -6757521147558143649L;

    public LaunchException(String message) {
        super(message);
    }

    public LaunchException(String message, Exception cause) {
        super(message, cause);
    }
}
