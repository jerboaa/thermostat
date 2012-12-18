/*
 * Copyright 2012 Red Hat, Inc.
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
import java.util.UUID;
import java.util.concurrent.Executor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.storage.model.AgentIdPojo;
import com.redhat.thermostat.storage.model.Pojo;


public class QueuedStorageTest {

    private static class TestExecutor implements Executor {

        private Runnable task;

        @Override
        public void execute(Runnable task) {
            this.task = task;
        }

        Runnable getTask() {
            return task;
        }
    }

    private static class TestPojo implements Pojo {
        
    }

    private QueuedStorage queuedStorage;
    private Storage delegateStorage;

    private TestExecutor executor;
    private TestExecutor fileExecutor;

    @SuppressWarnings("rawtypes")
    private Cursor expectedResults;
    private TestPojo expectedResult;
    private InputStream expectedFile;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        executor = new TestExecutor();
        fileExecutor = new TestExecutor();
        delegateStorage = mock(Storage.class);
        Update update = mock(Update.class);
        Remove remove = mock(Remove.class);
        Query query = mock(Query.class);
        when(delegateStorage.createUpdate()).thenReturn(update);
        when(delegateStorage.createRemove()).thenReturn(remove);
        when(delegateStorage.createQuery()).thenReturn(query);
        expectedResults = mock(Cursor.class);
        when(delegateStorage.findAllPojos(query, TestPojo.class)).thenReturn(expectedResults);
        expectedResult = new TestPojo();
        when(delegateStorage.findPojo(query, TestPojo.class)).thenReturn(expectedResult);
        when(delegateStorage.getCount(any(Category.class))).thenReturn(42l);
        expectedFile = mock(InputStream.class);
        when(delegateStorage.loadFile(anyString())).thenReturn(expectedFile);
        when(delegateStorage.getAgentId()).thenReturn("huzzah");
        queuedStorage = new QueuedStorage(delegateStorage, executor, fileExecutor);
        
    }

    @After
    public void tearDown() {
        expectedFile = null;
        expectedResult = null;
        expectedResults = null;
        queuedStorage = null;
        delegateStorage = null;
        fileExecutor = null;
        executor = null;
    }

    @Test
    public void testPutPojo() {
        Category category = mock(Category.class);
        AgentIdPojo pojo = mock(AgentIdPojo.class);

        queuedStorage.putPojo(category, true, pojo);

        Runnable r = executor.getTask();
        assertNotNull(r);
        verifyZeroInteractions(delegateStorage);
        r.run();
        verify(delegateStorage, times(1)).putPojo(category, true, pojo);
        verifyNoMoreInteractions(delegateStorage);

        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testUpdatePojo() {

        Update update = queuedStorage.createUpdate();
        verify(delegateStorage).createUpdate();
        verifyNoMoreInteractions(delegateStorage);

        queuedStorage.updatePojo(update);

        Runnable r = executor.getTask();
        assertNotNull(r);
        verifyZeroInteractions(delegateStorage);
        r.run();
        verify(delegateStorage, times(1)).updatePojo(update);
        verifyNoMoreInteractions(delegateStorage);

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
        Query query = queuedStorage.createQuery();
        verify(delegateStorage).createQuery();
        verifyNoMoreInteractions(delegateStorage);

        Cursor<TestPojo> result = queuedStorage.findAllPojos(query, TestPojo.class);
        verify(delegateStorage).findAllPojos(query, TestPojo.class);
        assertSame(expectedResults, result);

        assertNull(executor.getTask());
        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testFindPojo() {
        Query query = queuedStorage.createQuery();
        verify(delegateStorage).createQuery();
        verifyNoMoreInteractions(delegateStorage);

        TestPojo result = queuedStorage.findPojo(query, TestPojo.class);
        verify(delegateStorage).findPojo(query, TestPojo.class);
        assertSame(expectedResult, result);

        assertNull(executor.getTask());
        assertNull(fileExecutor.getTask());
    }

    @Test
    public void testGetCount() {
        Category category = mock(Category.class);

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

        Category category = mock(Category.class);

        queuedStorage.registerCategory(category);

        Runnable task = executor.getTask();
        verifyZeroInteractions(delegateStorage);
        task.run();
        verify(delegateStorage).registerCategory(category);

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
}
