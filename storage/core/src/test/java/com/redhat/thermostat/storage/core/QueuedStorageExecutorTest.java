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


package com.redhat.thermostat.storage.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class QueuedStorageExecutorTest {

    private static final int NUM_THREADS = 50;
    private static final int TASK_DURATION_MS = 50;

    private QueuedStorage queuedStorage;

    private volatile int activeTasks;

    private CountDownLatch latch;

    private class TestRunnable implements Runnable {
        public void run() {
            synchronized (QueuedStorageExecutorTest.class) {
                activeTasks++;
                try {
                    Thread.sleep(TASK_DURATION_MS);
                } catch (InterruptedException e) {
                    // Get out of here ASAP.
                }
                activeTasks--;
            }
            latch.countDown();
        }
    }

    @Before
    public void setUp() {
        Storage mockStorage = mock(Storage.class);
        queuedStorage = new QueuedStorage(mockStorage);
        activeTasks = 0;
        latch = null;
    }

    @After
    public void tearDown() {
        queuedStorage = null;
        activeTasks = 0;
        latch = null;
    }

    @Test
    public void testMainExecutor() {
        testExecutor(queuedStorage.getExecutor());
    }

    @Test
    public void testFileExecutor() {
        testExecutor(queuedStorage.getFileExecutor());
    }

    private void testExecutor(final Executor executor) {
        latch = new CountDownLatch(NUM_THREADS);
        Thread[] threads = new Thread[NUM_THREADS]; 
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i] = new Thread() {
                public void run() {
                    executor.execute(new TestRunnable());
                }
            };
        }
        for (int i = 0; i < NUM_THREADS; i++) {
            threads[i].start();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            // Get out as soon as possible.
        }
        assertTrue(activeTasks == 0);
    }


    @Test
    public void testShutdown() {
        queuedStorage.shutdown();
        assertTrue(queuedStorage.getExecutor().isShutdown());
        assertTrue(queuedStorage.getFileExecutor().isShutdown());
    }
}

