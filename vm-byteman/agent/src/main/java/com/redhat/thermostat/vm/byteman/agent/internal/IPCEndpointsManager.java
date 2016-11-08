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

package com.redhat.thermostat.vm.byteman.agent.internal;

import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Class responsible for starting and stopping IPC endpoints,
 * the inbound communication endpoint for messages from Byteman
 * via the Thermostat Byteman helper. 
 *
 */
class IPCEndpointsManager {
    
    private static final Logger logger = LoggingUtils.getLogger(IPCEndpointsManager.class);
    
    private final Set<String> sockets = new HashSet<String>();
    private final AgentIPCService ipcService;
    
    IPCEndpointsManager(AgentIPCService ipcService) {
        this.ipcService = ipcService;
    }
    
    synchronized void startIPCEndpoint(final VmSocketIdentifier socketId, final ThermostatIPCCallbacks callback, 
            final UserPrincipal owner) {
        logger.fine("Starting IPC socket for byteman helper");
        String sId = socketId.getName();
        if (!sockets.contains(sId)) {
            try {
                if (ipcService.serverExists(sId)) {
                    // We create the sockets in a way that's unique per agent/vmId/pid. If we have
                    // two such sockets there is a problem somewhere.
                    logger.warning("Socket with id: " + sId + " already exists. Bug?");
                    return;
                }
                ipcService.createServer(sId, callback, owner);
                sockets.add(sId);
                logger.fine("Created IPC endpoint for id: " + sId);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to start IPC entpoint for id: " + sId);
            }
        }
    }
    
    synchronized void stopIPCEndpoint(VmSocketIdentifier socketId) {
        String sId = socketId.getName();
        if (sockets.contains(sId)) {
            logger.fine("Destroying socket for id: " + sId);
            try {
                ipcService.destroyServer(sId);
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to destroy socket id: " + sId, e);
            }
            sockets.remove(sId);
        }
    }
    
    // for testing
    synchronized boolean isSocketTracked(String name) {
        return sockets.contains(name);
    }
}
