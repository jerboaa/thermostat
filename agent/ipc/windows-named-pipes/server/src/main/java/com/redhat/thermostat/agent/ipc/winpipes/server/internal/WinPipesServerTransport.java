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

package com.redhat.thermostat.agent.ipc.winpipes.server.internal;

import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.server.ServerTransport;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesIPCProperties;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * Creates instances of Windows Pipes server channels.
 * There is one channel per pipe name.
 */
class WinPipesServerTransport implements ServerTransport {
    
    private static final Logger logger = LoggingUtils.getLogger(WinPipesServerTransport.class);

    // Access/modification of this field should by synchronized
    // key is pipename (as it appears in ipc.properties, not decorated with "\\pipe\\ ...")
    private final Map<String, WinPipesServerChannelImpl> pipes;
    private final PipenameValidator validator;
    private final ChannelUtils channelUtils;
    
    private WinPipesIPCProperties props;
    
    WinPipesServerTransport() {
        this(new PipenameValidator(), new ChannelUtils());
    }
    
    WinPipesServerTransport(PipenameValidator validator,
                            ChannelUtils channelCreator) {
        this.pipes = new HashMap<>();
        this.validator = validator;
        this.channelUtils = channelCreator;
    }
    
    @Override
    public void start(IPCProperties props) throws IOException {
        if (!(props instanceof WinPipesIPCProperties)) {
            IPCType type = props.getType();
            throw new IOException("Unsupported IPC type: " + type.getConfigValue());
        }
        this.props = (WinPipesIPCProperties) props;
        // for windows pipes, each individual named pipe will have a thread.
        // we don't staart one master accept thread here.
        logger.info("Agent IPC Windows Pipes server service started");
    }
    
    @Override
    public IPCType getType() {
        return IPCType.WINDOWS_NAMED_PIPES;
    }

    private void checkName(String name) throws IOException {
        Objects.requireNonNull(name, "Server name cannot be null");
        if (name.isEmpty()) {
            throw new IOException("Server name cannot be empty");
        }
        // Require limited character set for name
        boolean okay = validator.validate(name);
        if (!okay) {
            throw new IOException("Illegal server name");
        }
    }

    @Override
    public synchronized void createServer(String name, ThermostatIPCCallbacks callbacks) throws IOException {

        checkName(name);

        // Check if the pipe has already been created and we know about it
        if (pipes.containsKey(name)) {
            throw new IOException("Named pipe with name \"" + name + "\" already exists");
        }

        // Create and save pipe server
        WinPipesServerChannelImpl channel = channelUtils.createServerChannel(name, callbacks, props);
        pipes.put(name, channel);
    }

    @Override
    public void createServer(String name, ThermostatIPCCallbacks callbacks, UserPrincipal owner) throws IOException {
        createServer(name,callbacks);
    }

    @Override
    public synchronized boolean serverExists(String name) throws IOException {
        return pipes.containsKey(name);
    }     
    
    @Override
    public synchronized void destroyServer(String name) throws IOException {
        if (!pipes.containsKey(name)) {
            throw new IOException("IPC server with name \"" + name + "\" does not exist");
        }
        // Remove pipe from known pipes
        WinPipesServerChannelImpl channel = pipes.remove(name);

        // Close channel and stop accept thread
        channel.close();
    }

    @Override
    public void shutdown() throws IOException {
        // Stop accepting connections and close selector afterward
        // make a copy since we'll be removing elements from the original
        Set<String> pipeNames = new HashSet<>(pipes.keySet());
        for (final String pipeName : pipeNames) {
            destroyServer(pipeName);
        }
        logger.info("Agent IPC windows pipes server service stopped");
    }

    /* For testing purposes */
    Map<String, WinPipesServerChannelImpl> getPipes() {
        return pipes;
    }
    
    /* For testing purposes */
    static class ChannelUtils {
        WinPipesServerChannelImpl createServerChannel(String name, ThermostatIPCCallbacks callbacks, WinPipesIPCProperties props) throws IOException {
            return WinPipesServerChannelImpl.createChannel(name, callbacks, props);
        }
    }
}
