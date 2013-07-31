/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.model.Pojo;


public class QueuedBackingStorageTest {

    private static class TestExecutor implements ExecutorService {

        private Runnable task;
        private boolean shutdown;

        @Override
        public void execute(Runnable task) {
            this.task = task;
        }

        Runnable getTask() {
            return task;
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            // Not used.
            shutdown = true;
            return null;
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            // Not used.
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return true;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            // Not used.
            return null;
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            // Not used.
            return null;
        }

        @Override
        public Future<?> submit(Runnable task) {
            // Not used.
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(
                Collection<? extends Callable<T>> tasks)
                throws InterruptedException {
            // Not used.
            return null;
        }

        @Override
        public <T> List<Future<T>> invokeAll(
                Collection<? extends Callable<T>> tasks, long timeout,
                TimeUnit unit) throws InterruptedException {
            // Not used.
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            // Not used.
            return null;
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                long timeout, TimeUnit unit) throws InterruptedException,
                ExecutionException, TimeoutException {
            // Not used.
            return null;
        }

    }
    
    private QueuedBackingStorage queuedStorage;
    private BackingStorage delegateStorage;
    private Query<TestPojo> delegateQuery;

    private TestExecutor executor;
    private TestExecutor fileExecutor;

    private Cursor<TestPojo> expectedResults;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        executor = new TestExecutor();
        fileExecutor = new TestExecutor();
        delegateStorage = mock(BackingStorage.class);

        delegateQuery = (Query<TestPojo>) mock(Query.class);
        expectedResults = (Cursor<TestPojo>) mock(Cursor.class);
        when(delegateStorage.createQuery(any(Category.class))).thenReturn(delegateQuery);
        when(delegateQuery.execute()).thenReturn(expectedResults);
        
        queuedStorage = new QueuedBackingStorage(delegateStorage, executor, fileExecutor);
    }

    @After
    public void tearDown() {
        expectedResults = null;
        queuedStorage = null;
        delegateStorage = null;
        fileExecutor = null;
        executor = null;
        delegateQuery = null;
    }
    
    @Test
    public void testCreateQuery() {
        @SuppressWarnings("unchecked")
        Category<TestPojo> category = (Category<TestPojo>) mock(Category.class);
        Query<TestPojo> query = queuedStorage.createQuery(category);
        verify(delegateStorage).createQuery(category);
        verifyNoMoreInteractions(delegateStorage);

        Cursor<TestPojo> result = query.execute();
        verify(delegateQuery).execute();
        assertSame(expectedResults, result);

        assertNull(executor.getTask());
        assertNull(fileExecutor.getTask());
    }
    
    private static class TestPojo implements Pojo {
        
    }

}

