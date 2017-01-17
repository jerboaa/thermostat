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

package com.redhat.thermostat.agent.ipc.tcpsocket.common.internal;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

class ChannelTestUtils {
    
    // Expected to be in-order position arguments
    static byte[][] splitByteArray(byte[] data, int... splitPos) {
        int numSplits = splitPos.length;
        if (numSplits == 0) {
            throw new IllegalArgumentException("Give at least one split position");
        }
        byte[][] result = new byte[numSplits + 1][];
        
        result[0] = Arrays.copyOfRange(data, 0, splitPos[0]);
        for (int i = 1; i < numSplits; i++) {
            result[i] = Arrays.copyOfRange(data, splitPos[i - 1], splitPos[i]);
        }
        result[numSplits] = Arrays.copyOfRange(data, splitPos[numSplits - 1], data.length);
        return result;
    }
    
    static byte[] joinByteArrays(byte[] first, byte[]... more) {
        if (more.length == 0) {
            return first;
        }
        
        int size = first.length;
        for (byte[] other : more) {
            size += other.length;
        }
        
        byte[] result = new byte[size];
        System.arraycopy(first, 0, result, 0, first.length);
        int ix = first.length;
        for (byte[] other : more) {
            System.arraycopy(other, 0, result, ix, other.length);
            ix += other.length;
        }
        return result;
    }
    
    static byte[] createHeader(int messageSize, boolean moreData) {
        MessageHeader header = new MessageHeader();
        header.setMessageSize(messageSize);
        header.setMoreData(moreData);
        return header.toByteArray();
    }
    
    static class ReadAnswer implements Answer<Integer> {
        
        private byte[][] bufs;
        private int count;
        
        ReadAnswer(byte[][] bufs) {
            this.bufs = bufs;
            this.count = 0;
        }

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            // read(ByteBuffer)
            ByteBuffer dst = (ByteBuffer) invocation.getArguments()[0];
            byte[] buf = bufs[count++];
            dst.put(buf);
            return buf.length;
        }
        
    }
    
    static byte[][] createByteArrays(int firstLength, int... moreLengths) {
        int numSplits = moreLengths.length + 1;
        byte[][] result = new byte[numSplits][];
        
        result[0] = new byte[firstLength];
        for (int i = 1; i < numSplits; i++) {
            result[i] = new byte[moreLengths[i - 1]];
        }
        return result;
    }
    
    static class WriteAnswer implements Answer<Integer> {
        
        private final byte[][] bufs;
        private int count;
        
        WriteAnswer(byte[][] bufs) {
            this.bufs = bufs;
            this.count = 0;
        }

        @Override
        public Integer answer(InvocationOnMock invocation) throws Throwable {
            // write(ByteBuffer)
            ByteBuffer src = (ByteBuffer) invocation.getArguments()[0];
            // Don't write all at once
            byte[] buf = bufs[count++];
            int numToCopy = Math.min(buf.length, src.remaining());
            for (int i = 0; i < numToCopy; i++) {
                buf[i] = src.get();
            }
            return numToCopy;
        }
        
    }

}
