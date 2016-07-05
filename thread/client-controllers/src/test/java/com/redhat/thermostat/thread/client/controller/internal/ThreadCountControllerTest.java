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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.common.internal.test.Bug;
import com.redhat.thermostat.thread.client.controller.internal.cache.AppCache;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.model.Range;
import com.redhat.thermostat.thread.client.common.chart.LivingDaemonThreadDifferenceChart;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.view.ThreadCountView;
import com.redhat.thermostat.thread.model.SessionID;
import com.redhat.thermostat.thread.model.ThreadSession;
import com.redhat.thermostat.thread.model.ThreadSummary;

import net.java.openjdk.cacio.ctc.junit.CacioFESTRunner;

// this is not a GUI test, but testGetThreadInformation uses AWT under the hood 
@RunWith(CacioFESTRunner.class)
public class ThreadCountControllerTest {
    
    private Timer timer;
    private Runnable threadAction;
    private ThreadCountView view;
    private ThreadCollector collector;
    
    private ActionListener<ThreadCountView.Action> actionListener;
    private AppCache cache;

    @Before
    public void setUp() {
        timer = mock(Timer.class);
        view = mock(ThreadCountView.class);
        collector = mock(ThreadCollector.class);
        cache = mock(AppCache.class);
    }
    
    @Test
    public void testGetThreadInformation() {
        
        ArgumentCaptor<LivingDaemonThreadDifferenceChart> modelCaptor =
                ArgumentCaptor.forClass(LivingDaemonThreadDifferenceChart.class);
        
        
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(captor.capture());

        ThreadCountController controller = new ThreadCountController(view, collector, timer, cache);
        controller.initialize();

        ThreadSummary summary = mock(ThreadSummary.class);
        when(summary.getTimeStamp()).thenReturn(5l);
        when(summary.getCurrentLiveThreads()).thenReturn(42l);
        when(summary.getCurrentDaemonThreads()).thenReturn(2l);

        ThreadSummary summary0 = mock(ThreadSummary.class);
        when(summary0.getTimeStamp()).thenReturn(2l);
        when(summary0.getCurrentLiveThreads()).thenReturn(43l);
        when(summary0.getCurrentDaemonThreads()).thenReturn(1l);
        
        List<ThreadSummary> summaries = new ArrayList<>();
        summaries.add(summary);
        summaries.add(summary0);

        SessionID lastSession = mock(SessionID.class);
        when(collector.getLastThreadSession()).thenReturn(lastSession);

        when(collector.getLatestThreadSummary()).thenReturn(summary);

        List<ThreadSession> sessions = new ArrayList<>();
        when(collector.getThreadSessions(isA(Range.class))).thenReturn(sessions);
        when(collector.getThreadSummary(isA(Range.class))).thenReturn(summaries);

        threadAction = captor.getValue();
        threadAction.run();

        verify(collector).getLatestThreadSummary();

        verify(view).setLiveThreads("42");
        verify(view).setDaemonThreads("2");
        
        verify(view).updateLivingDaemonTimeline(modelCaptor.capture());
        LivingDaemonThreadDifferenceChart model = modelCaptor.getValue();
        
        assertNotNull(model);
        
        JFreeChart chart = model.createChart(100, Color.BLACK);
        XYDataset dataSet = chart.getXYPlot().getDataset();
        assertEquals(2, dataSet.getSeriesCount());
        
        // total and living
        assertEquals(2l, dataSet.getX(0, 0));
        assertEquals(5l, dataSet.getX(0, 1));

        // the actual numbers
        assertEquals(43.0, dataSet.getY(0, 0));
        assertEquals(42.0, dataSet.getY(0, 1));
        assertEquals(1.0, dataSet.getY(1, 0));        
        assertEquals(2.0, dataSet.getY(1, 1));
    }
    
    @Test
    public void testTimerStartAndStop() {
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());

        ThreadCountController controller = new ThreadCountController(view, collector, timer, cache);
        controller.initialize();
                
        actionListener = viewArgumentCaptor.getValue();
        actionListener.actionPerformed(new ActionEvent<>(view, BasicView.Action.VISIBLE));
        
        verify(timer).start();
        
        actionListener.actionPerformed(new ActionEvent<>(view, BasicView.Action.HIDDEN));

        verify(timer).stop();        
    }

    @Bug(id  = "2037",
         url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=2037",
         summary = " Thread count view queries all summaries for the last hour, every second")
    @Test
    public void testPR2037() throws InterruptedException {
        ArgumentCaptor<Range> collectorCaptor = ArgumentCaptor.forClass(Range.class);
        when(collector.getThreadSummary(collectorCaptor.capture())).thenReturn(new ArrayList());
        when(collector.getLatestThreadSummary()).thenReturn(mock(ThreadSummary.class));

        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(captor.capture());

        ThreadCountController controller = new ThreadCountController(view, collector, timer, cache);
        controller.initialize();

        threadAction = captor.getValue();
        threadAction.run();

        Range<Long> range1 =  collectorCaptor.getValue();

        // The code relies on the fact that it can register at least a
        // millisecond difference between the two calls of run, otherwise the
        // range remains the same and doesn't execute the actionPerformed
        Thread.sleep(1);

        threadAction.run();
        Range<Long> range2 =  collectorCaptor.getValue();

        assertEquals(range1.getMax(), range2.getMin());
    }
}

