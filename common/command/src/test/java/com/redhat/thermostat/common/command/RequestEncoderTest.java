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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.Test;

import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestEncoder;
import com.redhat.thermostat.common.command.Request.RequestType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class RequestEncoderTest {

    private static final boolean DEBUG = false;
    /**
     * Represents low-level bytes for:
     * <pre>
     * Request request = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress(20))
     * request.setParameter("receiver", "com.redhat.foo.bar.Receiver");
     * </pre>
     */
    private static final byte[] REQUEST_BYTES = new byte[] { 0, 0, 0, 17, 82,
            69, 83, 80, 79, 78, 83, 69, 95, 69, 88, 80, 69, 67, 84, 69, 68, 0,
            0, 0, 1, 0, 0, 0, 8, 0, 0, 0, 27, 114, 101, 99, 101, 105, 118, 101,
            114, 99, 111, 109, 46, 114, 101, 100, 104, 97, 116, 46, 102, 111,
            111, 46, 98, 97, 114, 46, 82, 101, 99, 101, 105, 118, 101, 114 };

    @Test
    public void testEncodingRequestToArray() {
        Request request = new Request(RequestType.RESPONSE_EXPECTED, new InetSocketAddress(20));
        request.setParameter("receiver", "com.redhat.foo.bar.Receiver");
        RequestEncoder encoder = new RequestEncoder();
        ByteBuf buf = encoder.encode(request);
        byte[] array = Unpooled.copiedBuffer(buf).array();
        assertTrue(Arrays.equals(REQUEST_BYTES, array));
    }
    
    @Test
    public void canEncodeSimpleRequestWithNoParams() throws Exception {
        RequestEncoder encoder = new RequestEncoder();
        String responseExp = "RESPONSE_EXPECTED";
        ByteBuf stringBuf = Unpooled.copiedBuffer(responseExp, Charset.defaultCharset());
        ByteBuf buf = Unpooled.buffer(4);
        buf.writeInt(responseExp.getBytes().length);
        ByteBuf buf2 = Unpooled.wrappedBuffer(buf, stringBuf);
        buf = Unpooled.buffer(4);
        buf.writeInt(0);
        ByteBuf expected = Unpooled.wrappedBuffer(buf2, buf);
        InetSocketAddress addr = new InetSocketAddress("testhost", 12);
        Request item = new Request(RequestType.RESPONSE_EXPECTED, addr);
        ByteBuf actual = encoder.encode(item);
        if (DEBUG) {
            printBuffers(actual, expected);
        }
        assertEquals(0, ByteBufUtil.compare(expected, actual));
    }
    
    @Test
    public void canEncodeRequestWithParams() throws Exception {
        InetSocketAddress addr = new InetSocketAddress(1234);

        // Prepare request we'd like to encode
        Request item = new Request(RequestType.RESPONSE_EXPECTED, addr);
        String param1Name = "param1";
        String param1Value = "value1";
        String param2Name = "param2";
        String param2Value = "value2";
        item.setParameter(param1Name, param1Value);
        item.setParameter(param2Name, param2Value);
        RequestEncoder encoder = new RequestEncoder();
        
        // build expected
        String responseExp = "RESPONSE_EXPECTED";
        ByteBuf stringBuf = Unpooled.copiedBuffer(responseExp, Charset.defaultCharset());
        ByteBuf buf = Unpooled.buffer(4);
        buf.writeInt(responseExp.getBytes().length);
        ByteBuf buf2 = Unpooled.wrappedBuffer(buf, stringBuf);
        buf = Unpooled.buffer(4);
        buf.writeInt(2);
        ByteBuf request = Unpooled.wrappedBuffer(buf2, buf);
        ByteBuf nameLen = Unpooled.buffer(4);
        nameLen.writeInt(param1Name.getBytes().length);
        ByteBuf valueLen = Unpooled.buffer(4);
        valueLen.writeInt(param1Value.getBytes().length);
        ByteBuf lens = Unpooled.wrappedBuffer(nameLen, valueLen);
        ByteBuf nameBuf = Unpooled.copiedBuffer(param1Name, Charset.defaultCharset());
        ByteBuf valueBuf = Unpooled.copiedBuffer(param1Value, Charset.defaultCharset());
        ByteBuf payload = Unpooled.wrappedBuffer(nameBuf, valueBuf);
        ByteBuf param1Buf = Unpooled.wrappedBuffer(lens, payload);
        nameLen = Unpooled.buffer(4);
        nameLen.writeInt(param2Name.getBytes().length);
        valueLen = Unpooled.buffer(4);
        valueLen.writeInt(param2Value.getBytes().length);
        lens = Unpooled.wrappedBuffer(nameLen, valueLen);
        nameBuf = Unpooled.copiedBuffer(param2Name, Charset.defaultCharset());
        valueBuf = Unpooled.copiedBuffer(param2Value, Charset.defaultCharset());
        payload = Unpooled.wrappedBuffer(nameBuf, valueBuf);
        ByteBuf param2Buf = Unpooled.wrappedBuffer(lens, payload);
        ByteBuf params = Unpooled.wrappedBuffer(param1Buf, param2Buf);
        ByteBuf expected = Unpooled.wrappedBuffer(request, params);
        
        // Encode item for actual
        ByteBuf actual = encoder.encode(item);
        if (DEBUG) {
            printBuffers(actual, expected);
        }
        assertEquals(0, ByteBufUtil.compare(expected, actual));
    }

    private void printBuffers(ByteBuf actual, ByteBuf expected) {
        System.out.println("hexdump expected\n-------------------------------------");
        System.out.println(ByteBufUtil.hexDump(expected));
        System.out.println("\nhexdump actual\n-------------------------------------");
        System.out.println(ByteBufUtil.hexDump(actual) + "\n\n");
    }
}

