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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.tcpsocket.server.internal.ThermostatServerSocketChannelImpl.TcpServerSocketChannelHelper;

public class ThermostatServerSocketChannelImplTest {
    
    private static final String SERVER_NAME = "test";
    
    private TcpServerSocketChannelHelper channelHelper;
    private AcceptThread acceptThread;
    private SocketAddress addr;
    private ServerSocketChannel impl;
    private Selector selector;
    private SelectionKey key;
    private ThermostatIPCCallbacks callbacks;
    private IPCProperties props;
    
    @Before
    public void setUp() throws IOException {
        channelHelper = mock(TcpServerSocketChannelHelper.class);
        ThermostatServerSocketChannelImpl.setChannelHelper(channelHelper);
        addr = mock(SocketAddress.class);

        acceptThread = mock(AcceptThread.class);

        impl = mock(ServerSocketChannel.class);
        when(channelHelper.open()).thenReturn(impl);
        when(channelHelper.isOpen(impl)).thenReturn(true);
        
        selector = mock(Selector.class);
        callbacks = mock(ThermostatIPCCallbacks.class);

        key = mock(SelectionKey.class);
        when(channelHelper.register(acceptThread, impl, SelectionKey.OP_ACCEPT)).thenReturn(key);
        props = mock(IPCProperties.class);
        File propFile = mock(File.class);
        when(props.getPropertiesFile()).thenReturn(propFile);
    }
    
    @After
    public void tearDown() {
        ThermostatServerSocketChannelImpl.setChannelHelper(new TcpServerSocketChannelHelper());
    }
    
    @Test
    public void testOpen() throws Exception {
        ThermostatServerSocketChannelImpl channel = createChannel();
        verifyOpenSuccess(channel);
    }

    @Test
    public void testCreateChannel() throws Exception {
        try {
            createChannel();
        } catch (IOException e) {
            verifyOpenFailure();
        }
    }

    private void verifyOpenSuccess(ThermostatServerSocketChannelImpl channel) throws IOException {
        verify(channelHelper).open();
        verify(channelHelper).bind(impl, addr);
        verify(channelHelper).configureBlocking(impl, false);
        verify(channelHelper).register(acceptThread, impl, SelectionKey.OP_ACCEPT);
        verify(channelHelper).attachToKey(key, channel);
        verify(selector).wakeup();
        
        assertEquals(SERVER_NAME, channel.getName());
        assertEquals(callbacks, channel.getCallbacks());
    }
    
    private void verifyOpenFailure() throws IOException {
        verify(channelHelper, never()).open();
        verify(channelHelper, never()).bind(impl, addr);
        verify(channelHelper, never()).configureBlocking(impl, false);
        verify(channelHelper, never()).register(acceptThread, impl, SelectionKey.OP_ACCEPT);
        verify(channelHelper, never()).attachToKey(eq(key), any());
        verify(selector, never()).wakeup();
    }

    @Test
    public void testIsOpen() throws IOException {
        ThermostatServerSocketChannelImpl channel = createChannel();
        channel.isOpen();
        verify(channelHelper).isOpen(impl);
    }
    
    @Test
    public void testAccept() throws IOException {
        ThermostatServerSocketChannelImpl channel = createChannel();
        SocketChannel clientImpl = mock(SocketChannel.class);
        when(impl.accept()).thenReturn(clientImpl);
        channel.accept();
        verify(impl).accept();
        verify(channelHelper).configureBlocking(clientImpl, false);
        verify(channelHelper).register(acceptThread, clientImpl, SelectionKey.OP_READ);
    }
    
    @Test
    public void testAcceptClosed() throws IOException {
        when(channelHelper.isOpen(impl)).thenReturn(false);
        ThermostatServerSocketChannelImpl channel = createChannel();
        SocketChannel clientImpl = mock(SocketChannel.class);
        when(impl.accept()).thenReturn(clientImpl);

        try {
            channel.accept();
            fail("Expected IOException");
        } catch (IOException e) {
            verify(impl, never()).accept();
        }
    }
    
    @Test
    public void testClose() throws IOException {
        ThermostatServerSocketChannelImpl channel = createChannel();
        channel.close();
        verify(key).cancel();
        verify(channelHelper).close(impl);
    }

    private ThermostatServerSocketChannelImpl createChannel() throws IOException {
        return ThermostatServerSocketChannelImpl.open(SERVER_NAME, addr, callbacks, props, selector, acceptThread);
    }

}
