/*
 * Copyright 2012 Red Hat, Inc.
 *
 * This file is part of Thermostat.
 *
 * Thermostat is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2, or (at your
 * option) any later version.
 *
 * Thermostat is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Thermostat; see the file COPYING.  If not see
 * <http://www.gnu.org/licenses/>.
 *
 * Linking this code with other modules is making a combined work
 * based on this code.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this code give
 * you permission to link this code with independent modules to
 * produce an executable, regardless of the license terms of these
 * independent modules, and to copy and distribute the resulting
 * executable under terms of your choice, provided that you also
 * meet, for each linked independent module, the terms and conditions
 * of the license of that module.  An independent module is a module
 * which is not derived from or based on this code.  If you modify
 * this code, you may extend this exception to your version of the
 * library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.redhat.thermostat.backend;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.storage.Category;
import com.redhat.thermostat.common.storage.Chunk;
import com.redhat.thermostat.common.storage.Storage;

/**
 * Represents a monitoring back-end. All the {@link Backend}s should be
 * registered with the {@link BackendRegistry}.
 */
public abstract class Backend {

    private boolean initialConfigurationComplete = false;
    private Storage storage = null;
    private boolean observeNewJvm = attachToNewProcessByDefault();

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
        for (Category cat : getCategories()) {
            storage.registerCategory(cat);
        }
    }

    protected abstract Collection<Category> getCategories();

    /**
     * Set the named configuration to the given value.
     * @param name
     * @param value
     * @throws IllegalArgumentException if either the key does not refer to a valid configuration option
     *                                  for this backend or the value is not valid for the key
     */
    protected void setConfigurationValue(String name, String value) {
        throw new IllegalArgumentException("Backend " + getName() + " does not support any specific configuration values.");
    }

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

    /** Get a map containing the current settings of this backend.
     * Implementors of this abstract class which have some settings that
     * are be configurable by the client must override this method
     * to provide an appropriate map.
     * 
     * @return a map containing the settings of this backend
     */
    public Map<String, String> getConfigurationMap() {
        return new HashMap<String, String>();
    }

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

    /**
     * A {@link Backend} may be configured to automatically begin collecting from new Java
     * processes.  This method determines whether this will be the case when the backend
     * is initially started.
     * 
     * @return true if the initial backend behaviour is to attach to new java processes, false otherwise.
     */
    public abstract boolean attachToNewProcessByDefault();

    /**
     * Indicate whether this backend will attach to new java processes.
     * 
     * @return true if this backend will attach to new java processes, false otherwise.
     */
    public boolean getObserveNewJvm() {
        return observeNewJvm;
    }

    /**
     * Set whether this backend will attach to new java processes.
     * 
     * @param newValue
     */
    public void setObserveNewJvm(boolean newValue) {
        observeNewJvm = newValue;
    }

    public void store(Chunk chunk) {
        storage.putChunk(chunk);
    }

    public void update(Chunk chunk) {
        storage.updateChunk(chunk);
    }
}
