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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.MessageHeader;

public class MessageHeaderTest {
    
    private static final int INT_BYTES = 4;

    @Test
    public void testNewHeaderBase() throws Exception {
        MessageHeader header = new MessageHeader();
        
        assertEquals(MessageHeader.getDefaultProtocolVersion(), header.getProtocolVersion());
        assertEquals(MessageHeader.getDefaultHeaderSize(), header.getHeaderSize());
        
        // Not part of minimal header
        assertEquals(-1, header.getMessageSize());
        assertFalse(header.isMoreData());
    }
    
    @Test
    public void testNewHeaderV1() throws Exception {
        MessageHeader header = new MessageHeader();
        header.setMessageSize(50);
        header.setMoreData(true);
        
        assertEquals(MessageHeader.getDefaultProtocolVersion(), header.getProtocolVersion());
        assertEquals(MessageHeader.getDefaultHeaderSize(), header.getHeaderSize());
        
        // Protocol version 1 fields
        assertEquals(50, header.getMessageSize());
        assertTrue(header.isMoreData());
    }
    
    @Test
    public void testToBytesBase() throws Exception {
        final byte[] expected = getBytes(MessageHeader.getDefaultProtocolVersion(), 
                MessageHeader.getDefaultHeaderSize(), -1, false);
        
        MessageHeader header = new MessageHeader();
        assertArrayEquals(expected, header.toByteArray());
    }
    
    @Test
    public void testToBytesV1() throws Exception {
        final byte[] expected = getBytes(1, MessageHeader.getDefaultHeaderSize(), 8000, true);
        
        MessageHeader header = new MessageHeader();
        // Protocol version 1 fields
        header.setMessageSize(8000);
        header.setMoreData(true);
        
        assertArrayEquals(expected, header.toByteArray());
    }
    
    @Test
    public void testFromByteBuffer() throws Exception {
        ByteBuffer buf = createByteBuffer(5, 20, 8000, true);
        
        MessageHeader header = MessageHeader.fromByteBuffer(buf);
        assertEquals(5, header.getProtocolVersion());
        assertEquals(20, header.getHeaderSize());
    }
    
    @Test(expected=IOException.class)
    public void testFromByteBufferShortMagic() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(3);
        buf.put(new byte[] { 'T', 'H', 'E' });
        buf.flip();
        
