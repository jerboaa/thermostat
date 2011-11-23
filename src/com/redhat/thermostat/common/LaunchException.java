package com.redhat.thermostat.common;
/**
 * Exception that should be thrown when a condition is detected that prevents proper startup
 * of program.
 *
 */
@SuppressWarnings("serial")
public class LaunchException extends Exception {

    public LaunchException(String message) {
        super(message);
    }

    public LaunchException(String message, Exception cause) {
        super(message, cause);
    }
}
