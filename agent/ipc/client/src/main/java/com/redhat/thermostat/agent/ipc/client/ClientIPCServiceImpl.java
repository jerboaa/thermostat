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

package com.redhat.thermostat.agent.ipc.client;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.util.ServiceLoader;

import com.redhat.thermostat.agent.ipc.client.internal.ClientIPCPropertiesBuilder;
import com.redhat.thermostat.agent.ipc.client.internal.ClientTransport;
import com.redhat.thermostat.agent.ipc.client.internal.ClientTransportProvider;
import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;

class ClientIPCServiceImpl implements ClientIPCService {
    
    private final ClientTransport transport;
    
    ClientIPCServiceImpl(File ipcProperties) throws IOException {
        this(ipcProperties, new ClientIPCPropertiesBuilder(), new ServiceLoaderHelper());
    }
    
    ClientIPCServiceImpl(File ipcProperties, ClientIPCPropertiesBuilder builder, 
            ServiceLoaderHelper serviceHelper) throws IOException {
        IPCProperties props = builder.getProperties(ipcProperties);
        this.transport = createTransport(props, serviceHelper);
    }
    
    @Override
    public ByteChannel connectToServer(String name) throws IOException {
        return transport.connect(name);
    }

    private ClientTransport createTransport(IPCProperties props, ServiceLoaderHelper serviceHelper) throws IOException {
        ClientTransport result = null;
        IPCType type = props.getType();
        Iterable<ClientTransportProvider> providers = serviceHelper.getServiceLoader();
        for (ClientTransportProvider provider : providers) {
            if (provider.getType().equals(type)) {
                result = provider.create(props);
            }
        }
        if (result == null) {
            throw new IOException("Unable to create transport for IPC type: " + type.getConfigValue());
        }
        return result;
    }
    
    // For testing purposes
    ClientTransport getTransport() {
        return transport;
    }
    
    // For testing purposes. ServiceLoader is final and can't be mocked.
    static class ServiceLoaderHelper {
        Iterable<ClientTransportProvider> getServiceLoader() {
            return ServiceLoader.load(ClientTransportProvider.class);
        }
    }
    
}
