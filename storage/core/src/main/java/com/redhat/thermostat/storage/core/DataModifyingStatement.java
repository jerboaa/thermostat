package com.redhat.thermostat.storage.core;

import com.redhat.thermostat.storage.model.Pojo;

/**
 * Marker interface for {@link Statement}s which perform write operations on
 * storage. These statements usually only return success/failure responses or
 * more specific error codes.
 *
 */
public interface DataModifyingStatement<T extends Pojo> extends Statement<T> {

    /**
     * Default success status. The status code itself has no meaning other than
     * indicating success. Suitable to be returned on {@link #apply()}.
     */
    public static final int DEFAULT_STATUS_SUCCESS = 0;
    /**
     * Default failure status. The status code itself has no meaning other than
     * indicating failure. Suitable to be returned on {@link #apply()}.
     */
    public static final int DEFAULT_STATUS_FAILURE = -1;
    
    /**
     * Executes this statement.
     * 
     * @return a number greater than or equal to zero on success. A negative
     *         failure code otherwise.
     */
    int apply();
}
