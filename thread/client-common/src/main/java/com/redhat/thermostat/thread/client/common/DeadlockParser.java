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

package com.redhat.thermostat.thread.client.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeadlockParser {

    @SuppressWarnings("serial")
    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class Information {
        public final List<Thread> threads;

        public Information(List<Thread> threads) {
            this.threads = threads;
        }
    }

    public static class Thread {

        public enum State {
            WAITING;
        }

        public final String name;
        public final String id;
        public final State state;
        public final List<String> stackTrace;
        public final List<Lock> ownedLocks;
        public final Lock waitingOn;

        public Thread(String id, String name, State state,
                List<String> stackTrace,
                List<Lock> ownedLocks, Lock waitingOn) {
            this.id = id;
            this.name = name;
            this.state = state;
            this.stackTrace = stackTrace;
            this.ownedLocks = ownedLocks;
            this.waitingOn = waitingOn;
        }
    }

    public static class Lock {
        public final String name;
        public final String ownerId;

        public Lock(String name, String ownerId) {
            this.name = name;
            this.ownerId = ownerId;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (!this.getClass().equals(obj.getClass())) {
                return false;
            }
            Lock other = (Lock) obj;
            return Objects.equals(other.name, this.name) &&
                    Objects.equals(other.ownerId, this.ownerId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.ownerId);
        }
    }

    public Information parse(File file) throws IOException, ParseException {
        try (Reader reader = new FileReader(file)) {
            return parse(reader);
        }
    }

    public Information parse(Reader reader) throws IOException, ParseException {
        try (BufferedReader buffered = new BufferedReader(reader)) {
            return parse(buffered);
        }
    }

    public Information parse(BufferedReader reader) throws IOException, ParseException {
        List<Thread> threads = new ArrayList<>();

        while (true) {
            Thread threadInfo = parseThread(reader);
            if (threadInfo == null) {
                break;
            }

            threads.add(threadInfo);
        }

        return new Information(threads);
    }

    private Thread parseThread(BufferedReader reader) throws IOException, ParseException {
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        String regex = "\"(.*)\" Id=(\\d+) (WAITING) on (.*) owned by \"(.*)\" Id=(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);
        if (!matcher.matches()) {
            throw new ParseException("Failed to parse: '" + line + "'");
        }
        String name = matcher.group(1);
        String id = matcher.group(2);
        Thread.State state = Thread.State.valueOf(matcher.group(3));
        Lock waitingOn = new Lock(matcher.group(4), matcher.group(6));

        List<String> stackTrace = new ArrayList<>();

        // read stack trace
        while (true) {
            line = reader.readLine();

            if (line == null || line.equals("")) {
                break;
            }
            line = line.trim();

            if (line.startsWith("at ")) {
                stackTrace.add(line.substring("at ".length()));
            } else if (line.startsWith("-  waiting on")) {
                // ignore this
            } else if (line.startsWith("...")) {
                // nothing to do
            } else {
                throw new ParseException("Unrecognized input: '" + line + "'");
            }
        }

        final String TEXT_BEFORE = "Number of locked synchronizers = ";
        List<Lock> ownedLocks = new ArrayList<>();
        int acquiredLocks = 0;
        // read locks
        while (true) {
            line = reader.readLine();
            if (line == null || line.equals("")) {
                break;
            }
            line = line.trim();

            if (line.startsWith(TEXT_BEFORE)) {
                acquiredLocks = Integer.valueOf(line.substring(TEXT_BEFORE.length()));
            } else if (line.startsWith("- ")){
                ownedLocks.add(new Lock(line.substring("- ".length()), id));
            }
        }

        if (acquiredLocks != ownedLocks.size()) {
            throw new AssertionError("Incorrectly parsed owned locks");
        }

        /* discard = */ reader.readLine();

        return new Thread(id, name, state, stackTrace, ownedLocks, waitingOn);
    }

}
