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

package com.redhat.thermostat.vm.profiler.agent.jvm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ProfileRecorder {

    private static final ProfileRecorder profileRecorder = new ProfileRecorder(new TimeSource());

    /** shared between threads */
    private ConcurrentHashMap<String, AtomicLong> profileData = new ConcurrentHashMap<String, AtomicLong>();

    // TODO deal with thread id wrap-around

    /**
     * thread id -> info.
     * <p>
     * only the thread with the matching thread id is allowed to mutate 'info'
     */
    private Map<Long, Info> threads = new ConcurrentHashMap<Long, Info>();

    private final TimeSource timeSource;

    final static class Info {
        public Deque<String> stackFrames = new ArrayDeque<String>();
        public long timeStamp = Long.MIN_VALUE;
    }

    /** for testing */
    static class TimeSource {
        public long nanoTime() {
            return System.nanoTime();
        }
    }

    /** for testing only */
    ProfileRecorder(TimeSource timeSource) {
        this.timeSource = timeSource;
    }

    public static ProfileRecorder getInstance() {
        return profileRecorder;
    }

    /** called by instrumented code on every method enter */
    public void enterMethod(String fullyQualifiedName) {
        long currentTime = timeSource.nanoTime();
        long threadId = Thread.currentThread().getId();

        Info info = threads.get(threadId);

        if (info == null) {
            info = new Info();
            threads.put(threadId, info);
        }

        if (info.stackFrames.size() != 0) {
            // update time for previous method on the stack
            Long oldTime = info.timeStamp;
            long diff = currentTime - oldTime;
            addData(info.stackFrames.peek(), diff);
        }

        info.timeStamp = currentTime;
        info.stackFrames.push(fullyQualifiedName);
    }

    /** called by instrumented code on every method exit */
    public void exitMethod(String fullyQualifiedName) {
        long currentTime = timeSource.nanoTime();
        long threadId = Thread.currentThread().getId();

        Info info = threads.get(threadId);
        Queue<String> stack = info.stackFrames;

        Long oldTime = info.timeStamp;
        long diff = currentTime - oldTime;

        if (!fullyQualifiedName.equals(stack.peek())) {
            throw new AssertionError("should not happen:\n"
                    + "name: '" + fullyQualifiedName + "'\n"
                    + "stack top: '" + stack.peek() + "'\n"
                    + "stack: " + stack);
        }

        addData(stack.poll(), diff);

        info.timeStamp = currentTime;
    }

    private void addData(String dataName, long time) {
        AtomicLong value = profileData.get(dataName);
        if (value == null) {
            value = profileData.putIfAbsent(dataName, new AtomicLong(time));
        }
        if (value != null) {
            value.addAndGet(time);
        }
    }

    public Map<String, AtomicLong> getData() {
        return profileData;
    }

    public void clearData() {
        profileData.clear();
    }

}
