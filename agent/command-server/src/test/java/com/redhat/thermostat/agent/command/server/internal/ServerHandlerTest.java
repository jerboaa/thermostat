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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.ssl.SslHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.command.server.internal.RequestEncoder;
import com.redhat.thermostat.agent.command.server.internal.ResponseParser;
import com.redhat.thermostat.agent.command.server.internal.ServerHandler;
import com.redhat.thermostat.agent.command.server.internal.ServerHandler.SSLHandshakeDoneListener;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class ServerHandlerTest {

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
    }

    @Test
    public void channelConnectedAddsSSLListener() throws Exception {
        SSLConfiguration mockSSLConf = mock(SSLConfiguration.class);
        when(mockSSLConf.enableForCmdChannel()).thenReturn(true);
        ServerHandler handler = new ServerHandler(mockSSLConf);
        
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

        ServerHandler handler = new ServerHandler(null);
        handler.messageReceived(ctx, event);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(channel).write(responseCaptor.capture());
        assertEquals(ResponseType.ERROR, responseCaptor.getValue().getType());
    }
    
    @Test
    public void testRequestReceived() throws IOException {
        Request request = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress("127.0.0.1", 123));
        request.setReceiver("com.example.MyReceiver");
        request.setParameter("hello", "world");
        byte[] expectedRequest = constructRequestAsBytes(request);
        
        ResponseParser responseParser = mock(ResponseParser.class);
        Response response = new Response(ResponseType.OK);
        when(responseParser.parseResponse(any(InputStream.class))).thenReturn(response);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ServerHandler handler = new ServerHandler(null, responseParser, stdout);

        MessageEvent event = mock(MessageEvent.class);
        when(event.getMessage()).thenReturn(request);

        handler.messageReceived(ctx, event);
        
        assertArrayEquals(expectedRequest, stdout.toByteArray());

        verify(channel).write(response);
    }
    
    private byte[] constructRequestAsBytes(Request request) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF(CommandChannelConstants.BEGIN_REQUEST_TOKEN);
        dos.writeUTF("127.0.0.1");
        dos.writeInt(123);
        RequestEncoder encoder = new RequestEncoder();
        ChannelBuffer buf = encoder.encode(request);
        byte[] reqBytes = buf.toByteBuffer().array();
        dos.writeInt(reqBytes.length);
        dos.write(reqBytes);
        dos.writeUTF(CommandChannelConstants.END_REQUEST_TOKEN);
        dos.close();
        return baos.toByteArray();
    }
    
}