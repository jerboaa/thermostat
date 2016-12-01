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

package com.redhat.thermostat.agent.ipc.tcpsocket.server.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.SocketAddress;

import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.TcpSocketIPCProperties;
import com.redhat.thermostat.agent.ipc.tcpsocket.server.internal.TcpSocketServerTransport.ChannelUtils;
import com.redhat.thermostat.agent.ipc.tcpsocket.server.internal.TcpSocketServerTransport.ThreadCreator;

public class TcpSocketServerTransportTest {
    
    private static final String SERVER_NAME = "test";
    private static final String BAD_SERVER_NAME = "junk";
    private static final String USERNAME = "testUser";
    private static final int TEST_PORT = 9876;
    
    private TcpSocketServerTransport transport;
    private SelectorProvider provider;
    private AbstractSelector selector;
    private ExecutorService execService;
    private AcceptThread acceptThread;
    private ThreadCreator threadCreator;
    private ThermostatIPCCallbacks callbacks;
    private ChannelUtils channelUtils;
    private ThermostatServerSocketChannelImpl channel;
    private TcpSocketIPCProperties props;
    private SocketAddress addr;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        provider = mock(SelectorProvider.class);
        selector = mock(AbstractSelector.class);
        when(provider.openSelector()).thenReturn(selector);

        addr = mock(SocketAddress.class);
        
        props = mock(TcpSocketIPCProperties.class);
        //when(props.getSocketAddr(SERVER_NAME)).thenReturn(new InetSocketAddress(InetAddress.getLoopbackAddress(),TEST_PORT));
        when(props.getSocketAddr(SERVER_NAME)).thenReturn(addr);
        when(props.getType()).thenReturn(IPCType.TCP_SOCKET);
        doThrow(new IOException()).when(props).getSocketAddr(BAD_SERVER_NAME);
        
        execService = mock(ExecutorService.class);
        
        acceptThread = mock(AcceptThread.class);
        threadCreator = mock(ThreadCreator.class);
        when(threadCreator.createAcceptThread(selector, execService)).thenReturn(acceptThread);
        
        channelUtils = mock(ChannelUtils.class);
        channel = mock(ThermostatServerSocketChannelImpl.class);
        
        callbacks = mock(ThermostatIPCCallbacks.class);
        when(channelUtils.createServerSocketChannel(SERVER_NAME, addr, callbacks, props, selector, acceptThread)).thenReturn(channel);
        
        transport = new TcpSocketServerTransport(provider, execService, threadCreator, channelUtils);
    }
    
    @Test
    public void testInit() throws Exception {
        transport.start(props);
        verify(provider).openSelector();
        verify(threadCreator).createAcceptThread(selector, execService);
    }
    
    @Test(expected=IOException.class)
    public void testStartBadProperties() throws Exception {
        // Not TcpSocketIPCProperties
        IPCProperties badProps = mock(IPCProperties.class);
        when(badProps.getType()).thenReturn(IPCType.UNKNOWN);
        transport = new TcpSocketServerTransport(provider, execService, threadCreator, channelUtils);
        transport.start(badProps);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testStartSuccess() throws Exception {
        transport.start(props);
        
        // this test (in unixsocket) used to verify the thread to accept connections is started
        // this is not true for TCP - locks up verify(acceptThread).start();

        // instead, make sure we haven't started the new thread
        verify(acceptThread,never()).start();

        assertEquals(acceptThread, transport.getAcceptThread());
    }
    
    @Test
    public void testShutdownSuccess() throws Exception {
        transport.start(props);
        transport.shutdown();
        verify(acceptThread).shutdown();
        verify(channelUtils).closeSelector(selector);
    }
    
    //@Test
    @Test(expected=IOException.class)
    public void testShutdownFailure() throws Exception {
        transport.start(props);
        doThrow(new IOException()).when(acceptThread).shutdown();
        transport.shutdown();
        fail("Expected IO Exception");
    }
    
    @Test
    public void testCreateServer() throws Exception {
        transport.start(props);
        transport.createServer(SERVER_NAME, callbacks);
        checkChannel();
    }

    private void checkChannel() throws IOException {
        verify(channelUtils).createServerSocketChannel(SERVER_NAME, addr, callbacks, props, selector, acceptThread);
        ThermostatServerSocketChannelImpl result = transport.getSockets().get(SERVER_NAME);
        assertEquals(channel, result);
    }

    @Test(expected=IOException.class)
    public void testCreateServerServerExists() throws Exception {
        ThermostatServerSocketChannelImpl channel = mock(ThermostatServerSocketChannelImpl.class);
        transport.getSockets().put(SERVER_NAME, channel);
        transport.createServer(SERVER_NAME, callbacks);
    }
    
    @Test(expected=IOException.class)
    public void testCreateServerUnknownName() throws Exception {
        transport.start(props);
        transport.createServer(BAD_SERVER_NAME, callbacks);
    }

    @Test
    public void testServerExists() throws Exception {
        assertFalse(transport.serverExists(SERVER_NAME));
        transport.getSockets().put(SERVER_NAME, channel);
        assertTrue(transport.serverExists(SERVER_NAME));
    }
    
    @Test
    public void testDestroyServer() throws Exception {
        transport.getSockets().put(SERVER_NAME, channel);
        transport.destroyServer(SERVER_NAME);
        verify(channel).close();
    }
    
    @Test(expected=IOException.class)
    public void testDestroyServerNotExist() throws Exception {
        transport.destroyServer(BAD_SERVER_NAME);
    }
    
    //@Test
    @Test(expected=IOException.class)
    public void testDestroyServerCloseFails() throws Exception {
        doThrow(new IOException()).when(channel).close();
        transport.start(props);
        transport.getSockets().put(SERVER_NAME, channel);
        transport.destroyServer(SERVER_NAME);
        //fail("Expected IOException");
    }
}
