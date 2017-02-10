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

package com.redhat.thermostat.agent.ipc.winpipes.server.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.AsyncMessageReader;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.AsyncMessageWriter;
import com.redhat.thermostat.agent.ipc.winpipes.common.internal.MessageListener;
import com.redhat.thermostat.common.utils.LoggingUtils;

class ClientHandler implements MessageListener {
    
    private static final Logger logger = LoggingUtils.getLogger(ClientHandler.class);
    private static final boolean LOG_DATA = true;

    // Increments for each instance made. Only for debugging purposes.
    private static final AtomicInteger handlerCount = new AtomicInteger();
    
    private final WritableByteChannel channel;
    private final ExecutorService execService;
    private final ThermostatIPCCallbacks callbacks;
    private final AsyncMessageReader reader;
    private final AsyncMessageWriter writer;
    private final MessageCreator messageCreator;
    private final int handlerNum;
    
    ClientHandler(WritableByteChannel channel, ExecutorService execService, ThermostatIPCCallbacks callbacks) {
        this.channel = channel;
        this.execService = execService;
        this.callbacks = callbacks;
        this.reader = new AsyncMessageReader(this);
        this.writer = new AsyncMessageWriter(channel);
        this.messageCreator = new MessageCreator();
        this.handlerNum = handlerCount.getAndIncrement();
    }
    
    ClientHandler(WritableByteChannel channel, ExecutorService execService, ThermostatIPCCallbacks callbacks,
                  AsyncMessageReader reader, AsyncMessageWriter writer, MessageCreator messageCreator) {
        this.channel = channel;
        this.execService = execService;
        this.callbacks = callbacks;
        this.reader = reader;
        this.writer = writer;
        this.messageCreator = messageCreator;
        this.handlerNum = handlerCount.getAndIncrement();
    }

    /**
     * handle some bytes from the client.  Build up a complete message over multiple calls
     *
     * @return true if complete message is read
     */
    boolean handleRead(ByteBuffer data) throws IOException {
        logger.finest("Got read from client for \"" + channel + "\" [" + handlerNum + "] bytes=" + data.remaining());
        if (LOG_DATA) {
            byte[] xx = new byte[data.remaining()];
            data.get(xx);
            data.position(0);
            final String s = new String(xx, "UTF8");
            logger.finest("message is '" + s + "'");
        }
        return reader.process(data);
    }

    // only called by tests
    boolean handleWrite() throws IOException {
        try {
            logger.finest("handleWrite() Got write for client for \"" + channel + "\" [" + handlerNum + "]");
            // write message to client
            writer.writeData();
        } catch (IOException e) {
            channel.close();
            throw new IOException("handleWrite() Communication error from handler " + handlerNum, e);
        }
        return writer.hasMoreMessages();
    }

    @Override
    public void messageRead(ByteBuffer buf) {

        logger.finest("messageRead() entered");

        // Create new message and notify caller
        final MessageImpl message = messageCreator.createMessage(buf, this);

        // Execute callback in a separate thread to ensure we don't block
        execService.submit(new Runnable() {
            @Override
            public void run() {
                callbacks.messageReceived(message);
            }
        });

        logger.finest("messageRead() exited");
    }

    @Override
    public void writeMessage(ByteBuffer buf) throws IOException {
        // Request write with selector
        logger.finest("writeMessage() enquing message (length=" + buf.remaining() + ") for client \"" + channel + "\" [" + handlerNum + "]");

        // Enqueue this message for writing when selected
        writer.enqueueForWriting(buf);

        while (writer.hasMoreMessages()) {
            try {
                writer.writeData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    static class MessageCreator {
        MessageImpl createMessage(ByteBuffer data, MessageListener listener) {
            return new MessageImpl(data, listener);
        }
    }
}