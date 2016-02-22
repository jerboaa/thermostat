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

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.AcceptedLocalSocketChannelImpl;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.ClientHandler;

public class ClientHandlerTest {
    
    private final byte[] MESSAGE = { 'h', 'e', 'l', 'l', 'o' };
    private final byte[] RESPONSE = { 'w', 'o', 'r', 'l', 'd' };
    
    private AcceptedLocalSocketChannelImpl client;
    private ThermostatIPCCallbacks callbacks;
    private SelectionKey key;
    private Selector selector;
    
    @Before
    public void setup() throws Exception {
        client = mock(AcceptedLocalSocketChannelImpl.class);
        
        key = mock(SelectionKey.class);
        selector = mock(Selector.class);
        when(key.selector()).thenReturn(selector);
        when(client.getSelectionKey()).thenReturn(key);
        
        callbacks = mock(ThermostatIPCCallbacks.class);
    }
    
    @Test
    public void testRead() throws Exception {
        mockRead(client, MESSAGE);
        when(callbacks.dataReceived(any(byte[].class))).thenReturn(RESPONSE);
        
        ClientHandler handler = new ClientHandler(client, callbacks);
        handler.call();
        
        // Verify that callbacks are called with correct message and response is written
        verify(client).read(any(ByteBuffer.class));
        verify(callbacks).dataReceived(MESSAGE);
        
        ByteBuffer output = ByteBuffer.wrap(RESPONSE);
        verify(client).write(eq(output));
        
        // Verify selector notified about interest in subsequent reads
        verify(key).interestOps(SelectionKey.OP_READ);
        verify(selector).wakeup();
        
        // Should not close connection
        verify(client, never()).close();
    }

    @Test
    public void testReadEOF() throws Exception {
        when(client.read(any(ByteBuffer.class))).thenReturn(-1);
        
        ClientHandler handler = new ClientHandler(client, callbacks);
        handler.call();
        
        // Should just close channel
        verify(client).read(any(ByteBuffer.class));
        
        verify(callbacks, never()).dataReceived(any(byte[].class));
        verify(client, never()).write(any(ByteBuffer.class));
        verify(key, never()).interestOps(anyInt());
        verify(selector, never()).wakeup();
        
        verify(client).close();
    }
    
    @Test
    public void testReadException() throws Exception {
        when(client.read(any(ByteBuffer.class))).thenThrow(new IOException());
        
        ClientHandler handler = new ClientHandler(client, callbacks);
        
        try {
            handler.call();
            fail("Expected IOException");
        } catch (IOException e) {
            // Should just close channel
            verify(client).read(any(ByteBuffer.class));
            
            verify(callbacks, never()).dataReceived(any(byte[].class));
            verify(client, never()).write(any(ByteBuffer.class));
            verify(key, never()).interestOps(anyInt());
            verify(selector, never()).wakeup();
            
            verify(client).close();
        }
    }
    
    @Test
    public void testReadNoResponse() throws Exception {
        mockRead(client, MESSAGE);
        when(callbacks.dataReceived(any(byte[].class))).thenReturn(null);
        
        ClientHandler handler = new ClientHandler(client, callbacks);
        handler.call();
        
        // Verify that callbacks are called with correct message and no response is written
        verify(client).read(any(ByteBuffer.class));
        verify(callbacks).dataReceived(MESSAGE);
        
        verify(client, never()).write(any(ByteBuffer.class));
        
        // Verify selector notified about interest in subsequent reads
        verify(key).interestOps(SelectionKey.OP_READ);
        verify(selector).wakeup();
        
        // Should not close connection
        verify(client, never()).close();
    }
    
    @Test
    public void testReadOutputTooLarge() throws Exception {
        mockRead(client, MESSAGE);
        
        byte[] bigResponse = new byte[ClientHandler.MAX_BUFFER_SIZE + 1];
        when(callbacks.dataReceived(any(byte[].class))).thenReturn(bigResponse);
        
        ClientHandler handler = new ClientHandler(client, callbacks);
        
        try {
            handler.call();
            fail("Expected IOException");
        } catch (IOException e) {

            // Verify that callbacks are called with correct message and response is not written
            verify(client).read(any(ByteBuffer.class));
            verify(callbacks).dataReceived(MESSAGE);

            verify(client, never()).write(any(ByteBuffer.class));

            // Should just close the connection
            verify(key, never()).interestOps(anyInt());
            verify(selector, never()).wakeup();

            verify(client).close();
        }
    }

    private void mockRead(AcceptedLocalSocketChannelImpl client, final byte[] message) throws IOException {
        // Place message into ByteBuffer when read is called
        when(client.read(any(ByteBuffer.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                ByteBuffer buf = (ByteBuffer) invocation.getArguments()[0];
                buf.put(message);
                buf.flip();
                return message.length;
            }
        });
    }

}
