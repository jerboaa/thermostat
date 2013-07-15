package com.redhat.thermostat.storage.core;

import com.redhat.thermostat.storage.model.Pojo;


/**
 * A prepared statement.
 * 
 * @see Statement
 * @see DataModifyingStatement
 * @see Storage#prepareStatement(StatementDescriptor)
 *
 */
public interface PreparedStatement<T extends Pojo> {
    
    void setBoolean(int paramIndex, boolean paramValue);
    
    void setLong(int paramIndex, long paramValue);
    
    void setInt(int paramIndex, int paramValue);
    
    void setString(int paramIndex, String paramValue);
    
    void setStringList(int paramIndex, String[] paramValue);

    /**
     * Executes a predefined {@link DataModifyingStatement}.
     * 
     * @return a non-zero error code on failure. Zero otherwise.
     * 
     * @throws StatementExecutionException
     *             If the prepared statement wasn't valid for execution.
     */
    int execute() throws StatementExecutionException;

    /**
     * Executes a predefined {@link Query}.
     * 
     * @return a {@link Cursor} as a result to the underlying {@link Query}
     * 
     * @throws StatementExecutionException
     *             If the prepared statement wasn't valid for execution.
     */
    Cursor<T> executeQuery() throws StatementExecutionException;
    
    /**
     * 
     * @return The unique ID of this predefined statement for the underlying
     *         {@link Storage}.
     */
    int getId();
    
    /**
     * @return the {@link StatementDescriptor} that describes this statement.
     */
    StatementDescriptor<T> getDescriptor();
}
