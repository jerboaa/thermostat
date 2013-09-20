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

package com.redhat.thermostat.agent.command;

import java.io.IOException;

import com.redhat.thermostat.annotations.Service;
import com.redhat.thermostat.common.command.Request;

/**
 * An agent-side service that allows starting and stopping the command-channel
 * server.
 *
 * The command-channel server allows the agent to receive and process
 * {@link Request}s.
 */
@Service
public interface ConfigurationServer {

    /**
     * Starts the configuration server so it listens at the interface specified
     * by the {@code host} and {@code port}.
     * @throws IOException if it fails to listen
     */
    void startListening(String host, int port) throws IOException;

    /**
     * Shuts down the configuration server. No additional {@link Request}s will
     * be accepted after this is called, but existing ones may be processed to
     * completion.
     */
    void stopListening();

}
