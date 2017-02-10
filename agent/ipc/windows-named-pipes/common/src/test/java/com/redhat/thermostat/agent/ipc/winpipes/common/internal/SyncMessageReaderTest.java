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

package com.redhat.thermostat.agent.ipc.winpipes.common.internal;

import static com.redhat.thermostat.agent.ipc.winpipes.common.internal.ChannelTestUtils.createHeader;
import static com.redhat.thermostat.agent.ipc.winpipes.common.internal.ChannelTestUtils.joinByteArrays;
import static com.redhat.thermostat.agent.ipc.winpipes.common.internal.ChannelTestUtils.splitByteArray;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.winpipes.common.internal.ChannelTestUtils.ReadAnswer;

public class SyncMessageReaderTest {

    private static final int FULL_HEADER_SIZE = MessageHeader.getDefaultHeaderSize();
    private static final int MIN_HEADER_SIZE = MessageHeader.getMinimumHeaderSize();
    
    private WinPipesChannelImpl channel;
    private SyncMessageReader reader;

    @Before
    public void setUp() throws Exception {
        channel = mock(WinPipesChannelImpl.class);
        reader = new SyncMessageReader(channel);
    }

    @Test
    public void testReadDataSingle() throws Exception {
        final byte[] messageBytes = "hello".getBytes(Charset.forName("UTF-8"));
        final byte[] headerBytes = createHeader(messageBytes.length, false);
        
        byte[] data = joinByteArrays(headerBytes, messageBytes);
        // Split min header, remaining header and message into two pieces each
        mockReads(data, MIN_HEADER_SIZE - 3, MIN_HEADER_SIZE,
                FULL_HEADER_SIZE - 3, FULL_HEADER_SIZE, 
                FULL_HEADER_SIZE + 3);
        ByteBuffer result = reader.readData();
        assertEquals(ByteBuffer.wrap(messageBytes), result);
    }
    
    @Test
    public void testReadDataMulti() throws Exception {
        final String messageString = "hello world";
        final byte[] fullMessageBytes = messageString.getBytes(Charset.forName("UTF-8"));
        final byte[] message1Bytes = Arrays.copyOfRange(fullMessageBytes, 0, 5);
        final byte[] message2Bytes = Arrays.copyOfRange(fullMessageBytes, 5, fullMessageBytes.length);
        final byte[] header1Bytes = createHeader(message1Bytes.length, true);
        final byte[] header2Bytes = createHeader(message2Bytes.length, false);
        
        byte[] data = joinByteArrays(header1Bytes, message1Bytes, header2Bytes, message2Bytes);
        int messageBoundary = header1Bytes.length + message1Bytes.length;
        // Split min header, remaining header and message into two pieces each
        mockReads(data, MIN_HEADER_SIZE - 3, MIN_HEADER_SIZE,
                FULL_HEADER_SIZE - 3, FULL_HEADER_SIZE, 
                FULL_HEADER_SIZE + 3, messageBoundary, 
                messageBoundary + MIN_HEADER_SIZE - 3,
                messageBoundary + MIN_HEADER_SIZE,
                messageBoundary + FULL_HEADER_SIZE - 3,
                messageBoundary + FULL_HEADER_SIZE,
                messageBoundary + FULL_HEADER_SIZE + 3);
        ByteBuffer result = reader.readData();
        assertEquals(ByteBuffer.wrap(messageString.getBytes(Charset.forName("UTF-8"))), result);
    }
    
    private void mockReads(byte[] data, int... splitPos) throws IOException {
        byte[][] splitData = splitByteArray(data, splitPos);
        when(channel.read(any(ByteBuffer.class))).thenAnswer(new ReadAnswer(splitData));
    }
    
}
