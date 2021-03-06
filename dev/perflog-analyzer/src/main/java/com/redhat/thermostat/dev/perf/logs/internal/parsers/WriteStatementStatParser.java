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

package com.redhat.thermostat.dev.perf.logs.internal.parsers;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.redhat.thermostat.dev.perf.logs.internal.Duration;
import com.redhat.thermostat.dev.perf.logs.internal.LineParseException;
import com.redhat.thermostat.dev.perf.logs.internal.LineStat;
import com.redhat.thermostat.dev.perf.logs.internal.LogTag;
import com.redhat.thermostat.dev.perf.logs.internal.MessageDuration;
import com.redhat.thermostat.dev.perf.logs.internal.SharedStatementState;
import com.redhat.thermostat.dev.perf.logs.internal.WriteStatementStat;

public class WriteStatementStatParser extends StatementStatParser {
    
    private static final String DB_WRITE_PREFIX = "DB_WRITE";
    private static final String REGEXP_MSG_PATTERN = DB_WRITE_PREFIX + "\\(([0-9]+)\\) (.*)";
    private static final Pattern MSG_PATTERN = Pattern.compile(REGEXP_MSG_PATTERN);
    private static final long ILLEGAL_VALUE = -300;

    public WriteStatementStatParser(SharedStatementState state) {
        super(state);
    }
    
    @Override
    public boolean matches(String msg) {
        return msg.startsWith(DB_WRITE_PREFIX);
    }

    @Override
    public LineStat parse(Date timestamp, boolean hasDuration, LogTag logToken,
            MessageDuration msg) throws LineParseException {
        String strMsg = msg.getMsg();
        Duration duration = msg.getDuration();
        Matcher matcher = MSG_PATTERN.matcher(strMsg);
        if (matcher.matches()) {
            String records = matcher.group(1);
            long recsAffected = ILLEGAL_VALUE; // should get set below
            try {
                recsAffected = Long.parseLong(records);
            } catch (NumberFormatException e) {
                throw new LineParseException("records affected failed to parse");
            }
            String descriptor = matcher.group(2);
            int descId = super.getDescriptorId(descriptor);
            return new WriteStatementStat(getState(), timestamp, logToken, descId, duration, recsAffected);
        }
        return null;
    }
}