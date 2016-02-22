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

package com.redhat.thermostat.agent.ipc.unixsocket.server.internal;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCPropertiesBuilder;
import com.redhat.thermostat.agent.ipc.common.internal.IPCPropertiesProvider;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.UnixSocketIPCPropertiesProvider;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;

import jnr.enxio.channels.NativeSelectorProvider;

public class Activator implements BundleActivator {
    
    private static final Logger logger = LoggingUtils.getLogger(Activator.class);
    private final SelectorProvider provider;
    private final IPCServiceHelper helper;
    private AgentIPCServiceImpl ipcService;
    private Selector selector;
    private MultipleServiceTracker tracker;
    private ServiceRegistration reg;
    
    public Activator() {
        this(NativeSelectorProvider.getInstance(), new IPCServiceHelper());
    }
    
    Activator(SelectorProvider provider, IPCServiceHelper helper) {
        this.provider = provider;
        this.helper = helper;
    }

    public void start(final BundleContext context) throws Exception {
        // Register the Unix socket IPC properties provider
        context.registerService(IPCPropertiesProvider.class.getName(), new UnixSocketIPCPropertiesProvider(), null);
        selector = provider.openSelector();
        Class<?>[] deps = { CommonPaths.class, IPCPropertiesBuilder.class };
        tracker = new MultipleServiceTracker(context, deps, new Action() {

            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                CommonPaths paths = (CommonPaths) services.get(CommonPaths.class.getName());
                IPCPropertiesBuilder builder = (IPCPropertiesBuilder) services.get(IPCPropertiesBuilder.class.getName());
                try {
                    IPCProperties props = getIPCProperties(builder, paths);
                    // Only register service if IPC type is Unix sockets
                    if (props.getType() == IPCType.UNIX_SOCKET) {
                        ipcService = helper.createService(selector, props);
                        ipcService.start();
                        reg = context.registerService(AgentIPCService.class.getName(), ipcService, null);
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "IPC service failed to start", e);
                }
            }

            @Override
            public void dependenciesUnavailable() {
                try {
                    if (reg != null) {
                        reg.unregister();
                    }
                    if (ipcService != null) {
                        ipcService.shutdown();
                        ipcService = null;
                    }
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to shutdown IPC service", e);
                }
            }
        });
        tracker.open();
    }

    private IPCProperties getIPCProperties(IPCPropertiesBuilder builder, CommonPaths paths) throws IOException {
        // Get IPC config properties
        File ipcConfig = paths.getUserIPCConfigurationFile();
        // If configuration file doesn't exist, create it
        if (!helper.configFileExists(ipcConfig)) {
            IPCConfigurationWriter writer = helper.getConfigurationWriter(paths);
            writer.write();
        }
        return builder.getProperties(ipcConfig);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
        selector.close();
    }
    
    /* For testing purposes */
    static class IPCServiceHelper {
        AgentIPCServiceImpl createService(Selector sel, IPCProperties props) throws IOException {
            return new AgentIPCServiceImpl(sel, props);
        }
        
        boolean configFileExists(File configFile) {
            return configFile.exists();
        }
        
        UnixSocketIPCPropertiesProvider getProvider() {
            return new UnixSocketIPCPropertiesProvider();
        }
        
        IPCConfigurationWriter getConfigurationWriter(CommonPaths paths) {
            return new IPCConfigurationWriter(paths);
        }
    }

}

