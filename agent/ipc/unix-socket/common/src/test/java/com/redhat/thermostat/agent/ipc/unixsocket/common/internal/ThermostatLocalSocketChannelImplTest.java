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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.agent.ipc.unixsocket.common.internal.ThermostatLocalSocketChannelImpl.UnixSocketChannelHelper;

import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

public class ThermostatLocalSocketChannelImplTest {
    
    private static final int TEST_MAX_MESSAGE_SIZE = 5;
    
    private UnixSocketChannelHelper channelHelper;
    private File socketFile;
    private UnixSocketAddress addr;
    private UnixSocketChannel impl;

    @Before
    public void setUp() throws Exception {
        this.channelHelper = mock(UnixSocketChannelHelper.class);
        socketFile = mock(File.class);
        addr = mock(UnixSocketAddress.class);
        when(channelHelper.createAddress(socketFile)).thenReturn(addr);
        
        impl = mock(UnixSocketChannel.class);
        when(channelHelper.open(addr)).thenReturn(impl);
        when(channelHelper.isOpen(impl)).thenReturn(true);
        
        ThermostatLocalSocketChannelImpl.setChannelHelper(channelHelper);
    }
    
    @After
    public void tearDown() throws Exception {
        ThermostatLocalSocketChannelImpl.setChannelHelper(new UnixSocketChannelHelper());
    }

    @Test
    public void testOpen() throws Exception {
        ThermostatLocalSocketChannelImpl channel = ThermostatLocalSocketChannelImpl.open("test", socketFile);
        verify(channelHelper).createAddress(socketFile);
        verify(channelHelper).open(addr);
        
        assertEquals("test", channel.getName());
    }

    @Test
    public void testReadSingle() throws IOException {
        final byte[] message = "hello".getBytes("UTF-8");
        MessageHeader header = createHeader(message);
        final byte[] headerBytes = header.toByteArray();
        
        ByteBuffer input = ByteBuffer.allocate(headerBytes.length + message.length);
        input.put(headerBytes);
        input.put(message);
        input.flip();
        
        ReadAnswer answer = new ReadAnswer(input);
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        ByteBuffer output = ByteBuffer.allocate(TEST_MAX_MESSAGE_SIZE);
        channel.read(output);
        byte[] result = getBytes(output);
        
        assertArrayEquals(message, result);
        verify(channelHelper, times(3)).read(eq(impl), any(ByteBuffer.class));
    }

    private byte[] getBytes(ByteBuffer output) {
        int length = output.limit() - output.position();
        byte[] result = new byte[length];
        output.get(result);
        return result;
    }

    @Test
    public void testReadEOF() throws IOException {
        ByteBuffer input = ByteBuffer.allocate(0);
        ReadAnswer answer = new ReadAnswer(input);
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        ByteBuffer buf = ByteBuffer.allocate(TEST_MAX_MESSAGE_SIZE);
        int read = channel.read(buf);
        
        assertEquals(-1, read);
        
        byte[] empty = new byte[TEST_MAX_MESSAGE_SIZE];
        assertArrayEquals(empty, buf.array());
        verify(channelHelper, times(1)).read(eq(impl), any(ByteBuffer.class));
    }
    
    @Test(expected=IOException.class)
    public void testReadShortBaseHeader() throws IOException {
        final byte[] message = "hello".getBytes("UTF-8");
        MessageHeader header = createHeader(message);
        final byte[] headerBytes = header.toByteArray();
        
        ByteBuffer input = ByteBuffer.allocate(MessageHeader.getMinimumHeaderSize() - 1);
        input.put(headerBytes, 0, MessageHeader.getMinimumHeaderSize() - 1);
        input.flip();
        
        ReadAnswer answer = new ReadAnswer(input);
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        ByteBuffer buf = ByteBuffer.allocate(message.length);
        channel.read(buf);
    }
    
    @Test(expected=IOException.class)
    public void testReadEOFExtHeader() throws IOException {
        final byte[] message = "hello".getBytes("UTF-8");
        MessageHeader header = createHeader(message);
        final byte[] headerBytes = header.toByteArray();
        
        ByteBuffer input = ByteBuffer.allocate(MessageHeader.getMinimumHeaderSize());
        input.put(headerBytes, 0, MessageHeader.getMinimumHeaderSize());
        input.flip();
        
        ReadAnswer answer = new ReadAnswer(input);
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        ByteBuffer buf = ByteBuffer.allocate(message.length);
        channel.read(buf);
    }
    
