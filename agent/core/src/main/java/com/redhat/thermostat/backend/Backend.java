/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import com.redhat.thermostat.annotations.ExtensionPoint;
import com.redhat.thermostat.common.Ordered;

/**
 * Represents a plugin that runs on the agent and performs monitoring of host
 * and applications.
 * <p>
 * To register a new backend, register an implementation of this interface with the OSGi
 * service registry.
 * 
 * Implementations are encouraged to make use of existing abstract helper classes, such
 * as {@link BaseBackend} or {@link VmListenerBackend}
 * 
 * @see BaseBackend
 * @see VmListenerBackend
 */
@ExtensionPoint
public interface Backend extends Ordered {

    /**
     * @return the name of the {@link Backend}
     */
    public String getName();

    /**
     * @returns the description of the {@link Backend}
     */
    public String getDescription();

    /**
     * @return the vendor of the {@link Backend}
     */
    public String getVendor();

    /** 
     * @return the version of the {@link Backend}
     */
    public String getVersion();

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
     * @return {@code true} if the backend was activated successfully or
     * already active. {@code false} if there was an error
     */
    public boolean activate();

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
     * @return {@code true} if the backend was successfully deactivated or
     * already inactive. {@code false} if the backend is still active.
     */
    public boolean deactivate();

    /**
     * @return a boolean indicating whether the backend is currently active on this host
     */
    public boolean isActive();

    /**
     * Indicate whether this backend will attach to new java processes.
     * 
     * @return true if this backend will attach to new java processes, false otherwise.
     */
    public boolean getObserveNewJvm();

    /**
     * Set whether this backend will attach to new java processes.
     * 
     * @param newValue
     */
    public void setObserveNewJvm(boolean newValue);

}

