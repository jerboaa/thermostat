/*
 * Copyright 2012 Red Hat, Inc.
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
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.client.osgi.service.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.thread.client.common.ThreadTableBean;
import com.redhat.thermostat.thread.client.common.ThreadTableView;
import com.redhat.thermostat.thread.collector.ThreadCollector;
import com.redhat.thermostat.thread.collector.ThreadInfo;

public class ThreadTableController implements CommonController {
    
    private ThreadTableView threadTableView;
    private ThreadCollector collector;
    private Timer timer;
    
    public ThreadTableController(ThreadTableView threadTableView,
                                 ThreadCollector collector,
                                 Timer timer)
    {
        this.collector = collector;
        this.threadTableView = threadTableView;
        this.timer = timer;
    }

    @Override
    public void initialize() {
        
        timer.setInitialDelay(0);
        timer.setDelay(1000);
        timer.setTimeUnit(TimeUnit.MILLISECONDS);
        timer.setSchedulingType(SchedulingType.FIXED_RATE);
        timer.setAction(new ThreadTableControllerAction());
        
        threadTableView.addActionListener(new ActionListener<Action>() {
            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                case VISIBLE:
                    timer.start();
                    break;

                case HIDDEN:
                    timer.stop();
                    break;

                default:
                    break;
                }
            }
        });
    }
    
    private class ThreadTableControllerAction implements Runnable {
        @Override
        public void run() {
            List<ThreadInfo> infos = collector.getThreadInfo();
            if(infos.size() > 0) {
                
                long lastPollTimestamp = infos.get(0).getTimeStamp();
                
                // build the statistics for each thread in the
                // collected thread list
                
                // first, get a map of all threads with the respective info
                // the list will contain an ordered-by-timestamp list
                // with the known history for each thread
                Map<ThreadInfo, List<ThreadInfo>> stats = new HashMap<>();
                for (ThreadInfo info : infos) {
                    List<ThreadInfo> beanList = stats.get(info);
                    if (beanList == null) {
                        beanList = new ArrayList<ThreadInfo>();
                        stats.put(info, beanList);
                    }                    
                    beanList.add(info);
                }
                
                List<ThreadTableBean> tableBeans = new ArrayList<>();
                
                // now we have the list, we can do all the analysis we need
                for (ThreadInfo key : stats.keySet()) {
                    ThreadTableBean bean = new ThreadTableBean();
                    
                    bean.setName(key.getName());
                    bean.setId(key.getThreadID());
                    
                    bean.setWaitedCount(key.getWaitedCount());
                    bean.setBlockedCount(key.getBlockedCount());
                    
                    // get start time and stop time, if any
                    List<ThreadInfo> beanList = stats.get(key);
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
                    for (ThreadInfo info : beanList) {
                        State state = info.getState();
                        switch (state) {
                        case RUNNABLE:
                            running++;
                            break;
                        case NEW:
                        case TERMINATED:
                            System.err.println("yeah!");
                            break;
                        case BLOCKED:
                        case TIMED_WAITING:
                        case WAITING:
                            waiting++;
                        default:
                            break;
                        }
                    }
                    int polls = beanList.size();
                    double runningPercent = (running/polls) * 100;
                    double waitingPercent = (waiting/polls) * 100;
                    
                    bean.setRunningPercent(runningPercent);
                    bean.setWaitingPercent(waitingPercent);
                    
                    tableBeans.add(bean);
                }
                
                threadTableView.display(tableBeans);
            }
            
        }
    }
}
