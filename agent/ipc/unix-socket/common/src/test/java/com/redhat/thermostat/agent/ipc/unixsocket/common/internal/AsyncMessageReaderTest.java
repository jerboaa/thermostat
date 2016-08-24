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

package com.redhat.thermostat.agent.ipc.unixsocket.common.internal;

import static com.redhat.thermostat.agent.ipc.unixsocket.common.internal.ChannelTestUtils.createHeader;
import static com.redhat.thermostat.agent.ipc.unixsocket.common.internal.ChannelTestUtils.joinByteArrays;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.ChannelTestUtils.ReadAnswer;

public class AsyncMessageReaderTest {

    private ThermostatLocalSocketChannelImpl channel;
    private MessageListener listener;
    private AsyncMessageReader reader;

    @Before
    public void setUp() throws Exception {
        channel = mock(ThermostatLocalSocketChannelImpl.class);
        listener = mock(MessageListener.class);
        reader = new AsyncMessageReader(channel, listener);
    }

    @Test
    public void testReadDataSingle() throws Exception {
        final byte[] messageBytes = "Hello".getBytes(Charset.forName("UTF-8"));
        final byte[] headerBytes = createHeader(messageBytes.length, false);
        
        // First read header, then message
        when(channel.read(any(ByteBuffer.class))).thenAnswer(new ReadAnswer(new byte[][] { headerBytes, messageBytes }));
        
        reader.readData();
        verify(listener, never()).messageRead(any(ByteBuffer.class));
        
        reader.readData();
        ByteBuffer expected = ByteBuffer.wrap(messageBytes);
        verify(listener).messageRead(expected);
    }
    
    @Test
    public void testReadDataMulti() throws Exception {
        final byte[] fullMessageBytes = "Hello World.".getBytes(Charset.forName("UTF-8"));
        final byte[] message1Bytes = Arrays.copyOfRange(fullMessageBytes, 0, 5);
        final byte[] header1Bytes = createHeader(message1Bytes.length, true);
        final byte[] message2Bytes = Arrays.copyOfRange(fullMessageBytes, 5, fullMessageBytes.length);
        final byte[] header2Bytes = createHeader(message2Bytes.length, false);
        
        // First first header, then all remaining data
        byte[] joined = joinByteArrays(message1Bytes, header2Bytes, message2Bytes);
        byte[][] bufs = new byte[][] { header1Bytes, joined };
        when(channel.read(any(ByteBuffer.class))).thenAnswer(new ReadAnswer(bufs));
        
        reader.readData();
        verify(listener, never()).messageRead(any(ByteBuffer.class));
        
        reader.readData();
        ByteBuffer expected = ByteBuffer.wrap(fullMessageBytes);
        verify(listener).messageRead(expected);
    }
    
}
