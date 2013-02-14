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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
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


public class QueuedStorageTest {

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
        public boolean awaitTermination(long timeout, TimeUnit unit)
                throws InterruptedException {
            // Not used.
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

    private static class TestPojo implements Pojo {
        
    }

    private QueuedStorage queuedStorage;
    private Storage delegateStorage;
    private Add delegateAdd;
    private Replace delegateReplace;
    private Query<?> delegateQuery;

    private TestExecutor executor;
    private TestExecutor fileExecutor;

    @SuppressWarnings("rawtypes")
    private Cursor expectedResults;
    private InputStream expectedFile;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        executor = new TestExecutor();
        fileExecutor = new TestExecutor();
        delegateStorage = mock(Storage.class);

        delegateAdd = mock(Add.class);
        delegateReplace = mock(Replace.class);

        Remove remove = mock(Remove.class);
        delegateQuery = mock(Query.class);
        when(delegateStorage.createAdd(any(Category.class))).thenReturn(delegateAdd);
        when(delegateStorage.createReplace(any(Category.class))).thenReturn(delegateReplace);
        when(delegateStorage.createRemove()).thenReturn(remove);
        when(delegateStorage.createQuery(any(Category.class))).thenReturn(delegateQuery);
        expectedResults = mock(Cursor.class);
        when(delegateQuery.execute()).thenReturn(expectedResults);
        when(delegateStorage.getCount(any(Category.class))).thenReturn(42l);
        expectedFile = mock(InputStream.class);
        when(delegateStorage.loadFile(anyString())).thenReturn(expectedFile);
        when(delegateStorage.getAgentId()).thenReturn("huzzah");
        queuedStorage = new QueuedStorage(delegateStorage, executor, fileExecutor);
        
    }

    @After
    public void tearDown() {
        expectedFile = null;
        expectedResults = null;
        queuedStorage = null;
        delegateStorage = null;
        fileExecutor = null;
        executor = null;
        delegateQuery = null;
    }

    @Test
    public void testInsert() {
        Category<?> category = mock(Category.class);
        Pojo pojo = mock(Pojo.class);

        Put put = queuedStorage.createReplace(category);
        put.setPojo(pojo);
        put.apply();

        Runnable r = executor.getTask();
        assertNotNull(r);
        verifyZeroInteractions(delegateStorage);
        verifyZeroInteractions(delegateReplace);

        r.run();
        verify(delegateStorage).createReplace(category);
        verify(delegateReplace).setPojo(pojo);
        verify(delegateReplace).apply();
        verifyNoMoreInteractions(delegateStorage);

        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testUpdatePojo() {
        Update delegateUpdate = mock(Update.class);
        when(delegateStorage.createUpdate(any(Category.class))).thenReturn(delegateUpdate);

        Category<?> category = mock(Category.class);

        Update update = queuedStorage.createUpdate(category);
        verify(delegateStorage).createUpdate(category);
        verifyNoMoreInteractions(delegateStorage);

        update.apply();

        Runnable r = executor.getTask();
        assertNotNull(r);
        verifyZeroInteractions(delegateUpdate);
        r.run();
        verify(delegateUpdate).apply();
        verifyNoMoreInteractions(delegateUpdate);

        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testRemovePojo() {

        Remove remove = queuedStorage.createRemove();
        verify(delegateStorage).createRemove();
        verifyNoMoreInteractions(delegateStorage);

        queuedStorage.removePojo(remove);

        Runnable r = executor.getTask();
        assertNotNull(r);
        verifyZeroInteractions(delegateStorage);
        r.run();
        verify(delegateStorage, times(1)).removePojo(remove);
        verifyNoMoreInteractions(delegateStorage);

        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testPurge() {

        queuedStorage.purge();

        Runnable r = executor.getTask();
        assertNotNull(r);
        verifyZeroInteractions(delegateStorage);
        r.run();
        verify(delegateStorage, times(1)).purge();
        verifyNoMoreInteractions(delegateStorage);

        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testFindAllPojos() {
        @SuppressWarnings("unchecked")
        Category<TestPojo> category = mock(Category.class);
        Query<TestPojo> query = queuedStorage.createQuery(category);
        verify(delegateStorage).createQuery(category);
        verifyNoMoreInteractions(delegateStorage);

        Cursor<TestPojo> result = query.execute();
        verify(delegateQuery).execute();
        assertSame(expectedResults, result);

        assertNull(executor.getTask());
        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testGetCount() {
        Category<?> category = mock(Category.class);

        long result = queuedStorage.getCount(category);
        assertEquals(42, result);

        assertNull(executor.getTask());
        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testSaveFile() {
        InputStream stream = mock(InputStream.class);

        queuedStorage.saveFile("fluff", stream);

        Runnable task = fileExecutor.getTask();
        assertNotNull(task);
        verifyZeroInteractions(delegateStorage);
        task.run();
        verify(delegateStorage).saveFile(eq("fluff"), same(stream));

        assertNull(executor.getTask());
    }

    @Test
    public void testLoadFile() {

        InputStream stream = queuedStorage.loadFile("fluff");

        assertSame(expectedFile, stream);

        assertNull(executor.getTask());
        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testSetAgentId() {
        UUID id = new UUID(123, 456);

        queuedStorage.setAgentId(id);

        verifyZeroInteractions(delegateStorage);
        Runnable task = executor.getTask();
        task.run();
        verify(delegateStorage).setAgentId(id);
        
        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testGetAgentId() {
        String agentId = queuedStorage.getAgentId();

        verify(delegateStorage).getAgentId();
        assertEquals("huzzah", agentId);

        assertNull(executor.getTask());
        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testRegisterCategory() {

        Category<?> category = mock(Category.class);

        queuedStorage.registerCategory(category);

        verify(delegateStorage).registerCategory(category);

        assertNull(executor.getTask());
        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testGetConnection() {
        Connection connection = mock(Connection.class);
        when(delegateStorage.getConnection()).thenReturn(connection);

        Connection conn = queuedStorage.getConnection();

        verify(delegateStorage).getConnection();

        assertSame(conn, connection);

        assertNull(executor.getTask());
        assertNull(fileExecutor.getTask());
    }
    
    @Test
    public void testShutdown() {
        queuedStorage.shutdown();
        verify(delegateStorage).shutdown();
        assertTrue(executor.isShutdown());
        assertTrue(fileExecutor.isShutdown());
    }
}

