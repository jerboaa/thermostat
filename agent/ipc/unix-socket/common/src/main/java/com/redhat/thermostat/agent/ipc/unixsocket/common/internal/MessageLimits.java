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

class MessageLimits {
    
    private static final int DEFAULT_MAX_MESSAGE_PART_SIZE = 0x800; // 2 KiB
    private static final int DEFAULT_MAX_MESSAGE_SIZE = 0x20000; // 128 KiB
    private static final int DEFAULT_MAX_HEADER_SIZE = 0x80; // 128 B
    private static final int DEFAULT_BUFFER_SIZE = DEFAULT_MAX_MESSAGE_PART_SIZE;
    
    /* 
     * Values below should adhere to the following:
     * 0 <= MinHdr <= MaxHdr <= MaxMsgPart <= Buffer <= MaxMsg
     */
    // Maximum value for a single part of a message
    private final int maxMessagePartSize;
    // Maximum value for the combined payload of a multi-part message
    private final int maxMessageSize;
    // Maximum value for a message header
    private final int maxHeaderSize;
    // Size used for read/write buffers
    private final int bufferSize;
    
    MessageLimits() {
        this.maxMessagePartSize = DEFAULT_MAX_MESSAGE_PART_SIZE;
        this.maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;
        this.maxHeaderSize = DEFAULT_MAX_HEADER_SIZE;
        this.bufferSize = DEFAULT_BUFFER_SIZE;
    }
    
    int getMaxMessagePartSize() {
        return maxMessagePartSize;
    }

    int getMaxMessageSize() {
        return maxMessageSize;
    }
    
    int getMaxHeaderSize() {
        return maxHeaderSize;
    }
    
    int getBufferSize() {
        return bufferSize;
    }

}
