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

package com.redhat.thermostat.agent.command.internal;

import static org.junit.Assert.assertEquals;

import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.Test;

import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;

public class ResponseEncoderTest {

    private static final boolean DEBUG = false;
    
    @Test
    public void testEncode() throws Exception {
        ResponseEncoder encoder = new ResponseEncoder();
        String responseExp = "OK";
        ChannelBuffer stringBuf = ChannelBuffers.copiedBuffer(responseExp, Charset.defaultCharset());
        ChannelBuffer buf = ChannelBuffers.buffer(4);
        buf.writeInt(responseExp.getBytes().length);
        ChannelBuffer expected = ChannelBuffers.wrappedBuffer(buf, stringBuf);
        Response ok = new Response(ResponseType.OK);
        ChannelBuffer actual = (ChannelBuffer)encoder.encode(ok);
        if (DEBUG) {
            printBuffers(actual, expected);
        }
        assertEquals(0, ChannelBuffers.compare(expected, actual));
    }
    
    private void printBuffers(ChannelBuffer actual, ChannelBuffer expected) {
        System.out.println("hexdump expected\n-------------------------------------");
        System.out.println(ChannelBuffers.hexDump(expected));
        System.out.println("\nhexdump actual\n-------------------------------------");
        System.out.println(ChannelBuffers.hexDump(actual) + "\n\n");
    }
}

