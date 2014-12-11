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
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView;

public class ThreadTableController extends CommonController {
    
    private ThreadTableView threadTableView;
    private ThreadCollector collector;

    private Range<Long> lastRangeChecked;
//    private Map<ThreadHeader, ThreadTableBean> threadStates;

    public ThreadTableController(ThreadTableView threadTableView,
                                 ThreadCollector collector,
                                 Timer timer)
    {
        super(timer, threadTableView);
        timer.setAction(new ThreadTableControllerAction());

//        threadStates = new HashMap<>();
        this.collector = collector;
        this.threadTableView = threadTableView;
    }

    
    private class ThreadTableControllerAction implements Runnable {
        @Override
        public void run() {

//            if (lastRangeChecked == null) {
//                lastRangeChecked = collector.getThreadStateTotalTimeRange();
//            } else {
//                lastRangeChecked = new Range<>(lastRangeChecked.getMax(),
//                                               System.currentTimeMillis());
//            }
//
//            List<ThreadTableBean> tableBeans = new ArrayList<>();
//
//            List<ThreadHeader> threads = collector.getThreads();
//
//            for (ThreadHeader thread : threads) {
//
//                ThreadTableBean bean = threadStates.get(thread);
//                if (bean == null) {
//                    bean = new ThreadTableBean();
//                    bean.setName(thread.getThreadName());
//                    bean.setId(thread.getThreadId());
//
//                    threadStates.put(thread, bean);
//                }
//
//                List<ThreadState> states = collector.getThreadStates(thread, lastRangeChecked);
//                for (ThreadState state : states) {
//
//                    Thread.State threadState = Thread.State.valueOf(state.getState());
//
//                    Range<Long> range = state.getRange();
//                    long currentRangeInCollection = range.getMax() - range.getMin();
//                    long currentStateInBean = getCurrentStateInBean(bean, threadState);
//
//                    currentStateInBean += currentRangeInCollection;
//                    setCurrentStateInBean(bean, threadState, currentStateInBean);
//
//                    double totalRunningTime = bean.getRunningTime()  +
//                                              bean.getMonitorTime()  +
//                                              bean.getSleepingTime() +
//                                              bean.getWaitingTime();
//
//                    if (totalRunningTime > 0) {
//                        double percent = (bean.getRunningTime() / totalRunningTime) * 100;
//                        bean.setRunningPercent(percent);
//
//                        percent = (bean.getWaitingTime() / totalRunningTime) * 100;
//                        bean.setWaitingPercent(percent);
//
//                        percent = (bean.getMonitorTime() / totalRunningTime) * 100;
//                        bean.setMonitorPercent(percent);
//
//                        percent = (bean.getSleepingTime() / totalRunningTime) * 100;
//                        bean.setSleepingPercent(percent);
//                    }
//                }
//
//                // check the latest stat regarding wait and block count
//                ThreadContentionSample sample = collector.getLatestContentionSample(thread);
//                if (sample != null) {
//                    bean.setBlockedCount(sample.getBlockedCount());
//                    bean.setWaitedCount(sample.getWaitedCount());
//                }
//
//                // finally, the time range for this thread
//                Range<Long> dataRange = collector.getThreadStateRange(thread);
//                if (dataRange != null) {
//                    bean.setStartTimeStamp(dataRange.getMin());
//                    bean.setStopTimeStamp(dataRange.getMax());
//                }
//
//                tableBeans.add(bean);
//            }
//
//            threadTableView.display(tableBeans);
        }
    }

    long getCurrentStateInBean(ThreadTableBean bean, Thread.State threadState) {
        long currentStateInBean = 0l;

        switch (threadState) {
            case RUNNABLE:
                currentStateInBean = bean.getRunningTime();
                break;

            case BLOCKED:
                currentStateInBean = bean.getMonitorTime();
                break;

            case TIMED_WAITING:
                currentStateInBean = bean.getSleepingTime();
                break;

            case WAITING:
                currentStateInBean = bean.getWaitingTime();
                break;

            case NEW:
            case TERMINATED:
            default:
                break;
        }

        return currentStateInBean;
    }

    void setCurrentStateInBean(ThreadTableBean bean, Thread.State threadState, long range) {

        switch (threadState) {
            case RUNNABLE:
                bean.setRunningTime(range);
                break;

            case BLOCKED:
                bean.setMonitorTime(range);
                break;

            case TIMED_WAITING:
                bean.setSleepingTime(range);
                break;

            case WAITING:
                bean.setWaitingTime(range);
                break;

            case NEW:
            case TERMINATED:
            default:
                break;
        }
    }
}

