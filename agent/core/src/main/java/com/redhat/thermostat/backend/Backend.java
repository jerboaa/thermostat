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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.storage.core.Storage;

/**
 * Represents a plugin that runs on the agent and performs monitoring of host
 * and applications.
 */
public abstract class Backend implements Ordered {

    private boolean initialConfigurationComplete = false;
    protected DAOFactory df = null;
    private boolean observeNewJvm = attachToNewProcessByDefault();

    private String version;
    private String vendor;
    private String description;
    
    private BackendID id;

    public Backend(BackendID id) {
        this.id = id;
    }
    
    /**
     * 
     * @param configMap a map containing the settings that this backend has been configured with.
     * @throws LaunchException if map contains values that this backend does not accept.
     */
    protected final void setInitialConfiguration(Map<String, String> configMap) throws BackendLoadException {
        if (initialConfigurationComplete) {
            throw new BackendLoadException(id, "The backend " + id.toString() + "may only receive initial configuration once.");
        }
        for (Entry<String, String> e : configMap.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            try {
                setConfigurationValue(key, value);
            } catch (IllegalArgumentException iae) {
                throw new BackendLoadException(id, "Attempt to set invalid backend configuration for " + getName()
                        + " backend.  Key: " + key + "   Value: " + value, iae);
            }
        }
        initialConfigurationComplete = true;
    }

    public final void setDAOFactory(DAOFactory df) {
        this.df = df;
        setDAOFactoryAction();
    }

    protected abstract void setDAOFactoryAction();

    /**
     * Set the named configuration to the given value.
     * The basic special properties {@code name}, {@code version} and
     * {@code description} are parsed here.
     * 
     * <br /><br />
     * 
     * Subclasses can just override the
     * {@link #setConfigurationValueImpl(String, String)}
     * method if they are not interested in parsing and setting those
     * properties directly.
     * 
     * @param name
     * @param value
     * @throws IllegalArgumentException if either the key does not refer to a valid configuration option
     *                                  for this backend or the value is not valid for the key
     */
    protected void setConfigurationValue(String name, String value) {
        
        if (name.equals(BackendsProperties.DESCRIPTION.name())) {
            this.description = value;
        } else if (name.equals(BackendsProperties.VERSION.name())) {
            this.version = value;
        } else if (name.equals(BackendsProperties.VENDOR.name())) {
            this.vendor = value;
        } else {
            setConfigurationValueImpl(name, value);
        }
    }
    
    /**
     * Set the named configuration to the given value.
     * By default, does nothing.
     */
    protected void setConfigurationValueImpl(String name, String value) {}
    
    /**
     * @return the name of the {@link Backend}
     */
    public String getName() {
        return id.getSimpleName();
    }

    /**
     * @returns the description of the {@link Backend}
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the vendor of the {@link Backend}
     */
    public String getVendor() {
        return vendor;
    }

    /** 
     * @return the version of the {@link Backend}
     */
    public String getVersion() {
        return version;
    }

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
     * begin pushing data to the {@link Storage} layer.
     * If the {@link Backend} is already active, this method should have no
     * effect.
     * 
     * <br /><br />
     * 
     * This method is called by the framework when the {@link Backend} is
     * registered.
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
     * <br /><br />
     * 
     * This method is called by the framework when the {@link Backend} is
     * deregistered.
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
    
    public BackendID getID() {
        return id;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((vendor == null) ? 0 : vendor.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Backend other = (Backend) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (vendor == null) {
            if (other.vendor != null)
                return false;
        } else if (!vendor.equals(other.vendor))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Backend [version=" + version + ", vendor=" + vendor
                + ", description=" + description + ", id=" + id + "]";
    }
}
