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

package com.redhat.thermostat.thread.client.controller.impl;

import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.storage.core.experimental.statement.ResultHandler;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.model.timeline.ThreadInfo;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadState;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ThreadTableController extends CommonController {
    
    private ThreadTableView threadTableView;
    private ThreadCollector collector;

    public ThreadTableController(ThreadTableView threadTableView,
                                 ThreadCollector collector,
                                 Timer timer)
    {
        super(timer, threadTableView);
        this.threadTableView = threadTableView;
        this.collector = collector;
        timer.setAction(new ThreadTableControllerAction());
    }

    private class ThreadTableControllerAction implements Runnable {

        private ThreadResultHandler handler;
        private Range<Long> range;
        private SessionID lastSession;
        private long lastUpdate;

        public ThreadTableControllerAction() {
            handler = new ThreadResultHandler();
            resetState();
        }

        private void resetState() {
            handler.threadStates.clear();
            threadTableView.clear();
            range = null;
            lastUpdate = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1);
        }

        @Override
        public void run() {

            SessionID session = collector.getLastThreadSession();
            if (session == null) {
                // ok, no data, let's skip this round
                return;
            }
            if (lastSession == null ||
                    !session.get().equals(lastSession.get())) {
                // since we only visualise one sessions at a time and this
                // is a new session needs, let's clear the view
                resetState();
            }
            lastSession = session;

            // get the full range of known timelines per vm
            Range<Long> totalRange = collector.getThreadRange(session);
            if (totalRange == null) {
                // this just means we don't have any data yet
                return;
            }

            range = new Range<>(lastUpdate, totalRange.getMax());
            lastUpdate = totalRange.getMax();

            collector.getThreadStates(session,
                                      handler,
                                      range);
            threadTableView.submitChanges();
        }
    }

    private class ThreadResultHandler implements ResultHandler<ThreadState> {
        private Map<ThreadInfo, ThreadTableBean> threadStates;

        public ThreadResultHandler() {
            this.threadStates = new HashMap<>();
        }

        @Override
        public void onResult(ThreadState thread) {

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

            threadTableView.display(bean);
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

