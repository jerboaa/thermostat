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

package com.redhat.thermostat.agent.ipc.unixsocket.server.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.common.utils.LoggingUtils;

class ClientHandler implements Callable<Void> {
    
    private static final Logger logger = LoggingUtils.getLogger(ClientHandler.class);
    // Maximum message size we can read from the client
    static final int MAX_BUFFER_SIZE = 8092;
    // Increments for each instance made. Only for debugging purposes.
    private static final AtomicInteger handlerCount = new AtomicInteger();
    
    private final AcceptedLocalSocketChannelImpl client;
    private final ThermostatIPCCallbacks callbacks;
    private final int handlerNum;
    
    ClientHandler(AcceptedLocalSocketChannelImpl client, ThermostatIPCCallbacks callbacks) {
        this.client = client;
        this.callbacks = callbacks;
        this.handlerNum = handlerCount.getAndIncrement();
    }

    @Override
    public Void call() throws IOException {
        handleClient();
        logger.finest("Client [" + handlerNum + "] done");
        return null;
    }
    
    private void handleClient() throws IOException {
        try {
            logger.fine("Got read from client for \"" + client.getName() + "\"");
            // Read message from client
            ByteBuffer buf = ByteBuffer.allocate(MAX_BUFFER_SIZE);
            int read;
            read = client.read(buf);
            if (read < 0) {
                // Received EOF
                logger.fine("Closing client for \"" + client.getName() + "\"");
                client.close();
            } else {
                // Set limit to mark end of data
                buf.limit(read);
                byte[] input = new byte[buf.remaining()];
                buf.get(input);
                
                // Call supplied callback
                byte[] output = callbacks.dataReceived(input);
                if (output != null) {
                    // Ensure output received is smaller than MAX_BUFFER_SIZE
                    if (output.length > MAX_BUFFER_SIZE) {
                        throw new IOException("Output must be at most " + MAX_BUFFER_SIZE + " bytes");
                    }

                    // Write message to client
                    buf = ByteBuffer.wrap(output);
                    client.write(buf);
                    logger.fine("Wrote reply to client on \"" + client.getName() + "\"");
                }
                
                // Read finished, reset interest set to accept subsequent reads for this client
                SelectionKey key = client.getSelectionKey();
                key.interestOps(SelectionKey.OP_READ);
                // Wakeup selector since we've changed this key's interest set from another thread
                key.selector().wakeup();
            }
        } catch (IOException e) {
            client.close();
            throw new IOException("Communication error from handler " + handlerNum, e);
        }
            
    }
    
}