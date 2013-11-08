/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.common;

/**
 * This interface defines a standard way for Thermostat plug-ins to specify
 * an ordering for their services in relation to others of the same type.
 */
public interface Ordered {
    
    /**
     * Order group for services that provide generic overview information
     * about a Host or VM.
     */
    public static final int ORDER_DEFAULT_GROUP = 0;
    /**
     * Order group for services that provide information about a Host
     * or VM's CPU usage.
     */
    public static final int ORDER_CPU_GROUP = 100;
    /**
     * Order group for services that provide information about a Host
     * or VM's memory usage.
     */
    public static final int ORDER_MEMORY_GROUP = 200;
    /**
     * Order group for services that provide information about a Host
     * or VM's network usage.
     */
    public static final int ORDER_NETWORK_GROUP = 300;
    /**
     * Order group for services that provide information about a Host
     * or VM's I/O usage.
     */
    public static final int ORDER_IO_GROUP = 400;
    /**
     * Order group for services that provide information about a Host
     * or VM's threads.
     */
    public static final int ORDER_THREAD_GROUP = 500;
    /**
     * Order group for user-defined services. This should always be
     * the last order group.
     */
    public static final int ORDER_USER_GROUP = 5000;

    /**
     * Order group for services that should be executed first. 
     */
    public static final int ORDER_FIRST = ORDER_DEFAULT_GROUP;
    
    /**
     * Order group for services that should be executed last. 
     */
    public static final int ORDER_LAST = Integer.MAX_VALUE;

    /**
     * Defines a value to be used for assigning an order to
     * services. A service with a lower order value will
     * be processed before a service of a higher order value. This
     * ordering is used, for example, to sort views in a client's UI.
     * Please take care to ensure the value returned by this
     * implementation does not collide with other implementations.
     * 
     * The order value should be offset from one of the provided
     * constants in this class. Such as {@link #ORDER_DEFAULT_GROUP}.
     * @return the order value
     */
    public int getOrderValue();

}

