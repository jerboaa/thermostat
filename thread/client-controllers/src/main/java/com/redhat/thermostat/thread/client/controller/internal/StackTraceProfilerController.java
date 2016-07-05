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
import com.redhat.thermostat.thread.cache.RangedCache;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.view.StackTraceProfilerView;
import com.redhat.thermostat.thread.client.controller.internal.cache.AppCache;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.StackFrame;
import com.redhat.thermostat.thread.model.StackTrace;
import com.redhat.thermostat.thread.model.ThreadState;
import com.redhat.thermostat.ui.swing.model.Trace;
import com.redhat.thermostat.ui.swing.model.TraceElement;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 */
public class StackTraceProfilerController extends CommonController {

    private ThreadCollector collector;
    private StackTraceProfilerView stackTraceProfilerView;
    public StackTraceProfilerController(StackTraceProfilerView view,
                                        ThreadCollector collector,
                                        Timer timer,
                                        VmRef ref,
                                        AppCache cache)
    {
        super(timer, view, cache);
        this.stackTraceProfilerView = view;
        this.collector = collector;

        view.createModel(ref.getName());

        timer.setAction(new StackTraceProfilerControllerAction());
    }

    RangedCache<ThreadState> getCache(final SessionID session) {

        return executeInCriticalSection(new Callable<RangedCache<ThreadState>>() {
            @Override
            public RangedCache<ThreadState> call() throws Exception {
                Map<SessionID, RangedCache<ThreadState>> threadStatesCache =
                        cache.retrieve(CommonController.THREAD_STATE_CACHE);

                RangedCache<ThreadState> rangedCache = threadStatesCache.get(session);
                if (rangedCache == null) {
                    rangedCache = new RangedCache<>();
                    threadStatesCache.put(session, rangedCache);
                }

                return rangedCache;
            }
        });
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
            // let's see what do we have in the cache
            RangedCache<ThreadState> cache = getCache(session);
            List<ThreadState> values = cache.getValues(range);
            if (!values.isEmpty()) {
                // add the results we have to the view
                threadStateResultHandler.setCacheResults(false);
                for (ThreadState state : values) {
                    threadStateResultHandler.onResult(state);
                }
                threadStateResultHandler.setCacheResults(true);

                ThreadState threadState = values.get(values.size() - 1);

                long lastSampled = threadState.getTimeStamp() + 1;
                long delta = range.getMax() - lastSampled;
                if (delta > PERIOD) {
                    Range<Long> rangeToQuery = new Range<>(lastSampled, range.getMax());
                    collector.getThreadStates(session,  threadStateResultHandler, rangeToQuery);
                }
            } else {
                collector.getThreadStates(session,  threadStateResultHandler, range);
            }

            stackTraceProfilerView.rebuild();
        }

        @Override
        protected void onNewSession() {
            resetState();
        }

        @Override
        protected long getTimeDeltaOnNewSession() {
            long delta = __test__getTimeDeltaOnNewSession();
            if (delta < 0) {
                delta = super.getTimeDeltaOnNewSession();
            }
            return delta;
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

    class ThreadStateResultHandler implements ResultHandler<ThreadState> {

        private boolean cache = true;

        public void setCacheResults(boolean cache) {
            this.cache = cache;
        }

        @Override
        public boolean onResult(ThreadState state) {

            if (cache) {
                RangedCache<ThreadState> cache = getCache(new SessionID(state.getSession()));
                cache.put(state);
            }

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
