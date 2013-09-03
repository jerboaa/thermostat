package com.redhat.thermostat.storage.core;

import com.redhat.thermostat.storage.model.Pojo;

public class StatementDescriptor<T extends Pojo> {
    
    private final Category<T> category;
    private final String desc;
    
    public StatementDescriptor(Category<T> category, String desc) {
        this.category = category;
        this.desc = desc;
    }

    /**
     * Describes this statement for preparation. For example:
     * 
     * <pre>
     * QUERY host-info WHERE 'agentId' = ?s LIMIT 1
     * </pre>
     * 
     * @return The statement descriptor.
     */
    public String getDescriptor() {
        return desc;
    }
    
    public Category<T> getCategory() {
        return category;
    }
    
    @Override
    public String toString() {
        return desc;
    }
    
}
