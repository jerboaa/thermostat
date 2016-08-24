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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.MessageWriter.MessageToWrite;

public class MessageWriterTest {
    
    private static final int MAX_MESSAGE_SIZE = 5;
    
    private MessageWriter writer;

    @Before
    public void setUp() throws Exception {
        MessageLimits limits = mock(MessageLimits.class);
        when(limits.getMaxMessagePartSize()).thenReturn(MAX_MESSAGE_SIZE);
        writer = new TestMessageWriter(limits);
    }

    @Test
    public void testGetNextMessageSingle() {
        String message = "Hello";
        byte[] messageBytes = message.getBytes(Charset.forName("UTF-8"));
        ByteBuffer buf = ByteBuffer.wrap(messageBytes);
        MessageToWrite toWrite = writer.getNextMessage(buf);
        
        MessageHeader header = new MessageHeader();
        header.setMessageSize(5);
        header.setMoreData(false);
        ByteBuffer headerBuf = toWrite.getHeader();
        assertArrayEquals(header.toByteArray(), headerBuf.array());
        
        ByteBuffer messageBuf = toWrite.getMessage();
        assertArrayEquals(messageBytes, messageBuf.array());
    }
    
    @Test
    public void testGetNextMessageMulti() {
        String message = "Hello World";
        byte[] messageBytes = message.getBytes(Charset.forName("UTF-8"));
        ByteBuffer buf = ByteBuffer.wrap(messageBytes);
        
        // First part
        MessageToWrite toWrite = writer.getNextMessage(buf);
        
        MessageHeader header = new MessageHeader();
        header.setMessageSize(5);
        header.setMoreData(true);
        ByteBuffer headerBuf = toWrite.getHeader();
        assertArrayEquals(header.toByteArray(), headerBuf.array());
        
        ByteBuffer messageBuf = toWrite.getMessage();
        ByteBuffer expectedMessage = ByteBuffer.wrap(Arrays.copyOfRange(messageBytes, 0, 5));
        assertEquals(expectedMessage, messageBuf);
        
        // Second part
        toWrite = writer.getNextMessage(buf);
        
        // Header should be the same
        assertArrayEquals(header.toByteArray(), headerBuf.array());
        
        messageBuf = toWrite.getMessage();
        expectedMessage = ByteBuffer.wrap(Arrays.copyOfRange(messageBytes, 5, 10));
        assertEquals(expectedMessage, messageBuf);
        
        // Third part
        toWrite = writer.getNextMessage(buf);
        
        header = new MessageHeader();
        header.setMessageSize(1);
        header.setMoreData(false);
        headerBuf = toWrite.getHeader();
        assertArrayEquals(header.toByteArray(), headerBuf.array());
        
        messageBuf = toWrite.getMessage();
        expectedMessage = ByteBuffer.wrap(Arrays.copyOfRange(messageBytes, 10, 11));
        assertEquals(expectedMessage, messageBuf);
    }
    
    private static class TestMessageWriter extends MessageWriter {
        
        private TestMessageWriter(MessageLimits limits) {
            super(limits);
        }
        
    }

}
