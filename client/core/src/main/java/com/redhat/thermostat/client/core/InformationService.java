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

package com.redhat.thermostat.client.core;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.common.dao.Ref;


/**
 * Marker interface for information services.
 *
 */
public interface InformationService<T extends Ref> {
    
    /**
     * Priority group for services that provide generic overview information
     * about a Host or VM.
     */
    public static final int PRIORITY_DEFAULT_GROUP = 0;
    /**
     * Priority group for services that provide information about a Host
     * or VM's CPU usage.
     */
    public static final int PRIORITY_CPU_GROUP = 100;
    /**
     * Priority group for services that provide information about a Host
     * or VM's memory usage.
     */
    public static final int PRIORITY_MEMORY_GROUP = 200;
    /**
     * Priority group for services that provide information about a Host
     * or VM's network usage.
     */
    public static final int PRIORITY_NETWORK_GROUP = 300;
    /**
     * Priority group for services that provide information about a Host
     * or VM's I/O usage.
     */
    public static final int PRIORITY_IO_GROUP = 400;
    /**
     * Priority group for services that provide information about a Host
     * or VM's threads.
     */
    public static final int PRIORITY_THREAD_GROUP = 500;
    /**
     * Priority group for user-defined services. This should always be
     * the last priority group.
     */
    public static final int PRIORITY_USER_GROUP = 5000;

    /**
     * Defines a priority to be used for assigning an order to
     * InformationServices. A service with a lower-valued priority will
     * be processed before a service of a higher-valued priority. This
     * ordering is used, for example, to sort views in a client's UI.
     * 
     * The priority value should be offset from one of the provided
     * constants in this class. Such as {@link #PRIORITY_DEFAULT_GROUP}.
     * @return the priority value
     */
    public int getPriority();

    public Filter<T> getFilter();

    public InformationServiceController<T> getInformationServiceController(T ref);

}
