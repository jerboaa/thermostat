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

import static org.jboss.netty.channel.Channels.fireMessageReceived;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import com.redhat.thermostat.common.utils.LoggingUtils;

public abstract class MessageDecoder extends SimpleChannelUpstreamHandler {

    // testing hook
    boolean exceptionCaught = false;
    
    private static final Logger logger = LoggingUtils.getLogger(MessageDecoder.class);
    
    protected MessageDecoder() {
        super();
    }

    @Override
    public void handleUpstream(
            ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        if (!(evt instanceof MessageEvent)) {
            ctx.sendUpstream(evt);
            return;
        }

        MessageEvent e = (MessageEvent) evt;
        Object originalMessage = e.getMessage();
        if (!(originalMessage instanceof ChannelBuffer)) {
            // Skip decoding, since we've received something
            // we don't know how to deal with anyway.
            ctx.sendUpstream(evt);
        }
        Message decodedMessage = null;
        try {
            decodedMessage = decode(e.getChannel(), (ChannelBuffer)originalMessage);
        } catch (InvalidMessageException ex) {
            logger.log(Level.WARNING, "Decoding failed on received message. Possible DoS attack!", ex);
            exceptionCaught = true;
        }
        if (decodedMessage != null) {
            fireMessageReceived(ctx, decodedMessage, e.getRemoteAddress());
        }
    }

    /**
     * Transforms the specified received message into another message and return
     * the transformed message.  Return {@code null} if the received message
     * is supposed to be discarded.
     * 
     * @throws InvalidMessageException If the received message was not properly encoded.
     */
    protected abstract Message decode(Channel channel, ChannelBuffer msg) throws InvalidMessageException;
}

