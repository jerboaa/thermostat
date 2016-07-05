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
import com.redhat.thermostat.storage.core.experimental.statement.ResultHandler;
import com.redhat.thermostat.thread.cache.RangedCache;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.model.timeline.ThreadInfo;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView;
import com.redhat.thermostat.thread.client.controller.internal.cache.AppCache;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.StackTrace;
import com.redhat.thermostat.thread.model.ThreadState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ThreadTableController extends CommonController {
    
    private ThreadTableView threadTableView;
    private ThreadCollector collector;

    private volatile boolean stopLooping;

    public ThreadTableController(ThreadTableView threadTableView,
                                 ThreadCollector collector,
                                 Timer timer, AppCache cache)
    {
        super(timer, threadTableView, cache);
        this.threadTableView = threadTableView;
        this.collector = collector;
        timer.setAction(new ThreadTableControllerAction());
    }

    @Override
    protected void onViewVisible() {
        stopLooping = false;
    }

    @Override
    protected void onViewHidden() {
        stopLooping = true;
    }

    RangedCache<ThreadState> getCache(final SessionID sessionID) {

        return executeInCriticalSection(new Callable<RangedCache<ThreadState>>() {
            @Override
            public RangedCache<ThreadState> call() throws Exception {
                Map<SessionID, RangedCache<ThreadState>> threadStatesCache =
                        cache.retrieve(CommonController.THREAD_STATE_CACHE);

                RangedCache<ThreadState> rangedCache = threadStatesCache.get(sessionID);
                if (rangedCache == null) {
                    rangedCache = new RangedCache<>();
                    threadStatesCache.put(sessionID, rangedCache);
                }

                return rangedCache;
            }
        });
    }

    private class ThreadTableControllerAction extends SessionCheckingAction {

        private ThreadResultHandler handler;

        public ThreadTableControllerAction() {
            handler = new ThreadResultHandler();
            resetState();
        }

        @Override
        protected void onNewSession() {
            resetState();
        }

        private void resetState() {
            handler.threadStates.clear();
            threadTableView.clear();
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
                for (ThreadState state : values) {
                    handler.onResult(state);
                }

                ThreadState threadState = values.get(values.size() - 1);

                long lastSampled = threadState.getTimeStamp() + 1;
                long delta = range.getMax() - lastSampled;
                if (delta > PERIOD) {
                    Range<Long> rangeToQuery = new Range<>(lastSampled, range.getMax());
                    collector.getThreadStates(session,
                                              handler,
                                              rangeToQuery);
                }
            } else {
                collector.getThreadStates(session, handler, range);
            }

            threadTableView.submitChanges();
        }

        @Override
        protected SessionID getCurrentSessionID() {
            return session;
        }

        @Override
        protected SessionID getLastAvailableSessionID() {
            return collector.getLastThreadSession();
        }
    }

    private class ThreadResultHandler implements ResultHandler<ThreadState> {
        private Map<ThreadInfo, ThreadTableBean> threadStates;
        private boolean cache;

        public ThreadResultHandler() {
            this.threadStates = new HashMap<>();
            cache = true;
        }

        public void setCacheResults(boolean cache) {
            this.cache = cache;
        }

        @Override
        public boolean onResult(ThreadState thread) {

            if (cache) {
                RangedCache<ThreadState> cache = getCache(new SessionID(thread.getSession()));
                cache.put(thread);
            }

            ThreadInfo key = new ThreadInfo();
            key.setName(thread.getName());
            key.setId(thread.getId());

            ThreadTableBean bean = threadStates.get(key);
            if (bean == null) {
                bean = new ThreadTableBean();
                bean.setName(thread.getName());
                bean.setId(thread.getId());
                bean.setStartTimeStamp(thread.getTimeStamp());
                threadStates.put(key, bean);
            }

            setCurrentStateTime(bean, thread);

            double totalRunningTime = bean.getRunningTime()  +
                                      bean.getMonitorTime()  +
                                      bean.getSleepingTime() +
                                      bean.getWaitingTime();
            if (totalRunningTime > 0) {
                double percent = (bean.getRunningTime() / totalRunningTime) * 100;
                bean.setRunningPercent(percent);

                percent = (bean.getWaitingTime() / totalRunningTime) * 100;
                bean.setWaitingPercent(percent);

                percent = (bean.getMonitorTime() / totalRunningTime) * 100;
                bean.setMonitorPercent(percent);

                percent = (bean.getSleepingTime() / totalRunningTime) * 100;
                bean.setSleepingPercent(percent);
            }

            bean.setBlockedCount(thread.getBlockedCount());
            bean.setWaitedCount(thread.getWaitedCount());

            bean.setStopTimeStamp(thread.getTimeStamp());

            bean.addStackTrace(StackTrace.fromJson(thread.getStackTrace()));

            threadTableView.display(bean);

            boolean _stopLooping = stopLooping;
            return !_stopLooping;
        }
    }

    void setCurrentStateTime(ThreadTableBean bean, ThreadState thread) {
        Thread.State threadState = Thread.State.valueOf(thread.getState());

        switch (threadState) {
            case RUNNABLE:
                bean.setRunningTime(bean.getRunningTime() + 1);
                break;

            case BLOCKED:
                bean.setMonitorTime(bean.getMonitorTime() + 1);
                break;

            case TIMED_WAITING:
                bean.setSleepingTime(bean.getSleepingTime() + 1);
                break;

            case WAITING:
                bean.setWaitingTime(bean.getWaitingTime() + 1);
                break;

            case NEW:
            case TERMINATED:
            default:
                break;
        }
    }
}

