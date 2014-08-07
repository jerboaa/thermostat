/*
 * Copyright 2012-2014 Red Hat, Inc.
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
package com.redhat.thermostat.storage.internal;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.storage.core.PerformanceLogger;
import com.redhat.thermostat.storage.core.PerformanceLoggerBuilder;

/**
 * @see PerformanceLogger
 * @see PerformanceLoggerBuilder
 */
public class PerformanceLoggerImpl implements PerformanceLogger, AutoCloseable, Closeable {
    
    private static final char MICRO_SIGN = '\u00b5';
    private static final String DAYS_SHORT = "days";
    private static final String HOURS_SHORT = "hours";
    private static final String MINUTES_SHORT = "mins";
    private static final String SECONDS_SHORT = "s";
    private static final String MILLIS_SHORT = "ms";
    private static final String MICROS_SHORT = MICRO_SIGN + "s";
    private static final String NANOS_SHORT = "ns";
    // unique-token, msg, duration
    private static final String FORMAT_WITH_DURATION = "1|%s|%s|%s";
    // unique-token, msg
    private static final String FORMAT_NO_DURATION = "0|%s|%s";
    private final File filename;
    private final TimeUnit timeUnit;
    private final String durationFormat;
    private boolean isClosed;
    private TimeStampedPrintWriter pw;
    
    public PerformanceLoggerImpl(File filename, TimeUnit timeUnit) {
        this.filename = filename;
        this.timeUnit = timeUnit;
        this.durationFormat = getDurationFormat(timeUnit);
        initOutStreams();
    }

    private String getDurationFormat(TimeUnit unit) {
        String format = "%s ";
        switch (unit) {
        case DAYS:
            return format + DAYS_SHORT;
        case HOURS:
            return format + HOURS_SHORT;
        case MINUTES:
            return format + MINUTES_SHORT;
        case SECONDS:
            return format + SECONDS_SHORT;
        case MILLISECONDS:
            return format + MILLIS_SHORT;
        case MICROSECONDS:
            return format + MICROS_SHORT;
        case NANOSECONDS:
            return format + NANOS_SHORT;
        default:
            return format;
        }
    }

    private void initOutStreams() {
        try {
            FileOutputStream fout = new FileOutputStream(filename, true);
            this.pw = new TimeStampedPrintWriter(fout);
            this.isClosed = false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void log(String uniqueToken, String msg, long durationInNanos) {
        String duration = getFormattedDuration(durationInNanos);
        String logMsg = String.format(FORMAT_WITH_DURATION, uniqueToken, msg, duration);
        doLog(logMsg);
    }
    
    @Override
    public void log(String uniqueToken, String msg) {
        String logMsg = String.format(FORMAT_NO_DURATION, uniqueToken, msg);
        doLog(logMsg);
    }

    private String getFormattedDuration(long durationInNanos) {
        long convertedTime = timeUnit.convert(durationInNanos, TimeUnit.NANOSECONDS);
        return String.format(durationFormat, convertedTime);
    }

    private synchronized void doLog(String logMsg) {
        if (isClosed) {
            initOutStreams();
        }
        pw.println(logMsg);
        pw.flush();
    }

    @Override
    public synchronized void close() throws IOException {
        if (isClosed) {
            return;
        }
        pw.close();
        this.isClosed = true;
    }

}
