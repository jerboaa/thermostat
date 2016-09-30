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

package com.redhat.thermostat.agent.ipc.tcpsocket.client.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.SyncMessageReader;
import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.SyncMessageWriter;
import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.ThermostatSocketChannelImpl;

public class TcpSocketMessageChannelTest {
    
    private SyncMessageReader reader;
    private SyncMessageWriter writer;
    private TcpSocketMessageChannel channel;
    private ThermostatSocketChannelImpl sock;

    @Before
    public void setUp() {
        sock = mock(ThermostatSocketChannelImpl.class);
        reader = mock(SyncMessageReader.class);
        writer = mock(SyncMessageWriter.class);
        channel = new TcpSocketMessageChannel(sock, reader, writer);
    }
    
    @Test
    public void testReadMessage() throws Exception {
        ByteBuffer buf = mock(ByteBuffer.class);
        when(reader.readData()).thenReturn(buf);
        ByteBuffer result = channel.readMessage();
        assertEquals(buf, result);
    }
    
    @Test
    public void testWriteMessage() throws Exception {
        ByteBuffer buf = mock(ByteBuffer.class);
        channel.writeMessage(buf);
        verify(writer).writeData(buf);
    }
    
    @Test
    public void testIsOpen() throws Exception {
        when(sock.isOpen()).thenReturn(true);
        assertTrue(channel.isOpen());
        verify(sock).isOpen();
    }
    
    @Test
    public void testClose() throws Exception {
        channel.close();
        verify(sock).close();
    }

}
