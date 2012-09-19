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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.thread.client.common.ThreadTimelineBean;
import com.redhat.thermostat.thread.client.common.ThreadTimelineView;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.model.ThreadInfoData;

public class ThreadTimelineController extends CommonController {

    private ThreadTimelineView view;
    private ThreadCollector collector;
    
    public ThreadTimelineController(ThreadTimelineView view, ThreadCollector collector, Timer timer) {
        super(timer, view);
        timer.setAction(new ThreadTimelineControllerAction());
        this.view = view;
        this.collector = collector;
    }

    private class ThreadTimelineControllerAction implements Runnable {
        @Override
        public void run() {
            List<ThreadInfoData> infos = collector.getThreadInfo();
            if(infos.size() > 0) {
                
                Map<ThreadInfoData, List<ThreadTimelineBean>> timelines = new HashMap<>();
                
                Map<ThreadInfoData, List<ThreadInfoData>> stats =
                        ThreadInfoHelper.getThreadInfoDataMap(infos);
                for (List<ThreadInfoData> beanList : stats.values()) {
                    
                    // the list is ordered in most recent first
                    // the first element is the latest sample we have of this
                    // thread, so we use it as stop time. 
                    
                    ThreadInfoData lastThreadInfo = beanList.get(beanList.size() - 1);
                    Thread.State lastState = lastThreadInfo.getState();
                    
                    ThreadTimelineBean timeline = new ThreadTimelineBean();
                    timeline.setName(lastThreadInfo.getThreadName());
                    timeline.setState(lastThreadInfo.getState());
                    timeline.setStartTime(lastThreadInfo.getTimeStamp()); 
                    
                    long stopTime = beanList.get(0).getTimeStamp();
                    timeline.setStopTime(stopTime);
                    
                    Stack<ThreadTimelineBean> threadTimelines = new Stack<ThreadTimelineBean>();
                    timelines.put(lastThreadInfo, threadTimelines);
                    threadTimelines.push(timeline);
                    
                    for (int i = beanList.size() - 1; i >= 0; i--) {
                        ThreadInfoData threadInfo = beanList.get(i);
                        
                        Thread.State currentState = threadInfo.getState();
                        if (currentState != lastState) {
                            lastState = currentState;
                            
                            timeline = threadTimelines.pop();
                            timeline.setStopTime(threadInfo.getTimeStamp());
                            
                            threadTimelines.push(timeline);
                            
                            timeline = new ThreadTimelineBean();
                            timeline.setName(threadInfo.getThreadName());
                            timeline.setState(threadInfo.getState());
                            timeline.setStartTime(threadInfo.getTimeStamp());
                            timeline.setStopTime(stopTime);

                            lastThreadInfo = threadInfo;
                            lastState = currentState;
                            
                            // add the new thread stat
                            threadTimelines.push(timeline);
                        }
                    }
                }
                
                view.displayStats(timelines, infos.get(infos.size() - 1).getTimeStamp(), infos.get(0).getTimeStamp());
            }
        }
    }
}
