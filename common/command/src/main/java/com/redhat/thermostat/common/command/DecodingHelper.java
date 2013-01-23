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

package com.redhat.thermostat.common.command;

import org.jboss.netty.buffer.ChannelBuffer;

public class DecodingHelper {

    public static String decodeString(ChannelBuffer buffer) {
        if (buffer.readableBytes() < 4) {
            return null;
        }
        int length = buffer.readInt();
        return decodeString(length, buffer);
    }

    public static boolean decodeParameters(ChannelBuffer buffer, Request request) {
        int bytesLeft = buffer.readableBytes();
        if (bytesLeft == 0) {
            // Exactly zero parameters in this request.
            return true;
        }
        if (bytesLeft < 4) {
            // Bad encoding or some stream issue.
            return false;
        }
        int numParms = buffer.readInt();
        for (int i = 0; i < numParms; i++) {
            if (!decodeParameter(buffer, request)) {
                return false;
            }
        }
        return true;
    }

    private static boolean decodeParameter(ChannelBuffer buffer, Request request) {
        if (buffer.readableBytes() < 8) {
            return false;
        }
        int nameLength = buffer.readInt();
        int valueLength = buffer.readInt();
        String name = decodeString(nameLength, buffer);
        if (name == null) {
            return false;
        }
        String value = decodeString(valueLength, buffer);
        if (value == null) {
            return false;
        }
        request.setParameter(name, value);
        return true;
    }

    private static String decodeString(int length, ChannelBuffer buffer) {
        if (buffer.readableBytes() < length) {
            return null;
        }
        byte[] stringBytes = buffer.readBytes(length).array();
        return new String(stringBytes);
    }
}

