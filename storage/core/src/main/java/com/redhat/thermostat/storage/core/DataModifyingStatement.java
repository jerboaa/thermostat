package com.redhat.thermostat.storage.core;

/**
 * Marker interface for {@link Statement}s which perform write operations on
 * storage. These statements usually only return success/failure responses or
 * more specific error codes.
 *
 */
public interface DataModifyingStatement extends Statement {

    /**
     * Executes this statement.
     * 
     * @return Zero on success. A non-zero failure code otherwise.
     */
    int execute();
}
