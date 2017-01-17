/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadSession;
import com.redhat.thermostat.thread.model.ThreadState;

/*
 */
public class HarvesterHelperTest {

    private static final long DEFAULT_TIMESTAMP = -1l;

    private ThreadDao threadDao;

    private Clock clock;
    private String vmId;

    private ThreadSummaryHelper summaryHelper;
    private ThreadStateHelper stateHelper;
    private ThreadSessionHelper threadSessionHelper;

    private ThreadMXBean collectorBean;
    private ThreadSession session;
    @Before
    public void setUp() {
        summaryHelper = mock(ThreadSummaryHelper.class);
        stateHelper = mock(ThreadStateHelper.class);

        threadDao = mock(ThreadDao.class);
        collectorBean = mock(ThreadMXBean.class);

        clock = mock(Clock.class);
        when(clock.getRealTimeMillis()).thenReturn(DEFAULT_TIMESTAMP);

        vmId = "42";

        session = new ThreadSession();
        session.setSession("0xcafe");

        threadSessionHelper = mock(ThreadSessionHelper.class);
    }

    @Test
    public void testThreadInfoPassedToThreadStateHelper() {
        ThreadInfo[] infos = new ThreadInfo[3];
        infos[0] = mock(ThreadInfo.class);
        infos[1] = mock(ThreadInfo.class);
        infos[2] = mock(ThreadInfo.class);

        when(collectorBean.dumpAllThreads(eq(false), eq(false))).thenReturn(infos);

        ThreadState state1 = mock(ThreadState.class);
        ThreadState state2 = mock(ThreadState.class);
        ThreadState state3 = mock(ThreadState.class);

        when(stateHelper.createThreadState(eq(infos[0]),eq(session.getSessionID()), eq(DEFAULT_TIMESTAMP))).thenReturn(state1);
        when(stateHelper.createThreadState(eq(infos[1]), eq(session.getSessionID()), eq(DEFAULT_TIMESTAMP))).thenReturn(state2);
        when(stateHelper.createThreadState(eq(infos[2]), eq(session.getSessionID()), eq(DEFAULT_TIMESTAMP))).thenReturn(state3);

        HarvesterHelper harvester = new HarvesterHelper(clock, vmId,
                                                        stateHelper,
                                                        threadSessionHelper);
        harvester.collectAndSaveThreadData(session, collectorBean);

        verify(stateHelper).saveThreadState(state1);
        verify(stateHelper).saveThreadState(state2);
        verify(stateHelper).saveThreadState(state3);
    }
}