    @Test(expected=IOException.class)
    public void testReadShortExtHeader() throws IOException {
        final byte[] message = "hello".getBytes("UTF-8");
        MessageHeader header = createHeader(message);
        final byte[] headerBytes = header.toByteArray();
        
        ByteBuffer input = ByteBuffer.allocate(header.getHeaderSize() - 1);
        input.put(headerBytes, 0, header.getHeaderSize() - 1);
        input.flip();
        
        ReadAnswer answer = new ReadAnswer(input);
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        ByteBuffer buf = ByteBuffer.allocate(message.length);
        channel.read(buf);
    }
    
    @Test(expected=IOException.class)
    public void testReadEOFMessage() throws Exception {
        final byte[] message = "hello".getBytes("UTF-8");
        MessageHeader header = createHeader(message);
        final byte[] headerBytes = header.toByteArray();
        
        ByteBuffer input = ByteBuffer.allocate(header.getHeaderSize());
        input.put(headerBytes, 0, header.getHeaderSize());
        input.flip();
        
        ReadAnswer answer = new ReadAnswer(input);
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        ByteBuffer buf = ByteBuffer.allocate(message.length);
        channel.read(buf);
    }
    
    @Test(expected=IOException.class)
    public void testReadShortMessage() throws Exception {
        final byte[] message = "hello".getBytes("UTF-8");
        MessageHeader header = createHeader(message);
        final byte[] headerBytes = header.toByteArray();
        
        ByteBuffer input = ByteBuffer.allocate(header.getHeaderSize() + message.length - 1);
        input.put(headerBytes);
        input.put(message, 0, message.length - 1);
        input.flip();
        
        ReadAnswer answer = new ReadAnswer(input);
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        ByteBuffer buf = ByteBuffer.allocate(message.length);
        channel.read(buf);
    }
    
    @Test(expected=IOException.class)
    public void testReadBufferTooSmall() throws Exception {
        final byte[] message = "hello".getBytes("UTF-8");
        MessageHeader header = createHeader(message);
        final byte[] headerBytes = header.toByteArray();
        
        ByteBuffer input = ByteBuffer.allocate(headerBytes.length + message.length);
        input.put(headerBytes);
        input.put(message);
        input.flip();
        
        ReadAnswer answer = new ReadAnswer(input);
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        ByteBuffer buf = ByteBuffer.allocate(message.length - 1); // Too small
        channel.read(buf);
    }
    
    @Test
    public void testReadMultipart() throws Exception {
        final byte[] message = "hello me".getBytes("UTF-8");
        final byte[] messagePartOne = Arrays.copyOfRange(message, 0, TEST_MAX_MESSAGE_SIZE);
        final byte[] messagePartTwo = Arrays.copyOfRange(message, TEST_MAX_MESSAGE_SIZE, message.length);
        MessageHeader headerOne = createHeader(messagePartOne, true /* moreData */);
        final byte[] headerOneBytes = headerOne.toByteArray();
        MessageHeader headerTwo = createHeader(messagePartTwo);
        final byte[] headerTwoBytes = headerTwo.toByteArray();
        
        ByteBuffer input = ByteBuffer.allocate(headerOneBytes.length + messagePartOne.length
                + headerTwoBytes.length + messagePartTwo.length);
        input.put(headerOneBytes);
        input.put(messagePartOne);
        input.put(headerTwoBytes);
        input.put(messagePartTwo);
        input.flip();
        
        ReadAnswer answer = new ReadAnswer(input);
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        ByteBuffer output = ByteBuffer.allocate(message.length);
        channel.read(output);
        byte[] result = getBytes(output);
        
        assertArrayEquals(message, result);
        verify(channelHelper, times(6)).read(eq(impl), any(ByteBuffer.class));
    }
    
