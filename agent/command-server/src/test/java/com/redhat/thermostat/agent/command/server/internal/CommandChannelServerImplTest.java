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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;

public class CommandChannelServerImplTest {

    private CommandChannelServerContext ctx;
    private ServerBootstrap bootstrap;
    private EventLoopGroup group;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Before
    public void setUp() {
        bootstrap = mock(ServerBootstrap.class);
        when(bootstrap.bind(any(InetSocketAddress.class))).thenReturn(mock(ChannelFuture.class));
        group = mock(EventLoopGroup.class);
        when(bootstrap.group()).thenReturn(group);
        ctx = mock(CommandChannelServerContext.class);
        when(ctx.getBootstrap()).thenReturn((AbstractBootstrap) bootstrap);
    }

    @Test
    public void testStartListening() throws IOException {
        CommandChannelServerImpl server = new CommandChannelServerImpl(ctx);
        server.startListening("127.0.0.1", 123);

        ArgumentCaptor<InetSocketAddress> argument = ArgumentCaptor.forClass(InetSocketAddress.class);
        verify(bootstrap).bind(argument.capture());
        
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 123);
        assertEquals(addr, argument.getValue());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void startListeningFailureThrowsException() {
        CommandChannelServerImpl server = new CommandChannelServerImpl(ctx);

        when(bootstrap.bind(any(InetSocketAddress.class))).thenThrow(IOException.class);
        
        try {
            server.startListening("does-not-resolve.example.com", 123);
            fail("Should have thrown exception");
        } catch (IOException e) {
            // pass
        }
    }

    @Test
    public void testStopListening() {
        CommandChannelServerImpl server = new CommandChannelServerImpl(ctx);
        server.stopListening();

        verify(group).shutdownGracefully();
    }
    
}

