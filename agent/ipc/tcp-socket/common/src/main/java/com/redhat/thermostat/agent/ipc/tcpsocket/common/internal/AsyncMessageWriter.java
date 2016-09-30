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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class AsyncMessageWriter extends MessageWriter {

    private final ThermostatSocketChannelImpl channel;
    private final Queue<MessageToWrite> messages;
    private final MessageLimits limits;
    
    private boolean headerSent;
    
    public AsyncMessageWriter(ThermostatSocketChannelImpl channel) {
        this(channel, new MessageLimits());
    }
    
    AsyncMessageWriter(ThermostatSocketChannelImpl channel, MessageLimits limits) {
        super(limits);
        this.channel = channel;
        this.limits = limits;
        this.messages = new LinkedList<MessageToWrite>();
        this.headerSent = false;
    }

    public synchronized void writeData() throws IOException {
        if (messages.isEmpty()) {
            throw new IllegalStateException("Write requested, but nothing to write");
        }

        boolean done = writeMessage(messages.peek());
        if (done) {
            // Reset state for next message
            headerSent = false;
            // Remove header and message from the queue
            messages.remove();
        }
    }
    
    private boolean writeMessage(MessageToWrite toWrite) throws IOException {
        boolean done = false;
        ByteBuffer currentHeader = toWrite.getHeader();
        ByteBuffer currentMessage = toWrite.getMessage();
        
        // Write the header if not yet written fully
        if (!headerSent) {
            channel.write(currentHeader);
            // Check if the entire header was written
            headerSent = !currentHeader.hasRemaining();
        }
        
        // Write the message, if we've sent the full header
        if (headerSent) {
            channel.write(currentMessage);
            // Check if the message has been fully written
            done = !currentMessage.hasRemaining();
        }
        return done;
    }
    
    public synchronized boolean hasMoreMessages() {
        return !messages.isEmpty();
    }
    
    public synchronized void enqueueForWriting(ByteBuffer buf) throws IOException {
        if (buf.remaining() > limits.getMaxMessageSize()) {
            throw new IOException("Total message size is larger than maximum of " 
                    + limits.getMaxMessageSize() + " bytes");
        }
        // Split into messages and add headers
        ByteBuffer fullMessage = buf.duplicate();
        while (fullMessage.hasRemaining()) {
            MessageToWrite message = getNextMessage(fullMessage);
            // Add header and message to queue
            messages.add(message);
        }
    }
    
}
