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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.model.LongRange;
import com.redhat.thermostat.thread.client.common.Timeline;
import com.redhat.thermostat.thread.client.common.TimelineInfo;
import com.redhat.thermostat.thread.client.common.chart.ChartColors;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView.ThreadTimelineViewAction;
import com.redhat.thermostat.thread.model.ThreadInfoData;

public class ThreadTimelineController extends CommonController {

    private ThreadTimelineView view;
    private ThreadCollector collector;
    
    private final String lock = new String("ThreadTimelineController"); 
    
    public ThreadTimelineController(ThreadTimelineView view, ThreadCollector collector, Timer timer) {
        super(timer, view);
        timer.setAction(new ThreadTimelineControllerAction());
        this.view = view;
        this.view.addThreadSelectionActionListener(new ThreadTimelineSelectedAction());
        this.collector = collector;
    }

    private class ThreadTimelineSelectedAction implements ActionListener<ThreadTimelineViewAction> {
        
        @Override
        public void actionPerformed(ActionEvent<ThreadTimelineViewAction> actionEvent) {
            // TODO
        }
    }
    
    private class ThreadTimelineControllerAction implements Runnable {
        @Override
        public void run() {
            
            synchronized (lock) {
                // FIXME: only load latest, not all the info all the time
                LongRange range = new LongRange(Long.MAX_VALUE, Long.MIN_VALUE);
                List<ThreadInfoData> infos = collector.getThreadInfo();
                if(infos.size() > 0) {
                    Map<ThreadInfoData, List<ThreadInfoData>> stats = ThreadInfoHelper.getThreadInfoDataMap(infos);
                    List<Timeline> timelines =  new ArrayList<>();
                    for (List<ThreadInfoData> beanList : stats.values()) {
                        Timeline timeline = new Timeline(beanList.get(0).getThreadName(), beanList.get(0).getThreadId());
 
                        for (ThreadInfoData data : beanList) {
                            Palette palette = ChartColors.getPaletteColor(data.getState());
                            long timestamp = data.getTimeStamp();
                            TimelineInfo info = new TimelineInfo(palette, timestamp);
                            timeline.add(info);

                            if (range.getMin() > timestamp) {
                                range.setMin(timestamp);
                            }
                            if (range.getMax() < timestamp) {
                                range.setMax(timestamp);
                            }
                        }
                        timelines.add(timeline);
                    }
                    view.displayStats(timelines, range);
                }
            }
        }
    }
}

