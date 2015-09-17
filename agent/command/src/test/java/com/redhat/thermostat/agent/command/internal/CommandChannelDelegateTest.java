/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.agent.command.internal.CommandChannelDelegate.ProcessCreator;
import com.redhat.thermostat.agent.command.internal.CommandChannelDelegate.ReaderCreator;
import com.redhat.thermostat.agent.command.internal.CommandChannelDelegate.StorageGetter;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.SecureStorage;

public class CommandChannelDelegateTest {
    
    private StorageGetter storageGetter;
    private ProcessCreator processCreator;
    private ReaderCreator readerCreator;
    private ReceiverRegistry receivers;
    private File binPath;
    private CommandChannelDelegate delegate;
    private InputStream stdout;
    private InputStream stderr;
    private OutputStream stdin;
    private Process process;

    @Before
    public void setUp() throws IOException {
        receivers = mock(ReceiverRegistry.class);
        SSLConfiguration sslConf = mock(SSLConfiguration.class);
        binPath = new File("/path/to/thermostat/home/");
        storageGetter = mock(StorageGetter.class);
        processCreator = mock(ProcessCreator.class);
        process = mock(Process.class);
        
        readerCreator = mock(ReaderCreator.class);
        stdout = mock(InputStream.class);
        BufferedReader br = mock(BufferedReader.class);
        when(br.readLine()).thenReturn(CommandChannelConstants.SERVER_STARTED_TOKEN);
        when(readerCreator.createReader(stdout)).thenReturn(br);
        stderr = mock(InputStream.class);
        when(stderr.read(any(byte[].class), anyInt(), anyInt())).thenReturn(-1);
        stdin = mock(OutputStream.class);
        
        when(process.getInputStream()).thenReturn(stdout);
        when(process.getErrorStream()).thenReturn(stderr);
        when(process.getOutputStream()).thenReturn(stdin);
        when(processCreator.startProcess(any(String[].class))).thenReturn(process);
        delegate = new CommandChannelDelegate(receivers, sslConf, binPath, storageGetter, processCreator, readerCreator);
    }

    @Test
    public void testProcessCmdLine() throws IOException {
        delegate.startListening("127.0.0.1", 123);
        
        String[] args = new String[] { 
                "/path/to/thermostat/home/thermostat-command-channel",
                "127.0.0.1",
                "123"
        };
        verify(processCreator).startProcess(eq(args));
    }
    
