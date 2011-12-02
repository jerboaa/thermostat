package com.redhat.thermostat.backend;

import java.util.Map;
import java.util.Map.Entry;

import com.redhat.thermostat.agent.Storage;

/**
 * Represents a monitoring back-end. All the {@link Backend}s should be
 * registered with the {@link BackendRegistry}.
 */
public abstract class Backend {

    protected Storage storage;

    public final void setInitialConfiguration(Map<String, String> configMap) {
        for (Entry<String, String> e : configMap.entrySet()) {
            setConfigurationValue(e.getKey(), e.getValue());
        }
    }

    protected abstract void setConfigurationValue(String name, String value);



    /** Returns the name of the {@link Backend} */
    public abstract String getName();

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

    protected void setStorage(Storage storage) {
        this.storage = storage;
    }

    /**
     * Activate the {@link Backend}.  Based on the current configuration,
     * begin pushing data to the Storage layer.  If the {@link Backend} is
     * already active, this method should have no effect
     *
     * @return true on success, false if there was an error
     */
    public abstract boolean activate();

    /**
     * Deactivate the {@link Backend}. The backend should release any
     * resources that were obtained as a direct result of a call to
     * {@link #activate()}.  If the {@link Backend} is not active, this
     * method should have no effect
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
