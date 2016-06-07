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
import com.redhat.thermostat.ui.swing.model.TraceElement;

import java.util.List;

/**
 */
public class StackTraceProfilerController extends CommonController {

    private ThreadCollector collector;
    private StackTraceProfilerView stackTraceProfilerView;
    public StackTraceProfilerController(StackTraceProfilerView view,
                                        ThreadCollector collector,
                                        Timer timer,
                                        VmRef ref)
    {
        super(timer, view);
        this.stackTraceProfilerView = view;
        this.collector = collector;

        view.createModel(ref.getName());

        timer.setAction(new StackTraceProfilerControllerAction());
    }

    private class StackTraceProfilerControllerAction extends SessionCheckingAction {

        private ThreadStateResultHandler threadStateResultHandler;

        public StackTraceProfilerControllerAction() {
            threadStateResultHandler = new ThreadStateResultHandler();
            resetState();
        }

        private void resetState() {
            stackTraceProfilerView.clear();
        }

        @Override
        protected SessionID getCurrentSessionID() {
            return session;
        }

        @Override
        protected SessionID getLastAvailableSessionID() {
            return collector.getLastThreadSession();
        }

        @Override
        protected Range<Long> getTotalRange(SessionID session) {
            return collector.getThreadRange(session);
        }

        @Override
        protected void actionPerformed(SessionID session, Range<Long> range,
                                       Range<Long> totalRange)
        {
            collector.getThreadStates(session,  threadStateResultHandler, range);
            stackTraceProfilerView.rebuild();
        }

        @Override
        protected void onNewSession() {
            resetState();
        }
    }

    // for testing
    StackTrace getStackTrace(ThreadState state) {
        return StackTrace.fromJson(state.getStackTrace());
    }

    private volatile boolean stopLooping;

    @Override
    protected void onViewVisible() {
        stopLooping = false;
    }

    @Override
    protected void onViewHidden() {
        stopLooping = true;
    }

    private class ThreadStateResultHandler implements ResultHandler<ThreadState> {

        @Override
        public boolean onResult(ThreadState state) {

            Thread.State threadState = Thread.State.valueOf(state.getState());
            switch (threadState) {
                // all other cases, we want to track this thread
                case BLOCKED:
                case WAITING:
                case TIMED_WAITING:
                case TERMINATED:
                    return true;
            }

            StackTrace stackTrace = getStackTrace(state);

            Trace trace = new Trace(state.getName());
            List<StackFrame> frames = stackTrace.getFrames();
            int frameID = frames.size() - 1;

            while (frameID >= 0) {
                StackFrame frame = frames.get(frameID);
                frameID--;

                TraceElement element =
                        new TraceElement(frame.getClassName()  + "." +
                                         frame.getMethodName() + ":" +
                                         frame.getLineNumber());
                trace.add(element);
            }

            stackTraceProfilerView.addTrace(trace);

            boolean _stopLooping = stopLooping;
            return !_stopLooping;
        }
    }
}
