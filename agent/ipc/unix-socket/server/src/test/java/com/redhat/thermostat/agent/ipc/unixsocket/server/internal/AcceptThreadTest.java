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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.AcceptThread;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.AcceptedLocalSocketChannelImpl;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.ClientHandler;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.ThermostatLocalServerSocketChannelImpl;
import com.redhat.thermostat.agent.ipc.unixsocket.server.internal.AcceptThread.ClientHandlerCreator;

public class AcceptThreadTest {
    
    private Selector selector;
    private ExecutorService execService;
    private ClientHandlerCreator handlerCreator;
    private ThermostatLocalServerSocketChannelImpl serverSock;
    private AcceptedLocalSocketChannelImpl clientSock;
    private ThermostatIPCCallbacks callbacks;
    private ClientHandler handler;
    private SelectionKey clientKey;
    private SelectionKey acceptKey;
    private AcceptThread thread;

    @Before
    public void setUp() throws IOException {
        selector = mock(Selector.class);
        
        // Mock sockets
        serverSock = mock(ThermostatLocalServerSocketChannelImpl.class);
        clientSock = mock(AcceptedLocalSocketChannelImpl.class);
        when(serverSock.accept()).thenReturn(clientSock);    
        callbacks = mock(ThermostatIPCCallbacks.class);
        when(serverSock.getCallbacks()).thenReturn(callbacks);
        
        // Mock selection keys
        clientKey = mock(SelectionKey.class);
        when(clientKey.readyOps()).thenReturn(SelectionKey.OP_READ);
        when(clientKey.isValid()).thenReturn(true);
        when(clientSock.getSelectionKey()).thenReturn(clientKey);
        acceptKey = mock(SelectionKey.class);
        acceptKey.attach(serverSock);
        when(acceptKey.readyOps()).thenReturn(SelectionKey.OP_ACCEPT);
        
        execService = mock(ExecutorService.class);
        handlerCreator = mock(ClientHandlerCreator.class);
        handler = mock(ClientHandler.class);
        clientKey.attach(handler);
        when(handlerCreator.createHandler(clientSock, execService, callbacks)).thenReturn(handler);
        thread = new AcceptThread(selector, execService, handlerCreator);
    }
    
    @Test
    public void testSelectOneAccept() throws IOException {
        mockSelectionKeys(acceptKey);
        selectAndShutdown(thread, 1);
        thread.run();
        
        verify(selector).select();
        verify(serverSock).accept();
        verify(handlerCreator).createHandler(clientSock, execService, callbacks);
        assertEquals(handler, clientKey.attachment());
        
        verify(handler, never()).handleRead();
        verify(handler, never()).handleWrite();
    }
    
    private void selectAndShutdown(AcceptThread thread, int returnValue) throws IOException {
        selectAndShutdown(thread, 1, new int[] { returnValue });
    }

    private void selectAndShutdown(AcceptThread thread, int numIterations, int[] returnValues) throws IOException {
        when(selector.select()).thenAnswer(new SelectAnswer(thread, numIterations, returnValues));
    }
    
    @Test
    public void testSelectOneRead() throws IOException {
        mockSelectionKeys(clientKey);
        selectAndShutdown(thread, 1);
        thread.run();
        
        verify(selector).select();
        verify(handler).handleRead();
        
        verify(serverSock, never()).accept();
    }
    
    @Test
    public void testSelectOneAcceptAndRead() throws IOException {
        mockSelectionKeys(acceptKey, clientKey);
        selectAndShutdown(thread, 2, new int[] { 1, 1 });
        thread.run();
        
        verify(selector, times(2)).select();
        verify(serverSock).accept();
        verify(handlerCreator).createHandler(clientSock, execService, callbacks);
        assertEquals(handler, clientKey.attachment());
        
        verify(handler).handleRead();
    }
    
