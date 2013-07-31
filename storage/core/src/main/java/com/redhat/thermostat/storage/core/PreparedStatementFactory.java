package com.redhat.thermostat.storage.core;

import com.redhat.thermostat.storage.internal.statement.PreparedStatementImpl;
import com.redhat.thermostat.storage.model.Pojo;

/**
 * Factory for instantiating a {@link PreparedStatement}.
 *
 */
public class PreparedStatementFactory {

    public static <T extends Pojo> PreparedStatement<T> getInstance(BackingStorage storage,
            StatementDescriptor<T> desc) throws DescriptorParsingException {
        // This is the sole method in order to avoid leaking impl details of
        // this OSGi module. Storage implementations will have to use this
        // factory.
        return new PreparedStatementImpl<>(storage, desc);
    }
}
