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

package com.redhat.thermostat.agent.command.server.internal;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.command.server.internal.CommandChannelServerMain.ServerCreator;
import com.redhat.thermostat.agent.command.server.internal.CommandChannelServerMain.ShutdownHookHandler;
import com.redhat.thermostat.agent.command.server.internal.CommandChannelServerMain.Sleeper;
import com.redhat.thermostat.agent.ipc.client.ClientIPCService;
import com.redhat.thermostat.agent.ipc.client.IPCMessageChannel;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class CommandChannelServerMainTest {
    
    private CommandChannelServerImpl server;
    private ClientIPCService ipcService;
    private IPCMessageChannel agentChannel;
    private SSLConfigurationParser parser;
    private ShutdownHookHandler shutdownHandler;
    private Sleeper sleeper;

    @Before
    public void setUpOnce() throws IOException {
        System.setProperty(CommandChannelServerMain.CONFIG_FILE_PROP, "/path/to/ipc/config");
        ipcService = mock(ClientIPCService.class);
        agentChannel = mock(IPCMessageChannel.class);
        when(ipcService.connectToServer(CommandChannelServerMain.IPC_SERVER_NAME)).thenReturn(agentChannel);
        
        SSLConfiguration config = mock(SSLConfiguration.class);
        parser = mock(SSLConfigurationParser.class);
        when(parser.parseSSLConfiguration(agentChannel)).thenReturn(config);
        
        server = mock(CommandChannelServerImpl.class);
        ServerCreator creator = mock(ServerCreator.class);
        when(creator.createServer(config, agentChannel)).thenReturn(server);
        
        shutdownHandler = mock(ShutdownHookHandler.class);
        sleeper = mock(Sleeper.class);
        
        CommandChannelServerMain.setIPCService(ipcService);
        CommandChannelServerMain.setSSLConfigurationParser(parser);
        CommandChannelServerMain.setServerCreator(creator);
        CommandChannelServerMain.setShutdownHookHandler(shutdownHandler);
        CommandChannelServerMain.setSleeper(sleeper);
    }
    
    @After
    public void tearDownOnce() {
        System.clearProperty(CommandChannelServerMain.CONFIG_FILE_PROP);
        CommandChannelServerMain.setIPCService(null);
        CommandChannelServerMain.setSSLConfigurationParser(new SSLConfigurationParser());
        CommandChannelServerMain.setServerCreator(new ServerCreator());
        CommandChannelServerMain.setShutdownHookHandler(new ShutdownHookHandler());
        CommandChannelServerMain.setSleeper(new Sleeper());
    }

    @Test
    public void testNotEnoughArgs() throws IOException {
        try {
            CommandChannelServerMain.main(new String[] { "hello" });
            fail("Expected IOException");
        } catch (IOException e) {
            verify(server, never()).startListening(any(String.class), anyInt());
            verify(sleeper, never()).sleepWait();
        }
    }
    
    @Test
    public void testBadPort() throws IOException {
        try {
            CommandChannelServerMain.main(new String[] { "hello", "world" });
            fail("Expected IOException");
        } catch (IOException e) {
            verify(server, never()).startListening(any(String.class), anyInt());
            verify(sleeper, never()).sleepWait();
        }
    }
    
    @Test
    public void testSSLConfigParseFailed() throws IOException {
        when(parser.parseSSLConfiguration(agentChannel)).thenThrow(new IOException("TEST"));
        try {
            CommandChannelServerMain.main(new String[] { "hello", "123" });
            fail("Expected IOException");
        } catch (IOException e) {
            verify(server, never()).startListening(any(String.class), anyInt());
            verify(sleeper, never()).sleepWait();
        }
    }
    
    @Test
    public void testStartListeningFailed() throws IOException {
        doThrow(new IOException("TEST")).when(server).startListening(any(String.class), anyInt());
        try {
            CommandChannelServerMain.main(new String[] { "hello", "123" });
            fail("Expected IOException");
        } catch (IOException e) {
            verify(shutdownHandler, never()).addShutdownHook(any(Thread.class));
            verify(sleeper, never()).sleepWait();
            verify(server).stopListening();
        }
    }
    
    @Test
    public void testStartedMessageException() throws IOException {
        doThrow(new IOException("TEST")).when(agentChannel).writeMessage(any(ByteBuffer.class));
        try {
            CommandChannelServerMain.main(new String[] { "hello", "123" });
            fail("Expected IOException");
        } catch (IOException e) {
            verify(shutdownHandler, never()).addShutdownHook(any(Thread.class));
            verify(sleeper, never()).sleepWait();
        }
    }
    
    @Test
    public void testReadyMessageException() throws IOException {
        doNothing().doThrow(new IOException("TEST")).when(agentChannel).writeMessage(any(ByteBuffer.class));
        try {
            CommandChannelServerMain.main(new String[] { "hello", "123" });
            fail("Expected IOException");
        } catch (IOException e) {
            verify(shutdownHandler, never()).addShutdownHook(any(Thread.class));
            verify(sleeper, never()).sleepWait();
        }
    }
    
    @Test
    public void testSuccess() throws IOException {
        CommandChannelServerMain.main(new String[] { "hello", "123" });
        
        verify(server).startListening("hello", 123);
        verify(sleeper).sleepWait();
    }
    
    @Test
    public void testShutdownHook() throws IOException, InterruptedException {
        CommandChannelServerMain.main(new String[] { "hello", "123" });
        
        ArgumentCaptor<Thread> hookCaptor = ArgumentCaptor.forClass(Thread.class);
        verify(shutdownHandler).addShutdownHook(hookCaptor.capture());
        Thread hook = hookCaptor.getValue();
        
        hook.start();
        hook.join();
        
        verify(server).stopListening();
    }

}
