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

package com.redhat.thermostat.thread.harvester.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.management.ThreadInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadState;

/**
 */
public class ThreadStateHelperTest {

    private static String DEFAULT_W_ID = "0xcafe";
    private static String DEFAULT_HEADER_REF_ID = "42";
    private static String DEFAULT_SESSION_ID = "This Session";

    private String vmId;
    private WriterID writerId;
    private ThreadDao threadDao;

    private SessionID sessionID;

    @Before
    public void setUp() throws Exception {
        vmId = "testVM";
        writerId  = mock(WriterID.class);
        when(writerId.getWriterID()).thenReturn(DEFAULT_W_ID);

        threadDao = mock(ThreadDao.class);

        sessionID = mock(SessionID.class);
        when(sessionID.get()).thenReturn(DEFAULT_SESSION_ID);
    }

    @Test
    public void testCreateThreadState() throws Exception {
        ThreadStateHelper helper = new ThreadStateHelper(threadDao, writerId, vmId);

        ThreadInfo info = mock(ThreadInfo.class);
        when(info.getThreadState()).thenReturn(Thread.State.BLOCKED);
        when(info.getStackTrace()).thenReturn(new StackTraceElement[0]);

        long timestamp = -1l;

        ThreadState state = helper.createThreadState(info, sessionID, timestamp);
        assertNotNull(state);

        assertEquals(timestamp, state.getTimeStamp());
        assertEquals(Thread.State.BLOCKED.name(), state.getState());
        assertEquals(DEFAULT_SESSION_ID, state.getSession());
    }

    @Test
    public void testSaveThreadState() throws Exception {
        ThreadStateHelper helper = new ThreadStateHelper(threadDao, writerId, vmId);
        ThreadState state = mock(ThreadState.class);

        helper.saveThreadState(state);

        ArgumentCaptor<ThreadState> captor =
                ArgumentCaptor.forClass(ThreadState.class);
        verify(threadDao).addThreadState(captor.capture());

        ThreadState argumentToDao = captor.getValue();
        assertEquals(argumentToDao, state);
    }
}