    @Test
    public void testSelectTwoAcceptAndRead() throws IOException {
        mockSelectionKeys(acceptKey, clientKey);
        selectAndShutdown(thread, 2);
        thread.run();
        
        verify(selector).select();
        verify(serverSock).accept();
        verify(handlerCreator).createHandler(clientSock, execService, callbacks);
        assertEquals(handler, clientKey.attachment());
        
        verify(handler).handleRead();
    }
    
    @Test
    public void testSelectOneAcceptAndReadWithZeroReturn() throws IOException {
        mockSelectionKeys(acceptKey, clientKey);
        selectAndShutdown(thread, 3, new int[] { 1, 0, 1 });
        thread.run();
        
        verify(selector, times(3)).select();
        verify(serverSock).accept();
        verify(handlerCreator).createHandler(clientSock, execService, callbacks);
        assertEquals(handler, clientKey.attachment());
        
        verify(handler).handleRead();
    }
    
    @Test
    public void testSelectOneWrite() throws IOException {
        when(clientKey.readyOps()).thenReturn(SelectionKey.OP_WRITE);
        mockSelectionKeys(clientKey);
        selectAndShutdown(thread, 1);
        thread.run();
        
        verify(selector).select();
        verify(handler).handleWrite();
        
        verify(serverSock, never()).accept();
        verify(handler, never()).handleRead();
    }
    
    @Test
    public void testSelectOneAcceptAndWrite() throws IOException {
        when(clientKey.readyOps()).thenReturn(SelectionKey.OP_WRITE);
        mockSelectionKeys(acceptKey, clientKey);
        selectAndShutdown(thread, 2, new int[] { 1, 1 });
        thread.run();
        
        verify(selector, times(2)).select();
        verify(serverSock).accept();
        verify(handlerCreator).createHandler(clientSock, execService, callbacks);
        assertEquals(handler, clientKey.attachment());
        
        verify(handler).handleWrite();
    }
    
    @Test
    public void testSelectReadAndWrite() throws IOException {
        when(clientKey.readyOps()).thenReturn(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        mockSelectionKeys(clientKey);
        selectAndShutdown(thread, 1);
        thread.run();
        
        verify(selector).select();
        verify(handler).handleRead();
        verify(handler).handleWrite();
        verify(serverSock, never()).accept();
    }
    
    @Test
    public void testSelectInvalidWrite() throws IOException {
        when(clientKey.isValid()).thenReturn(false);
        when(clientKey.readyOps()).thenReturn(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        mockSelectionKeys(clientKey);
        selectAndShutdown(thread, 1);
        thread.run();
        
        verify(selector).select();
        verify(handler).handleRead();
        verify(handler, never()).handleWrite();
        verify(serverSock, never()).accept();
    }
    
    @Test
    public void testSelectError() throws IOException {
        mockSelectionKeys(acceptKey);
        // Use numIterations == 2 to bypass normal shutdown
        selectAndShutdown(thread, 2, new int[] { -1 });
        
        thread.run();
        
        verify(selector).select();
        assertTrue(thread.isShutdown());
    }

    private void mockSelectionKeys(SelectionKey... key) {
        Set<SelectionKey> keys = new HashSet<>();
        for (SelectionKey k : key) {
            keys.add(k);
        }
        when(selector.selectedKeys()).thenReturn(keys);
    }
    
    private static class SelectAnswer implements Answer<Integer> {
        
        private final AcceptThread thread;
        private final int numIterations;
        private final int[] returnValues;
        
        private int count;
        
        private SelectAnswer(AcceptThread thread, int numIterations, int[] returnValues) {
            this.thread = thread;
            this.numIterations = numIterations;
            this.returnValues = returnValues;
            this.count = 0;
        }
        
        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            if (count + 1 >= numIterations) {
                // Call shutdown to end select loop after this iteration
                thread.shutdown();
            }
            int retVal = returnValues[count];
            count++;
            return retVal;
        }
    }
}
