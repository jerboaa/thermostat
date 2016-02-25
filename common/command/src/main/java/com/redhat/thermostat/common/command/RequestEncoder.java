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

package com.redhat.thermostat.common.command;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/**
 * <p>
 * {@link Request} objects are serialized over the command channel in the
 * following format:
 * <pre>
 * -------------------------
 * | A | TYPE | B | PARAMS |
 * -------------------------
 * 
 * A is an 32 bit integer representing the length - in bytes - of TYPE. TYPE
 * is a byte array representing the string of the request type (e.g.
 * "RESPONSE_EXPECTED") B is a 32 bit integer representing the number of
 * request parameters which follow.
 * 
 * PARAMS (if B > 0) is a variable length stream of the following format:
 * 
 * It is a simple encoding of name => value pairs.
 * 
 * -----------------------------------------------------------------------------------------------
 * | I_1 | K_1 | P_1 | V_1 | ... | I_(n-1) | K_(n-1) | P_(n-1) | V_(n-1) | I_n | K_n | P_n | V_n |
 * -----------------------------------------------------------------------------------------------
 * 
 * I_n  A 32 bit integer representing the length - in bytes - of the n'th
 *      parameter name.
 * K_n  A 32 bit integer representing the length - in bytes - of the n'th
 *      parameter value.
 * P_n  A byte array representing the string of the n'th parameter name.
 * V_n  A byte array representing the string of the n'th parameter value.
 * </pre>
 * </p>
 */
public class RequestEncoder extends MessageEncoder {

    private static final Logger logger = LoggingUtils.getLogger(RequestEncoder.class);

    /*
     * See the javadoc of RequestEncoder for a description of the encoding.
     */
    @Override
    public ByteBuf encode(Message msg) {
        // At this point we are only getting Messages. Since our only
        // registered MessageEncoder is the one for Requests a cast
        // to Request should be safe.
        Request request = (Request) msg;
        logger.log(Level.FINEST, "encoding Request object " + request.toString());

        // Request Type
        String requestType = EncodingHelper.trimType(request.getType()
                .toString());
        ByteBuf typeBuffer = EncodingHelper.encode(requestType);

        // Parameters
        ByteBuf parmsBuffer = Unpooled.buffer();
        Collection<String> parmNames = request.getParameterNames();
        parmsBuffer.writeInt(parmNames.size());
        for (String parmName : parmNames) {
            EncodingHelper.encode(parmName, request.getParameter(parmName),
                    parmsBuffer);
        }
        // Compose the full message.
        ByteBuf buf = Unpooled.wrappedBuffer(typeBuffer, parmsBuffer);
        logger.log(Level.FINEST, "encoded reqest as: " + ByteBufUtil.hexDump(buf));
        return buf;
    }
}