        MessageHeader.fromByteBuffer(buf);
    }
    
    @Test(expected=IOException.class)
    public void testFromByteBufferShortProtocolVersion() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(7);
        buf.put(new byte[] { 'T', 'H', 'E', 'R' });
        buf.put(new byte[] { 0x0, 0x0, 0x1 });
        buf.flip();
        
        MessageHeader.fromByteBuffer(buf);
    }
    
    @Test(expected=IOException.class)
    public void testFromByteBufferShortHeaderSize() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(11);
        buf.put(new byte[] { 'T', 'H', 'E', 'R' });
        buf.put(intToBytes(5));
        buf.put(new byte[] { 0x0, 0x5, 0x2 });
        buf.flip();
        
        MessageHeader.fromByteBuffer(buf);
    }
    
    @Test(expected=IOException.class)
    public void testFromByteBufferNegativeProtocolVersion() throws Exception {
        ByteBuffer buf = createByteBuffer(-4, 25, 20, true);
        MessageHeader.fromByteBuffer(buf);
    }
    
    @Test(expected=IOException.class)
    public void testFromByteBufferZeroProtocolVersion() throws Exception {
        ByteBuffer buf = createByteBuffer(0, 25, 20, true);
        MessageHeader.fromByteBuffer(buf);
    }
    
    @Test(expected=IOException.class)
    public void testFromByteBufferNegativeHeaderSize() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(MessageHeader.getDefaultHeaderSize());
        fillByteBuffer(buf, 5, -22, 20, true);
        MessageHeader.fromByteBuffer(buf);
    }
    
    @Test(expected=IOException.class)
    public void testFromByteBufferZeroHeaderSize() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(MessageHeader.getDefaultHeaderSize());
        fillByteBuffer(buf, 5, 0, 20, true);
        MessageHeader.fromByteBuffer(buf);
    }
    
    @Test(expected=IOException.class)
    public void testSetRemainingFieldsV0() throws Exception {
        ByteBuffer buf = createByteBuffer(0, MessageHeader.getDefaultHeaderSize(), 20, true);
        MessageHeader header = MessageHeader.fromByteBuffer(buf);
        header.setRemainingFields(buf);
        
        // Expect no change from default
        assertEquals(-1, header.getMessageSize());
        assertEquals(false, header.isMoreData());
    }
    
    @Test
    public void testSetRemainingFieldsV1() throws Exception {
        ByteBuffer buf = createByteBuffer(1, MessageHeader.getDefaultHeaderSize(), 20, true);
        MessageHeader header = MessageHeader.fromByteBuffer(buf);
        header.setRemainingFields(buf);
        
        // Expect remaining fields copied from buf
        assertEquals(20, header.getMessageSize());
        assertTrue(header.isMoreData());
    }
    
    @Test(expected=IOException.class)
    public void testSetRemainingFieldsV1ShortMessageSize() throws Exception {
        ByteBuffer buf = createByteBuffer(1, MessageHeader.getDefaultHeaderSize(), 20, true);
        // Subtract moreData bit, and one bit of message size
        buf.limit(buf.limit() - 2);
        
        MessageHeader header = MessageHeader.fromByteBuffer(buf);
        header.setRemainingFields(buf);
    }
    
    @Test(expected=IOException.class)
    public void testSetRemainingFieldsV1NegativeMessageSize() throws Exception {
        ByteBuffer buf = createByteBuffer(1, MessageHeader.getDefaultHeaderSize(), -20, true);
        MessageHeader header = MessageHeader.fromByteBuffer(buf);
        header.setRemainingFields(buf);
    }
    
    @Test(expected=IOException.class)
    public void testSetRemainingFieldsV1ZeroMessageSize() throws Exception {
        ByteBuffer buf = createByteBuffer(1, MessageHeader.getDefaultHeaderSize(), 0, true);
        MessageHeader header = MessageHeader.fromByteBuffer(buf);
        header.setRemainingFields(buf);
    }
    
    @Test(expected=IOException.class)
    public void testSetRemainingFieldsV1ShortMoreData() throws Exception {
        ByteBuffer buf = createByteBuffer(1, MessageHeader.getDefaultHeaderSize(), 20, true);
        // Subtract moreData bit
        buf.limit(buf.limit() - 1);
        
        MessageHeader header = MessageHeader.fromByteBuffer(buf);
        header.setRemainingFields(buf);
    }
    
    private byte[] getBytes(int protoVer, int headerSize, int messageSize, boolean moreData) {
        ByteBuffer buf = createByteBuffer(protoVer, headerSize, messageSize, moreData);
        return buf.array();
    }
    
    private ByteBuffer createByteBuffer(int protoVer, int headerSize, int messageSize, boolean moreData) {
        ByteBuffer buf = ByteBuffer.allocate(headerSize);
        fillByteBuffer(buf, protoVer, headerSize, messageSize, moreData);
        return buf;
    }
    
    private void fillByteBuffer(ByteBuffer buf, int protoVer, int headerSize, int messageSize, boolean moreData) {
        // Magic number
        buf.put(new byte[] { 'T', 'H', 'E', 'R' });
        buf.put(intToBytes(protoVer));
        buf.put(intToBytes(headerSize));
        buf.put(intToBytes(messageSize));
        buf.put((byte) (moreData ? 1 : 0));
        buf.flip();
    }
    
    private byte[] intToBytes(int num) {
        ByteBuffer buf = ByteBuffer.allocate(INT_BYTES);
        buf.putInt(num);
        return buf.array();
    }
    
}
