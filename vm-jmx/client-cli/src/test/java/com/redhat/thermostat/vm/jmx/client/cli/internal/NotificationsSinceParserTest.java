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

package com.redhat.thermostat.vm.jmx.client.cli.internal;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.cli.CommandException;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotificationsSinceParserTest {

    private static final long FAKE_CURRENT_TIMESTAMP = 100_000_000L;
    private Clock clock;
    private NotificationsSinceParser parser;

    @Before
    public void setup() {
        clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(FAKE_CURRENT_TIMESTAMP);
        parser = new NotificationsSinceParser(clock);
    }

    @Test
    public void testPlainLongTimestamp() throws CommandException {
        long result = parser.parse("100");
        assertThat(result, is(100L));
    }

    @Test
    public void testNegativeLongTimestamp() throws CommandException {
        long result = parser.parse("-5");
        assertThat(result, is(-1L));
    }

    @Test(expected = CommandException.class)
    public void testInvalidOffsetSuffix() throws CommandException {
        parser.parse("1y"); // we don't support "in the last year" as an offset
    }

    @Test(expected = CommandException.class)
    public void testLongTimestampTooLarge() throws CommandException {
        String timestamp = String.valueOf(Long.MAX_VALUE) + "0"; // 10x Long.MAX_VALUE
        parser.parse(timestamp);
    }

    @Test
    public void testSecondsOffset() throws CommandException {
        long result = parser.parse("5s");
        long delta = TimeUnit.SECONDS.toMillis(5);
        assertThat(result, is(FAKE_CURRENT_TIMESTAMP - delta));
    }

    @Test(expected = CommandException.class)
    public void testSecondsOffsetDoesNotAllowNegativeOffsets() throws CommandException {
        parser.parse("-5s");
    }

    @Test(expected = CommandException.class)
    public void testSecondsOffsetRequiresIntegerOffsets() throws CommandException {
        parser.parse("5.5s");
    }

    @Test(expected = CommandException.class)
    public void testSecondsOffsetIsCaseSensitive() throws CommandException {
        parser.parse("5S");
    }

    @Test
    public void testMinutesOffset() throws CommandException {
        long result = parser.parse("5m");
        long delta = TimeUnit.MINUTES.toMillis(5);
        assertThat(result, is(FAKE_CURRENT_TIMESTAMP - delta));
    }

    @Test(expected = CommandException.class)
    public void testMinutesOffsetDoesNotAllowNegativeOffsets() throws CommandException {
        parser.parse("-5m");
    }

    @Test(expected = CommandException.class)
    public void testMinutesOffsetRequiresIntegerOffsets() throws CommandException {
        parser.parse("5.5m");
    }

    @Test(expected = CommandException.class)
    public void testMinutesOffsetIsCaseSensitive() throws CommandException {
        parser.parse("5M");
    }

    @Test
    public void testHoursOffset() throws CommandException {
        long result = parser.parse("5h");
        long delta = TimeUnit.HOURS.toMillis(5);
        assertThat(result, is(FAKE_CURRENT_TIMESTAMP - delta));
    }

    @Test(expected = CommandException.class)
    public void testHoursOffsetDoesNotAllowNegativeOffsets() throws CommandException {
        parser.parse("-5h");
    }

    @Test(expected = CommandException.class)
    public void testHoursOffsetRequiresIntegerOffsets() throws CommandException {
        parser.parse("5.5h");
    }

    @Test(expected = CommandException.class)
    public void testHoursOffsetIsCaseSensitive() throws CommandException {
        parser.parse("5H");
    }

    @Test
    public void testDaysOffset() throws CommandException {
        long result = parser.parse("5d");
        long delta = TimeUnit.DAYS.toMillis(5);
        assertThat(result, is(FAKE_CURRENT_TIMESTAMP - delta));
    }

    @Test(expected = CommandException.class)
    public void testDaysOffsetDoesNotAllowNegativeOffsets() throws CommandException {
        parser.parse("-5d");
    }

    @Test(expected = CommandException.class)
    public void testDaysOffsetRequiresIntegerOffsets() throws CommandException {
        parser.parse("5.5d");
    }

    @Test(expected = CommandException.class)
    public void testDaysOffsetIsCaseSensitive() throws CommandException {
        parser.parse("5D");
    }

}
