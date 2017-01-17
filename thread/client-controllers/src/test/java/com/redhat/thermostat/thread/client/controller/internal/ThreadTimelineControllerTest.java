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

package com.redhat.thermostat.thread.client.controller.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.redhat.thermostat.thread.cache.RangedCache;
import com.redhat.thermostat.thread.client.controller.internal.cache.AppCache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.experimental.statement.ResultHandler;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.model.timeline.ThreadInfo;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ThreadTimelineControllerTest {

    private ThreadTimelineView view;
    private ThreadCollector collector;
    private Timer timer;
    private SessionID session;

    private AppCache cache;
    private Map<SessionID, RangedCache<ThreadState>> threadStatesCache;

    @Before
    public void setup() {
        threadStatesCache = new ConcurrentHashMap<>();

        cache = mock(AppCache.class);
        when(cache.retrieve(CommonController.THREAD_STATE_CACHE)).thenReturn(threadStatesCache);
        view = mock(ThreadTimelineView.class);
        collector = mock(ThreadCollector.class);
        timer = mock(Timer.class);
        session = mock(SessionID.class);
        when(session.get()).thenReturn("42");
        when(collector.getLastThreadSession()).thenReturn(session);
    }

    @Test
    public void verifySession() {
        ArgumentCaptor<Runnable> captor =
                ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(captor.capture());

        ThreadTimelineController controller =
                new ThreadTimelineController(view, collector, timer, cache);
        Runnable timerAction = captor.getValue();

        timerAction.run();

        verify(collector).getLastThreadSession();
        verify(collector).getThreadRange(session);
    }

    @Test
    public void verifySessionIfSet() {
        ArgumentCaptor<Runnable> captor =
                ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(captor.capture());

        SessionID newSession = mock(SessionID.class);
        when(newSession.get()).thenReturn("0");

        ThreadTimelineController controller =
                new ThreadTimelineController(view, collector, timer, cache);
        Runnable timerAction = captor.getValue();

        timerAction.run();

        verify(collector).getThreadRange(session);

        controller.setSession(newSession);

        timerAction.run();

        verify(collector).getThreadRange(newSession);
    }

    @Test
    public void verifyRange() {
        ArgumentCaptor<Runnable> captor =
                ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(captor.capture());

        Range<Long> range = new Range<>(0l, 10l);
        when(collector.getThreadRange(session)).thenReturn(range);

        ThreadTimelineController controller =
                new ThreadTimelineController(view, collector, timer, cache) {
                    @Override
                    long __test__getTimeDeltaOnNewSession() {
                        return 0;
                    }
                };
        Runnable timerAction = captor.getValue();

        timerAction.run();

        verify(collector).getThreadRange(session);
        verify(view).setTotalRange(range);
    }

    @Test
    public void testAllBeansAreLoaded() {
        ArgumentCaptor<Runnable> captor =
                ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(captor.capture());

        ArgumentCaptor<ResultHandler> captor2 =
                ArgumentCaptor.forClass(ResultHandler.class);
        doNothing().when(collector).getThreadStates(any(SessionID.class),
                                                    captor2.capture(),
                                                    any(Range.class));

        Range<Long> range = new Range<>(0l, 10l);
        when(collector.getThreadRange(session)).thenReturn(range);

        ThreadTimelineController controller =
                new ThreadTimelineController(view, collector, timer, cache) {
                    @Override
                    long __test__getTimeDeltaOnNewSession() {
                        return 0;
                    }
                };
        Runnable timerAction = captor.getValue();

        timerAction.run();

        ThreadTimelineController.ThreadStateResultHandler handler =
                (ThreadTimelineController.ThreadStateResultHandler) captor2.getValue();
        handler.setCacheResults(false);

        ThreadState state0 = mock(ThreadState.class);
        when(state0.getName()).thenReturn("state0");
        when(state0.getId()).thenReturn(0l);
        when(state0.getState()).thenReturn("NEW");

        ThreadState state1 = mock(ThreadState.class);
        when(state1.getName()).thenReturn("state1");
        when(state1.getId()).thenReturn(1l);
        when(state1.getState()).thenReturn("NEW");

        ThreadState state2 = mock(ThreadState.class);
        when(state2.getName()).thenReturn("state2");
        when(state2.getId()).thenReturn(2l);
        when(state2.getState()).thenReturn("NEW");

        handler.onResult(state0);
        handler.onResult(state1);
        handler.onResult(state2);

        ThreadInfo info = new ThreadInfo();
        info.setName("state0");
        info.setId(0l);

        verify(view).addThread(info);

        info.setName("state1");
        info.setId(1l);
        verify(view).addThread(info);

        info.setName("state2");
        info.setId(2l);
        verify(view).addThread(info);
    }
}
