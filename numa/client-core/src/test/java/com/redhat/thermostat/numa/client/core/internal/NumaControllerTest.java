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

package com.redhat.thermostat.numa.client.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Duration;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.numa.client.core.NumaView;
import com.redhat.thermostat.numa.client.core.NumaViewProvider;
import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.numa.common.NumaNodeStat;
import com.redhat.thermostat.numa.common.NumaStat;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.model.DiscreteTimeData;

public class NumaControllerTest {

    private NumaController numaController;
    private Timer timer;
    private NumaView view;
    @SuppressWarnings("rawtypes")
    private ArgumentCaptor<ActionListener> actionListener;
    private ArgumentCaptor<Runnable> timerAction;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        ApplicationService appSvc = mock(ApplicationService.class);
        TimerFactory timerFactory = mock(TimerFactory.class);
        timer = mock(Timer.class);
        timerAction = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerAction.capture());
        when(timerFactory.createTimer()).thenReturn(timer);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);
        HostRef hostRef = new HostRef("fluff", "boo");
        NumaDAO numaDAO = mock(NumaDAO.class);

        List<NumaStat> stats = createTestData();
        when(numaDAO.getLatestNumaStats(eq(hostRef), anyLong())).thenReturn(stats);

        when(numaDAO.getNumberOfNumaNodes(hostRef)).thenReturn(3);
        NumaViewProvider numaViewProvider = mock(NumaViewProvider.class);
        view = mock(NumaView.class);
        when(view.getUserDesiredDuration()).thenReturn(new Duration(10, TimeUnit.MINUTES));
        actionListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(actionListener.capture());
        when(numaViewProvider.createView()).thenReturn(view);
        numaController = new NumaController(appSvc, numaDAO, hostRef, numaViewProvider);
    }

    private List<NumaStat> createTestData() {
        NumaNodeStat nodeStat11 = new NumaNodeStat();
        nodeStat11.setNumaHit(100);
        nodeStat11.setNumaMiss(0);
        nodeStat11.setNumaForeign(0);
        NumaNodeStat nodeStat12 = new NumaNodeStat();
        nodeStat12.setNumaHit(50);
        nodeStat12.setNumaMiss(50);
        nodeStat12.setNumaForeign(10);
        NumaNodeStat nodeStat13 = new NumaNodeStat();
        nodeStat13.setNumaHit(70);
        nodeStat13.setNumaMiss(30);
        nodeStat13.setNumaForeign(0);
        NumaStat stat1 = new NumaStat("fluff");
        stat1.setTimeStamp(100);
        stat1.setNodeStats(new NumaNodeStat[] {nodeStat11, nodeStat12, nodeStat13 });
        NumaNodeStat nodeStat21 = new NumaNodeStat();
        nodeStat21.setNumaHit(110);
        nodeStat21.setNumaMiss(10);
        nodeStat21.setNumaForeign(50);
        NumaNodeStat nodeStat22 = new NumaNodeStat();
        nodeStat22.setNumaHit(60);
        nodeStat22.setNumaMiss(55);
        nodeStat22.setNumaForeign(15);
        NumaNodeStat nodeStat23 = new NumaNodeStat();
        nodeStat23.setNumaHit(80);
        nodeStat23.setNumaMiss(40);
        nodeStat23.setNumaForeign(5);
        NumaStat stat2 = new NumaStat("fluff");
        stat2.setTimeStamp(200);
        stat2.setNodeStats(new NumaNodeStat[] {nodeStat21, nodeStat22, nodeStat23 });
        List<NumaStat> stats = Arrays.asList(stat1, stat2);
        return stats;
    }

    @After
    public void tearDown() {
        timer = null;
        view = null;
        numaController = null;
    }

    @Test
    public void verifyTimerSettings() {
        verify(timer).setAction(any(Runnable.class));
        verify(timer).setSchedulingType(SchedulingType.FIXED_RATE);
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
        verify(timer).setInitialDelay(0);
        verify(timer).setDelay(5);
        verifyNoMoreInteractions(timer);
    }

    @Test
    public void verifyNumCharts() {
        verify(view).addChart(eq("Node 0"));
        verify(view).addChart(eq("Node 1"));
        verify(view).addChart(eq("Node 2"));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void verifyViewActions() {
        actionListener.getValue().actionPerformed(new ActionEvent(view, NumaView.Action.VISIBLE));
        verify(timer).start();

        actionListener.getValue().actionPerformed(new ActionEvent(view, NumaView.Action.HIDDEN));
        verify(timer).stop();

        // Can't test the default branch.
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void verifyTimerAction() {
        timerAction.getValue().run();
        ArgumentCaptor<List> dataCaptor = ArgumentCaptor.forClass(List.class);
        verify(view).addData(eq("Node 0"), dataCaptor.capture());
        verify(view).addData(eq("Node 1"), dataCaptor.capture());
        verify(view).addData(eq("Node 2"), dataCaptor.capture());

        List list1 = dataCaptor.getAllValues().get(0);
        DiscreteTimeData<Double>[] data1 = (DiscreteTimeData<Double>[]) list1.get(0);
        assertEquals((Double)0.1, data1[0].getData());
        assertEquals((Double)0.1, data1[1].getData());
        assertEquals((Double)0.5, data1[2].getData());

        List list2 = dataCaptor.getAllValues().get(1);
        DiscreteTimeData<Double>[] data2 = (DiscreteTimeData<Double>[]) list2.get(0);
        assertEquals((Double)0.1, data2[0].getData());
        assertEquals((Double)0.05, data2[1].getData());
        assertEquals((Double)0.05, data2[2].getData());

        List list3 = dataCaptor.getAllValues().get(2);
        DiscreteTimeData<Double>[] data3 = (DiscreteTimeData<Double>[]) list3.get(0);
        assertEquals((Double)0.1, data3[0].getData());
        assertEquals((Double)0.1, data3[1].getData());
        assertEquals((Double)0.05, data3[2].getData());

    }

    @Test
    public void testView() {
        assertSame(view, numaController.getView());
    }

    @Test
    public void testLocalizedName() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertEquals("NUMA", numaController.getLocalizedName().getContents());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}

