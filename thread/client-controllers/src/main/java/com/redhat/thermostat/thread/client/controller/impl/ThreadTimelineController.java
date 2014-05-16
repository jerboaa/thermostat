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

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.common.chart.ChartColors;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.model.timeline.Timeline;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineDimensionModel;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineInfo;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView.ThreadTimelineViewAction;
import com.redhat.thermostat.thread.model.ThreadHeader;
import com.redhat.thermostat.thread.model.ThreadState;
import java.util.List;

public class ThreadTimelineController extends CommonController {

    private static final boolean _DEBUG_BLOCK_TIMING_ = false;

    private ThreadTimelineView view;
    private ThreadCollector collector;
    
    private static final long EXTRA_TIMELINE_BUFFER = 2000;
    
    private final String lock = new String("ThreadTimelineController"); 

    private boolean followMode;

    private TimelineDimensionModel timelineDimensionModel;
    private Range<Long> pageRangeInStaticMode;

    public ThreadTimelineController(ThreadTimelineView view, ThreadCollector collector, Timer timer,
                                    TimelineDimensionModel timelineDimensionModel) {
        super(timer, view);
        timer.setAction(new ThreadTimelineControllerAction());
        this.view = view;
        this.view.addThreadSelectionActionListener(new ThreadTimelineSelectedAction());
        this.collector = collector;
        followMode = false;

        this.timelineDimensionModel = timelineDimensionModel;
    }

    private class ThreadTimelineSelectedAction implements ActionListener<ThreadTimelineViewAction> {
        
        @Override
        public void actionPerformed(ActionEvent<ThreadTimelineViewAction> actionEvent) {
            switch (actionEvent.getActionId()) {
            case THREAD_TIMELINE_SELECTED:
                break;
                
            case SWITCH_TO_FOLLOW_MODE:
                synchronized (lock) {
                    followMode = true;
                }
                view.ensureTimelineState(ThreadTimelineView.TimelineSelectorState.FOLLOWING);
                break;
                
            case SWITCH_TO_STATIC_MODE:
                synchronized (lock) {                    
                    followMode = false;
                    pageRangeInStaticMode = (Range<Long>) actionEvent.getPayload();
                }
                view.ensureTimelineState(ThreadTimelineView.TimelineSelectorState.STATIC);
                break;
            
            default:
                break;
            }
        }
    }
    
    private class ThreadTimelineControllerAction implements Runnable {

        @Override
        public void run() {
            
            synchronized (lock) {

                long timelineLength = timelineDimensionModel.getLengthInMillis();

                Range<Long> totalRange = collector.getThreadStateTotalTimeRange();
                if (totalRange == null) {
                    // that simply means we don't have data yet, let's just skip
                    // this loop
                    return;
                }
                view.getGroupDataModel().setTotalRange(totalRange);

                List<ThreadHeader> threads = collector.getThreads();

                view.updateThreadList(threads);

                Range<Long> pageRange = null;
                Range<Long> visibleRange = null;
                List<ThreadState> states = null;
                if (followMode || pageRangeInStaticMode == null) {
                    // get the latest info available, ensure a little of extra
                    // buffer so that we are sure to have continuity around the
                    // timeline edges
                    long max = totalRange.getMax();
                    long pageMin = max - timelineLength;
                    pageRange = new Range<>(pageMin, totalRange.getMax());

                    long min = pageMin - EXTRA_TIMELINE_BUFFER;
                    visibleRange = new Range<>(min, max);

                    view.getGroupDataModel().setPageRange(pageRange);

                } else {
                    long max = pageRangeInStaticMode.getMax() + EXTRA_TIMELINE_BUFFER;
                    long pageMin = max - timelineLength;
                    long min = pageMin - EXTRA_TIMELINE_BUFFER;
                    visibleRange = new Range<>(min, max);
                    view.getGroupDataModel().setPageRange(pageRangeInStaticMode);
                }

                long sampleStart = 0l;
                if (_DEBUG_BLOCK_TIMING_) {
                    sampleStart = System.currentTimeMillis();
                }

                for (ThreadHeader thread : threads) {
                    states = collector.getThreadStates(thread, visibleRange);
                    Timeline threadTimeline = new Timeline(thread.getThreadName(), thread.getThreadId());
                    for (ThreadState state : states) {
                        TimelineInfo info = new TimelineInfo();
                        info.setColor(ChartColors.getPaletteColor(state.getState()));
                        info.setRange(state.getRange());
                        threadTimeline.add(info);
                    }
                    view.displayTimeline(thread, threadTimeline);
                }

                if (_DEBUG_BLOCK_TIMING_) {
                    long sampleStop = System.currentTimeMillis();
                    System.err.println("getThreadStates time: " + (sampleStop - sampleStart));
                }

                view.submitChanges();
            }
        }
    }
}

