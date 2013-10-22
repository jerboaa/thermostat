package com.redhat.thermostat.storage.core;

import com.redhat.thermostat.storage.model.Pojo;

/**
 * Implementations of this interface represent operations on storage. This
 * includes queries and statements manipulating data.
 * 
 * @see BackingStorage
 * @see Query
 * @see Update
 * @see Replace
 * @see Add
 * @see Remove
 */
public interface Statement<T extends Pojo> {

    /**
     * Produces a copy of this statement as if it was just created with the
     * corresponding factory method in {@link BackingStorage}.
     * 
     * @return A new raw instance of this statement.
     */
    Statement<T> getRawDuplicate();
}
