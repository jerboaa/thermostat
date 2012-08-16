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

import java.util.Collection;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.command.internal.RequestDecoder;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class RequestDecoderTest {
    private static final byte[] TYPE = RequestType.RESPONSE_EXPECTED.toString().getBytes();

    private Channel channel;
    private RequestDecoder decoder;

    @Before
    public void setUp() {
        channel = mock(Channel.class);
        decoder = new RequestDecoder();
    }

    @Test
    public void testDecode() {
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        buffer.writeInt(TYPE.length);
        buffer.writeBytes(TYPE);

        Request request = (Request) decoder.decode(null, channel, buffer);

        assertTrue(RequestType.RESPONSE_EXPECTED == (RequestType) request.getType());
    }

    @Test
    public void testDecodeWithParameters() {
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
        
        Request request = (Request) decoder.decode(null, channel, buffer);
        Collection<String> parmNames = request.getParameterNames();

        assertEquals(1, parmNames.size());
        assertTrue(parmNames.contains(parmName));
        String decodedValue = request.getParameter(parmName);
        assertEquals(parmValue, decodedValue);
    }
}
