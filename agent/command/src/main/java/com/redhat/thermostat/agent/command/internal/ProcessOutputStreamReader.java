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

package com.redhat.thermostat.agent.command.internal;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;

import com.redhat.thermostat.common.command.InvalidMessageException;
import com.redhat.thermostat.common.command.Request;

class ProcessOutputStreamReader extends ProcessStreamReader {
    
    // Should only be one listener
    private final ProcessOutputStreamRequestListener requestListener;

    ProcessOutputStreamReader(InputStream is, ProcessOutputStreamRequestListener requestListener,
            ExceptionListener exceptionListener) {
        this(is, requestListener, exceptionListener, new ReaderCreator());
    }
    
    ProcessOutputStreamReader(InputStream is, ProcessOutputStreamRequestListener listener, 
            ExceptionListener exceptionListener, ReaderCreator readerCreator) {
        super(is, exceptionListener, readerCreator);
        this.requestListener = listener;
    }

    @Override
    void handleInput(DataInputStream input) {
        boolean done = false;
        while (!done) {
            try {
                Request req = readRequest(input);
                if (req == null) {
                    done = true;
                } else {
                    // Notify request listener
                    requestListener.requestReceived(req);
                }
            } catch (ClosedByInterruptException e) {
                done = true;
            } catch (IOException e) {
                exceptionListener.notifyException(e);
            }
        }
    }

    /**
     * Read requests using the following protocol:
     * <pre>
     * '&lt;BEGIN REQUEST&gt;'
     * Target Address Host
     * Target Address Port
     * Length of encoded request in bytes
     * Encoded Request (see format: {@link AgentRequestDecoder})
     * '&lt;END REQUEST&gt;'
     * </pre>
     */
    private Request readRequest(DataInputStream input) throws IOException {
        String token;
        try {
            token = input.readUTF();
        } catch (EOFException e) {
            // No more requests to read
            return null;
        }
        
        // First line should be '<BEGIN REQUEST>'
        if (!CommandChannelConstants.BEGIN_REQUEST_TOKEN.equals(token)) {
            throw new CommandChannelIOException("Expected " + CommandChannelConstants.BEGIN_REQUEST_TOKEN + ", got: " + token);
        }
        
        // Next two lines should be the target address
        InetSocketAddress target = parseTargetAddress(input);
        
        // Next line should be length of encoded request
        int length = input.readInt();
        
        // Next 'length' bytes should be the encoded request
        byte[] buf = new byte[length];
        input.read(buf);
        
        // Next line should be '<END REQUEST>'
        token = input.readUTF();
        if (!CommandChannelConstants.END_REQUEST_TOKEN.equals(token)) {
            throw new CommandChannelIOException("Expected " + CommandChannelConstants.END_REQUEST_TOKEN + ", got: " + token);
        }
        
        // Construct the new request
        AgentRequestDecoder decoder = new AgentRequestDecoder();
        Request req;
        try {
            req = decoder.decode(target, buf);
        } catch (InvalidMessageException e) {
            throw new CommandChannelIOException("Error decoding request from command channel", e);
        }
        return req;
    }

    private InetSocketAddress parseTargetAddress(DataInputStream input) throws IOException {
        // Parse address
        InetSocketAddress address;
        String host = input.readUTF();
        int port = input.readInt();
        try {
            address = new InetSocketAddress(host, port);
        } catch (IllegalArgumentException e) {
            throw new CommandChannelIOException("Invalid target address", e);
        }
        return address;
    }

    static interface ProcessOutputStreamRequestListener {
        
        void requestReceived(Request req);
        
    }

}