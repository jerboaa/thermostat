package com.redhat.thermostat.storage.core;

import com.redhat.thermostat.storage.model.Pojo;

public class StatementDescriptor<T extends Pojo> {
    
    private Category<T> category;
    private String desc;
    
    public StatementDescriptor(Category<T> category, String desc) {
        this.category = category;
        this.desc = desc;
    }

    /**
     * Describes this statement for preparation. For example:
     * 
     * <pre>
     * Query host-info where agentId = ?
     * </pre>
     * 
     * @return The statement descriptor.
     */
    public String getQueryDescriptor() {
        return desc;
    }
    
    public Category<T> getCategory() {
        return category;
    }
    
}
