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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.agent.command.RequestReceiver;
import com.redhat.thermostat.agent.command.internal.CommandChannelDelegate.FileSystemUtils;
import com.redhat.thermostat.agent.command.internal.CommandChannelDelegate.ProcessCreator;
import com.redhat.thermostat.agent.command.internal.CommandChannelDelegate.StorageGetter;
import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.server.IPCMessage;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.shared.config.OS;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.core.AuthToken;
import com.redhat.thermostat.storage.core.SecureStorage;

public class CommandChannelDelegateTest {
    
    private static final String IPC_SERVER_NAME = "command-channel";
    private static final byte[] ENCODED_SSL_CONFIG = { 'S', 'S', 'L' };
    private static final byte[] ENCODED_REQUEST = { 'R', 'E', 'Q' };
    private static final byte[] ENCODED_RESPONSE_OK = { 'O', 'K' };
    private static final byte[] ENCODED_RESPONSE_AUTH_FAILED = { 'A', 'U', 'T', 'H' };
    private static final byte[] ENCODED_RESPONSE_ERROR = { 'E', 'R', 'R' };
    
    private StorageGetter storageGetter;
    private ProcessCreator processCreator;
    private ReceiverRegistry receivers;
    private File binPath;
    private CommandChannelDelegate delegate;
    private Process process;
    private AgentIPCService ipcService;
    private File ipcConfig;
    private AgentRequestDecoder requestDecoder;
    private AgentResponseEncoder responseEncoder;
    private SSLConfigurationEncoder sslConfEncoder;
    private CountDownLatch latch;
    private SSLConfiguration sslConf;
    private IPCMessage startedMessage;
    private FileSystemUtils fsUtils;
    private ProcessUserInfoBuilder userInfoBuilder;

