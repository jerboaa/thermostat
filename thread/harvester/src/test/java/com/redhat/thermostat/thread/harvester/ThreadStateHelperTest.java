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

package com.redhat.thermostat.thread.harvester;

import com.redhat.thermostat.storage.core.WriterID;

import com.redhat.thermostat.thread.dao.ThreadDao;

import com.redhat.thermostat.thread.model.ThreadHeader;
import com.redhat.thermostat.thread.model.ThreadState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.management.ThreadInfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
public class ThreadStateHelperTest {

    private static String DEFAULT_W_ID = "0xcafe";
    private static String DEFAULT_HEADER_REF_ID = "42";

    private String vmId;
    private WriterID writerId;
    private ThreadDao threadDao;

    private ThreadHeader header;

    @Before
    public void setUp() throws Exception {
        vmId = "testVM";
        writerId  = mock(WriterID.class);
        when(writerId.getWriterID()).thenReturn(DEFAULT_W_ID);

        threadDao = mock(ThreadDao.class);

        header = mock(ThreadHeader.class);
        when(header.getReferenceID()).thenReturn(DEFAULT_HEADER_REF_ID);
    }

    @Test
    public void testCreateThreadState() throws Exception {
        ThreadStateHelper helper =
                new ThreadStateHelper(threadDao, writerId, vmId);

        ThreadInfo info = mock(ThreadInfo.class);
        when(info.getThreadState()).thenReturn(Thread.State.BLOCKED);

        long timestamp = -1l;

        ThreadState state = helper.createThreadState(header, info, timestamp);
        assertNotNull(state);

        assertEquals(state.getProbeStartTime(), timestamp);
        assertEquals(state.getProbeEndTime(), timestamp);
        assertEquals(state.getState(), Thread.State.BLOCKED.name());
        assertEquals(state.getHeader(), header);
        assertEquals(state.getReferenceID(), DEFAULT_HEADER_REF_ID);
    }

    @Test
    public void testSaveThreadState() throws Exception {
        // this test assumes there is no data in the database yet,
        // so a database entry will be created with the template
        // object as input
        ThreadStateHelper helper =
                new ThreadStateHelper(threadDao, writerId, vmId);

        ThreadHeader header = mock(ThreadHeader.class);
        ThreadState state = mock(ThreadState.class);
        when(state.getState()).thenReturn(Thread.State.BLOCKED.name());
        when(state.getHeader()).thenReturn(header);

        when(threadDao.getLastThreadState(header)).thenReturn(null);

        ThreadState result = helper.saveThreadState(state);
        assertNotNull(result);

        verify(threadDao).getLastThreadState(header);
        verify(state, times(0)).getState();

        ArgumentCaptor<ThreadState> captor =
                ArgumentCaptor.forClass(ThreadState.class);
        verify(threadDao).addThreadState(captor.capture());

        ThreadState argumentToDao = captor.getValue();
        assertEquals(argumentToDao, state);
        assertEquals(result, state);
    }

    @Test
    public void testSaveThreadStateInsertNew() throws Exception {
        // this test assumes there is already data in the database,
        // but the data object has a different state, hence a new one
        // will be created. This is mostly similar in behaviour to the
        // first test, except that the dao returns a non null state
        // object
        ThreadStateHelper helper =
                new ThreadStateHelper(threadDao, writerId, vmId);

        ThreadHeader header = mock(ThreadHeader.class);
        ThreadState state = mock(ThreadState.class);
        when(state.getState()).thenReturn(Thread.State.BLOCKED.name());
        when(state.getHeader()).thenReturn(header);

        ThreadState inDao = mock(ThreadState.class);
        when(inDao.getState()).thenReturn(Thread.State.TIMED_WAITING.name());
        when(inDao.getHeader()).thenReturn(header);

        when(threadDao.getLastThreadState(header)).thenReturn(inDao);

        ThreadState result = helper.saveThreadState(state);
        assertNotNull(result);

        verify(threadDao).getLastThreadState(header);
        verify(inDao).getState();

        ArgumentCaptor<ThreadState> captor =
                ArgumentCaptor.forClass(ThreadState.class);
        verify(threadDao).addThreadState(captor.capture());

        ThreadState argumentToDao = captor.getValue();
        assertEquals(argumentToDao, state);
        assertEquals(result, state);
    }

    @Test
    public void testSaveThreadStateUpdateExisting() throws Exception {
        // this test assumes there is already data in the database,
        // and the object has the same state as the one passed as input,
        // so an update will be performed
        ThreadStateHelper helper =
                new ThreadStateHelper(threadDao, writerId, vmId);

        ThreadHeader header = mock(ThreadHeader.class);
        ThreadState state = mock(ThreadState.class);
        when(state.getState()).thenReturn(Thread.State.BLOCKED.name());
        when(state.getHeader()).thenReturn(header);
        when(state.getProbeEndTime()).thenReturn(42l);

        ThreadState inDao = mock(ThreadState.class);
        when(inDao.getState()).thenReturn(Thread.State.BLOCKED.name());
        when(inDao.getHeader()).thenReturn(header);

        when(threadDao.getLastThreadState(header)).thenReturn(inDao);

        ThreadState result = helper.saveThreadState(state);
        assertNotNull(result);

        verify(threadDao).getLastThreadState(header);
        verify(inDao).getState();
        verify(state).getProbeEndTime();

        verify(inDao).setProbeEndTime(42l);

        verify(threadDao).updateThreadState(inDao);

        assertEquals(result, inDao);
        assertNotSame(result, state);
    }
}
