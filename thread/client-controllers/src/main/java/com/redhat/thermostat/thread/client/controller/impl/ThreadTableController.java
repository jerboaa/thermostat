/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView;
import com.redhat.thermostat.thread.model.ThreadInfoData;

public class ThreadTableController extends CommonController {
    
    private ThreadTableView threadTableView;
    private ThreadCollector collector;
    
    public ThreadTableController(ThreadTableView threadTableView,
                                 ThreadCollector collector,
                                 Timer timer)
    {
        super(timer, threadTableView);
        timer.setAction(new ThreadTableControllerAction());

        this.collector = collector;
        this.threadTableView = threadTableView;
    }

    
    private class ThreadTableControllerAction implements Runnable {
        @Override
        public void run() {
            List<ThreadInfoData> infos = collector.getThreadInfo();
            if(infos.size() > 0) {
                
                long lastPollTimestamp = infos.get(0).getTimeStamp();
                
                // build the statistics for each thread in the
                // collected thread list
                
                // first, get a map of all threads with the respective info
                // the list will contain an ordered-by-timestamp list
                // with the known history for each thread
                Map<ThreadInfoData, List<ThreadInfoData>> stats =
                        ThreadInfoHelper.getThreadInfoDataMap(infos);
                
                List<ThreadTableBean> tableBeans = new ArrayList<>();
                
                // now we have the list, we can do all the analysis we need
                for (ThreadInfoData key : stats.keySet()) {
                    ThreadTableBean bean = new ThreadTableBean();
                    
                    bean.setName(key.getThreadName());
                    bean.setId(key.getThreadId());
                    
                    bean.setWaitedCount(key.getThreadWaitCount());
                    bean.setBlockedCount(key.getThreadBlockedCount());
                    
                    // get start time and stop time, if any
                    List<ThreadInfoData> beanList = stats.get(key);
                    long last = beanList.get(0).getTimeStamp();
                    long first = beanList.get(beanList.size() - 1).getTimeStamp();
                    
                    bean.setStartTimeStamp(first);
                    if (last < lastPollTimestamp) {
                        // this thread died somewhere after this polling time... rip
                        bean.setStopTimeStamp(last);
                    }
                    
                    // time for some stats
                    double running = 0;
                    double waiting = 0;
                    double monitor = 0;
                    double sleeping = 0;
                    for (ThreadInfoData info : beanList) {
                        State state = info.getState();
                        switch (state) {
                        case RUNNABLE:
                            running++;
                            break;
                        case NEW:
                        case TERMINATED:
                            break;
                        case BLOCKED:
                            monitor++;
                            break;
                        case TIMED_WAITING:
                            sleeping++;
                            break;
                        case WAITING:
                            waiting++;
                        default:
                            break;
                        }
                    }
                    int polls = beanList.size();
                    double percent = (running/polls) * 100;
                    bean.setRunningPercent(percent);

                    percent = (waiting/polls) * 100;
                    bean.setWaitingPercent(percent);
                    
                    percent = (monitor/polls) * 100;
                    bean.setMonitorPercent(percent);

                    percent = (sleeping/polls) * 100;
                    bean.setSleepingPercent(percent);

                    tableBeans.add(bean);
                }
                
                threadTableView.display(tableBeans);
            }
            
        }
    }
}

