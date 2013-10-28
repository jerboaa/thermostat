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
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;
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
import org.mockito.Mockito;

import com.redhat.thermostat.storage.core.QueuedStorage.QueuedPreparedStatement;
import com.redhat.thermostat.storage.model.Pojo;


public class QueuedStorageTest {

    private static class TestExecutor implements ExecutorService {

        private Runnable task;
        private boolean shutdown;
        private long awaitTerminationTimeout;
        private TimeUnit awaitTerminationTimeUnit;

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
            awaitTerminationTimeout = timeout;
            awaitTerminationTimeUnit = unit;
            return true;
        }

        long getAwaitTerminationTimeout() {
            return awaitTerminationTimeout;
        }

        TimeUnit getAwaitTerminationTimeUnit() {
            return awaitTerminationTimeUnit;
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
    
    private static class TestShutdownExecutor extends TestExecutor {
        long executorShutDownTime = -1;
        
        @Override
        public void shutdown() {
            super.shutdown();
            executorShutDownTime = System.currentTimeMillis();
            // delay shutdown just a little
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private static final Category<FooPojo> TEST_CATEGORY = new Category<>("foo-table", FooPojo.class);

    private QueuedStorage queuedStorage;
    private Storage delegateStorage;
    
    private TestExecutor executor;
    private TestExecutor fileExecutor;

    private InputStream expectedFile;

    @Before
    public void setUp() {
        executor = new TestExecutor();
        fileExecutor = new TestExecutor();
        delegateStorage = mock(Storage.class);

        expectedFile = mock(InputStream.class);
        when(delegateStorage.loadFile(anyString())).thenReturn(expectedFile);
        queuedStorage = new QueuedStorage(delegateStorage, executor, fileExecutor);
    }

    @After
    public void tearDown() {
        expectedFile = null;
        queuedStorage = null;
        delegateStorage = null;
        fileExecutor = null;
        executor = null;
    }
    
    @Test
    public void testPurge() {

        queuedStorage.purge("fluff");

        Runnable r = executor.getTask();
        assertNotNull(r);
        verifyZeroInteractions(delegateStorage);
        r.run();
        verify(delegateStorage, times(1)).purge("fluff");
        verifyNoMoreInteractions(delegateStorage);

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
    public void testPrepareStatement() throws DescriptorParsingException, StatementExecutionException {
        @SuppressWarnings("unchecked")
        PreparedStatement<Pojo> statement = (PreparedStatement<Pojo>)mock(PreparedStatement.class);
        when(delegateStorage.prepareStatement(anyStatementDescriptor())).thenReturn(statement);
        
        
        StatementDescriptor<FooPojo> desc = new StatementDescriptor<>(TEST_CATEGORY, "QUERY foo-table");
        PreparedStatement<FooPojo> decorated = queuedStorage.prepareStatement(desc);
        assertNotNull(decorated);
        assertTrue(decorated instanceof QueuedPreparedStatement);
        assertNotSame(decorated, statement);
        assertNull(executor.getTask());
        
        // make sure execution queues a runnable
        decorated.execute();
        assertNotNull(executor.getTask());
    }
    
    /*
     * QueuedStorage decorates PreparedStatement, which may throw a
     * StatementExecution exception on stmt.execute(). All the decorator can do
     * is to log the exception and continue. As such, the decorated
     * PreparedStatement should never throw that exception. It can't since it
     * solely submits a runnable for execution.
     */
    @Test
    public void testExecutePreparedStatementFails()
            throws DescriptorParsingException, StatementExecutionException {
        @SuppressWarnings("unchecked")
        PreparedStatement<Pojo> statement = (PreparedStatement<Pojo>)mock(PreparedStatement.class);
        
        // make sure the delegate throws a StatementExecutionException
        Mockito.doThrow(StatementExecutionException.class).when(statement).execute();
        when(delegateStorage.prepareStatement(anyStatementDescriptor())).thenReturn(statement);
        
        StatementDescriptor<FooPojo> desc = new StatementDescriptor<>(TEST_CATEGORY, "QUERY foo-table");
        PreparedStatement<FooPojo> decorated = queuedStorage.prepareStatement(desc);
        assertNotNull(decorated);
        assertTrue(decorated instanceof QueuedPreparedStatement);
        assertNotSame(decorated, statement);
        assertNull(executor.getTask());
        
        // this should not throw the exception the decoratee throws.
        try {
            decorated.execute();
            // pass
        } catch (StatementExecutionException e) {
            fail("decorator should only submit task and continue");
        }
        assertNotNull(executor.getTask());
        try {
            executor.getTask().run();
            // pass
        } catch (Exception e) {
            fail("delegate's StatementExecutionException should have been caught");
        }
    }
    
    @Test
    public void testPrepareStatementDescriptorParseFailure() throws DescriptorParsingException, StatementExecutionException {
        
        Mockito.doThrow(DescriptorParsingException.class).when(delegateStorage).prepareStatement(anyStatementDescriptor());
        
        StatementDescriptor<FooPojo> desc = new StatementDescriptor<>(TEST_CATEGORY, "QUERY foo-table");
        
        try {
            queuedStorage.prepareStatement(desc);
            fail("should have thrown descptor parsing exception!");
        } catch (DescriptorParsingException e) {
            // pass
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Pojo> StatementDescriptor<T> anyStatementDescriptor() {
        return any(StatementDescriptor.class);
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
        assertEquals(3, executor.getAwaitTerminationTimeout());
        assertEquals(TimeUnit.SECONDS, executor.getAwaitTerminationTimeUnit());
        assertTrue(fileExecutor.isShutdown());
        assertEquals(3, fileExecutor.getAwaitTerminationTimeout());
        assertEquals(TimeUnit.SECONDS, fileExecutor.getAwaitTerminationTimeUnit());
    }
    
    @Test
    public void executorsShutdownBeforeDelegate() {
        StubStorage delegate = new StubStorage();
        TestShutdownExecutor executor = new TestShutdownExecutor();
        TestShutdownExecutor fileExecutor = new TestShutdownExecutor();
        queuedStorage = new QueuedStorage(delegate, executor, fileExecutor);
        queuedStorage.shutdown();
        
        // all shutdown methods should have been called
        assertTrue(-1 != delegate.shutDownTime);
        assertTrue(-1 != executor.executorShutDownTime);
        assertTrue(-1 != fileExecutor.executorShutDownTime);
        // delegate should have shut down last
        assertTrue(delegate.shutDownTime > executor.executorShutDownTime);
        assertTrue(delegate.shutDownTime > fileExecutor.executorShutDownTime);
    }
    
    private static class StubStorage implements Storage {

        long shutDownTime = -1;

        @Override
        public void registerCategory(Category<?> category) {
            // not implemented
            throw new AssertionError();
        }

        @Override
        public Connection getConnection() {
            // not implemented
            throw new AssertionError();
        }        

        @Override
        public void purge(String agentId) {
            // not implemented
            throw new AssertionError();
        }

        @Override
        public void saveFile(String filename, InputStream data) {
            // not implemented
            throw new AssertionError();

        }

        @Override
        public InputStream loadFile(String filename) {
            // not implemented
            throw new AssertionError();
        }

        @Override
        public void shutdown() {
            shutDownTime = System.currentTimeMillis();
            // delay shutdown just a little
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        public <T extends Pojo> PreparedStatement<T> prepareStatement(StatementDescriptor<T> desc)
                throws DescriptorParsingException {
            // not implemented
            return null;
        }
        
    }
    
    private static class FooPojo implements Pojo {
       // Dummy class for testing 
    }
}

