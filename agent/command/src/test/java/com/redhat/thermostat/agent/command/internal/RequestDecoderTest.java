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

package com.redhat.thermostat.agent.command.internal;

import java.net.SocketAddress;
import java.util.Collection;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.command.internal.RequestDecoder;
import com.redhat.thermostat.common.command.InvalidMessageException;
import com.redhat.thermostat.common.command.Message;
import com.redhat.thermostat.common.command.Messages;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class RequestDecoderTest {
    
    /*
     * This is serialized format for
     * req = new Request(RequestType.RESPONSE_EXPECTED, blah)
     * req.setParameter("param1", "value1");
     * req.setParameter("param2", "value2");
     */
    private static final byte[] ENCODED_REQEUEST_WITH_PARAMS = new byte[] {
        0x00, 0x00, 0x00, 0x11, 0x52, 0x45, 0x53, 0x50, 0x4f, 0x4e, 0x53, 0x45,
        0x5f, 0x45, 0x58, 0x50, 0x45, 0x43, 0x54, 0x45, 0x44, 0x00, 0x00, 0x00,
        0x02, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, 0x06, 0x70, 0x61, 0x72,
        0x61, 0x6d, 0x31, 0x76, 0x61, 0x6c, 0x75, 0x65, 0x31, 0x00, 0x00, 0x00,
        0x06, 0x00, 0x00, 0x00, 0x06, 0x70, 0x61, 0x72, 0x61, 0x6d, 0x32, 0x76,
        0x61, 0x6c, 0x75, 0x65, 0x32
    };
    
    /*
     * This is serialized format for
     * req = new Request(RequestType.RESPONSE_EXPECTED, blah)
     */
    private static final byte[] ENCODED_REQUEST_WITH_NO_PARAMS = new byte[] {
        0x00, 0x00, 0x00, 0x11, 0x52, 0x45, 0x53, 0x50, 0x4f, 0x4e, 0x53, 0x45,
        0x5f, 0x45, 0x58, 0x50, 0x45, 0x43, 0x54, 0x45, 0x44, 0x00, 0x00, 0x00,
        0x00
    };
    
    private static final byte[][] GARBAGE_AS_REQUEST = new byte[][] {
            // general garbage
            { 0x0d, 0x0b, 0x0e, 0x0e, 0x0f },
            // first two bytes are broken
            { 0x0f, 0x0d, 0x00, 0x11, 0x52, 0x45, 0x53, 0x50, 0x4f, 0x4e, 0x53,
                    0x45, 0x5f, 0x45, 0x58, 0x50, 0x45, 0x43, 0x54, 0x45, 0x44,
                    0x00, 0x00, 0x00, 0x00 },
            // last byte indicates params, which are missing
            { 0x00, 0x00, 0x00, 0x11, 0x52, 0x45, 0x53, 0x50, 0x4f, 0x4e, 0x53,
                    0x45, 0x5f, 0x45, 0x58, 0x50, 0x45, 0x43, 0x54, 0x45, 0x44,
                    0x00, 0x00, 0x00, 0x0f } };
    
    
    private static final byte[] TYPE = RequestType.RESPONSE_EXPECTED.toString().getBytes();

    private Channel channel;
    private RequestDecoder decoder;

    @Before
    public void setUp() {
        channel = mock(Channel.class);
        decoder = new RequestDecoder();
    }

    @Test
    public void testDecode() throws InvalidMessageException {
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeInt(TYPE.length);
        buffer.writeBytes(TYPE);

        Request request = (Request) decoder.decode(channel, buffer);

        assertTrue(RequestType.RESPONSE_EXPECTED == (RequestType) request.getType());
    }

    @Test
    public void testDecodeWithParameters() throws InvalidMessageException {
        String parmName = "parameter";
        String parmValue = "hello";
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeInt(TYPE.length);
        buffer.writeBytes(TYPE);
        buffer.writeInt(1);
        buffer.writeInt(parmName.getBytes().length);
        buffer.writeInt(parmValue.getBytes().length);
        buffer.writeBytes(parmName.getBytes());
        buffer.writeBytes(parmValue.getBytes());
        
        Request request = (Request) decoder.decode(channel, buffer);
        Collection<String> parmNames = request.getParameterNames();

        assertEquals(1, parmNames.size());
        assertTrue(parmNames.contains(parmName));
        String decodedValue = request.getParameter(parmName);
        assertEquals(parmValue, decodedValue);
    }
    
    @Test
    public void testDecodeWithParametersFromBytesArray() throws InvalidMessageException {
        ChannelBuffer buffer = ChannelBuffers.copiedBuffer(ENCODED_REQEUEST_WITH_PARAMS);
        Request expected = new Request(RequestType.RESPONSE_EXPECTED, null);
        expected.setParameter("param1", "value1");
        expected.setParameter("param2", "value2");
        Message actual = new RequestDecoder().decode(channel, buffer);
        assertTrue(actual instanceof Request);
        assertTrue(Messages.equal(expected, (Request)actual));
        SocketAddress addr = mock(SocketAddress.class);
        buffer = ChannelBuffers.copiedBuffer(ENCODED_REQUEST_WITH_NO_PARAMS);
        expected = new Request(RequestType.RESPONSE_EXPECTED, addr);
        actual = new RequestDecoder().decode(channel, buffer);
        assertTrue(actual instanceof Request);
        assertTrue(Messages.equal(expected, (Request)actual));
    }
    
    @Test
    public void decodingOfGarbageThrowsException()
            throws InvalidMessageException {
        int expectedFailures = GARBAGE_AS_REQUEST.length;
        int actualFailures = 0;
        for (int i = 0; i < GARBAGE_AS_REQUEST.length; i++) {
            ChannelBuffer buffer = ChannelBuffers
                    .copiedBuffer(GARBAGE_AS_REQUEST[0]);
            RequestDecoder decoder = new RequestDecoder();
            try {
                decoder.decode(channel, buffer);
            } catch (InvalidMessageException e) {
                // pass
                actualFailures++;
            }
        }
        assertEquals(expectedFailures, actualFailures);
    }
}
