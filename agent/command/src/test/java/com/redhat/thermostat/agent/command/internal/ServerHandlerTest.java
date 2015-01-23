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
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.ssl.SslHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.command.internal.ServerHandler.SSLHandshakeDoneListener;
import com.redhat.thermostat.agent.command.internal.ServerHandler.StorageGetter;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class ServerHandlerTest {

    private StorageGetter storageGetter;

    private Channel channel;
    private ChannelHandlerContext ctx;

    @Before
    public void setup() {
        channel = mock(Channel.class);
        when(channel.isConnected()).thenReturn(true);
        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(channel.write(isA(Response.class))).thenReturn(channelFuture);

        ctx = mock(ChannelHandlerContext.class);
        when(ctx.getChannel()).thenReturn(channel);

        storageGetter = mock(StorageGetter.class);
    }

    @Test
    public void channelConnectedAddsSSLListener() throws Exception {
        SSLConfiguration mockSSLConf = mock(SSLConfiguration.class);
        when(mockSSLConf.enableForCmdChannel()).thenReturn(true);
        ServerHandler handler = new ServerHandler(null, mockSSLConf, storageGetter);
        
        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        when(ctx.getPipeline()).thenReturn(pipeline);
        SslHandler sslHandler = mock(SslHandler.class);
        when(pipeline.get(SslHandler.class)).thenReturn(sslHandler);
        ChannelFuture handshakeFuture = mock(ChannelFuture.class);
        when(sslHandler.handshake()).thenReturn(handshakeFuture);
        
        handler.channelConnected(ctx, null);
        verify(handshakeFuture).addListener(any(SSLHandshakeDoneListener.class));
    }

    @Test
    public void invalidRequestReturnsAnErrorResponse() {
        // target and receiver are null
        Request request = mock(Request.class);
        MessageEvent event = mock(MessageEvent.class);
        when(event.getMessage()).thenReturn(request);

        ServerHandler handler = new ServerHandler(null, null, storageGetter);
        handler.messageReceived(ctx, event);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(channel).write(responseCaptor.capture());
        assertEquals(ResponseType.ERROR, responseCaptor.getValue().getType());
    }
}

