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

package com.redhat.thermostat.agent.ipc.winpipes.client.internal;

import java.io.IOException;

import com.redhat.thermostat.agent.ipc.client.IPCMessageChannel;
import com.redhat.thermostat.agent.ipc.client.internal.ClientTransport;
import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesChannelImpl;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.WinPipesIPCProperties;

public class WinPipesTransportImpl implements ClientTransport {

    private final WinPipesIPCProperties pipeProps;
    private final PipeHelper pipeHelper;
    
    WinPipesTransportImpl(WinPipesIPCProperties props) throws IOException {
        this(props, new PipeHelper());
    }
    
    WinPipesTransportImpl(WinPipesIPCProperties props, PipeHelper pipeHelper) throws IOException {
        this.pipeProps = props;
        this.pipeHelper = pipeHelper;
    }
    
    @Override
    public IPCMessageChannel connect(String serverName) throws IOException {
        requireNonNull(serverName, "server name cannot be null");
        final WinPipesChannelImpl channel = pipeHelper.openChannel(pipeProps.getPipeName(serverName));
        return pipeHelper.createMessageChannel(channel);
    }

    // java.lang.Objects is JDK 7+ and we need this to be JDK 6 compat.
    private static void requireNonNull(Object item, String message) {
        if (item == null) {
            throw new NullPointerException(message);
        }
    }

    // Helper class for testing
    static class PipeHelper {
        WinPipesChannelImpl openChannel(String name) throws IOException {
            return WinPipesChannelImpl.open(name);
        }
        
        WinPipesMessageChannel createMessageChannel(WinPipesChannelImpl channel) {
            return new WinPipesMessageChannel(channel);
        }
    }
}
