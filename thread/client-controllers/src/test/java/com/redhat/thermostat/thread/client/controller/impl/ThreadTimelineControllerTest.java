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
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.model.timeline.Timeline;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineDimensionModel;
import com.redhat.thermostat.thread.client.common.model.timeline.TimelineGroupDataModel;
import com.redhat.thermostat.thread.client.common.view.ThreadTimelineView;
import com.redhat.thermostat.thread.model.ThreadHeader;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ThreadTimelineControllerTest {

    private ArgumentCaptor<Runnable> timerActionCaptor;
    private Timer timer;

    private ThreadTimelineView view;
    private ThreadCollector collector;
    private TimelineDimensionModel timelineDimensionModel;
    private TimelineGroupDataModel groupDataModel;

    private Range<Long> totalRange = new Range<>(0l, 30_000l);

    @Before
    public void setUp() throws Exception {
        timer = mock(Timer.class);
        timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        view = mock(ThreadTimelineView.class);
        collector = mock(ThreadCollector.class);
        timelineDimensionModel = mock(TimelineDimensionModel.class);
        when(timelineDimensionModel.getLengthInMillis()).thenReturn(25_000l);
        when(collector.getThreadStateTotalTimeRange()).thenReturn(totalRange);

        groupDataModel = mock(TimelineGroupDataModel.class);
        when(view.getGroupDataModel()).thenReturn(groupDataModel);
    }

    @Test
    public void testTimelineController() {

        Range<Long> pageRange = new Range<>(5_000l, 30_000l);
        ArgumentCaptor<Range> pageRangeCaptor = ArgumentCaptor.forClass(Range.class);
        doNothing().when(groupDataModel).setPageRange(pageRangeCaptor.capture());

        ThreadHeader thread1 = mock(ThreadHeader.class);
        ThreadHeader thread2 = mock(ThreadHeader.class);

        List<ThreadHeader> threads = new ArrayList<>();

        threads.add(thread1);
        threads.add(thread2);

        when(collector.getThreads()).thenReturn(threads);

        ThreadTimelineController controller =
                new ThreadTimelineController(view, collector, timer,
                                             timelineDimensionModel);

        Runnable controllerRunnable = timerActionCaptor.getValue();

        controllerRunnable.run();

        // check that the thread list is correctly passed to the view
        verify(view).updateThreadList(threads);

        // verify group model gets updated with the page and total data
        verify(groupDataModel).setTotalRange(totalRange);
        Range<Long> pageRangeResult = pageRangeCaptor.getValue();
        assertEquals(pageRangeResult, pageRange);

        // check that the thread state is queried for each of the thread headers
        verify(collector).getThreadStates(eq(thread1), any(Range.class));
        verify(collector).getThreadStates(eq(thread2), any(Range.class));

        verify(view).displayTimeline(eq(thread1), any(Timeline.class));
        verify(view).displayTimeline(eq(thread2), any(Timeline.class));

        verify(view).submitChanges();
    }
}
