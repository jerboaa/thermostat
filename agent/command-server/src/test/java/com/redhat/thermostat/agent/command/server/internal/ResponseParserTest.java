/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.Test;

import com.redhat.thermostat.agent.command.server.internal.ResponseParser;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

public class ResponseParserTest {

    @Test
    public void testSuccess() throws IOException {
        final String input = CommandChannelConstants.BEGIN_RESPONSE_TOKEN + "\n"
                + "OK\n"
                + CommandChannelConstants.END_RESPONSE_TOKEN + "\n";
        Response response = parseResponse(input);
        
        assertEquals(ResponseType.OK, response.getType());
    }

    @Test(expected=IOException.class)
    public void testEOFBeginToken() throws IOException {
        ResponseParser parser = new ResponseParser();
        parser.parseResponse(new ByteArrayInputStream(new byte[0]));
    }
    
    @Test(expected=IOException.class)
    public void testBadBeginToken() throws IOException {
        final String input = "<BEGIN RESP>\n"
                + "OK\n"
                + CommandChannelConstants.END_RESPONSE_TOKEN + "\n";
        parseResponse(input);
    }
    
    @Test(expected=IOException.class)
    public void testEOFType() throws IOException {
        final String input = CommandChannelConstants.BEGIN_RESPONSE_TOKEN + "\n";
        parseResponse(input);
    }
    
    @Test(expected=IOException.class)
    public void testBadType() throws IOException {
        final String input = CommandChannelConstants.BEGIN_RESPONSE_TOKEN + "\n"
                + "HELLO_THERE\n"
                + CommandChannelConstants.END_RESPONSE_TOKEN + "\n";
        parseResponse(input);
    }
    
    @Test(expected=IOException.class)
    public void testEOFEndToken() throws IOException {
        final String input = CommandChannelConstants.BEGIN_RESPONSE_TOKEN + "\n"
                + "OK\n";
        parseResponse(input);
    }
    
    @Test(expected=IOException.class)
    public void testBadEndToken() throws IOException {
        final String input = CommandChannelConstants.BEGIN_RESPONSE_TOKEN + "\n"
                + "OK\n"
                + "<END RESP>\n";
        parseResponse(input);
    }

    private Response parseResponse(final String input) throws IOException {
        ResponseParser parser = new ResponseParser();
        Response response = parser.parseResponse(new ByteArrayInputStream(input.getBytes()));
        return response;
    }

}
