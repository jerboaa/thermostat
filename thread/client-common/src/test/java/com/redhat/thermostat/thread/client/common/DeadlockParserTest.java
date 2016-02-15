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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import com.redhat.thermostat.thread.client.common.DeadlockParser;
import com.redhat.thermostat.thread.client.common.DeadlockParser.Information;
import com.redhat.thermostat.thread.client.common.DeadlockParser.Lock;
import com.redhat.thermostat.thread.client.common.DeadlockParser.ParseException;
import com.redhat.thermostat.thread.client.common.DeadlockParser.Thread;
import com.redhat.thermostat.thread.client.common.DeadlockParser.Thread.State;

public class DeadlockParserTest {

    @Test
    public void testParsingEmptyStream() throws Exception {
        try (BufferedReader reader = new BufferedReader(new CharArrayReader(new char[0]))) {
            Information result = new DeadlockParser().parse(reader);
            assertNotNull(result);
            assertNotNull(result.threads);
            assertTrue(result.threads.isEmpty());
        }
    }

    @Test
    public void testParsing() throws Exception {
        try (InputStream stream = this.getClass().getResourceAsStream("deadlock.output");
             InputStreamReader streamReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader);
        ) {
            Information result = new DeadlockParser().parse(reader);
            assertEquals(3, result.threads.size());
        }
    }

    @Test
    public void testParsingSingleThreadInfo() throws Exception {
        String trace = ""
                + "\"Alice\" Id=8 WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@602fe64a owned by \"Bob\" Id=9\n"
                + "\tat sun.misc.Unsafe.park(Native Method)\n"
                + "\t-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@602fe64a\n"
                + "\tat java.util.concurrent.locks.LockSupport.park(LockSupport.java:186)\n"
                + "\tat com.redhat.thermostat.foo.Bar.run(Bar.java:1337)\n"
                + "\t...\n"
                + "\n"
                + "\tNumber of locked synchronizers = 1\n"
                + "\t- java.util.concurrent.locks.ReentrantLock$NonfairSync@6bd8b476\n";
        try (BufferedReader reader = new BufferedReader(new StringReader(trace))) {
            Information parsed = new DeadlockParser().parse(reader);

            assertNotNull(parsed);
            assertEquals(1, parsed.threads.size());

            Thread thread = parsed.threads.get(0);
            assertEquals("Alice", thread.name);
            assertEquals("8", thread.id);
            assertEquals(State.WAITING, thread.state);
            assertEquals(new Lock("java.util.concurrent.locks.ReentrantLock$NonfairSync@602fe64a", "9"),
                    thread.waitingOn);

            assertEquals(1, thread.ownedLocks.size());
            assertEquals(new Lock("java.util.concurrent.locks.ReentrantLock$NonfairSync@6bd8b476", "8"),
                    thread.ownedLocks.get(0));

            List<String> stackTrace = thread.stackTrace;
            assertNotNull(stackTrace);
            assertEquals(3, stackTrace.size());
            assertTrue(stackTrace.get(2).contains("Bar.java:1337"));
        }
    }
    
    @Test
    public void testParsingMultipleThreadInfo() throws Exception {
        String trace = "" +
                "\"Mallory\" Id=12 WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7 owned by \"Alice\" Id=10\n" +
                "\tat sun.misc.Unsafe.park(Native Method)\n" +
                "\t-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7\n" +
                "\tat java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" +
                "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
                "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" +
                "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" +
                "\tat java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
                "\tat java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" +
                "\tat com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" +
                "\t...\n\n" +
                "\tNumber of locked synchronizers = 1\n" +
                "\t- java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2" +
                "\n\n\n" +
                "\"Alice\" Id=10 WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e owned by \"Bob\" Id=11\n" +
                "\tat sun.misc.Unsafe.park(Native Method)\n" +
                "\t-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e\n" + 
                "\tat java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" + 
                "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
                "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" +
                "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" +
                "\tat java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
                "\tat java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" + 
                "\tat com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" +
                "\t...\n\n" + 
                "\tNumber of locked synchronizers = 1\n" +
                "\t- java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7\n" +
                "\n\n" +
                "\"Bob\" Id=11 WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2 owned by \"Mallory\" Id=12\n" +
                "\tat sun.misc.Unsafe.park(Native Method)\n" +
                "\t-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2\n" +
                "\tat java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" + 
                "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
                "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" + 
                "\tat java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" + 
                "\tat java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
                "\tat java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" +
                "\tat com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" + 
                "\t...\n\n" +
                "\tNumber of locked synchronizers = 1\n" +
                "\t- java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e\n\n\n";
        doTestMultiTrace(trace);
    }
    
    @Test
    public void testParsingMultipleThreadInfoNoTabs() throws Exception {
        String trace = "" +
                "\"Mallory\" Id=12 WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7 owned by \"Alice\" Id=10\n" +
                "at sun.misc.Unsafe.park(Native Method)\n" +
                "-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7\n" +
                "at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" +
                "at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
                "at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" +
                "at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" +
                "at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
                "at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" +
                "at com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" +
                "...\n\n" +
                "Number of locked synchronizers = 1\n" +
                "- java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2" +
                "\n\n\n" +
                "\"Alice\" Id=10 WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e owned by \"Bob\" Id=11\n" +
                "at sun.misc.Unsafe.park(Native Method)\n" +
                "-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e\n" + 
                "at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" + 
                "at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
                "at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" +
                "at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" +
                "at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
                "at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" + 
                "at com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" +
                "...\n\n" + 
                "Number of locked synchronizers = 1\n" +
                "- java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7\n" +
                "\n\n" +
                "\"Bob\" Id=11 WAITING on java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2 owned by \"Mallory\" Id=12\n" +
                "at sun.misc.Unsafe.park(Native Method)\n" +
                "-  waiting on java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2\n" +
                "at java.util.concurrent.locks.LockSupport.park(LockSupport.java:175)\n" + 
                "at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:836)\n" +
                "at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:870)\n" + 
                "at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1199)\n" + 
                "at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:209)\n" +
                "at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:285)\n" +
                "at com.redhat.thermostat.tests.DeadLock$Philosopher.run(DeadLock.java:57)\n" + 
                "...\n\n" +
                "Number of locked synchronizers = 1\n" +
                "- java.util.concurrent.locks.ReentrantLock$NonfairSync@105ff84e\n\n\n";
        doTestMultiTrace(trace);
    }

    private void doTestMultiTrace(String trace)
            throws IOException, ParseException {
        try (BufferedReader reader = new BufferedReader(new StringReader(trace))) {
            Information parsed = new DeadlockParser().parse(reader);

            assertNotNull(parsed);
            assertEquals(3, parsed.threads.size());

            Thread mallory = parsed.threads.get(0);
            assertEquals("Mallory", mallory.name);
            assertEquals("12", mallory.id);
            assertEquals(State.WAITING, mallory.state);
            assertEquals(new Lock("java.util.concurrent.locks.ReentrantLock$NonfairSync@52de95c7", "10"),
                    mallory.waitingOn);

            assertEquals(1, mallory.ownedLocks.size());
            assertEquals(new Lock("java.util.concurrent.locks.ReentrantLock$NonfairSync@441634c2", "12"),
                    mallory.ownedLocks.get(0));

            List<String> stackTrace = mallory.stackTrace;
            assertNotNull(stackTrace);
            assertEquals(8, stackTrace.size());
            assertTrue(stackTrace.get(7).contains("DeadLock.java:57"));
            
            Thread bob = parsed.threads.get(2);
            assertEquals("Bob", bob.name);
            assertEquals("11", bob.id);
            assertEquals(State.WAITING, bob.state);
            assertEquals(1, bob.ownedLocks.size());
            List<String> bobStackTrace = bob.stackTrace;
            assertTrue(bobStackTrace.get(7).contains("Philosopher.run"));
        }
    }

}
