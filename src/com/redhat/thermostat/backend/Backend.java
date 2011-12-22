package com.redhat.thermostat.backend;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.redhat.thermostat.agent.storage.Category;
import com.redhat.thermostat.agent.storage.Chunk;
import com.redhat.thermostat.agent.storage.Storage;

/**
 * Represents a monitoring back-end. All the {@link Backend}s should be
 * registered with the {@link BackendRegistry}.
 */
public abstract class Backend {

    private boolean initialConfigurationComplete = false;
    private Storage storage;

    /**
     * 
     * @param configMap a map containing the settings that this backend has been configured with.
     * @throws LaunchException if map contains values that this backend does not accept.
     */
    public final void setInitialConfiguration(Map<String, String> configMap) throws BackendLoadException {
        if (initialConfigurationComplete) {
            throw new BackendLoadException("A backend may only receive intitial configuration once.");
        }
        for (Entry<String, String> e : configMap.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            try {
                setConfigurationValue(key, value);
            } catch (IllegalArgumentException iae) {
                throw new BackendLoadException("Attempt to set invalid backend configuration for " + getName()
                        + " backend.  Key: " + key + "   Value: " + value, iae);
            }
        }
        initialConfigurationComplete = true;
    }

    public final void setStorage(Storage storage) {
        this.storage = storage;
        for (Iterator<Category> iter = getCategoryIterator(); iter.hasNext();) {
            storage.registerCategory(iter.next(), this);
        }
    }

    protected abstract Iterator<Category> getCategoryIterator();

    /**
     * Set the named configuration to the given value.
     * @param name
     * @param value
     * @throws IllegalArgumentException if either the key does not refer to a valid configuration option
     *                                  for this backend or the value is not valid for the key
     */
    protected abstract void setConfigurationValue(String name, String value);

    /**
     * @return the name of the {@link Backend}
     */
    public abstract String getName();

    /**
     * @returns the description of the {@link Backend}
     */
    public abstract String getDescription();

    /**
     * @return the vendor of the {@link Backend}
     */
    public abstract String getVendor();

    /** 
     * @return the version of the {@link Backend}
     */
    public abstract String getVersion();

    /**
     * @return a map containing the settings of this backend
     */
    public abstract Map<String, String> getConfigurationMap();

    /**
     * 
     * @param key The constant key that corresponds to the desired configuration value
     * @return The current value of the configuration value corresponding to the key given.
     * @throws IllegalArgumentException if the key does not refer to a valid configuration option for
     *                                  this backend
     */
    public abstract String getConfigurationValue(String key);

    /**
     * Activate the {@link Backend}.  Based on the current configuration,
     * begin pushing data to the {@link Storage} layer.  If the {@link Backend} is
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
     * @return a boolean indicating whether the backend is currently active on this host
     */
    public abstract boolean isActive();

    public final void store(Chunk chunk) {
        storage.putChunk(chunk, this);
    }
}
