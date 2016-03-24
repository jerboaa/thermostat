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

package com.redhat.thermostat.agent.command.server.internal;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.channels.ByteChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.agent.command.server.internal.CommandChannelServerContext.ServerChannelInitializer;
import com.redhat.thermostat.agent.command.server.internal.CommandChannelServerContext.ServerChannelPipelineInitializerCreator;
import com.redhat.thermostat.common.ssl.SSLContextFactory;
import com.redhat.thermostat.shared.config.SSLConfiguration;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SSLContextFactory.class,
        SSLEngine.class, SSLContext.class })
public class CommandChannelServerContextTest {

    private CommandChannelServerContext ctx;
    private SSLConfiguration mockSSLConf;
    private TestServerChannelPipelineInitializerCreator creator;

    @Before
    public void setUp() {
        creator = new TestServerChannelPipelineInitializerCreator();
        mockSSLConf = mock(SSLConfiguration.class);
        when(mockSSLConf.enableForCmdChannel()).thenReturn(false);
        ByteChannel agentChannel = mock(ByteChannel.class);
        ctx = new CommandChannelServerContext(mockSSLConf, agentChannel, creator);
    }

    @Test
    public void testBootstrap() throws Exception {
        AbstractBootstrap<?, ?> bootstrap = ctx.getBootstrap();
        assertNotNull(bootstrap);
        SocketChannel channel = mock(SocketChannel.class);
        ChannelPipeline mockPipeline = mock(ChannelPipeline.class);
        when(channel.pipeline()).thenReturn(mockPipeline);
        creator.initializer.initChannel(channel);
        InOrder inOrder = Mockito.inOrder(mockPipeline);
        inOrder.verify(mockPipeline).addLast(eq("decoder"), isA(CommandChannelRequestDecoder.class));
        inOrder.verify(mockPipeline).addLast(eq("encoder"), isA(ResponseEncoder.class));
        inOrder.verify(mockPipeline).addLast(eq("handler"), isA(ServerHandler.class));
    }
    
    @Test
    public void testBootstrapSSL() throws Exception {
        when(mockSSLConf.enableForCmdChannel()).thenReturn(true);
        PowerMockito.mockStatic(SSLContextFactory.class);
        // SSL classes need to be mocked with PowerMockito
        SSLContext context = PowerMockito.mock(SSLContext.class);
        when(SSLContextFactory.getServerContext(isA(SSLConfiguration.class))).thenReturn(context);
        SSLEngine engine = PowerMockito.mock(SSLEngine.class);
        when(context.createSSLEngine()).thenReturn(engine);
        when(engine.getSession()).thenReturn(mock(SSLSession.class));
        
        AbstractBootstrap<?, ?> bootstrap = ctx.getBootstrap();
        assertNotNull(bootstrap);
        SocketChannel channel = mock(SocketChannel.class);
        ChannelPipeline mockPipeline = mock(ChannelPipeline.class);
        when(channel.pipeline()).thenReturn(mockPipeline);
        creator.initializer.initChannel(channel);
        InOrder inOrder = Mockito.inOrder(mockPipeline);
        inOrder.verify(mockPipeline).addLast(eq("ssl"), isA(SslHandler.class));
        inOrder.verify(mockPipeline).addLast(eq("decoder"), isA(CommandChannelRequestDecoder.class));
        inOrder.verify(mockPipeline).addLast(eq("encoder"), isA(ResponseEncoder.class));
        inOrder.verify(mockPipeline).addLast(eq("handler"), isA(ServerHandler.class));
        Mockito.verify(engine).setUseClientMode(false);
    }
    
    static class TestServerChannelPipelineInitializerCreator extends ServerChannelPipelineInitializerCreator {
        
        TestServerPipelineInitializer initializer;
        
        @Override
        ServerChannelInitializer createInitializer(SSLConfiguration sslConf, ByteChannel agentChannel) {
            initializer = new TestServerPipelineInitializer(sslConf, agentChannel);
            return initializer;
        }
    }
    
    static class TestServerPipelineInitializer extends ServerChannelInitializer {
        
        
        TestServerPipelineInitializer(SSLConfiguration config, ByteChannel agentChannel) {
            super(config, agentChannel);
        }
        
        SocketChannel getInitializedChannel(SocketChannel ch) throws Exception {
            super.initChannel(ch);
            return ch;
        }
    }
}

