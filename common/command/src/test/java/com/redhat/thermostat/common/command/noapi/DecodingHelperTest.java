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

package com.redhat.thermostat.common.command.noapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DecodingHelperTest {

    /**
     * Fragmented string case. Not enough data for string decoding. Specifically
     * missing length of string.
     */
    @Test
    public void decodeStringMissingLengthBytes() {
        ByteBuf buf = Unpooled.buffer(0, 0);
        assertEquals(0, buf.readerIndex());
        StringDecodingContext ctx = DecodingHelper.decodeString(buf);
        assertEquals("reader index should be untouched", 0, buf.readerIndex());
        assertEquals(StringDecodingState.INCOMPLETE_LENGTH_VAL, ctx.getState());
        assertEquals(0, ctx.getBytesRead());
    }
    
    /**
     * Fragmented string case. Not enough data for string decoding. Specifically
     * missing string bytes.
     */
    @Test
    public void decodeStringMissingStringBytes() {
        ByteBuf buf = Unpooled.buffer(0, 4);
        buf.writeInt(5);
        assertEquals(0, buf.readerIndex());
        StringDecodingContext ctx = DecodingHelper.decodeString(buf);
        assertEquals("reader index should be untouched", 0, buf.readerIndex());
        assertEquals(StringDecodingState.INCOMPLETE_STR_VAL, ctx.getState());
        assertEquals(4, ctx.getBytesRead());
    }
    
    @Test
    public void canDecodeStringFull() {
        String original = "this is a test";
        ByteBuf buf = Unpooled.buffer(0, 4 + original.length());
        buf.writeInt(original.length());
        buf.writeBytes(original.getBytes());
        assertEquals(0, buf.readerIndex());
        StringDecodingContext ctx = DecodingHelper.decodeString(buf);
        assertEquals("reader index should be untouched", 0, buf.readerIndex());
        assertEquals(original.getBytes().length + 4, ctx.getBytesRead());
        assertEquals(StringDecodingState.VALUE_READ, ctx.getState());
        assertEquals(original, ctx.getValue());
    }
    
    @Test
    public void canDecodeParametersFull() {
        String key1 = "key1";
        String value1 = "value1";
        String key2 = "this is a key";
        String value2 = "value 2";
        int totalBytes = 4 /* # params */ +
                         8 /* len key + len value */ +
                         key1.getBytes().length +
                         value1.getBytes().length +
                         8 /* len key + len value */ +
                         key2.getBytes().length +
                         value2.getBytes().length;
        ByteBuf buf = Unpooled.buffer(0, totalBytes);
        buf.writeInt(2);
        buf.writeInt(key1.getBytes().length);
        buf.writeInt(value1.getBytes().length);
        buf.writeBytes(key1.getBytes());
        buf.writeBytes(value1.getBytes());
        buf.writeInt(key2.getBytes().length);
        buf.writeInt(value2.getBytes().length);
        buf.writeBytes(key2.getBytes());
        buf.writeBytes(value2.getBytes());
        assertEquals(0, buf.readerIndex());
        ParameterDecodingContext ctx = DecodingHelper.decodeParameters(buf);
        assertEquals(ParameterDecodingState.ALL_PARAMETERS_READ, ctx.getState());
        assertEquals(totalBytes, ctx.getBytesRead());
        assertEquals("reader index should be untouched", 0, buf.readerIndex());
        assertEquals(value1, ctx.getValues().get(key1));
        assertEquals(value2, ctx.getValues().get(key2));
    }
    
    @Test
    public void canDecodeParametersFullZeroParams() {
        int totalBytes = 4 /* # params */;
        ByteBuf buf = Unpooled.buffer(0, totalBytes);
        buf.writeInt(0);
        assertEquals(0, buf.readerIndex());
        ParameterDecodingContext ctx = DecodingHelper.decodeParameters(buf);
        assertEquals(ParameterDecodingState.ALL_PARAMETERS_READ, ctx.getState());
        assertEquals(totalBytes, ctx.getBytesRead());
        assertEquals("reader index should be untouched", 0, buf.readerIndex());
        assertTrue(ctx.getValues().isEmpty());
    }
    
    @Test
    public void canDecodeFragementedParamNoNumParams() {
        ByteBuf buf = Unpooled.buffer(0, 0);
        assertEquals(0, buf.readerIndex());
        ParameterDecodingContext ctx = DecodingHelper.decodeParameters(buf);
        assertEquals(ParameterDecodingState.INCOMPLETE_PARAMS_LENGTH, ctx.getState());
        assertEquals(0, ctx.getBytesRead());
        assertEquals("reader index should be untouched", 0, buf.readerIndex());
    }
    
    /**
     * Fragmented test case where the length of the value is missing in the
     * buffer.
     */
    @Test
    public void canDecodeFragementedParamNoLengthValue() {
        ByteBuf buf = Unpooled.buffer(0, 8);
        buf.writeInt(1); // one parameter
        buf.writeInt(3); // length of key
        assertEquals(0, buf.readerIndex());
        ParameterDecodingContext ctx = DecodingHelper.decodeParameters(buf);
        assertEquals(ParameterDecodingState.INCOMPLETE_PARAM_KV_LENGTH, ctx.getState());
        assertEquals(4, ctx.getBytesRead()); // num params have been read
        assertEquals("reader index should be untouched", 0, buf.readerIndex());
    }
    
    /**
     * Fragmented test case where the length of both the key and value is
     * missing in the buffer.
     */
    @Test
    public void canDecodeFragementedParamNoLengthKey() {
        ByteBuf buf = Unpooled.buffer(0, 4);
        buf.writeInt(1); // one parameter
        assertEquals(0, buf.readerIndex());
        ParameterDecodingContext ctx = DecodingHelper.decodeParameters(buf);
        assertEquals(ParameterDecodingState.INCOMPLETE_PARAM_KV_LENGTH, ctx.getState());
        assertEquals(4, ctx.getBytesRead()); // num params have been read
        assertEquals("reader index should be untouched", 0, buf.readerIndex());
    }
    
    /**
     * Fragmented test case where the key/value data is missing from
     * the buffer.
     */
    @Test
    public void canDecodeFragementedParamNoDataForKeyValue() {
        ByteBuf buf = Unpooled.buffer(0, 12);
        buf.writeInt(1); // one parameter
        buf.writeInt(2); // length of key
        buf.writeInt(7); // length of value
        assertEquals(0, buf.readerIndex());
        ParameterDecodingContext ctx = DecodingHelper.decodeParameters(buf);
        assertEquals(ParameterDecodingState.INCOMPLETE_PARAM_KV_DATA, ctx.getState());
        assertEquals(12, ctx.getBytesRead()); // num params + key/value length have been read
        assertEquals("reader index should be untouched", 0, buf.readerIndex());
    }
    
    /**
     * Fragmented test case where the key/value data is missing from
     * the buffer.
     */
    @Test
    public void canDecodeFragementedParamNoLengthForSecondParam() {
        String firstKey = "first";
        String firstValue = "value";
        int bytesInBuf = 12 + firstKey.getBytes().length + firstValue.getBytes().length;
        ByteBuf buf = Unpooled.buffer(0, bytesInBuf);
        buf.writeInt(2); // two params
        buf.writeInt(firstKey.getBytes().length);
        buf.writeInt(firstValue.getBytes().length);
        buf.writeBytes(firstKey.getBytes());
        buf.writeBytes(firstValue.getBytes());
        assertEquals(0, buf.readerIndex());
        ParameterDecodingContext ctx = DecodingHelper.decodeParameters(buf);
        assertEquals(ParameterDecodingState.INCOMPLETE_PARAM_KV_LENGTH, ctx.getState());
        assertEquals(bytesInBuf, ctx.getBytesRead()); // read up to the first param
        assertEquals("reader index should be untouched", 0, buf.readerIndex());
    }
}

