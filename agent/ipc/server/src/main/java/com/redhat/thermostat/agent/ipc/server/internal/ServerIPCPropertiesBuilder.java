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

package com.redhat.thermostat.agent.ipc.server.internal;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCPropertiesBuilder;
import com.redhat.thermostat.agent.ipc.common.internal.IPCPropertiesProvider;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;

/*
 * An IPC property builder that uses an OSGi service tracker to discover
 * IPC properties providers. JDK ServiceLoaders don't work properly
 * with OSGi.
 */
class ServerIPCPropertiesBuilder extends IPCPropertiesBuilder {
    
    private final ServiceTracker tracker;
    // Access/modification of this field should by synchronized
    private final Map<IPCType, IPCPropertiesProvider> providers;
    
    private boolean closed;
    
    ServerIPCPropertiesBuilder(BundleContext context) {
        this.providers = new HashMap<>();
        this.closed = false;
        this.tracker = new ServiceTracker(context, IPCPropertiesProvider.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                synchronized (providers) {
                    IPCPropertiesProvider provider = (IPCPropertiesProvider) super.addingService(reference);
                    providers.put(provider.getType(), provider);
                    return provider;
                }
            }
            @Override
            public void removedService(ServiceReference reference, Object service) {
                synchronized (providers) {
                    IPCPropertiesProvider provider = (IPCPropertiesProvider) service;
                    providers.remove(provider.getType());
                    super.removedService(reference, service);
                }
            }
        };
        tracker.open();
    }

    @Override
    protected IPCProperties getPropertiesForType(IPCType type, Properties props, File propFile) throws IOException {
        synchronized (providers) {
            IPCPropertiesProvider provider = providers.get(type);
            if (provider == null) {
                throw new IOException("Unsupported IPC type: " + type.getConfigValue());
            }
            return provider.create(props, propFile);
        }
    }
    
    void close() {
        tracker.close();
        closed = true;
    }
    
    // For testing purposes
    boolean isClosed() {
        return closed;
    }
    
    // For testing purposes
    Map<IPCType, IPCPropertiesProvider> getProviders() {
        synchronized (providers) {
            // Return a copy
            return new HashMap<>(providers);
        }
    }

}
