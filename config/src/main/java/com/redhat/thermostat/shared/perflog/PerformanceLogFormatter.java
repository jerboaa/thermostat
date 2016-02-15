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

package com.redhat.thermostat.shared.perflog;


/**
 * 
 * Log formatter for Thermostat internal performance metrics.
 * 
 * @see PerformanceLogFormatterBuilder
 *
 */
public interface PerformanceLogFormatter {
    
    /**
     * 
     * Log tags the Thermostat internal log analyzer knows about.
     *
     */
    public enum LogTag {
        
        // Note: Keep string values in sync with thermostat-perflog-analyzer's
        //       LogTag.
        
        /**
         * Log tag for storage related messages. In particular messages emitted
         * from backing proxied storage (usually from within the webapp).
         */
        STORAGE_BACKING_PROXIED("s-backing-proxied"),
        /**
         * Log tag for storage related messages. In particular messages emitted
         * from front-end storage.
         */
        STORAGE_FRONT_END("s-front-end"),
        ;
        
        private final String value;
        
        LogTag(String value) {
            this.value = value;
        }
        
        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * Format a log message with the given duration.
     * 
     * @param tag         Useful if logs are split across files
     *                    and should get combined or aggregated in some way via
     *                    this token. See also {@link LogTag#toString()}.
     * @param msg The message to log
     * @param durationInNanos
     * @return A formatted string suitable for logging at the
     *         {@link com.redhat.thermostat.common.utils.LoggingUtils.PERFLOG}
     *         level.
     */
    public String format(LogTag tag, String msg, long durationInNanos);
    
    /**
     * Format a log message.
     * 
     * @param tag Useful if logs are split across files
     *                    and should get combined or aggregated in some way via
     *                    this token. See also {@link LogTag#toString()}.
     * @param msg The message to log
     * @return A formatted string suitable for logging at the
     *         {@link com.redhat.thermostat.common.utils.LoggingUtils.PERFLOG}
     *         level.
     */
    public String format(LogTag tag, String msg);
}
