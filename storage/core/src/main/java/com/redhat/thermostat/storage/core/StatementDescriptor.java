package com.redhat.thermostat.storage.core;

public interface StatementDescriptor {

    /**
     * Describes this statement for preparation. For example:
     * 
     * <pre>
     * Query host-info where agentId = ?
     * </pre>
     * 
     * @return The statement descriptor.
     */
    String getQueryDescriptor();
    
    Category<?> getCategory();
    
}
