/*
 * Copyright 2012-2017 Red Hat, Inc.
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


/**
 * A Response object represents a response message passed from an agent
 * to a client.  Responses must not be used to send data; they reach only a
 * specific client and are not recorded.
 * <p>
 * The implementation details of this class are subject to change at any time.
 * <p>
 * Response objects are serialized over the command channel in the following
 * format:
 * <pre>
 * ------------
 * | A | TYPE |
 * ------------
 * </pre>
 * A is an 32 bit integer representing the length - in bytes - of TYPE. TYPE
 * is a byte array representing the string of the response type (e.g.
 * "OK").
 */
public class Response implements Message {

    // TODO add parameter support to provide more information in some of these types.
    public enum ResponseType implements MessageType {
        /** Request has been acknowledged and completed agent-side */
        OK,

        /** Request has been acknowledged and refused agent-side. */
        NOK,

        /**
         * Request has been acknowledged, but no action deemed necessary
         * agent-side.
         */
        NOOP,

        /**
         * An error occurred. The status of the request is not known.
         */
        ERROR,

	/**
         * When authentication fails in SecureStorage.
	 */
	AUTH_FAILED;
    }

    private ResponseType type;

    public Response (ResponseType type) {
        this.type = type;
    }

    @Override
    public ResponseType getType() {
        return type;
    }

}

