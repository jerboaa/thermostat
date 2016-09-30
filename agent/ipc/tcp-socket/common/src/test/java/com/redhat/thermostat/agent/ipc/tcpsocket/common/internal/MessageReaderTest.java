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

import static com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.ChannelTestUtils.createHeader;
import static com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.ChannelTestUtils.joinByteArrays;
import static com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.ChannelTestUtils.splitByteArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.MessageReader.ReadState;

public class MessageReaderTest {
    
    private TestMessageReader reader;
    private MessageLimits limits;

    @Before
    public void setUp() throws Exception {
        limits = mock(MessageLimits.class);
        when(limits.getMaxHeaderSize()).thenReturn(Integer.MAX_VALUE);
        when(limits.getMaxMessagePartSize()).thenReturn(Integer.MAX_VALUE);
        when(limits.getMaxMessageSize()).thenReturn(Integer.MAX_VALUE);
        reader = new TestMessageReader(limits);
    }
    
    @Test
    public void testOneRead() throws Exception {
        byte[] data = createMinHeader();
        byte[][] splitData = splitByteArray(data, 3);
        reader.readData(splitData[0]);
        
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        assertNull(reader.getCurrentHeader());
    }
    
    @Test
    public void testTwoRead() throws Exception {
        byte[] data = createMinHeader();
        byte[][] splitData = splitByteArray(data, 3, 7);
        reader.readData(splitData[0]);
        reader.readData(splitData[1]);
        
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        assertNull(reader.getCurrentHeader());
    }
    
    @Test
    public void testThreeReadGotMinHeader() throws Exception {
        byte[] data = createMinHeader();
        byte[][] splitData = splitByteArray(data, 3, 7, MessageReader.MIN_HEADER_SIZE);
        reader.readData(splitData[0]);
        reader.readData(splitData[1]);
        reader.readData(splitData[2]);
        
        assertEquals(ReadState.MIN_HEADER_READ, reader.getState());
        assertNotNull(reader.getCurrentHeader());
    }
    
    @Test
    public void testFourReadGotMinHeader() throws Exception {
        byte[] data = createMinHeader();
        byte[][] splitData = splitByteArray(data, 3, 7, MessageReader.MIN_HEADER_SIZE, MessageReader.MIN_HEADER_SIZE + 2);
        reader.readData(splitData[0]);
        reader.readData(splitData[1]);
        reader.readData(splitData[2]);
        reader.readData(splitData[3]);
        
        assertEquals(ReadState.MIN_HEADER_READ, reader.getState());
        assertNotNull(reader.getCurrentHeader());
        
    }
    
    @Test
    public void testFiveReadGotFullHeader() throws Exception {
        final String message = "hello";
        int messageSize = message.getBytes(Charset.forName("UTF-8")).length;
        byte[] data = createHeader(messageSize, false);
        byte[][] splitData = splitByteArray(data, 3, 7, MessageReader.MIN_HEADER_SIZE - 1, MessageReader.MIN_HEADER_SIZE + 2);
        for (int i = 0; i < splitData.length; i++) {
            reader.readData(splitData[i]);
        }
        
        assertEquals(ReadState.FULL_HEADER_READ, reader.getState());
        MessageHeader header = reader.getCurrentHeader();
        assertNotNull(header);
        assertEquals(messageSize, header.getMessageSize());
    }
    
    @Test
    public void testSixReadGotFullMessage() throws Exception {
        final String message = "hello";
        byte[] messageBytes = message.getBytes(Charset.forName("UTF-8"));
        int messageSize = messageBytes.length;
        byte[] headerBytes = createHeader(messageSize, false);
        byte[] data = joinByteArrays(headerBytes, messageBytes);
        byte[][] splitData = splitByteArray(data, 3, 7, MessageReader.MIN_HEADER_SIZE - 1, 
                MessageReader.MIN_HEADER_SIZE + 2, headerBytes.length + 1);
        for (int i = 0; i < splitData.length; i++) {
            reader.readData(splitData[i]);
        }
        
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        List<ByteBuffer> fullMessages = reader.getFullMessages();
        assertEquals(1, fullMessages.size());
        assertEquals(ByteBuffer.wrap(messageBytes), fullMessages.get(0));
    }
    
