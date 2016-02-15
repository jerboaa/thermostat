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

package com.redhat.thermostat.agent.command.server.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

class ResponseParser {
    
    Response parseResponse(InputStream is) throws IOException {
        // Wait for response from agent
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        // TODO Timeout?
        String line = br.readLine();
        requireNonNull(line, "Expected " + CommandChannelConstants.BEGIN_RESPONSE_TOKEN + ", got EOF");
        if (!CommandChannelConstants.BEGIN_RESPONSE_TOKEN.equals(line)) {
            throw new IOException("Expected " + CommandChannelConstants.BEGIN_RESPONSE_TOKEN + ", got: " + line);
        }
        
        // Parse response type
        line = br.readLine();
        ResponseType type;
        requireNonNull(line, "Expected ResponseType, got EOF");
        try {
            type = Enum.valueOf(ResponseType.class, line);
        } catch (IllegalArgumentException e) {
            throw new IOException("Expected ResponseType, got " + line);
        }
        
        line = br.readLine();
        requireNonNull(line, "Expected " + CommandChannelConstants.END_RESPONSE_TOKEN + ", got EOF");
        if (!CommandChannelConstants.END_RESPONSE_TOKEN.equals(line)) {
            throw new IOException("Expected " + CommandChannelConstants.END_RESPONSE_TOKEN + ", got: " + line);
        }
        
        return new Response(type);
    }

    private void requireNonNull(String line, String errorMessage) throws IOException {
        if (line == null) {
            throw new IOException(errorMessage);
        }
    }
}
