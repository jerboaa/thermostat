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
import java.util.ArrayList;
import java.util.List;

abstract class MessageReader {
    
    // States used to track how much of a message (header) we've processed
    protected static enum ReadState { NEW_MESSAGE, MIN_HEADER_READ, FULL_HEADER_READ, ERROR };
    // Number of bytes to read from the header to determine the total header size
    protected static final int MIN_HEADER_SIZE = MessageHeader.getMinimumHeaderSize();
    // If true, dumps header information for each header read/written
    private static final boolean DEBUG_HEADER = false;
    
    // Fixed size buffer used to read the initial part of a message header
    private final ByteBuffer minHeaderBuf;
    // Various message-related limits
    private final MessageLimits limits;
    
    // Current state of data that has been processed by this reader
    protected ReadState state;
    // Message header for the message currently being processed
    protected MessageHeader currentHeader;
    // Buffer used for the remainder of the message header, 
    // after the minimum has been read
    private ByteBuffer headerBuf;
    // Buffer used for the message payload of the message being processed currently
    private ByteBuffer messageBuf;
    // Messages that have finished being processed, but are part of a multi-part message
    // that has not been fully processed yet.
    private List<ByteBuffer> messages;
    
    protected MessageReader(MessageLimits limits) {
        this.state = ReadState.NEW_MESSAGE;
        this.currentHeader = null;
        this.minHeaderBuf = ByteBuffer.allocate(MIN_HEADER_SIZE);
        this.limits = limits;
        this.headerBuf = null;
        this.messageBuf = null;
        this.messages = new ArrayList<ByteBuffer>();
    }
    
    protected void processData(ByteBuffer readBuffer) throws IOException {
        try {
            while (readBuffer.hasRemaining()) {
                switch (state) {
                case NEW_MESSAGE:
                    // Append data to current buffer, if room
                    putMinRemaining(minHeaderBuf, readBuffer);

                    // Is there enough data to read the minimum header size?
                    if (minHeaderBuf.remaining() == 0) {
                        minHeaderBuf.position(0);
                        if (currentHeader != null || headerBuf != null) {
                            throw new IllegalStateException("Header already processed");
                        }
                        // Create Header
                        currentHeader = MessageHeader.fromByteBuffer(minHeaderBuf);
                        minHeaderBuf.clear();

                        // Check header size
                        // Set up reader to process remainder of header
                        int headerSize = currentHeader.getHeaderSize();
                        if (headerSize > limits.getMaxHeaderSize()) {
                            throw new IOException("Message header size larger than maximum of "
                                    + limits.getMaxHeaderSize() + " bytes");
                        }
                        int remainingHeader = headerSize - MIN_HEADER_SIZE;
                        headerBuf = ByteBuffer.allocate(remainingHeader);
                        state = ReadState.MIN_HEADER_READ;
                    }
                    break;
                case MIN_HEADER_READ:
                    if (currentHeader == null || headerBuf == null) {
                        throw new IllegalStateException("No header available");
                    } else if (messageBuf != null) {
                        throw new IllegalStateException("Message already processed");
                    }

                    // Append data to current buffer, if room
                    putMinRemaining(headerBuf, readBuffer);

                    // Have we read the full header?
                    if (headerBuf.remaining() == 0) {
                        headerBuf.position(0);

                        // Finish setting MessageHeader object fields
                        currentHeader.setRemainingFields(headerBuf);

                        // Set up reader to process message payload
                        int messageSize = currentHeader.getMessageSize();
                        if (messageSize > limits.getMaxMessagePartSize()) {
                            throw new IOException("Message part size larger than maximum of "
                                    + limits.getMaxMessagePartSize() + " bytes");
                        }
                        messageBuf = ByteBuffer.allocate(messageSize);
                        headerBuf = null;
                        state = ReadState.FULL_HEADER_READ;

                        // Dump header information if requested
                        if (DEBUG_HEADER) {
                            currentHeader.dumpHeader("[Read] ");
                        }
                    }
                    break;
                case FULL_HEADER_READ:
                    if (currentHeader == null || messageBuf == null) {
                        throw new IllegalStateException("Missing header or message");
                    }
                    // Append data to current buffer, if room
                    putMinRemaining(messageBuf, readBuffer);

                    // Have we read the full message?
                    if (messageBuf.remaining() == 0) {
                        messageBuf.position(0);

                        // Store this message until we received all parts
                        messages.add(messageBuf);
                        // Did we receive all parts of this message?
                        if (!currentHeader.isMoreData()) {
                            // Notify listener
                            ByteBuffer fullMessage = joinMessages();
                            readFullMessage(fullMessage);
                            // Start new list of message parts
                            messages = new ArrayList<ByteBuffer>();
                        }

                        // Reset reader state
                        messageBuf = null;
                        currentHeader = null;
                        state = ReadState.NEW_MESSAGE;
                    }
                    break;
                case ERROR:
                    throw new IOException("Reader state corrupted by previous fatal error"); 
                default:
                    throw new IllegalStateException("Unknown state: " + state.name());
                }
            }
        } catch (IOException e) {
            // Set to error state to stop this reader from processing more data
            this.state = ReadState.ERROR;
            throw e;
        } catch (IllegalStateException e) {
            // Set to error state to stop this reader from processing more data
            this.state = ReadState.ERROR;
            throw e;
        }
    }
    
    protected abstract void readFullMessage(ByteBuffer fullMessage);
    
    private ByteBuffer joinMessages() throws IOException {
        // Single part shortcut
        if (messages.size() == 1) {
            return messages.get(0);
        }
        
        int totalSize = 0;
        int maxMessageSize = limits.getMaxMessageSize();
        for (ByteBuffer buf : messages) {
            totalSize += buf.limit();
            // Check for overflow as well as size limit
            if (totalSize < 0 || totalSize > maxMessageSize) {
                throw new IOException("Total message size is larger than maximum of " + maxMessageSize + " bytes");
            }
        }
        
        ByteBuffer fullMessage = ByteBuffer.allocate(totalSize);
        for (ByteBuffer buf : messages) {
            fullMessage.put(buf);
        }
        fullMessage.flip();
        return fullMessage;
    }

    private void putMinRemaining(ByteBuffer dst, ByteBuffer src) {
        int minRemaining = Math.min(dst.remaining(), src.remaining());
        for (int i = 0; i < minRemaining; i++) {
            dst.put(src.get());
        }
    }
    
    // For testing purposes
    ReadState getState() {
        return state;
    }
    
    // For testing purposes
    MessageHeader getCurrentHeader() {
        return currentHeader;
    }

}
