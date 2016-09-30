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

package com.redhat.thermostat.agent.ipc.tcpsocket.common.internal;

import static com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.ChannelTestUtils.createByteArrays;
import static com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.ChannelTestUtils.createHeader;
import static com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.ChannelTestUtils.joinByteArrays;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.ChannelTestUtils.WriteAnswer;

public class SyncMessageWriterTest {
    
    private static final int MAX_MESSAGE_PART_SIZE = 5;

    private ThermostatSocketChannelImpl channel;
    private SyncMessageWriter writer;
    private MessageLimits limits;

    @Before
    public void setUp() throws Exception {
        channel = mock(ThermostatSocketChannelImpl.class);
        limits = mock(MessageLimits.class);
        when(limits.getMaxMessagePartSize()).thenReturn(MAX_MESSAGE_PART_SIZE);
        when(limits.getMaxMessageSize()).thenReturn(Integer.MAX_VALUE);
        writer = new SyncMessageWriter(channel, limits);
    }

    @Test
    public void testWriteDataSingle() throws Exception {
        final byte[] messageBytes = "hello".getBytes(Charset.forName("UTF-8"));
        final byte[] headerBytes = createHeader(messageBytes.length, false);
        final byte[][] results = createByteArrays(headerBytes.length, messageBytes.length);
        WriteAnswer answer = new WriteAnswer(results);
        when(channel.write(any(ByteBuffer.class))).thenAnswer(answer);
        
        ByteBuffer buf = ByteBuffer.wrap(messageBytes);
        writer.writeData(buf);
        
        assertArrayEquals(headerBytes, results[0]);
        assertArrayEquals(messageBytes, results[1]);
        verify(channel, times(2)).write(any(ByteBuffer.class));
    }
    
    @Test
    public void testWriteDataSplitHeader() throws Exception {
        final byte[] messageBytes = "hello".getBytes(Charset.forName("UTF-8"));
        final byte[] headerBytes = createHeader(messageBytes.length, false);
        final byte[][] results = createByteArrays(headerBytes.length - 2, 2, messageBytes.length);
        WriteAnswer answer = new WriteAnswer(results);
        when(channel.write(any(ByteBuffer.class))).thenAnswer(answer);
        
        ByteBuffer buf = ByteBuffer.wrap(messageBytes);
        writer.writeData(buf);
        
        final byte[] joinedHeader = joinByteArrays(results[0], results[1]);
        assertArrayEquals(headerBytes, joinedHeader);
        assertArrayEquals(messageBytes, results[2]);
        verify(channel, times(3)).write(any(ByteBuffer.class));
    }
    
    @Test
    public void testWriteDataSplitMessage() throws Exception {
        final byte[] messageBytes = "hello".getBytes(Charset.forName("UTF-8"));
        final byte[] headerBytes = createHeader(messageBytes.length, false);
        final byte[][] results = createByteArrays(headerBytes.length, messageBytes.length - 2, 2);
        WriteAnswer answer = new WriteAnswer(results);
        when(channel.write(any(ByteBuffer.class))).thenAnswer(answer);
        
        ByteBuffer buf = ByteBuffer.wrap(messageBytes);
        writer.writeData(buf);
        
        assertArrayEquals(headerBytes, results[0]);
        final byte[] joinedMessage = joinByteArrays(results[1], results[2]);
        assertArrayEquals(messageBytes, joinedMessage);
        verify(channel, times(3)).write(any(ByteBuffer.class));
    }
    
    @Test
    public void testWriteDataMulti() throws Exception {
        final String message = "hello world";
        final byte[] fullMessageBytes = message.getBytes(Charset.forName("UTF-8"));
        final byte[] message1Bytes = Arrays.copyOfRange(fullMessageBytes, 0, 5);
        final byte[] message2Bytes = Arrays.copyOfRange(fullMessageBytes, 5, 10);
        final byte[] message3Bytes = Arrays.copyOfRange(fullMessageBytes, 10, fullMessageBytes.length);
        final byte[] header1Bytes = createHeader(message1Bytes.length, true);
        final byte[] header2Bytes = createHeader(message2Bytes.length, true);
        final byte[] header3Bytes = createHeader(message3Bytes.length, false);
        
        final byte[][] results = createByteArrays(header1Bytes.length, message1Bytes.length, header2Bytes.length, message2Bytes.length, header3Bytes.length, message3Bytes.length);
        WriteAnswer answer = new WriteAnswer(results);
        when(channel.write(any(ByteBuffer.class))).thenAnswer(answer);
        
        ByteBuffer buf = ByteBuffer.wrap(fullMessageBytes);
        writer.writeData(buf);
        
        assertArrayEquals(header1Bytes, results[0]);
        assertArrayEquals(message1Bytes, results[1]);
        assertArrayEquals(header2Bytes, results[2]);
        assertArrayEquals(message2Bytes, results[3]);
        assertArrayEquals(header3Bytes, results[4]);
        assertArrayEquals(message3Bytes, results[5]);
        verify(channel, times(6)).write(any(ByteBuffer.class));
    }
    
    @Test(expected=IOException.class)
    public void testWriteMessageTooLarge() throws Exception {
        when(limits.getMaxMessageSize()).thenReturn(0);
        final byte[] messageBytes = "hello".getBytes(Charset.forName("UTF-8"));
        ByteBuffer buf = ByteBuffer.wrap(messageBytes);
        writer.writeData(buf);
    }

}
