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

import static org.junit.Assert.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.common.Timeline;
import com.redhat.thermostat.thread.client.common.TimelineInfo;
import com.redhat.thermostat.thread.client.common.chart.ChartColors;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.view.ThreadTableView;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.model.ThreadInfoData;

public class ThreadTimelineControllerTest {

    private ThreadTimelineView view;
    private ThreadCollector collector;
    
    private Timer timer;
    
    private ActionListener<ThreadTableView.Action> actionListener;
    ArgumentCaptor<Runnable> timerActionCaptor;
    
    @Before
    public void setUp() {
        collector = mock(ThreadCollector.class);
        
        timer = mock(Timer.class);
        
        view = mock(ThreadTimelineView.class);
        
        setUpTimers();
    }
    
    private void setUpTimers() {
        timer = mock(Timer.class);
        timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());
    }
    
    @Test
    public void testDisplayStats() {
        
        ThreadInfoData data1 = new ThreadInfoData();
        data1.setThreadName("test1");
        data1.setThreadId(1);
        data1.setState(Thread.State.RUNNABLE);
        data1.setTimeStamp(100);
        
        ThreadInfoData data2 = new ThreadInfoData();
        data2.setThreadName("test2");
        data2.setThreadId(2);
        data2.setTimeStamp(1000);
        data2.setState(Thread.State.BLOCKED);
        
        ThreadInfoData data3 = new ThreadInfoData();
        data3.setThreadName("test1");
        data3.setThreadId(1);
        data3.setState(Thread.State.TIMED_WAITING);
        data3.setTimeStamp(200);
        
        ThreadInfoData data4 = new ThreadInfoData();
        data4.setThreadName("test2");
        data4.setThreadId(2);
        data4.setState(Thread.State.BLOCKED);
        data4.setTimeStamp(2000);
        
        ThreadInfoData data5 = new ThreadInfoData();
        data5.setThreadName("test2");
        data5.setThreadId(2);
        data5.setState(Thread.State.RUNNABLE);
        data5.setTimeStamp(3000);
        
        List<ThreadInfoData> infos = new ArrayList<>();
        // descending order
        infos.add(data5);
        infos.add(data4);
        infos.add(data2);
        infos.add(data3);
        infos.add(data1);

        when(collector.getThreadInfo()).thenReturn(infos);
        
        ArgumentCaptor<Runnable> timerCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerCaptor.capture());
        
        ThreadTimelineController controller = new ThreadTimelineController(view, collector, timer);
        controller.initialize();
        
        Runnable action = timerCaptor.getValue();
        action.run();
        
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Range> rangeCaptor = ArgumentCaptor.forClass(Range.class);

        verify(view).displayStats(listCaptor.capture(), rangeCaptor.capture());
        
        List viewResult = listCaptor.getValue();
        Range<Long> rangeResult = rangeCaptor.getValue();
        assertEquals(2, viewResult.size());
        assertEquals(100l, (long) rangeResult.getMin());
        assertEquals(3000l, (long) rangeResult.getMax());

        Timeline timeline = (Timeline) viewResult.get(0);
        assertEquals(2, timeline.getId());
        assertEquals("test2", timeline.getName());
        assertEquals(3, timeline.size());
        
        TimelineInfo [] timelineInfos = timeline.toArray();
        assertEquals(ChartColors.getPaletteColor(data2.getState()), timelineInfos[0].getColor());
        assertEquals(ChartColors.getPaletteColor(data4.getState()), timelineInfos[1].getColor());
        assertEquals(ChartColors.getPaletteColor(data5.getState()), timelineInfos[2].getColor());

        
        timeline = (Timeline) viewResult.get(1);
        assertEquals(1, timeline.getId());
        assertEquals("test1", timeline.getName());
        assertEquals(2, timeline.size());
        
        timelineInfos = timeline.toArray();
        assertEquals(ChartColors.getPaletteColor(data1.getState()), timelineInfos[0].getColor());
        assertEquals(ChartColors.getPaletteColor(data3.getState()), timelineInfos[1].getColor());        
    }

    @Test
    public void testStartThreadTimelineController() {
        
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());
        
        ThreadTimelineController controller = new ThreadTimelineController(view, collector, timer);
        controller.initialize();

        actionListener = viewArgumentCaptor.getValue();
        actionListener.actionPerformed(new ActionEvent<>(view, BasicView.Action.VISIBLE));
        
        verify(timer).start();
        
        actionListener.actionPerformed(new ActionEvent<>(view, BasicView.Action.HIDDEN));

        verify(timer).stop();
    }
}

