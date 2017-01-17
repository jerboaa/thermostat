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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.command.server.internal.ServerHandler.SSLHandshakeDoneListener;
import com.redhat.thermostat.agent.ipc.client.IPCMessageChannel;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.shared.config.SSLConfiguration;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;

public class ServerHandlerTest {

    private ChannelPipeline pipeline;
    private ChannelHandlerContext ctx;
    private ServerHandler handler;
    private IPCMessageChannel agentChannel;
    private JsonResponseParser responseParser;
    private JsonRequestEncoder requestEncoder;

    @Before
    public void setup() {
        Channel channel = mock(Channel.class);
        pipeline = mock(ChannelPipeline.class);
        ChannelFuture channelFuture = mock(ChannelFuture.class);
        when(pipeline.write(isA(Response.class))).thenReturn(channelFuture);

        when(channel.pipeline()).thenReturn(pipeline);
        ctx = mock(ChannelHandlerContext.class);
        when(ctx.channel()).thenReturn(channel);
        
        SSLConfiguration mockSSLConf = mock(SSLConfiguration.class);
        when(mockSSLConf.enableForCmdChannel()).thenReturn(true);
        agentChannel = mock(IPCMessageChannel.class);
        when(agentChannel.isOpen()).thenReturn(true);
        
        requestEncoder = mock(JsonRequestEncoder.class);
        responseParser = mock(JsonResponseParser.class);
        handler = new ServerHandler(mockSSLConf, agentChannel, requestEncoder, responseParser);
    }

    @Test
    public void channelActiveAddsSSLListener() throws Exception {
        ChannelPipeline pipeline = mock(ChannelPipeline.class);
        when(ctx.pipeline()).thenReturn(pipeline);
        SslHandler sslHandler = mock(SslHandler.class);
        when(pipeline.get(SslHandler.class)).thenReturn(sslHandler);
        @SuppressWarnings("unchecked")
        Future<Channel> handshakeFuture = mock(Future.class);
        when(sslHandler.handshakeFuture()).thenReturn(handshakeFuture);
        
        handler.channelActive(ctx);
        verify(handshakeFuture).addListener(any(SSLHandshakeDoneListener.class));
    }

    @Test
    public void invalidRequestReturnsAnErrorResponse() throws Exception {
        // target and receiver are null
        Request request = mock(Request.class);
        handler.channelRead0(ctx, request);
        verify(requestEncoder, never()).encodeRequestAndSend(agentChannel, request);
        
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(pipeline).writeAndFlush(responseCaptor.capture());
        assertEquals(ResponseType.ERROR, responseCaptor.getValue().getType());
    }
    
    @Test
    public void testRequestReceived() throws Exception {
        Request request = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 123));
        request.setReceiver("com.example.MyReceiver");
        request.setParameter("hello", "world");
        
        Response response = new Response(ResponseType.OK);
        when(responseParser.parseResponse(agentChannel)).thenReturn(response);

        handler.channelRead0(ctx, request);
        
        verify(requestEncoder).encodeRequestAndSend(agentChannel, request);
        verify(pipeline).writeAndFlush(response);
    }
    
    @Test
    public void testRequestReceivedChannelClosed() throws Exception {
        Request request = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 123));
        request.setReceiver("com.example.MyReceiver");
        request.setParameter("hello", "world");
        
        when(agentChannel.isOpen()).thenReturn(false);
        handler.channelRead0(ctx, request);
        verify(requestEncoder, never()).encodeRequestAndSend(agentChannel, request);
        
        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(pipeline).writeAndFlush(responseCaptor.capture());
        assertEquals(ResponseType.ERROR, responseCaptor.getValue().getType());
    }
    
}