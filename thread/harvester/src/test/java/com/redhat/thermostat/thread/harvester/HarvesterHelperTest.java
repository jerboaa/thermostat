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

import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.model.ThreadHeader;
import com.redhat.thermostat.thread.model.ThreadSession;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.thread.model.ThreadSummary;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 */
public class HarvesterHelperTest {

    private static final long DEFAULT_TIMESTAMP = -1l;

    private ThreadDao threadDao;

    private Clock clock;
    private String vmId;

    private ThreadSummaryHelper summaryHelper;
    private ThreadHeaderHelper headerHelper;
    private ThreadStateHelper stateHelper;
    private ThreadContentionHelper contentionHelper;
    private ThreadSessionHelper threadSessionHelper;

    private ThreadMXBean collectorBean;
    private ThreadSession session;
    @Before
    public void setUp() {
        summaryHelper = mock(ThreadSummaryHelper.class);
        headerHelper = mock(ThreadHeaderHelper.class);
        stateHelper = mock(ThreadStateHelper.class);

        contentionHelper = mock(ThreadContentionHelper.class);

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
    public void testThreadSummarySaved() throws Exception {

        ThreadSummary summary = mock(ThreadSummary.class);
        when(summaryHelper.createThreadSummary(collectorBean,
                                               DEFAULT_TIMESTAMP,
                                               session)).
            thenReturn(summary);

        HarvesterHelper harvester = new HarvesterHelper(threadDao, clock, vmId,
                                                        summaryHelper,
                                                        headerHelper,
                                                        stateHelper,
                                                        contentionHelper,
                                                        threadSessionHelper);
        harvester.collectAndSaveThreadData(session, collectorBean);

        verify(clock).getRealTimeMillis();
        verify(summaryHelper).createThreadSummary(collectorBean,
                                                  DEFAULT_TIMESTAMP,
                                                  session);
        verify(summaryHelper).saveSummary(summary);
    }

    @Test
    public void testThreadInfoRetrievedMatchThreadIDs() {

        long[] ids = new long[] {0l, 1l, 2l};
        when(collectorBean.getAllThreadIds()).thenReturn(ids);

        HarvesterHelper harvester = new HarvesterHelper(threadDao, clock, vmId,
                                                        summaryHelper,
                                                        headerHelper,
                                                        stateHelper,
                                                        contentionHelper,
                                                        threadSessionHelper);
        harvester.collectAndSaveThreadData(session, collectorBean);
        verify(collectorBean).getAllThreadIds();

        verify(collectorBean).getThreadInfo(ids, true, true);
    }

    @Test
    public void testThreadInfoPassedToThreadStateHelper() {

        long[] ids = new long[] { 0l, 1l, 2l };

        when(collectorBean.getAllThreadIds()).thenReturn(ids);

        ThreadInfo[] infos = new ThreadInfo[3];
        infos[0] = mock(ThreadInfo.class);
        infos[1] = mock(ThreadInfo.class);
        infos[2] = mock(ThreadInfo.class);

        when(collectorBean.getThreadInfo(any(long[].class), eq(true), eq(true))).thenReturn(infos);

        ThreadHeader header1 = mock(ThreadHeader.class);
        ThreadHeader header2 = mock(ThreadHeader.class);

        when(headerHelper.createThreadHeader(infos[0], DEFAULT_TIMESTAMP)).thenReturn(header1);
        when(headerHelper.createThreadHeader(infos[1], DEFAULT_TIMESTAMP)).thenReturn(header2);
        when(headerHelper.createThreadHeader(infos[2], DEFAULT_TIMESTAMP)).thenReturn(header1);

        when(headerHelper.checkAndSaveThreadHeader(header1)).thenReturn(header1);
        when(headerHelper.checkAndSaveThreadHeader(header2)).thenReturn(header2);

        ThreadState state1 = mock(ThreadState.class);
        ThreadState state2 = mock(ThreadState.class);
        ThreadState state3 = mock(ThreadState.class);

        when(stateHelper.createThreadState(header1, infos[0], DEFAULT_TIMESTAMP)).thenReturn(state1);
        when(stateHelper.createThreadState(header2, infos[1], DEFAULT_TIMESTAMP)).thenReturn(state2);
        when(stateHelper.createThreadState(header1, infos[2], DEFAULT_TIMESTAMP)).thenReturn(state3);

        HarvesterHelper harvester = new HarvesterHelper(threadDao, clock, vmId,
                                                        summaryHelper,
                                                        headerHelper,
                                                        stateHelper,
                                                        contentionHelper,
                                                        threadSessionHelper);
        harvester.collectAndSaveThreadData(session, collectorBean);

        verify(headerHelper, times(2)).checkAndSaveThreadHeader(header1);
        verify(headerHelper).checkAndSaveThreadHeader(header2);

        verify(stateHelper).saveThreadState(state1);
        verify(stateHelper).saveThreadState(state2);
        verify(stateHelper).saveThreadState(state3);
    }
}
