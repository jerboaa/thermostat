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

package com.redhat.thermostat.agent.ipc.tcpsocket.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketAddress;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.client.IPCMessageChannel;
import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.tcpsocket.client.internal.TcpSocketTransportImpl.SocketHelper;
import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.ThermostatSocketChannelImpl;
import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.TcpSocketIPCProperties;

public class TcpSocketTransportImplTest {
    
    private static final String SERVER_NAME = "test";
    private SocketAddress socketAddr;
    private SocketHelper sockHelper;
    private TcpSocketMessageChannel messageChannel;
    private TcpSocketIPCProperties props;

    @Before
    public void setUp() throws Exception {
        socketAddr = mock(SocketAddress.class);
        sockHelper = mock(SocketHelper.class);
        ThermostatSocketChannelImpl sockChannel = mock(ThermostatSocketChannelImpl.class);
        when(sockHelper.openSocketChannel(eq(SERVER_NAME), eq(socketAddr))).thenReturn(sockChannel);
        when(sockHelper.createMessageChannel(sockChannel)).thenReturn(messageChannel);
        
        props = mock(TcpSocketIPCProperties.class);
        when(props.getSocketAddr(SERVER_NAME)).thenReturn(socketAddr);
    }

    @Test
    public void testConnectToServer() throws Exception {
        TcpSocketTransportImpl service = new TcpSocketTransportImpl(props, sockHelper);
        IPCMessageChannel result = service.connect(SERVER_NAME);
        assertEquals(messageChannel, result);
    }
    
    @Test(expected=IOException.class)
    public void testBadProperties() throws Exception {
        // Not TcpSocketIPCProperties
        IPCProperties props = mock(IPCProperties.class);
        new TcpSocketTransportImpl(props, sockHelper);
    }
/**
    @Test
    public void testConnectToServerNoOpen() throws Exception {
        TcpSocketTransportImpl service = new TcpSocketTransportImpl(props, sockHelper);
        
        try {
            service.connect(SERVER_NAME);
            fail("Expected IOException");
        } catch (IOException ignored) {
            verify(sockHelper, never()).openSocketChannel(SERVER_NAME, socketAddr);
        }
    }
 **/
 
    @Test
    public void testConnectToServerBadSocket() throws Exception {
        when(sockHelper.openSocketChannel(SERVER_NAME, socketAddr)).thenThrow(new IOException());
        TcpSocketTransportImpl service = new TcpSocketTransportImpl(props, sockHelper);
        
        try {
            service.connect(SERVER_NAME);
            fail("Expected IOException");
        } catch (IOException ignored) {
            verify(sockHelper).openSocketChannel(SERVER_NAME, socketAddr);
        }
    }

}
