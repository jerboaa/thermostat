/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.agent.ipc.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;

import com.redhat.thermostat.annotations.Service;

/**
 * Provides a standardized inter-process communication mechanism for the Thermostat agent.
 * This service manages the creation and destruction of IPC servers, an abstract construct
 * that allows communication between the agent and its subprocesses.
 */
@Service
public interface AgentIPCService {
    
    /**
     * Creates an IPC server identified by the provided name, which can be connected to
     * by IPC clients. The caller will be notified when this server receives data.
     * The created server will be owned by the user running the agent.
     * <p>
     * The server name must contain only letters, numbers, hyphens, and underscores.
     * @param name - A unique name for this IPC server, must not be null
     * @param callbacks - Object to be notified when data is received, must not be null
     * @throws IOException if this IPC server cannot be created for any reason
     */
    void createServer(String name, ThermostatIPCCallbacks callbacks) throws IOException;
    
    /**
     * Creates an IPC server identified by the provided name, which can be connected to
     * by IPC clients. The caller will be notified when this server receives data.
     * The created server will be owned by the user specified by the supplied {@link UserPrincipal}.
     * <p>
     * The server name must contain only letters, numbers, hyphens, and underscores.
     * @param name - A unique name for this IPC server, must not be null
     * @param callbacks - Object to be notified when data is received, must not be null
     * @param owner - principal representing the intended owner of the server
     * @throws IOException if this IPC server cannot be created for any reason
     */
    void createServer(String name, ThermostatIPCCallbacks callbacks, UserPrincipal owner) throws IOException;
    
    /**
     * Check if an IPC server exists with a given name.
     * @param name - name of server, must not be null
     * @return whether the named server exists
     * @throws IOException if an error occurred while checking for the named server
     */
    boolean serverExists(String name) throws IOException;
    
    /**
     * Destroys an IPC server identified by the provided name. Clients will no longer
     * be able to connect, or send/receive data from this server.
     * @param name - the name of the IPC server to destroy, must not be null
     * @throws IOException if this server does not exist, or cannot be destroyed for any reason
     */
    void destroyServer(String name) throws IOException;
    
    /**
     * Returns the configuration file used by this IPC service. This file contains properties used
     * to exchange information between the server-side and client-side IPC services.
     * @return the configuration file
     * @throws IOException if the configuration file cannot be obtained or the IPC service fails to start
     */
    File getConfigurationFile() throws IOException;
    
}
