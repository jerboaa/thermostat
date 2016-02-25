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

package com.redhat.thermostat.client.command.internal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

public class ResponseHandlerTest {

    @Test
    public void messageReceivedCallsFireComplete() throws Exception {
        Request req = mock(Request.class);
        List<RequestResponseListener> listeners = new ArrayList<>();
        ResponseListenerFixture fixture = new ResponseListenerFixture();
        listeners.add(fixture);
        when(req.getListeners()).thenReturn(listeners);
        ResponseHandler handler = new ResponseHandler(req);
        Response response = mock(Response.class);
        when(response.getType()).thenReturn(ResponseType.OK);
        
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        when(ctx.pipeline()).thenReturn(mock(ChannelPipeline.class));
        handler.channelRead0(ctx, response);
        assertTrue(fixture.isCalled());
    }
    
    @Test
    public void exceptionCaughtCallsFireCompleteAndClosesChannel() throws Exception {
        Request req = mock(Request.class);
        List<RequestResponseListener> listeners = new ArrayList<>();
        ResponseListenerFixture fixture = new ResponseListenerFixture();
        listeners.add(fixture);
        when(req.getListeners()).thenReturn(listeners);
        ResponseHandler handler = new ResponseHandler(req);
        Channel chan = mock(Channel.class);
        when(chan.close()).thenReturn(mock(ChannelFuture.class));
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        when(ctx.channel()).thenReturn(chan);
        when(ctx.pipeline()).thenReturn(mock(ChannelPipeline.class));
        handler.exceptionCaught(ctx, new Exception("Test me!"));
        assertTrue(fixture.isCalled());
        verify(chan).close();
    }
    
    private class ResponseListenerFixture implements RequestResponseListener {

        public boolean called = false;
        
        public boolean isCalled() {
            return called;
        }

        @Override
        public void fireComplete(Request request, Response response) {
            called = true;
        }
        
    }
}

