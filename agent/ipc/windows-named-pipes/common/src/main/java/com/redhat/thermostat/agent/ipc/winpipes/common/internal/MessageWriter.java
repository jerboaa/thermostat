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

import java.nio.ByteBuffer;

abstract class MessageWriter {
    
    // If true, dumps header information for each header read/written
    private static final boolean DEBUG_HEADER = false;
    
    // Various message-related limits
    private final MessageLimits limits;
    
    protected MessageWriter(MessageLimits limits) {
        this.limits = limits;
    }
    
    protected MessageToWrite getNextMessage(ByteBuffer fullMessage) {
        ByteBuffer message = ByteBuffer.allocate(limits.getMaxMessagePartSize());
        putMinRemaining(message, fullMessage);
        message.flip();
        
        // Create a message header for this part
        int messageSize = message.limit();
        boolean moreData = fullMessage.hasRemaining();
        MessageHeader header = new MessageHeader();
        header.setMessageSize(messageSize);
        header.setMoreData(moreData);
        
        // Dump header information if requested
        if (DEBUG_HEADER) {
            header.dumpHeader("[Write] ");
        }
        
        ByteBuffer headerBuf = ByteBuffer.wrap(header.toByteArray());
        return new MessageToWrite(headerBuf, message);
    }
    
    private void putMinRemaining(ByteBuffer dst, ByteBuffer src) {
        int minRemaining = Math.min(dst.remaining(), src.remaining());
        for (int i = 0; i < minRemaining; i++) {
            dst.put(src.get());
        }
    }
    
    // Container class to hold a single header and message
    protected static class MessageToWrite {
        private final ByteBuffer header;
        private final ByteBuffer message;
        
        public MessageToWrite(ByteBuffer header, ByteBuffer message) {
            this.header = header;
            this.message = message;
        }
        
        public ByteBuffer getHeader() {
            return header;
        }
        
        public ByteBuffer getMessage() {
            return message;
        }
    }

}