    private void captureOutput(final StringBuilder builder) throws IOException {
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                byte[] buf = (byte[]) args[0];
                int off = (int) args[1];
                int len = (int) args[2];
                
                builder.append(new String(buf, off, len));
                return null;
            }
        }).when(stdin).write(any(byte[].class), anyInt(), anyInt());
    }
    
    @Test(expected=IOException.class)
    public void testServerFailsToStart() throws IOException {
        BufferedReader br = mock(BufferedReader.class);
        when(readerCreator.createReader(stdout)).thenReturn(br);
        delegate.startListening("127.0.0.1", 123);
    }
    
    @Test
    public void testStopListening() throws IOException {
        delegate.startListening("127.0.0.1", 123);
        delegate.stopListening();
        
        verify(process).destroy();
    }
    
    @Test
    public void testRequestReceived() throws IOException {
        RequestReceiver receiver = mock(RequestReceiver.class);
        Request request = createRequest(receiver);
        
        String result = receiveRequest(request);
        
        verify(receivers).getReceiver("com.example.MyReceiver");
        verify(receiver).receive(request);
        
        final String response = CommandChannelConstants.BEGIN_RESPONSE_TOKEN + "\n"
                + "OK\n"
                + CommandChannelConstants.END_RESPONSE_TOKEN + "\n";
        assertEquals(response, result);
    }

    private String receiveRequest(Request request) throws IOException {
        delegate.startListening("127.0.0.1", 123);
        StringBuilder builder = new StringBuilder();
        captureOutput(builder);
        delegate.requestReceived(request);
        return builder.toString();
    }
    
    @Test
    public void testRequestReceivedNoReceiver() throws IOException {
        Request request = mock(Request.class);
        when(request.getType()).thenReturn(RequestType.RESPONSE_EXPECTED);
        
        String result = receiveRequest(request);
        
        final String response = CommandChannelConstants.BEGIN_RESPONSE_TOKEN + "\n"
                + "ERROR\n"
                + CommandChannelConstants.END_RESPONSE_TOKEN + "\n";
        assertEquals(response, result);
    }
    
    @Test
    public void testRequestReceivedNoType() throws IOException {
        Request request = mock(Request.class);
        
        when(request.getReceiver()).thenReturn("com.example.MyReceiver");
        RequestReceiver receiver = mock(RequestReceiver.class);
        when(receivers.getReceiver("com.example.MyReceiver")).thenReturn(receiver);
        when(receiver.receive(request)).thenReturn(new Response(ResponseType.OK));
        
        String result = receiveRequest(request);
        
        verify(receiver, never()).receive(request);
        
        final String response = CommandChannelConstants.BEGIN_RESPONSE_TOKEN + "\n"
                + "ERROR\n"
                + CommandChannelConstants.END_RESPONSE_TOKEN + "\n";
        assertEquals(response, result);
    }
    
    @Test
    public void testAuthenticateSuccess() throws IOException {
        SecureStorage secStorage = mock(SecureStorage.class);
        when(storageGetter.get()).thenReturn(secStorage);
        
        RequestReceiver receiver = mock(RequestReceiver.class);
        Request request = createRequest(receiver);
        
        // Create tokens
        final String authToken = "TXlBdXRoVG9rZW4=";
        final String clientToken = "TXlDbGllbnRUb2tlbg==";
        when(request.getParameter(Request.AUTH_TOKEN)).thenReturn(authToken);
        when(request.getParameter(Request.CLIENT_TOKEN)).thenReturn(clientToken);
        when(request.getParameter(Request.ACTION)).thenReturn("DoSomething");
        
        mockVerifyToken(secStorage, authToken, clientToken);
        
        String result = receiveRequest(request);
        
        verify(receiver).receive(request);
        final String response = CommandChannelConstants.BEGIN_RESPONSE_TOKEN + "\n"
                + "OK\n"
                + CommandChannelConstants.END_RESPONSE_TOKEN + "\n";
        assertEquals(response, result);
    }
    
    @Test
    public void testAuthenticateFailed() throws IOException {
        SecureStorage secStorage = mock(SecureStorage.class);
        when(storageGetter.get()).thenReturn(secStorage);
        
        RequestReceiver receiver = mock(RequestReceiver.class);
        Request request = createRequest(receiver);
        
        // Create tokens
        final String authToken = "TXlBdXRoVG9rZW4=";
        final String clientToken = "TXlDbGllbnRUb2tlbg==";
        when(request.getParameter(Request.AUTH_TOKEN)).thenReturn(authToken);
        when(request.getParameter(Request.CLIENT_TOKEN)).thenReturn(clientToken);
        when(request.getParameter(Request.ACTION)).thenReturn("DoSomething");
        
        mockVerifyToken(secStorage, "TXlFdmlsVG9rZW4=", clientToken);
        
        String result = receiveRequest(request);
        
        verify(receiver, never()).receive(request);
        final String response = CommandChannelConstants.BEGIN_RESPONSE_TOKEN + "\n"
                + "AUTH_FAILED\n"
                + CommandChannelConstants.END_RESPONSE_TOKEN + "\n";
        assertEquals(response, result);
    }

    @Test
    public void testAuthenticateNPE() throws IOException {
        SecureStorage secStorage = mock(SecureStorage.class);
        when(storageGetter.get()).thenReturn(secStorage);
        
        RequestReceiver receiver = mock(RequestReceiver.class);
        Request request = createRequest(receiver);
        
        // Create tokens
        final String authToken = "TXlBdXRoVG9rZW4=";
        final String clientToken = "TXlDbGllbnRUb2tlbg==";
        when(request.getParameter(Request.AUTH_TOKEN)).thenReturn(authToken);
        when(request.getParameter(Request.CLIENT_TOKEN)).thenReturn(clientToken);
        
        when(secStorage.verifyToken(any(AuthToken.class), any(String.class))).thenThrow(new NullPointerException());
        
        String result = receiveRequest(request);
        
        verify(receiver, never()).receive(request);
        final String response = CommandChannelConstants.BEGIN_RESPONSE_TOKEN + "\n"
                + "AUTH_FAILED\n"
                + CommandChannelConstants.END_RESPONSE_TOKEN + "\n";
        assertEquals(response, result);
    }
    
    private void mockVerifyToken(SecureStorage secStorage,
            final String authToken, final String clientToken) {
        when(secStorage.verifyToken(any(AuthToken.class), eq("DoSomething"))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                AuthToken token = (AuthToken) invocation.getArguments()[0];
                boolean authMatches = Arrays.equals(token.getToken(), Base64.decodeBase64(authToken));
                boolean clientMatches = Arrays.equals(token.getClientToken(), Base64.decodeBase64(clientToken));
                return authMatches && clientMatches;
            }
        });
    }

    private Request createRequest(RequestReceiver receiver) {
        Request request = mock(Request.class);
        when(request.getType()).thenReturn(RequestType.RESPONSE_EXPECTED);
        
        when(request.getReceiver()).thenReturn("com.example.MyReceiver");
        when(receivers.getReceiver("com.example.MyReceiver")).thenReturn(receiver);
        when(receiver.receive(request)).thenReturn(new Response(ResponseType.OK));
        return request;
    }
    
}