    @Before
    public void setUp() throws Exception {
        receivers = mock(ReceiverRegistry.class);
        sslConf = mock(SSLConfiguration.class);
        binPath = new File("/path/to/thermostat/home/");
        storageGetter = mock(StorageGetter.class);
        processCreator = mock(ProcessCreator.class);
        process = mock(Process.class);
        ipcService = mock(AgentIPCService.class);
        ipcConfig = new File("/path/to/ipc/config");
        
        requestDecoder = mock(AgentRequestDecoder.class);
        responseEncoder = mock(AgentResponseEncoder.class);
        // Return different encoded response for different response types
        when(responseEncoder.encodeResponse(any(Response.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Response resp = (Response) invocation.getArguments()[0];
                ResponseType type = resp.getType();
                switch (type) {
                case OK:
                    return ENCODED_RESPONSE_OK;
                case AUTH_FAILED:
                    return ENCODED_RESPONSE_AUTH_FAILED;
                case ERROR:
                    return ENCODED_RESPONSE_ERROR;
                default:
                    throw new IOException("Unexpected ResponseType: " + type.name());
                }
            }
        });
        sslConfEncoder = mock(SSLConfigurationEncoder.class);
        when(sslConfEncoder.encodeAsJson(sslConf)).thenReturn(ENCODED_SSL_CONFIG);
        
        when(processCreator.startProcess(any(ProcessBuilder.class))).thenReturn(process);
        when(ipcService.getConfigurationFile()).thenReturn(ipcConfig);
        
        latch = mock(CountDownLatch.class);
        fsUtils = mock(FileSystemUtils.class);
        userInfoBuilder = mock(ProcessUserInfoBuilder.class);
        delegate = new CommandChannelDelegate(receivers, sslConf, binPath, ipcService, 
                latch, sslConfEncoder, requestDecoder, responseEncoder, storageGetter, userInfoBuilder, 
                fsUtils, processCreator);
        
        startedMessage = mock(IPCMessage.class);
        when(startedMessage.get()).thenReturn(ByteBuffer.wrap(CommandChannelConstants.SERVER_STARTED_TOKEN));
        final IPCMessage readyMessage = mock(IPCMessage.class);
        when(readyMessage.get()).thenReturn(ByteBuffer.wrap(CommandChannelConstants.SERVER_READY_TOKEN));
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Invoke callbacks with started message
                delegate.messageReceived(startedMessage);
                verify(startedMessage).reply(eq(ByteBuffer.wrap(ENCODED_SSL_CONFIG)));
                // Invoke callbacks with ready message
                delegate.messageReceived(readyMessage);
                verify(readyMessage, never()).reply(any(ByteBuffer.class));
                return null;
            }
        }).when(latch).await();
    }

    @Test
    public void testServerStarted() throws Exception {
        delegate.startListening("127.0.0.1", 123);
        
        verify(ipcService).createServer(IPC_SERVER_NAME, delegate);
        verify(processCreator).startProcess(any(ProcessBuilder.class));
    }
    
    @Test
    public void testServerStartedPrivUser() throws Exception {
        when(userInfoBuilder.isPrivilegedUser()).thenReturn(true);
        Path scriptPath = mock(Path.class);
        when(fsUtils.getPath(binPath.getAbsolutePath(), "thermostat-command-channel")).thenReturn(scriptPath);
        UserPrincipal principal = mock(UserPrincipal.class);
        when(fsUtils.getOwner(scriptPath)).thenReturn(principal);
        delegate.startListening("127.0.0.1", 123);
        
        verify(ipcService).createServer(IPC_SERVER_NAME, delegate, principal);
        verify(processCreator).startProcess(any(ProcessBuilder.class));
    }
    
    @Test
    public void testServerFailsToStart() throws Exception {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Invoke callbacks with wrong started message
                IPCMessage message = mock(IPCMessage.class);
                ByteBuffer badData = ByteBuffer.wrap("not the server started message".getBytes(Charset.forName("UTF-8")));
                when(message.get()).thenReturn(badData);
                delegate.messageReceived(message);
                verify(message, never()).reply(any(ByteBuffer.class));
                return null;
            }
        }).when(latch).await();
        
        try {
            delegate.startListening("127.0.0.1", 123);
            fail("Expected IOException");
        } catch (IOException e) {
            verify(ipcService).createServer(IPC_SERVER_NAME, delegate);
            verify(processCreator).startProcess(any(ProcessBuilder.class));
        }
    }
    
    @Test
    public void testServerFailsToStartParseFail() throws Exception {
        when(sslConfEncoder.encodeAsJson(sslConf)).thenThrow(new IOException("TEST"));
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Invoke callbacks with started message
                delegate.messageReceived(startedMessage);
                return null;
            }
        }).when(latch).await();
        
        try {
            delegate.startListening("127.0.0.1", 123);
            fail("Expected IOException");
        } catch (IOException e) {
            verify(ipcService).createServer(IPC_SERVER_NAME, delegate);
            verify(processCreator).startProcess(any(ProcessBuilder.class));
        }
    }
    
    @Test
    public void testServerFailsToBecomeReady() throws Exception {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // Invoke callbacks with started message
                delegate.messageReceived(startedMessage);
                // Invoke callbacks with wrong ready message
                IPCMessage message = mock(IPCMessage.class);
                ByteBuffer badData = ByteBuffer.wrap("not the server started message".getBytes(Charset.forName("UTF-8")));
                when(message.get()).thenReturn(badData);
                delegate.messageReceived(message);
                verify(message, never()).reply(any(ByteBuffer.class));
                return null;
            }
        }).when(latch).await();
        
        try {
            delegate.startListening("127.0.0.1", 123);
            fail("Expected IOException");
        } catch (IOException e) {
            verify(ipcService).createServer(IPC_SERVER_NAME, delegate);
            verify(processCreator).startProcess(any(ProcessBuilder.class));
        }
    }

    @Test
    public void testProcessCmdLine() throws IOException {
        delegate.startListening("127.0.0.1", 123);
        
        String[] linuxArgs = new String[] {
                "/path/to/thermostat/home/thermostat-command-channel",
                "127.0.0.1",
                "123",
                "/path/to/ipc/config"
        };

        // in Windows we need to ensure the drive letter appears - by calling getAbsolutePath()
        String[] winArgs = new String[] {
                "cmd",
                "/c",
                new File("/path/to/thermostat/home/thermostat-command-channel.cmd").getAbsolutePath(),
                "127.0.0.1",
                "123",
                new File("/path/to/ipc/config").getAbsolutePath()
        };

        final String[] expectedArgs = OS.IS_UNIX ? linuxArgs : winArgs;
        
        ArgumentCaptor<ProcessBuilder> builderCaptor = ArgumentCaptor.forClass(ProcessBuilder.class);
        verify(processCreator).startProcess(builderCaptor.capture());
        ProcessBuilder builder = builderCaptor.getValue();
        final List<String> actualArgs = builder.command();

        assertEquals(Arrays.asList(expectedArgs), actualArgs);
        assertEquals(Redirect.INHERIT, builder.redirectError());
        assertEquals(Redirect.INHERIT, builder.redirectOutput());
        assertEquals(Redirect.INHERIT, builder.redirectInput());
    }
    
    @Test
    public void testStopListening() throws IOException {
        delegate.startListening("127.0.0.1", 123);
        when(ipcService.serverExists(IPC_SERVER_NAME)).thenReturn(true);
        delegate.stopListening();
        
        verify(process).destroy();
        verify(ipcService).destroyServer(IPC_SERVER_NAME);
    }
    
    @Test
    public void testStopListeningNotExist() throws IOException {
        delegate.startListening("127.0.0.1", 123);
        delegate.stopListening();
        
        verify(process).destroy();
        verify(ipcService, never()).destroyServer(IPC_SERVER_NAME);
    }
    
    @Test
    public void testRequestReceived() throws IOException {
        RequestReceiver receiver = mock(RequestReceiver.class);
        Request request = createRequest(receiver);
        
        byte[] result = receiveRequestAndReturnResponse(request);
        verify(receivers).getReceiver("com.example.MyReceiver");
        verify(receiver).receive(request);
        
        assertArrayEquals(ENCODED_RESPONSE_OK, result);
    }

    private byte[] receiveRequestAndReturnResponse(Request request) throws IOException {
        delegate.startListening("127.0.0.1", 123);
        
        // Receive encoded request
        when(requestDecoder.decodeRequest(ENCODED_REQUEST)).thenReturn(request);
        IPCMessage message = mock(IPCMessage.class);
        ByteBuffer data = ByteBuffer.wrap(ENCODED_REQUEST);
        when(message.get()).thenReturn(data);
        delegate.messageReceived(message);
        
        return getReply(message);
    }

    private byte[] getReply(IPCMessage message) throws IOException {
        ArgumentCaptor<ByteBuffer> replyCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(message).reply(replyCaptor.capture());
        ByteBuffer reply = replyCaptor.getValue();
        return reply.array();
    }
    
    @Test
    public void testRequestReceivedParseFail() throws IOException {
        delegate.startListening("127.0.0.1", 123);
        
        RequestReceiver receiver = mock(RequestReceiver.class);
        Request request = createRequest(receiver);
        
        // Should catch exception and return error response
        when(requestDecoder.decodeRequest(ENCODED_REQUEST)).thenThrow(new IOException("TEST"));
        
        IPCMessage message = mock(IPCMessage.class);
        ByteBuffer data = ByteBuffer.wrap(ENCODED_REQUEST);
        when(message.get()).thenReturn(data);
        delegate.messageReceived(message);
        
        byte[] result = getReply(message);
        verify(receivers, never()).getReceiver("com.example.MyReceiver");
        verify(receiver, never()).receive(request);
        
        assertArrayEquals(ENCODED_RESPONSE_ERROR, result);
    }
    
    @Test
    public void testRequestReceivedNoReceiver() throws IOException {
        Request request = mock(Request.class);
        when(request.getType()).thenReturn(RequestType.RESPONSE_EXPECTED);
        
        byte[] result = receiveRequestAndReturnResponse(request);
        assertArrayEquals(ENCODED_RESPONSE_ERROR, result);
    }
    
    @Test
    public void testRequestReceivedNoType() throws IOException {
        Request request = mock(Request.class);
        
        when(request.getReceiver()).thenReturn("com.example.MyReceiver");
        RequestReceiver receiver = mock(RequestReceiver.class);
        when(receivers.getReceiver("com.example.MyReceiver")).thenReturn(receiver);
        when(receiver.receive(request)).thenReturn(new Response(ResponseType.OK));
        
        byte[] result = receiveRequestAndReturnResponse(request);
        verify(receiver, never()).receive(request);
        assertArrayEquals(ENCODED_RESPONSE_ERROR, result);
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
        
        byte[] result = receiveRequestAndReturnResponse(request);
        verify(receiver).receive(request);
        assertArrayEquals(ENCODED_RESPONSE_OK, result);
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
        
        byte[] result = receiveRequestAndReturnResponse(request);
        verify(receiver, never()).receive(request);
        assertArrayEquals(ENCODED_RESPONSE_AUTH_FAILED, result);
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
        
        byte[] result = receiveRequestAndReturnResponse(request);
        verify(receiver, never()).receive(request);
        assertArrayEquals(ENCODED_RESPONSE_AUTH_FAILED, result);
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

