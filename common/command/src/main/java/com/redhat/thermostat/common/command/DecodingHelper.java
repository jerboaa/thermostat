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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class DecodingHelper {
    
    /**
     * Attempts to decode a String from the given buffer, carefully not changing
     * the reader index of the buffer. The returned decoding context will tell
     * you if the decoding has fully completed and if so how many bytes have
     * been consumed. It's the callers responsibility to free the buffer's
     * resources. Set the {@link ByteBuf#readerIndex(int)} to the value as
     * returned by the decoding context and then discard bytes using
     * {@link ByteBuf#discardReadBytes()}.
     * 
     * @param buffer
     *            The buffer from which to decode the String from.
     * 
     * @return A decoding context giving you details about the decoding status
     *         and a way to retrieve the decoded value.
     */
    public static StringDecodingContext decodeString(ByteBuf buffer) {
        StringDecodingContext ctx = new StringDecodingContext();
        return decodeString(buffer, ctx);
    }
    
    private static StringDecodingContext decodeString(ByteBuf buffer, StringDecodingContext ctx) {
        if (buffer.readableBytes() < 4) {
            ctx.setState(StringDecodingState.INCOMPLETE_LENGTH_VAL);
            return ctx;
        }
        int length = buffer.getInt(buffer.readerIndex());
        ctx.addToBytesRead(4);
        ctx.setState(StringDecodingState.LENGTH_READ);
        return decodeString(length, buffer, ctx);
    }

    /**
     * Attempts to decode String parameters from the given buffer, carefully not
     * changing the reader index of the buffer. The returned decoding context
     * will tell you if the decoding has fully completed and if so how many
     * bytes have been consumed. It's the callers responsibility to free the
     * buffer's resources. Set the {@link ByteBuf#readerIndex(int)} to the value
     * as returned by the decoding context and then discard bytes using
     * {@link ByteBuf#discardReadBytes()}.
     * 
     * @param buffer The buffer from which to decode the parameters from.
     * @return A decoding context giving you details about the decoding status
     *         and a way to retrieve the decoded parameters.
     */
    public static ParameterDecodingContext decodeParameters(ByteBuf buffer) {
        ParameterDecodingContext ctx = new ParameterDecodingContext();
        return decodeParameters(buffer, ctx);
    }
    
    private static ParameterDecodingContext decodeParameters(ByteBuf buffer, ParameterDecodingContext ctx) {
        if (buffer.readableBytes() < 4) {
            ctx.setState(ParameterDecodingState.INCOMPLETE_PARAMS_LENGTH);
            return ctx;
        }
        int numParms = buffer.getInt(buffer.readerIndex());
        ctx.addToBytesRead(4);
        ctx.setState(ParameterDecodingState.PARAMS_LENGTH_READ);
        for (int i = 0; i < numParms; i++) {
            decodeParameter(buffer, ctx);
        }
        if ( (ctx.getState() == ParameterDecodingState.PARAMS_LENGTH_READ && numParms == 0)
             || ctx.getState() == ParameterDecodingState.PARAM_KV_DATA_PLUS_ONE_READ) {
            // Either zero parameters, or all params successfully read
            ctx.setState(ParameterDecodingState.ALL_PARAMETERS_READ);
        }
        return ctx;
    }

    private static void decodeParameter(ByteBuf buffer, ParameterDecodingContext ctx) {
        if (buffer.readableBytes() < ctx.getBytesRead() + 8) {
            ctx.setState(ParameterDecodingState.INCOMPLETE_PARAM_KV_LENGTH);
            return;
        }
        int currIdx = buffer.readerIndex() + ctx.getBytesRead();
        int nameLength = buffer.getInt(currIdx);
        int valueLength = buffer.getInt(currIdx + 4);
        ctx.setState(ParameterDecodingState.PARAM_KV_LENGTH_READ);
        ctx.addToBytesRead(8);
        int nameStartIdx = buffer.readerIndex() + ctx.getBytesRead();
        ByteBuf nameBuf = buffer.slice(nameStartIdx, buffer.readableBytes() - nameStartIdx);
        StringDecodingContext nameCtx = decodeString(nameLength, nameBuf, new StringDecodingContext());
        if (nameCtx.getState() != StringDecodingState.VALUE_READ) {
            ctx.setState(ParameterDecodingState.INCOMPLETE_PARAM_KV_DATA);
            return;
        }
        String name = nameCtx.getValue();
        ctx.addToBytesRead(nameCtx.getBytesRead());
        int valueStartIdx = buffer.readerIndex() + ctx.getBytesRead();
        ByteBuf valueBuf = buffer.slice(valueStartIdx, buffer.readableBytes() - valueStartIdx);
        StringDecodingContext valueCtx = decodeString(valueLength, valueBuf, new StringDecodingContext());
        if (valueCtx.getState() != StringDecodingState.VALUE_READ) {
            ctx.setState(ParameterDecodingState.INCOMPLETE_PARAM_KV_DATA);
            return;
        }
        String value = valueCtx.getValue();
        ctx.addToBytesRead(valueCtx.getBytesRead());
        ctx.setState(ParameterDecodingState.PARAM_KV_DATA_PLUS_ONE_READ);
        ctx.addParameter(name, value);
    }

    private static StringDecodingContext decodeString(int length, ByteBuf buffer, StringDecodingContext ctx) {
        if (buffer.readableBytes() < ctx.getBytesRead() + length) {
            ctx.setState(StringDecodingState.INCOMPLETE_STR_VAL);
            return ctx;
        }
        int startIdx = buffer.readerIndex() + ctx.getBytesRead();
        ByteBuf valueBuf = buffer.slice(startIdx, length);
        byte[] stringBytes = Unpooled.copiedBuffer(valueBuf).array();
        ctx.setState(StringDecodingState.VALUE_READ);
        ctx.addToBytesRead(length);
        ctx.setValue(new String(stringBytes));
        return ctx;
    }
}

