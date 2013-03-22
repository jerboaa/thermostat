/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

import com.redhat.thermostat.common.command.DecodingHelper;
import com.redhat.thermostat.common.command.InvalidMessageException;
import com.redhat.thermostat.common.command.Message;
import com.redhat.thermostat.common.command.MessageDecoder;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.utils.LoggingUtils;

class RequestDecoder extends MessageDecoder {
    
    private static final Logger logger = LoggingUtils.getLogger(RequestDecoder.class);

    /*
     * See the javadoc of Request for a description of the encoding.
     */
    @Override
    protected Message decode(Channel channel, ChannelBuffer msg) throws InvalidMessageException {
        logger.log(Level.FINEST, "agent: decoding Request object");
        ChannelBuffer buffer = (ChannelBuffer) msg;
        buffer.markReaderIndex();
        String typeAsString = DecodingHelper.decodeString(buffer);
        if (typeAsString == null) {
            buffer.resetReaderIndex();
            throw new InvalidMessageException("Could not decode message: " + ChannelBuffers.hexDump(buffer));
        }
        // Netty javadoc tells us it's safe to downcast to more concrete type.
        InetSocketAddress addr = (InetSocketAddress)channel.getRemoteAddress();
        Request request = new Request(RequestType.valueOf(typeAsString), addr);
        if (!DecodingHelper.decodeParameters(buffer, request)) {
            buffer.resetReaderIndex();
            throw new InvalidMessageException("Could not decode message: " + ChannelBuffers.hexDump(buffer));
        }
        return request;
    }

}