    @Test
    public void testTwoReadGotTwoMessages() throws Exception {
        final String message1 = "hello";
        final String message2 = "world!";
        byte[] message1Bytes = message1.getBytes(Charset.forName("UTF-8"));
        byte[] message2Bytes = message2.getBytes(Charset.forName("UTF-8"));
        int message1Size = message1Bytes.length;
        int message2Size = message2Bytes.length;
        byte[] header1Bytes = createHeader(message1Size, true);
        byte[] header2Bytes = createHeader(message2Size, false);
        byte[] data = joinByteArrays(header1Bytes, message1Bytes, header2Bytes, message2Bytes);
        byte[][] splitData = splitByteArray(data, header1Bytes.length + message1Size);
        
        reader.readData(splitData[0]);
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        reader.readData(splitData[1]);
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        
        byte[] expected = message1.concat(message2).getBytes(Charset.forName("UTF-8"));
        List<ByteBuffer> fullMessages = reader.getFullMessages();
        assertEquals(1, fullMessages.size());
        assertEquals(ByteBuffer.wrap(expected), fullMessages.get(0));
    }
    
    @Test
    public void testThreeReadGotTwoMessages() throws Exception {
        final String message1 = "hello";
        final String message2 = "world!";
        byte[] message1Bytes = message1.getBytes(Charset.forName("UTF-8"));
        byte[] message2Bytes = message2.getBytes(Charset.forName("UTF-8"));
        int message1Size = message1Bytes.length;
        int message2Size = message2Bytes.length;
        byte[] header1Bytes = createHeader(message1Size, true);
        byte[] header2Bytes = createHeader(message2Size, false);
        byte[] data = joinByteArrays(header1Bytes, message1Bytes, header2Bytes, message2Bytes);
        int messageBoundary = header1Bytes.length + message1Size;
        byte[][] splitData = splitByteArray(data, messageBoundary - 2, messageBoundary + 2);
        
        reader.readData(splitData[0]);
        assertEquals(ReadState.FULL_HEADER_READ, reader.getState());
        reader.readData(splitData[1]);
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        reader.readData(splitData[2]);
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        
        byte[] expected = message1.concat(message2).getBytes(Charset.forName("UTF-8"));
        List<ByteBuffer> fullMessages = reader.getFullMessages();
        assertEquals(1, fullMessages.size());
        assertEquals(ByteBuffer.wrap(expected), fullMessages.get(0));
    }
    
    @Test
    public void testThreeReadGotTwoMessagesNotContinued() throws Exception {
        final String message1 = "hello";
        final String message2 = "world!";
        byte[] message1Bytes = message1.getBytes(Charset.forName("UTF-8"));
        byte[] message2Bytes = message2.getBytes(Charset.forName("UTF-8"));
        int message1Size = message1Bytes.length;
        int message2Size = message2Bytes.length;
        byte[] header1Bytes = createHeader(message1Size, false);
        byte[] header2Bytes = createHeader(message2Size, false);
        byte[] data = joinByteArrays(header1Bytes, message1Bytes, header2Bytes, message2Bytes);
        int messageBoundary = header1Bytes.length + message1Size;
        byte[][] splitData = splitByteArray(data, messageBoundary - 2, messageBoundary + 2);
        
        reader.readData(splitData[0]);
        assertEquals(ReadState.FULL_HEADER_READ, reader.getState());
        reader.readData(splitData[1]);
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        reader.readData(splitData[2]);
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        
        List<ByteBuffer> fullMessages = reader.getFullMessages();
        assertEquals(2, fullMessages.size());
        assertEquals(ByteBuffer.wrap(message1Bytes), fullMessages.get(0));
        assertEquals(ByteBuffer.wrap(message2Bytes), fullMessages.get(1));
    }
    
