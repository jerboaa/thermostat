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

package com.redhat.thermostat.agent.command.internal;

import static org.junit.Assert.assertEquals;

import java.net.InetSocketAddress;
import java.util.Arrays;

import org.junit.Test;

import com.redhat.thermostat.common.command.InvalidMessageException;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Request.RequestType;
import com.redhat.thermostat.common.command.RequestEncoder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class AgentRequestDecoderTest {

    @Test
    public void testDecodeSuccess() throws InvalidMessageException {
        Request request = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress(3));
        request.setParameter("receiver", "com.redhat.foo.bar.Receiver");
        ByteBuf buf = new RequestEncoder().encode(request);
        byte[] bytes = Unpooled.copiedBuffer(buf).array();
        AgentRequestDecoder decoder = new AgentRequestDecoder();
        Request actual = decoder.decode(new InetSocketAddress(3), bytes);
        assertEquals(request.getParameter("receiver"), actual.getParameter("receiver"));
    }
    
    @Test(expected = InvalidMessageException.class)
    public void testDecodeFailNotAllData() throws InvalidMessageException {
        Request request = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress(3));
        request.setParameter("receiver", "com.redhat.foo.bar.Receiver");
        ByteBuf buf = new RequestEncoder().encode(request);
        byte[] bytes = Unpooled.copiedBuffer(buf).array();
        byte[] tooShort = Arrays.copyOfRange(bytes, 0, bytes.length - 2);
        AgentRequestDecoder decoder = new AgentRequestDecoder();
        decoder.decode(new InetSocketAddress(3), tooShort);
    }
}
