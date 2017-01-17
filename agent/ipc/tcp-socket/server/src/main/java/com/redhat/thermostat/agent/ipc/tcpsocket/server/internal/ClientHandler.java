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

package com.redhat.thermostat.agent.ipc.tcpsocket.server.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.AsyncMessageReader;
import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.AsyncMessageWriter;
import com.redhat.thermostat.agent.ipc.tcpsocket.common.internal.MessageListener;
import com.redhat.thermostat.common.utils.LoggingUtils;

class ClientHandler implements MessageListener {
    
    private static final Logger logger = LoggingUtils.getLogger(ClientHandler.class);
    
    // Increments for each instance made. Only for debugging purposes.
    private static final AtomicInteger handlerCount = new AtomicInteger();
    
    private final AcceptedSocketChannelImpl client;
    private final ExecutorService execService;
    private final ThermostatIPCCallbacks callbacks;
    private final AsyncMessageReader reader;
    private final AsyncMessageWriter writer;
    private final MessageCreator messageCreator;
    private final int handlerNum;
    
    ClientHandler(AcceptedSocketChannelImpl client, ExecutorService execService, ThermostatIPCCallbacks callbacks) {
        this.client = client;
        this.execService = execService;
        this.callbacks = callbacks;
        this.reader = new AsyncMessageReader(client, this);
        this.writer = new AsyncMessageWriter(client);
        this.messageCreator = new MessageCreator();
        this.handlerNum = handlerCount.getAndIncrement();
    }
    
    ClientHandler(AcceptedSocketChannelImpl client, ExecutorService execService, ThermostatIPCCallbacks callbacks, 
            AsyncMessageReader reader, AsyncMessageWriter writer, MessageCreator messageCreator) {
        this.client = client;
        this.callbacks = callbacks;
        this.execService = execService;
        this.reader = reader;
        this.writer = writer;
        this.messageCreator = messageCreator;
        this.handlerNum = handlerCount.getAndIncrement();
    }
    
    void handleRead() throws IOException {
        try {
            logger.fine("Got read from client for \"" + client.getName() + "\" [" + handlerNum + "]");
            // Read message from client
            reader.readData();
        } catch (IOException e) {
            client.close();
            throw new IOException("Communication error from handler " + handlerNum, e);
        }
    }
    
    void handleWrite() throws IOException {
        try {
            logger.fine("Got write for client for \"" + client.getName() + "\" [" + handlerNum + "]");
            // Read message from client
            writer.writeData();
            
            // If no more messages, remove write from interestOps
            if (!writer.hasMoreMessages()) {
                SelectionKey key = client.getSelectionKey();
                int ops = key.interestOps();
                key.interestOps(ops & ~SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
            client.close();
            throw new IOException("Communication error from handler " + handlerNum, e);
        }
    }

    @Override
    public void messageRead(ByteBuffer buf) {
        // Create new message and notify caller
        final MessageImpl message = messageCreator.createMessage(buf, this);
        // Execute callback in a separate thread to ensure we don't block
        execService.submit(new Runnable() {
            @Override
            public void run() {
                callbacks.messageReceived(message);
            }
        });
    }

    @Override
    public void writeMessage(ByteBuffer buf) throws IOException {
        // Request write with selector
        SelectionKey key = client.getSelectionKey();
        int ops = key.interestOps();
        key.interestOps(ops | SelectionKey.OP_WRITE);
        
        // Enqueue this message for writing when selected
        writer.enqueueForWriting(buf);
        
        // Wakeup selector since we've changed this key's interest set from another thread
        key.selector().wakeup();
    }
    
    static class MessageCreator {
        MessageImpl createMessage(ByteBuffer data, MessageListener listener) {
            return new MessageImpl(data, listener);
        }
    }
}