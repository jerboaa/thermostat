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

package com.redhat.thermostat.common.utils;

public class DisplayableValues {

    private static final long BYTES_IN_KB = 1024;
    private static final long BYTES_IN_MB = 1024*BYTES_IN_KB;
    private static final long BYTES_IN_GB = 1024*BYTES_IN_MB;
    private static final long BYTES_IN_TB = 1024*BYTES_IN_GB;
    
    public enum Scale {
        B(1),
        KiB(BYTES_IN_KB),
        MiB(BYTES_IN_MB),
        GiB(BYTES_IN_GB),
        TiB(BYTES_IN_TB);

        private long numBytes;

        private Scale(long numBytes) {
            this.numBytes = numBytes;
        }

        public long getNumBytes() {
            return numBytes;
        }

        public static double convertTo(Scale scale, final long bytes) {
            
            return ((double) bytes) / ((double) scale.numBytes);
        }
        
        public static double convertTo(Scale scale, final long bytes, long roundTo) {
            
            double result = ((double) bytes) / ((double) scale.numBytes);
            result = ((double) Math.round(result * roundTo)) / roundTo;
            
            return result;
        }

        public static Scale getScale(final long bytes) {
            if (bytes < BYTES_IN_KB) {
                return Scale.B;
            } else if (bytes < BYTES_IN_MB) {
                return Scale.KiB;
            } else if (bytes < BYTES_IN_GB) {
                return Scale.MiB;
            } else if (bytes < BYTES_IN_TB) {
                return Scale.GiB;
            } else {
                return Scale.TiB;
            }
        }
    }
    
    private static final String DOUBLE_FORMAT_STRING = "%.1f";

    private DisplayableValues() {} // Not to be instantiated.

    public static String[] bytes(final long bytes) {
        if (bytes < BYTES_IN_KB) {
            return new String[] { String.valueOf(bytes), Scale.B.name() };
        } else if (bytes < BYTES_IN_MB) {
            return new String[] { String.format(DOUBLE_FORMAT_STRING, (double) bytes/BYTES_IN_KB), Scale.KiB.name() };
        } else if (bytes < BYTES_IN_GB) {
            return new String[] { String.format(DOUBLE_FORMAT_STRING, (double) bytes/BYTES_IN_MB), Scale.MiB.name() };
        } else if (bytes < BYTES_IN_TB) {
            return new String[] { String.format(DOUBLE_FORMAT_STRING, (double) bytes/BYTES_IN_GB), Scale.GiB.name() };
        } else {
            return new String[] { String.format(DOUBLE_FORMAT_STRING, (double) bytes/BYTES_IN_TB), Scale.TiB.name() };
        }
    }
}
