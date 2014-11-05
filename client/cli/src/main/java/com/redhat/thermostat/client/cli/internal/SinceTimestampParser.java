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

package com.redhat.thermostat.client.cli.internal;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SinceTimestampParser {

    private final List<String> acceptedTimeUnits = Arrays.asList(new String[]{"DAYS", "HOURS", "MINUTES", "SECONDS"});

    private final String parseString;
    private final long defaultSinceTimestamp;
    private final long startTimestamp;


    public SinceTimestampParser(String parseString, long startTimestamp, long defaultSinceTimestamp) {
        this.parseString = parseString;
        this.startTimestamp = startTimestamp;
        this.defaultSinceTimestamp = defaultSinceTimestamp;
    }

    public long parse() throws InvalidSinceTimestampFormatException {
        long since;
        if (parseString != null) {
            since = parseString();
        } else {
            since = defaultSinceTimestamp;
        }
        return since;
    }

    private long parseString() throws InvalidSinceTimestampFormatException {
        if (parseString.equals("all")) {
            return Long.MIN_VALUE;
        } else {
            return parseValueFromString();
        }
    }

    private long parseValueFromString() throws InvalidSinceTimestampFormatException {
        try {
            String[] split = parseString.split(":");
            if (split.length != 2) {
                throw new InvalidSinceTimestampFormatException("Invalid input");
            }

            long timeValue = Long.valueOf(split[0]);

            String timeString = split[1];
            TimeUnit timeUnit = getTimeUnitFromString(timeString);

            long value = timeUnit.toMillis(timeValue);

            if (!(value > 0)) {
                throw new InvalidSinceTimestampFormatException("Time input must be greater than 0");
            }

            return startTimestamp - value;
        } catch (Exception e) {
            throw new InvalidSinceTimestampFormatException(e.getMessage());
        }
    }

    private TimeUnit getTimeUnitFromString(String timeUnit) throws InvalidSinceTimestampFormatException {
        timeUnit = timeUnit.toUpperCase();
        if (!acceptedTimeUnits.contains(timeUnit)) {
            throw new InvalidSinceTimestampFormatException("Invalid time unit. Accepted time units are: days, hours, minutes or seconds");
        }
        return TimeUnit.valueOf(timeUnit);
    }

    public static class InvalidSinceTimestampFormatException extends Exception {
        InvalidSinceTimestampFormatException(String message) {
            super(message);
        }
    }
}