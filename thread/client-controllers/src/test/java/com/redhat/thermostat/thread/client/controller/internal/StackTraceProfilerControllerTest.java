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
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.view.StackTraceProfilerView;
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

    @Before
    public void setUp() {
        timer = mock(Timer.class);
        view = mock(StackTraceProfilerView.class);
        collector = mock(ThreadCollector.class);
        ref = mock(VmRef.class);
        when(ref.getName()).thenReturn(VM_ID);

        session = mock(SessionID.class);
    }

    @Test
    public void testModelCreated() {
        StackTraceProfilerController profiler =
                new StackTraceProfilerController(view, collector, timer, ref);

        verify(view).createModel(VM_ID);
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
                new StackTraceProfilerController(view, collector, timer, ref) {
                    @Override
                    StackTrace getStackTrace(ThreadState state) {
                        // the other are blocked or waiting
                        assertEquals(state, state0);
                        return trace0;
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

        ResultHandler handler = resultHandlerCaptor.getValue();
        handler.onResult(state0);
        handler.onResult(state1);
        handler.onResult(state2);

        verify(view).addTrace(traceCaptor.capture());

        Trace trace = traceCaptor.getValue();
        assertEquals("state0", trace.getName());
        assertEquals(2, trace.getTrace().size());
        assertEquals("class.method1:2", trace.getTrace().get(0).getName());
        assertEquals("class.method0:1", trace.getTrace().get(1).getName());
    }
}
