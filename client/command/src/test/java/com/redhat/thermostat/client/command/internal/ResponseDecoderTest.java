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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

import com.redhat.thermostat.common.command.InvalidMessageException;
import com.redhat.thermostat.common.command.Message;
import com.redhat.thermostat.common.command.Messages;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

public class ResponseDecoderTest {

    private static final byte[] ENCODED_OK_RESP = new byte[] {
        0x00, 0x00, 0x00, 0x02, 0x4f, 0x4b  
    };
    
    private static final byte[] GARBAGE_AS_RESPONSE = new byte[] {
        0x0d, 0x0b, 0x0e, 0x0e, 0x0f  
    };
    
    @Test
    public void testDecode() throws InvalidMessageException {
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(ENCODED_OK_RESP);
        Response expected = new Response(ResponseType.OK);
        ResponseDecoder decoder = new ResponseDecoder();
        Message actual = decoder.decode(null, buffer);
        assertTrue(actual instanceof Response);
        assertTrue(Messages.equal(expected, (Response)actual));
    }
    
    @Test
    public void verifyInvalidEncodingThrowsException() {
        ResponseDecoder decoder = new ResponseDecoder();
        ChannelBuffer garbage = ChannelBuffers.copiedBuffer(GARBAGE_AS_RESPONSE);
        try {
            decoder.decode(null, garbage);
            fail("Should have thrown decoding exception!");
        } catch (InvalidMessageException e) {
            // pass
        }
    }
}

