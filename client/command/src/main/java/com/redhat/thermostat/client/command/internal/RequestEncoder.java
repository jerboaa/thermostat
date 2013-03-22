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

package com.redhat.thermostat.client.command.internal;

import static org.jboss.netty.buffer.ChannelBuffers.dynamicBuffer;
import static org.jboss.netty.buffer.ChannelBuffers.wrappedBuffer;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.netty.buffer.ChannelBuffer;

import com.redhat.thermostat.common.command.EncodingHelper;
import com.redhat.thermostat.common.command.Message;
import com.redhat.thermostat.common.command.MessageEncoder;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.utils.LoggingUtils;

class RequestEncoder extends MessageEncoder {

    private static final Logger logger = LoggingUtils.getLogger(RequestEncoder.class);

    /*
     * See the javadoc of Request for a description of the encoding.
     */
    @Override
    protected ChannelBuffer encode(Message msg) {
        // At this point we are only getting Messages. Since our only
        // registered MessageEncoder is the one for Requests a cast
        // to Request should be safe.
        Request request = (Request) msg;
        logger.log(Level.FINEST, "encoding Request object " + request.toString());

        // Request Type
        String requestType = EncodingHelper.trimType(request.getType()
                .toString());
        ChannelBuffer typeBuffer = EncodingHelper.encode(requestType);

        // Parameters
        // TODO: if in practice parms take up more than 256 bytes, use
        // appropriate dynamicBuffer() variant to specify initial/estimated capacity.
        ChannelBuffer parmsBuffer = dynamicBuffer();
        Collection<String> parmNames = request.getParameterNames();
        parmsBuffer.writeInt(parmNames.size());
        for (String parmName : parmNames) {
            EncodingHelper.encode(parmName, request.getParameter(parmName),
                    parmsBuffer);
        }
        // Compose the full message.
        ChannelBuffer buf = wrappedBuffer(typeBuffer, parmsBuffer);
        // Just return the channel buffer which is our encoded message
        return buf;
    }
}

