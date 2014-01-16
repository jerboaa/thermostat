/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.common.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.junit.Test;

public class MessageDecoderTest {

    private static final byte[] GOOD_MSG = new byte[] {
        0x00, 0x00, 0x00, 0x02, 0x4f, 0x4b
    };
    
    private static final byte[] BAD_MSG = new byte[] {
        0x0b, 0x0e, 0x0e, 0x0f
    };
    
    @Test
    public void canDecodeGoodMessage() throws Exception {
        MessageDecoder decoder = new DummyMessageDecoder();
        // Sanity check
        assertFalse(decoder.exceptionCaught);
        MessageEvent e = mock(MessageEvent.class);
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(GOOD_MSG);
        when(e.getMessage()).thenReturn(buffer);
        when(ctx.getChannel()).thenReturn(channel);
        decoder.handleUpstream(ctx, e);
        assertFalse(decoder.exceptionCaught);
    }
    
    @Test
    public void decodingBadMessageThrowsException() throws Exception {
        MessageDecoder decoder = new DummyMessageDecoder();
        // Sanity check
        assertFalse(decoder.exceptionCaught);
        MessageEvent e = mock(MessageEvent.class);
        ChannelHandlerContext ctx = mock(ChannelHandlerContext.class);
        Channel channel = mock(Channel.class);
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(BAD_MSG);
        when(e.getMessage()).thenReturn(buffer);
        when(ctx.getChannel()).thenReturn(channel);
        decoder.handleUpstream(ctx, e);
        assertTrue(decoder.exceptionCaught);
    }
    
    private static class DummyMessageDecoder extends MessageDecoder {

        @Override
        protected Message decode(Channel channel, ChannelBuffer msg)
                throws InvalidMessageException {
            if (msg.readInt() == 0x0b0e0e0f) {
                throw new InvalidMessageException("Burn, baby, burn!");
            }
            return new Message() {

                @Override
                public MessageType getType() {
                    return new MessageType() {};
                }
                
            };
        }
        
    }
}

