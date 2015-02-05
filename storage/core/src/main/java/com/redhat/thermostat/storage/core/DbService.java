/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.storage.core;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.Connection.ConnectionListener;

/**
 * DbService provides API for handling database (i.e. {@link Storage})
 * connections. Once {@link DbService#connect()} has been called, the current
 * instance can be retrieved as service. This registered service instance could
 * then be used to disconnect from storage.
 * 
 * @see {@link DbServiceFactory}
 */
@Service
public interface DbService {

    /**
     * Connects to {@link Storage} and registers {@link Storage} instance which
     * was used for the connection as a service.
     * 
     * <br/>
     * <br/>
     * <strong>Pre:</strong> Neither DbService nor Storage are registered as
     * services. <br/>
     * <strong>Post:</strong> Both DbService and Storage are registered as
     * services.
     * 
     * @throws ConnectionException
     *             If DB connection cannot be established.
     */
    void connect() throws ConnectionException;

    /**
     * Disconnects from {@link Storage}.
     * 
     * <br/>
     * <br/>
     * <strong>Pre:</strong> Both DbService and Storage are registered as
     * services.
     * <br/>
     * <strong>Post:</strong> Neither DbService nor Storage are registered as
     * services.
     * 
     * @throws ConnectionException
     */
    void disconnect() throws ConnectionException;

    /**
     * @returns the storage URL which was used for connection.
     * 
     * @throws IllegalStateException
     *             if not connected to storage.
     */
    String getConnectionUrl();

    /**
     * @returns the username which was used for the connection
     * @see Connection#getUsername()
     */
    String getUserName();

    /**
     * Registers the supplied ConnectionListener to be notified when the status
     * of the database connection changes.
     * 
     * @param listener
     *            - the listener to be registered
     */
    void addConnectionListener(ConnectionListener listener);

    /**
     * Unregisters the supplied ConnectionListener if it was previously
     * registered via {@link #addConnectionListener(ConnectionListener)}.
     * 
     * @param listener
     *            - the listener to be unregistered
     */
    void removeConnectionListener(ConnectionListener listener);

}

