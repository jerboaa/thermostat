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

package com.redhat.thermostat.client.command.internal;

import java.util.Collection;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;

import com.redhat.thermostat.common.command.EncodingHelper;
import com.redhat.thermostat.common.command.MessageEncoder;
import com.redhat.thermostat.common.command.Request;

import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

class RequestEncoder extends MessageEncoder {

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) {

        Request request = (Request) e.getMessage();

        // Request Type
        String requestType = EncodingHelper.trimType(request.getType().toString());
        ChannelBuffer typeBuffer = EncodingHelper.encode(requestType);

        // Parameters
        // TODO: if in practice parms take up more than 256 bytes, use appropriate
        // dynamicBuffer() variant to specify initial/estimated capacity.
        ChannelBuffer parmsBuffer = dynamicBuffer();
        Collection<String> parmNames = request.getParameterNames();
        parmsBuffer.writeInt(parmNames.size());
        for (String parmName : parmNames) {
            EncodingHelper.encode(parmName, request.getParameter(parmName), parmsBuffer);
        }

        // Compose the full message.
        ChannelBuffer buf = wrappedBuffer(typeBuffer, parmsBuffer);
        Channels.write(ctx, e.getFuture(), buf);
    }

    // This must be implemented, even though we are simply passing on the exception.  If
    // not implemented, this exception ends up going uncaught which causes problems.
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
    	Channels.fireExceptionCaught(ctx, e.getCause());
    }
}