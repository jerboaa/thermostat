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

package com.redhat.thermostat.dev.perf.logs.internal;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.dev.perf.logs.internal.parsers.LineStatParser;
import com.redhat.thermostat.dev.perf.logs.internal.parsers.QueueStatParser;
import com.redhat.thermostat.dev.perf.logs.internal.parsers.ReadStatementStatParser;
import com.redhat.thermostat.dev.perf.logs.internal.parsers.WriteStatementStatParser;

class StatsParserBuilder {

    static StatsParser build() {
        return new StatsParserImpl(new SharedStatementState());
    }
    
    // package private for testing
    static class StatsParserImpl implements StatsParser {

        private static final String PERFLOG_PREFIX = "PERFLOG";
        private static final char MICRO_SIGN = '\u00b5';
        private static final String DAYS = "days";
        private static final String HOURS = "hours";
        private static final String MINUTES = "mins";
        private static final String SECONDS = "s";
        private static final String MILLIS = "ms";
        private static final String MICROS = MICRO_SIGN + "s";
        private static final String NANOS = "ns";
        private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        private static final DateFormat FORMAT = new SimpleDateFormat(DATE_FORMAT);
        private static final String VERTICAL_BAR = "\\|";
        private static final char COLON = ':';
        private final SharedStatementState state;
        private List<LineStatParser> parsers;
        
        private StatsParserImpl(SharedStatementState state) {
           parsers = new ArrayList<>(3);
           parsers.add(new QueueStatParser());
           parsers.add(new ReadStatementStatParser(state));
           parsers.add(new WriteStatementStatParser(state));
           this.state = state;
        }
        
        @Override
        public LineStat parse(String line) {
            if (!line.startsWith(PERFLOG_PREFIX)) {
                // Ignore non-perf log entries.
                return null;
            }
            try {
                TokenizedLine tokens = splitIntoTokens(extractMessage(line));
                MessageDuration md = tokens.getMessageDuration();
                String rawMessage = md.getMsg();
                for (LineStatParser p: parsers) {
                    if (p.matches(rawMessage)) {
                        return p.parse(tokens.getDate(), tokens.hasDuration(),
                                tokens.getLogToken(), md);
                    }
                }
                System.err.println("WARNING: No parser matched for: " + rawMessage);
            } catch (LineParseException e) {
                System.err.println(e.getMessage() + " log line was: '" + line + "'");
            }
            return null;
        }

        String extractMessage(String line) {
            int messageStart = line.indexOf(COLON) + 2;
            return line.substring(messageStart);
        }

        // package-private for testing
        TokenizedLine splitIntoTokens(String line) throws LineParseException {
            // Lines have format <timestamp>|(0|1)|<log_token>|msg|duration
            String[] tokens = line.split(VERTICAL_BAR);
            if (tokens.length < 4 || tokens.length > 5) {
                throw new LineParseException("Unexpected number of tokens. # of tokens was " + tokens.length);
            }
            try {
                Date date = FORMAT.parse(tokens[0]);
                boolean hasDuration = tokens[1].equals("1");
                LogTag logToken = LogTag.parseFromStringForm(tokens[2]);
                String msg = tokens[3];
                MessageDuration md = null;
                if (hasDuration) {
                    String duration = tokens[4];
                    Duration dur = parseDuration(duration);
                    md = new MessageDuration(msg, dur);
                } else {
                    md = new MessageDuration(msg);
                }
                return new TokenizedLine(date, hasDuration, logToken, md);
            } catch (ParseException e) {
                throw new LineParseException("Failed to parse timestamp");
            } catch (Exception e) {
                e.printStackTrace();
                throw new LineParseException("Unexpected parse exception");
            }
        }

        // package-private for testing
        Duration parseDuration(String duration) throws LineParseException {
            String[] tokens = duration.split(" ");
            if (tokens.length != 2) {
                throw new LineParseException("illegal duration format");
            }
            long val = -300;
            try {
                val = Long.parseLong(tokens[0]);
            } catch (NumberFormatException e) {
                throw new LineParseException("Duration value not a long.");
            }
            TimeUnit tu = parseTimeUnit(tokens[1]);
            return new Duration(val, tu);
        }

        private TimeUnit parseTimeUnit(String string) {
            switch(string) {
            case DAYS:
                return TimeUnit.DAYS;
            case MINUTES:
                return TimeUnit.MINUTES;
            case HOURS:
                return TimeUnit.HOURS;
            case SECONDS:
                return TimeUnit.SECONDS;
            case MILLIS:
                return TimeUnit.MILLISECONDS;
            case MICROS:
                return TimeUnit.MICROSECONDS;
            case NANOS:
                return TimeUnit.NANOSECONDS;
            default:
                return null;
            }
        }

        @Override
        public SharedStatementState getSharedState() {
            return state;
        }   
        
    }
}
