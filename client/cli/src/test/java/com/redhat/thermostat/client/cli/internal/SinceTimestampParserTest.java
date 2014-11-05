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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class SinceTimestampParserTest {

    private SinceTimestampParser parser;
    private long startTimestamp;

    @Before
    public void setup() {
        startTimestamp = System.currentTimeMillis();
    }

    @Test
    public void testAllInput() throws SinceTimestampParser.InvalidSinceTimestampFormatException {
        parser = setupDefaultParser("all");
        long result = parser.parse();

        assertEquals(Long.MIN_VALUE, result);
    }

    @Test
    public void testDefaultStamp() throws SinceTimestampParser.InvalidSinceTimestampFormatException {
        long defaultTimestamp = 0l;

        parser = new SinceTimestampParser(null, startTimestamp, defaultTimestamp);
        long result = parser.parse();

        assertEquals(defaultTimestamp, result);
    }

    @Test
    public void testSecondsTimestamp() throws SinceTimestampParser.InvalidSinceTimestampFormatException {
        parser = setupDefaultParser("1:seconds");
        long result = parser.parse();

        assertEquals(startTimestamp - TimeUnit.SECONDS.toMillis(1), result);
    }

    @Test
    public void testMinutesTimestamp() throws SinceTimestampParser.InvalidSinceTimestampFormatException {
        parser = setupDefaultParser("2:minutes");
        long result = parser.parse();
        assertEquals(startTimestamp - TimeUnit.MINUTES.toMillis(2), result);
    }

    @Test
    public void testHoursTimestamp() throws SinceTimestampParser.InvalidSinceTimestampFormatException {
        parser = setupDefaultParser("3:hours");
        long result = parser.parse();
        assertEquals(startTimestamp - TimeUnit.HOURS.toMillis(3), result);
    }

    @Test
    public void testDaysTimestamp() throws SinceTimestampParser.InvalidSinceTimestampFormatException {
        parser = setupDefaultParser("4:days");
        long result = parser.parse();
        assertEquals(startTimestamp - TimeUnit.DAYS.toMillis(4), result);
    }

    @Test (expected = SinceTimestampParser.InvalidSinceTimestampFormatException.class)
    public void testIncorrectTime() throws SinceTimestampParser.InvalidSinceTimestampFormatException {
        parser = setupDefaultParser("0:minutes");
        parser.parse();
    }

    @Test (expected = SinceTimestampParser.InvalidSinceTimestampFormatException.class)
    public void testNotAcceptedUnit() throws SinceTimestampParser.InvalidSinceTimestampFormatException {
        parser = setupDefaultParser("5:milliseconds");
        parser.parse();
    }

    @Test (expected = SinceTimestampParser.InvalidSinceTimestampFormatException.class)
    public void testIncorrectArgument() throws SinceTimestampParser.InvalidSinceTimestampFormatException {
        parser = setupDefaultParser("aString");
        parser.parse();
    }

    @Test (expected = SinceTimestampParser.InvalidSinceTimestampFormatException.class)
    public void testIncorrectArgumentTwo() throws SinceTimestampParser.InvalidSinceTimestampFormatException {
        parser = setupDefaultParser("5:seconds:hello");
        parser.parse();
    }

    public SinceTimestampParser setupDefaultParser(String sinceTimestamp) {
        return new SinceTimestampParser(sinceTimestamp, startTimestamp, 0l);
    }

}