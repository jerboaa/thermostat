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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.WriterID;

public class Activator implements BundleActivator {
    
    private static final Logger logger = LoggingUtils.getLogger(Activator.class);
    
    private ServerIPCPropertiesBuilder propBuilder;
    private AgentIPCServiceImpl ipcService;
    private MultipleServiceTracker tracker;
    private ServiceRegistration reg;
    
    public void start(final BundleContext context) throws Exception {
        propBuilder = new ServerIPCPropertiesBuilder(context);
        Class<?>[] deps = new Class<?>[] { CommonPaths.class, WriterID.class };
        tracker = new MultipleServiceTracker(context, deps, new Action() {
            
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                CommonPaths paths = (CommonPaths) services.get(CommonPaths.class);
                WriterID writerID = (WriterID) services.get(WriterID.class);
                File propFile = paths.getUserIPCConfigurationFile();
                ipcService = new AgentIPCServiceImpl(propBuilder, context, propFile, writerID);
                reg = context.registerService(AgentIPCService.class.getName(), ipcService, null);
            }

            @Override
            public void dependenciesUnavailable() {
                if (reg != null) {
                    try {
                        ipcService.shutdown();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Failed to stop IPC service", e);
                    }
                    reg.unregister();
                    reg = null;
                }
            }
        });
        
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        propBuilder.close();
        tracker.close();
    }
    
    // For testing purposes
    ServerIPCPropertiesBuilder getPropertiesBuilder() {
        return propBuilder;
    }
    
}

