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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

class MessageHeader {
    
    private static final int INTEGER_BYTES = (Integer.SIZE/Byte.SIZE);
    private static final int BYTE_BYTES = 1;
    private static final byte[] MAGIC = { 'T', 'H', 'E', 'R' };
    // Size upto and including header size value
    // Should remain constant across protocol versions
    private static final int MINIMUM_HEADER_SIZE = MAGIC.length + INTEGER_BYTES * 2;
    // Size of (magic + protocolVersion + headerSize + messageSize + moreData)
    private static final int DEFAULT_HEADER_SIZE = MAGIC.length + INTEGER_BYTES * 3 + BYTE_BYTES;
    // Protocol version to use for writing new MessageHeaders
    private static final int DEFAULT_PROTOCOL_VERSION = 1;
    
    private final int protocolVersion;
    private final int headerSize;
    private int messageSize;
    // If true, then the next message is a continuation of the data in this one
    private boolean moreData;
    
    MessageHeader() {
        this(DEFAULT_PROTOCOL_VERSION, DEFAULT_HEADER_SIZE);
    }
    
    // Only used for reading MessageHeaders
    MessageHeader(int protocolVersion, int headerSize) {
        this.protocolVersion = protocolVersion;
        this.headerSize = headerSize;
        this.messageSize = -1;
        this.moreData = false;
    }
    
    byte[] toByteArray() {
        ByteBuffer buf = ByteBuffer.allocate(getHeaderSize());
        buf.put(MAGIC);
        buf.putInt(protocolVersion);
        buf.putInt(headerSize);
        buf.putInt(messageSize);
        putBoolean(buf, moreData);
        return buf.array();
    }
    
    static MessageHeader fromByteBuffer(ByteBuffer buf) throws IOException {
        checkMagic(buf);
        int protoVersion = getPositiveIntOrThrow(buf, "Protocol version");
        int headerSize = getPositiveIntOrThrow(buf, "Header size");
        return new MessageHeader(protoVersion, headerSize);
    }
    
    void setRemainingFields(ByteBuffer buf) throws IOException {
        int effectiveProtocolVersion = getEffectiveProtocolVersion();
        if (effectiveProtocolVersion <= 0) {
            throw new IOException("Invalid protocol version: " + effectiveProtocolVersion);
        }
        if (effectiveProtocolVersion >= 1) {
            this.messageSize = getPositiveIntOrThrow(buf, "Message size");
            this.moreData = getBoolean(buf);
        }
    }

    private int getEffectiveProtocolVersion() {
        // If this header's protocol version is greater than we can handle
        // just use the maximum we know about
        return Math.min(protocolVersion, DEFAULT_PROTOCOL_VERSION);
    }

    private static void checkMagic(ByteBuffer buf) throws IOException {
        checkBytesRemaining(buf, MAGIC.length);
        
        byte[] bufMagic = new byte[MAGIC.length];
        buf.get(bufMagic);
        if (!Arrays.equals(bufMagic, MAGIC)) {
            throw new IOException("MessageHeader is invalid");
        }
    }

    private static void checkBytesRemaining(ByteBuffer buf, int size) throws IOException {
        if (buf.remaining() < size) {
            throw new IOException("MessageHeader too short");
        }
    }
    
    private static int getPositiveIntOrThrow(ByteBuffer buf, String errorMsgPrefix) throws IOException {
        checkBytesRemaining(buf, INTEGER_BYTES);
        int result = buf.getInt();
        if (result <= 0) {
            throw new IOException(errorMsgPrefix + " must be greater than zero");
        }
        return result;
    }
    
    static int getDefaultProtocolVersion() {
        return DEFAULT_PROTOCOL_VERSION;
    }
    
    int getProtocolVersion() {
        return protocolVersion;
    }

    static int getMinimumHeaderSize() {
        return MINIMUM_HEADER_SIZE;
    }
    
    static int getDefaultHeaderSize() {
        return DEFAULT_HEADER_SIZE;
    }

    int getHeaderSize() {
        return headerSize;
    }
    
    int getMessageSize() {
        return messageSize;
    }

    void setMessageSize(int messageSize) {
        this.messageSize = messageSize;
    }

    boolean isMoreData() {
        return moreData;
    }
    
    void setMoreData(boolean moreData) {
        this.moreData = moreData;
    }
    
    void dumpHeader(String prefix) {
        System.out.println(prefix + "Protocol Version: " + protocolVersion);
        System.out.println(prefix + "Header Length: " + headerSize);
        System.out.println(prefix + "Message Length: " + messageSize);
        System.out.println(prefix + "More Data: " + moreData);
    }
    
    private boolean getBoolean(ByteBuffer buf) throws IOException {
        checkBytesRemaining(buf, 1);
        byte b = buf.get();
        return (b != 0);
    }
    
    private void putBoolean(ByteBuffer buf, boolean value) {
        byte b = (byte) (value ? 1 : 0);
        buf.put(b);
    }

}