    @Test(expected=IOException.class)
    public void testReadMultipartEOF() throws Exception {
        final byte[] message = "hello".getBytes("UTF-8");
        MessageHeader header = createHeader(message, true /* moreData */);
        final byte[] headerBytes = header.toByteArray();
        
        ByteBuffer input = ByteBuffer.allocate(headerBytes.length + message.length);
        input.put(headerBytes);
        input.put(message);
        input.flip();
        
        ReadAnswer answer = new ReadAnswer(input);
        when(channelHelper.read(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        ByteBuffer output = ByteBuffer.allocate(TEST_MAX_MESSAGE_SIZE);
        channel.read(output);
    }

    @Test
    public void testWriteSingle() throws Exception {
        final byte[] message = "hello".getBytes("UTF-8");
        MessageHeader header = createHeader(message);
        final byte[] headerBytes = header.toByteArray();
        
        ByteBuffer expected = ByteBuffer.allocate(headerBytes.length + message.length);
        expected.put(headerBytes);
        expected.put(message);
        expected.flip();
        
        WriteAnswer answer = new WriteAnswer(expected.capacity());
        when(channelHelper.write(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        channel.write(ByteBuffer.wrap(message));
        ByteBuffer result = answer.getBuffer();
        
        assertArrayEquals(expected.array(), result.array());
        verify(channelHelper, times(2)).write(eq(impl), any(ByteBuffer.class));
    }
    
    @Test
    public void testWriteMultipart() throws Exception {
        final byte[] message = "hello me".getBytes("UTF-8");
        final byte[] messagePartOne = Arrays.copyOfRange(message, 0, TEST_MAX_MESSAGE_SIZE);
        final byte[] messagePartTwo = Arrays.copyOfRange(message, TEST_MAX_MESSAGE_SIZE, message.length);
        MessageHeader headerOne = createHeader(messagePartOne, true /* moreData */);
        final byte[] headerOneBytes = headerOne.toByteArray();
        MessageHeader headerTwo = createHeader(messagePartTwo);
        final byte[] headerTwoBytes = headerTwo.toByteArray();
        
        ByteBuffer expected = ByteBuffer.allocate(headerOneBytes.length + messagePartOne.length
                + headerTwoBytes.length + messagePartTwo.length);
        expected.put(headerOneBytes);
        expected.put(messagePartOne);
        expected.put(headerTwoBytes);
        expected.put(messagePartTwo);
        expected.flip();
        
        WriteAnswer answer = new WriteAnswer(expected.capacity());
        when(channelHelper.write(eq(impl), any(ByteBuffer.class))).thenAnswer(answer);
        
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        channel.write(ByteBuffer.wrap(message));
        ByteBuffer result = answer.getBuffer();
        
        assertArrayEquals(expected.array(), result.array());
        verify(channelHelper, times(4)).write(eq(impl), any(ByteBuffer.class));
    }

    @Test
    public void testConfigureBlocking() throws Exception {
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        channel.configureBlocking(true);
        verify(channelHelper).configureBlocking(impl, true);
    }

    @Test
    public void testIsOpen() throws Exception {
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        boolean open = channel.isOpen();
        verify(channelHelper).isOpen(impl);
        assertTrue(open);
    }

    @Test
    public void testClose() throws Exception {
        ThermostatLocalSocketChannelImpl channel = createChannelFixedMessageSize();
        channel.close();
        verify(channelHelper).close(impl);
    }

    private MessageHeader createHeader(final byte[] message) {
        return createHeader(message, false);
    }
    
    private MessageHeader createHeader(final byte[] message, boolean moreData) {
        MessageHeader header = new MessageHeader(MessageHeader.getDefaultProtocolVersion(), 
                MessageHeader.getDefaultHeaderSize());
        header.setMessageSize(message.length);
        header.setMoreData(moreData);
        return header;
    }

    private ThermostatLocalSocketChannelImpl createChannelFixedMessageSize() throws IOException {
        return new ThermostatLocalSocketChannelImpl("test", impl, TEST_MAX_MESSAGE_SIZE);
    }

    private static class ReadAnswer implements Answer<Integer> {
        
        private ByteBuffer buf;
        
        private ReadAnswer(ByteBuffer buf) {
            this.buf = buf;
        }

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            int copied = -1;
            // read(UnixSocketChannel, ByteBuffer)
            ByteBuffer dst = (ByteBuffer) invocation.getArguments()[1];
            if (buf.remaining() > 0) {
                ByteBuffer src = buf.slice();
                // Only put dst.remaining() bytes into dst
                int length = dst.remaining();
                // There may not be length bytes to copy
                int numToCopy = Math.min(length, src.limit());
                src.limit(numToCopy);
                dst.put(src);
                copied = numToCopy;
                
                // Advance original buffer position by number of bytes copied
                buf.position(buf.position() + numToCopy);
            }
            return copied;
        }
        
    }
    
    private static class WriteAnswer implements Answer<Integer> {
        
        private ByteBuffer buf;
        
        private WriteAnswer(int bufferSize) {
            this.buf = ByteBuffer.allocate(bufferSize);
        }

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            // write(UnixSocketChannel, ByteBuffer)
            ByteBuffer src = (ByteBuffer) invocation.getArguments()[1];
            int numToCopy = src.remaining();
            buf.put(src);
            return numToCopy;
        }
        
        public ByteBuffer getBuffer() {
            return buf;
        }
        
    }
}
