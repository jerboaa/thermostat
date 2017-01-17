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

package com.redhat.thermostat.agent.ipc.tcpsocket.common.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.ThermostatSocketChannelImpl.TcpSocketChannelHelper;


public class ThermostatSocketChannelImplTest {
    
    private TcpSocketChannelHelper channelHelper;
    private SocketAddress addr;
    private SocketChannel impl;

    @Before
    public void setUp() throws Exception {
        this.channelHelper = mock(TcpSocketChannelHelper.class);
        addr = mock(SocketAddress.class);
        
        impl = mock(SocketChannel.class);
        when(channelHelper.open(addr)).thenReturn(impl);
        when(channelHelper.isOpen(impl)).thenReturn(true);
        
        ThermostatSocketChannelImpl.setChannelHelper(channelHelper);
    }
    
    @After
    public void tearDown() throws Exception {
        ThermostatSocketChannelImpl.setChannelHelper(new TcpSocketChannelHelper());
    }

    @Test
    public void testOpen() throws Exception {
        ThermostatSocketChannelImpl channel = ThermostatSocketChannelImpl.open("test", addr);
        verify(channelHelper).open(addr);
        
        assertEquals("test", channel.getName());
    }

    @Test
    public void testReadSingle() throws IOException {
        final int readSize = 20;
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenReturn(readSize);
        
        ThermostatSocketChannelImpl channel = createChannel();
        ByteBuffer buf = ByteBuffer.allocate(readSize);
        int read = channel.read(buf);
        
        assertEquals(readSize, read);
        verify(channelHelper).read(impl, buf);
    }

    @Test
    public void testReadEOF() throws IOException {
        final int readSize = 20;
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenReturn(-1);
        
        ThermostatSocketChannelImpl channel = createChannel();
        ByteBuffer buf = ByteBuffer.allocate(readSize);
        int read = channel.read(buf);
        
        assertEquals(-1, read);
        
        verify(channelHelper).read(impl, buf);
    }
    
    @Test
    public void testWriteSingle() throws Exception {
        final int writeSize = 20;
        when(channelHelper.write(eq(impl), any(ByteBuffer.class))).thenReturn(writeSize);
        
        ThermostatSocketChannelImpl channel = createChannel();
        ByteBuffer buf = ByteBuffer.allocate(writeSize);
        int written = channel.write(buf);
        
        assertEquals(writeSize, written);
        verify(channelHelper).write(impl, buf);
    }
    
    @Test
    public void testIsOpen() throws Exception {
        ThermostatSocketChannelImpl channel = createChannel();
        boolean open = channel.isOpen();
        verify(channelHelper).isOpen(impl);
        assertTrue(open);
    }

    @Test
    public void testClose() throws Exception {
        ThermostatSocketChannelImpl channel = createChannel();
        channel.close();
        verify(channelHelper).close(impl);
    }

    private ThermostatSocketChannelImpl createChannel() throws IOException {
        return new ThermostatSocketChannelImpl("test", impl);
    }

}
