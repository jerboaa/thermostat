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

package com.redhat.thermostat.vm.jmx.client.cli.internal;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.vm.jmx.client.cli.locale.LocaleResources;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationsSinceParser {

    public static final long NO_SINCE_OPTION = -1L;

    private static final Pattern SINCE_SECONDS_OFFSET_PATTERN = Pattern.compile("^([0-9]+)s$");
    private static final Pattern SINCE_MINUTES_OFFSET_PATTERN = Pattern.compile("^([0-9]+)m$");
    private static final Pattern SINCE_HOURS_OFFSET_PATTERN = Pattern.compile("^([0-9]+)h$");
    private static final Pattern SINCE_DAYS_OFFSET_PATTERN = Pattern.compile("^([0-9]+)d$");

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private final Clock clock;

    public NotificationsSinceParser(Clock clock) {
        this.clock = clock;
    }

    public long parse(String sinceArg) throws CommandException {
        try {
            if (hasSinceSecondsArgument(sinceArg)) {
                return getSinceSecondsArgument(sinceArg);
            } else if (hasSinceMinutesArgument(sinceArg)) {
                return getSinceMinutesArgument(sinceArg);
            } else if (hasSinceHoursArgument(sinceArg)) {
                return getSinceHoursArgument(sinceArg);
            } else if (hasSinceDaysArgument(sinceArg)) {
                return getSinceDaysArgument(sinceArg);
            }
            long parsed = Long.parseLong(sinceArg);
            if (parsed < NO_SINCE_OPTION) {
                parsed = NO_SINCE_OPTION;
            }
            return parsed;
        } catch (NumberFormatException nfe) {
            throw new CommandException(t.localize(LocaleResources.UNRECOGNIZED_SINCE_FORMAT), nfe);
        }
    }

    private boolean hasSinceSecondsArgument(String s) {
        return SINCE_SECONDS_OFFSET_PATTERN.matcher(s).matches();
    }

    private long getSinceSecondsArgument(String s) {
        Matcher matcher = SINCE_SECONDS_OFFSET_PATTERN.matcher(s);
        verifyMatcher(matcher);
        long seconds = Long.parseLong(matcher.group(1));
        return clock.getRealTimeMillis() - TimeUnit.SECONDS.toMillis(seconds);
    }

    private boolean hasSinceMinutesArgument(String s) {
        return SINCE_MINUTES_OFFSET_PATTERN.matcher(s).matches();
    }

    private long getSinceMinutesArgument(String s) {
        Matcher matcher = SINCE_MINUTES_OFFSET_PATTERN.matcher(s);
        verifyMatcher(matcher);
        long minutes = Long.parseLong(matcher.group(1));
        return clock.getRealTimeMillis() - TimeUnit.MINUTES.toMillis(minutes);
    }

    private boolean hasSinceHoursArgument(String s) {
        return SINCE_HOURS_OFFSET_PATTERN.matcher(s).matches();
    }

    private long getSinceHoursArgument(String s) {
        Matcher matcher = SINCE_HOURS_OFFSET_PATTERN.matcher(s);
        verifyMatcher(matcher);
        long hours = Long.parseLong(matcher.group(1));
        return clock.getRealTimeMillis() - TimeUnit.HOURS.toMillis(hours);
    }

    private boolean hasSinceDaysArgument(String s) {
        return SINCE_DAYS_OFFSET_PATTERN.matcher(s).matches();
    }

    private long getSinceDaysArgument(String s) {
        Matcher matcher = SINCE_DAYS_OFFSET_PATTERN.matcher(s);
        verifyMatcher(matcher);
        long days = Long.parseLong(matcher.group(1));
        return clock.getRealTimeMillis() - TimeUnit.DAYS.toMillis(days);
    }

    private void verifyMatcher(Matcher matcher) {
        if (!matcher.matches()) {
            throw new AssertionError("Invalid \"since\" format");
        }
    }

}
