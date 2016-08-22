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

package com.redhat.thermostat.thread.client.controller.internal;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.core.experimental.statement.ResultHandler;
import com.redhat.thermostat.thread.cache.RangedCache;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.view.StackTraceProfilerView;
import com.redhat.thermostat.thread.client.controller.internal.StackTraceProfilerController.ThreadStateResultHandler;
import com.redhat.thermostat.thread.client.controller.internal.cache.AppCache;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.StackFrame;
import com.redhat.thermostat.thread.model.StackTrace;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.ui.swing.model.Trace;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
public class StackTraceProfilerControllerTest {
    private Timer timer;
    private Runnable threadAction;
    private StackTraceProfilerView view;
    private ThreadCollector collector;

    private ActionListener<StackTraceProfilerView.Action> actionListener;

    private VmRef ref;

    private static final String VM_ID = "42";

    private SessionID session;
    private AppCache cache;

    private Map<SessionID, RangedCache<ThreadState>> threadStatesCache;

    @Before
    public void setUp() {
        threadStatesCache = new ConcurrentHashMap<>();

        timer = mock(Timer.class);
        view = mock(StackTraceProfilerView.class);
        collector = mock(ThreadCollector.class);
        ref = mock(VmRef.class);
        when(ref.getName()).thenReturn(VM_ID);

        session = mock(SessionID.class);

        cache = mock(AppCache.class);
        when(cache.retrieve(CommonController.THREAD_STATE_CACHE)).thenReturn(threadStatesCache);
    }

    @Test
    public void testModelCreated() {
        StackTraceProfilerController profiler =
                new StackTraceProfilerController(view, collector, timer, ref, cache);

        verify(view).createModel(VM_ID);
    }

    @Test
    public void testCompactTraces() {

        final ThreadState state0 = mock(ThreadState.class);
        when(state0.getName()).thenReturn("state0");
        when(state0.getId()).thenReturn(0l);
        when(state0.getState()).thenReturn("NEW");
        final StackTrace trace0 = mock(StackTrace.class);

        // resulting trace: A -> B -> C(x2) -> C -> D
        StackFrame frame0 = new StackFrame("file", "package", "class", "methodD", 5, false);
        StackFrame frame1 = new StackFrame("file", "package", "class", "methodC", 4, false);
        StackFrame frame2 = new StackFrame("file", "package", "class", "methodC", 3, false);
        StackFrame frame3 = new StackFrame("file", "package", "class", "methodC", 3, false);
        StackFrame frame4 = new StackFrame("file", "package", "class", "methodB", 2, false);
        StackFrame frame5 = new StackFrame("file", "package", "class", "methodA", 1, false);

        List<StackFrame> frames = new ArrayList<>();
        frames.add(frame0);
        frames.add(frame1);
        frames.add(frame2);
        frames.add(frame3);
        frames.add(frame4);
        frames.add(frame5);

        when(trace0.getFrames()).thenReturn(frames);

        StackTraceProfilerController profiler =
                new StackTraceProfilerController(view, collector, timer, ref, cache) {
                    @Override
                    StackTrace getStackTrace(ThreadState state) {
                        return trace0;
                    }
                };
        ThreadStateResultHandler handler = profiler.getThreadStateResultHandler();
        handler.setCacheResults(false);

        handler.onResult(state0);

        ArgumentCaptor<Trace> traceCaptor = ArgumentCaptor.forClass(Trace.class);
        verify(view).addTrace(any(Trace.class), traceCaptor.capture());

        Trace trace = traceCaptor.getValue();
        assertEquals("state0", trace.getName());
        assertEquals(5, trace.getTrace().size());
        assertEquals("class.methodA:1", trace.getTrace().get(0).getName());
        assertEquals("class.methodB:2", trace.getTrace().get(1).getName());
        assertEquals("class.methodC:3", trace.getTrace().get(2).getName());
        assertEquals("class.methodC:4", trace.getTrace().get(3).getName());
        assertEquals("class.methodD:5", trace.getTrace().get(4).getName());

        assertEquals(2, trace.getTrace().get(2).getWeight());
        assertEquals(1, trace.getTrace().get(3).getWeight());
    }

    @Test
    public void testGetThreadTraces() {

        final ThreadState state0 = mock(ThreadState.class);
        when(state0.getName()).thenReturn("state0");
        when(state0.getId()).thenReturn(0l);
        when(state0.getState()).thenReturn("NEW");
        final StackTrace trace0 = mock(StackTrace.class);

        ThreadState state1 = mock(ThreadState.class);
        when(state1.getName()).thenReturn("state1");
        when(state1.getId()).thenReturn(1l);
        when(state1.getState()).thenReturn("BLOCKED");
        StackTrace trace1 = mock(StackTrace.class);

        ThreadState state2 = mock(ThreadState.class);
        when(state2.getName()).thenReturn("state2");
        when(state2.getId()).thenReturn(2l);
        when(state2.getState()).thenReturn("WAITING");
        StackTrace trace2 = mock(StackTrace.class);

        ArgumentCaptor<SessionCheckingAction> captor = ArgumentCaptor.forClass(SessionCheckingAction.class);
        doNothing().when(timer).setAction(captor.capture());
        ArgumentCaptor<ResultHandler> resultHandlerCaptor = ArgumentCaptor.forClass(ResultHandler.class);

        Range<Long> range = new Range<>(0l, 10l);
        when(collector.getThreadRange(session)).thenReturn(range);

        StackTraceProfilerController profiler =
                new StackTraceProfilerController(view, collector, timer, ref, cache) {
                    @Override
                    StackTrace getStackTrace(ThreadState state) {
                        // the other are blocked or waiting
                        assertEquals(state, state0);
                        return trace0;
                    }

                    @Override
                    long __test__getTimeDeltaOnNewSession() {
                        return 0;
                    }
                };

        SessionCheckingAction action = captor.getValue();
        assertNotNull(action);

        action.actionPerformed(session, range, range);

        verify(view).rebuild();
        verify(collector).getThreadStates(any(SessionID.class), resultHandlerCaptor.capture(), any(Range.class));

        StackFrame frame0 = new StackFrame("file", "package", "class", "method0", 1, false);
        StackFrame frame1 = new StackFrame("file", "package", "class", "method1", 2, false);

        List<StackFrame> frames = new ArrayList<>();
        frames.add(frame0);
        frames.add(frame1);

        when(trace0.getFrames()).thenReturn(frames);

        ArgumentCaptor<Trace> traceCaptor = ArgumentCaptor.forClass(Trace.class);

        StackTraceProfilerController.ThreadStateResultHandler handler =
                (StackTraceProfilerController.ThreadStateResultHandler) resultHandlerCaptor.getValue();
        handler.setCacheResults(false);

        handler.onResult(state0);
        handler.onResult(state1);
        handler.onResult(state2);

        verify(view).addTrace(traceCaptor.capture(), any(Trace.class));

        Trace trace = traceCaptor.getValue();
        assertEquals("state0", trace.getName());
        assertEquals(2, trace.getTrace().size());
        assertEquals("class.method1:2", trace.getTrace().get(0).getName());
        assertEquals("class.method0:1", trace.getTrace().get(1).getName());
    }
}