    @Test
    public void testFourReadGotThreeMessagesOneContinued() throws Exception {
        final String message1 = "hello";
        final String message2 = "world!";
        final String message3 = "test";
        byte[] message1Bytes = message1.getBytes(Charset.forName("UTF-8"));
        byte[] message2Bytes = message2.getBytes(Charset.forName("UTF-8"));
        byte[] message3Bytes = message3.getBytes(Charset.forName("UTF-8"));
        int message1Size = message1Bytes.length;
        int message2Size = message2Bytes.length;
        int message3Size = message3Bytes.length;
        byte[] header1Bytes = createHeader(message1Size, true);
        byte[] header2Bytes = createHeader(message2Size, false);
        byte[] header3Bytes = createHeader(message3Size, false);
        byte[] data = joinByteArrays(header1Bytes, message1Bytes, header2Bytes, message2Bytes, header3Bytes, message3Bytes);
        int messageBoundary = header1Bytes.length + message1Size;
        int messageBoundary2 = messageBoundary + header2Bytes.length + message2Size;
        byte[][] splitData = splitByteArray(data, messageBoundary - 2, messageBoundary + 2, messageBoundary2);
        
        reader.readData(splitData[0]);
        assertEquals(ReadState.FULL_HEADER_READ, reader.getState());
        reader.readData(splitData[1]);
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        reader.readData(splitData[2]);
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        reader.readData(splitData[3]);
        assertEquals(ReadState.NEW_MESSAGE, reader.getState());
        
        byte[] expected = message1.concat(message2).getBytes(Charset.forName("UTF-8"));
        List<ByteBuffer> fullMessages = reader.getFullMessages();
        assertEquals(2, fullMessages.size());
        assertEquals(ByteBuffer.wrap(expected), fullMessages.get(0));
        assertEquals(ByteBuffer.wrap(message3Bytes), fullMessages.get(1));
    }
    
    @Test
    public void testHeaderTooLarge() throws Exception {
        when(limits.getMaxHeaderSize()).thenReturn(0);
        byte[] data = createMinHeader();
        try {
            reader.readData(data);
            fail("Expected IOException");
        } catch (IOException ignored) {
            assertEquals(ReadState.ERROR, reader.getState());
        }
    }
    
    @Test
    public void testMessagePartTooLarge() throws Exception {
        when(limits.getMaxMessagePartSize()).thenReturn(0);
        
        final String message = "hello";
        byte[] messageBytes = message.getBytes(Charset.forName("UTF-8"));
        int messageSize = messageBytes.length;
        byte[] headerBytes = createHeader(messageSize, false);
        byte[] data = joinByteArrays(headerBytes, messageBytes);
        try {
            reader.readData(data);
            fail("Expected IOException");
        } catch (IOException ignored) {
            assertEquals(ReadState.ERROR, reader.getState());
        }
    }
    
    @Test
    public void testMessageTooLarge() throws Exception {
        when(limits.getMaxMessageSize()).thenReturn(0);
        
        final String message1 = "hello";
        final String message2 = "world!";
        byte[] message1Bytes = message1.getBytes(Charset.forName("UTF-8"));
        byte[] message2Bytes = message2.getBytes(Charset.forName("UTF-8"));
        int message1Size = message1Bytes.length;
        int message2Size = message2Bytes.length;
        byte[] header1Bytes = createHeader(message1Size, true);
        byte[] header2Bytes = createHeader(message2Size, false);
        byte[] data = joinByteArrays(header1Bytes, message1Bytes, header2Bytes, message2Bytes);
        try {
            reader.readData(data);
            fail("Expected IOException");
        } catch (IOException ignored) {
            assertEquals(ReadState.ERROR, reader.getState());
            assertTrue(reader.getFullMessages().isEmpty());
        }
    }

    private byte[] createMinHeader() {
        MessageHeader header = new MessageHeader();
        return header.toByteArray();
    }
    
    static class TestMessageReader extends MessageReader {
        
        private static final int BUFFER_SIZE = 1024;
        private final ByteBuffer readBuffer;
        private final List<ByteBuffer> fullMessages;
        
        TestMessageReader(MessageLimits limits) {
            super(limits);
            this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
            this.fullMessages = new ArrayList<ByteBuffer>();
        }

        @Override
        protected void readFullMessage(ByteBuffer fullMessage) {
            fullMessages.add(fullMessage);
        }
        
        void readData(byte[] buf) throws IOException {
            readBuffer.clear();
            readBuffer.put(buf);
            readBuffer.flip();
            processData(readBuffer);
        }
        
        List<ByteBuffer> getFullMessages() {
            return fullMessages;
        }
        
    }
}
