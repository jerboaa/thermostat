package com.redhat.thermostat.storage.core;

/**
 * Exception thrown if something was wrong with a {@link PreparedStatement}
 * and it was attempted to execute it.
 *
 */
@SuppressWarnings("serial")
public class StatementExecutionException extends Exception {

    public StatementExecutionException(Throwable cause) {
        super(cause);
    }
}
