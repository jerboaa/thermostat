package com.redhat.thermostat.backend;

import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents a monitoring back-end. All the {@link Backend}s should be
 * registered with the {@link BackendRegistry}.
 */
public abstract class Backend {
    private String name;

    public Backend(String name, Map<String, String> properties) {
        this.name = name;
        for (Entry<String, String> e : properties.entrySet()) {
            setConfigurationValue(e.getKey(), e.getValue());
        }
    }

    public abstract void setConfigurationValue(String name, String value);

    /** Returns the name of the {@link Backend} */
    public String getName() {
        return name;
    }

    /** Returns the description of the {@link Backend} */
    public abstract String getDescription();

    /** Returns the vendor of the {@link Backend} */
    public abstract String getVendor();

    /** Returns the version of the {@link Backend} */
    public abstract String getVersion();

    /**
     * Returns a map containing the settings of this backend
     */
    public abstract Map<String, String> getConfigurationMap();

    /**
     * Activate the {@link Backend}. The backend should start gathering general
     * data (not specific to any vm) and start sending it
     *
     * @return true on success, false if there was an error
     */
    public abstract boolean activate();

    /**
     * Deactivate the {@link Backend}. The backend should release resources at
     * this point.
     *
     * @return true on success
     */
    public abstract boolean deactivate();

    /**
     * Returns a boolean indicating if the backend is currently active on this
     * host
     */
    public abstract boolean isActive();

}
