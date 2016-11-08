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

package com.redhat.thermostat.agent.ipc.server.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.attribute.UserPrincipal;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.server.ServerTransport;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;

class AgentIPCServiceImpl implements AgentIPCService {
    
    private final ServerIPCPropertiesBuilder propBuilder;
    private final ServiceTracker transportTracker;
    // Access/modification of this field should by synchronized
    private final Map<IPCType, ServerTransport> transports;
    private final File propFile;
    private final FileHelper helper;
    
    private ServerTransport transport;
    private IPCProperties props;
    private boolean started;
    
    AgentIPCServiceImpl(ServerIPCPropertiesBuilder propBuilder, BundleContext context, File propFile) {
        this(propBuilder, context, propFile, new FileHelper());
    }
            
    AgentIPCServiceImpl(ServerIPCPropertiesBuilder propBuilder, BundleContext context, File propFile, FileHelper helper) {
        this.propBuilder = propBuilder;
        this.propFile = propFile;
        this.transports = new HashMap<>();
        this.helper = helper;
        this.started = false;
        
        this.transportTracker = new ServiceTracker(context, ServerTransport.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                synchronized (transports) {
                    ServerTransport transport = (ServerTransport) super.addingService(reference);
                    transports.put(transport.getType(), transport);
                    return transport;
                }
            }
            @Override
            public void removedService(ServiceReference reference, Object service) {
                synchronized (transports) {
                    ServerTransport transport = (ServerTransport) service;
                    transports.remove(transport.getType());
                    super.removedService(reference, service);
                }
            }
        };
        
        transportTracker.open();
    }
    
    void shutdown() throws IOException {
        transportTracker.close();
        if (started) {
            transport.shutdown();
        }
    }

    @Override
    public synchronized void createServer(String name, ThermostatIPCCallbacks callbacks) throws IOException {
        // Start the service if not already started
        if (!started) {
            startService();
        }
        transport.createServer(name, callbacks);
    }
    
    @Override
    public synchronized void createServer(String name, ThermostatIPCCallbacks callbacks, UserPrincipal owner) throws IOException {
        // Start the service if not already started
        if (!started) {
            startService();
        }
        transport.createServer(name, callbacks, owner);
    }

    @Override
    public synchronized boolean serverExists(String name) throws IOException {
        // Start the service if not already started
        if (!started) {
            startService();
        }
        return transport.serverExists(name);
    }

    @Override
    public synchronized void destroyServer(String name) throws IOException {
        // Start the service if not already started
        if (!started) {
            startService();
        }
        transport.destroyServer(name);
    }
    
    private void startService() throws IOException {
        this.props = getIPCProperties();
        this.transport = chooseTransport();
        
        // Initialize ServerTransport with IPCProperties
        transport.start(props);
        this.started = true;
    }

    boolean isStarted() {
        return started;
    }
    
    private ServerTransport chooseTransport() throws IOException {
        synchronized (transports) {
            IPCType type = props.getType();
            ServerTransport transport = transports.get(type);
            if (transport == null) {
                throw new IOException("No transport implementation installed for IPC type: " + type.getConfigValue());
            }
            return transport;
        }
    }
    
    private IPCProperties getIPCProperties() throws IOException {
        // If configuration file doesn't exist, create it
        if (!helper.configFileExists(propFile)) {
            IPCConfigurationWriter writer = helper.getConfigurationWriter(propFile);
            writer.write();
        }
        return propBuilder.getProperties(propFile);
    }
    
    // Helper class for testing purposes
    static class FileHelper {
        boolean configFileExists(File ipcConfig) {
            return ipcConfig.exists();
        }
        IPCConfigurationWriter getConfigurationWriter(File configFile) {
            return new IPCConfigurationWriter(configFile);
        }
    }

    // For testing purposes only
    Map<IPCType, ServerTransport> getTransports() {
        synchronized (transports) {
            // Return a copy            
            return new HashMap<>(transports);
        }
    }

}
