/*
 * Copyright 2012 Red Hat, Inc.
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

import java.nio.ByteBuffer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.agent.command.internal.ResponseEncoder;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Channels.class)
public class ResponseEncoderTest {

    private MessageEvent e;

    @Before
    public void setUp() {
        Response r = new Response(ResponseType.OK);
        e = mock(MessageEvent.class);
        when(e.getMessage()).thenReturn(r);
        when(e.getFuture()).thenReturn(null);
        
    }

    @Test
    public void testWriteRequested() {
        PowerMockito.mockStatic(Channels.class);
        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);

        ResponseEncoder encoder = new ResponseEncoder();
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        encoder.writeRequested(ctx, e);
        
        PowerMockito.verifyStatic();
        Channels.write(any(ChannelHandlerContext.class), any(ChannelFuture.class), argument.capture());

        ChannelBuffer buf = (ChannelBuffer) argument.getValue();
        int messageLength = buf.readInt();
        assertEquals(2, messageLength);
        ByteBuffer bbuf = ByteBuffer.allocate(buf.readableBytes());
        buf.readBytes(bbuf);
        String message = new String(bbuf.array());
        assertEquals("OK", message);
    }
}
