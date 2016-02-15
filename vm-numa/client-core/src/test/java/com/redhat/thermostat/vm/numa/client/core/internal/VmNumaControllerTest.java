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

package com.redhat.thermostat.vm.numa.client.core.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.model.DiscreteTimeData;
import com.redhat.thermostat.vm.numa.client.core.VmNumaView;
import com.redhat.thermostat.vm.numa.client.core.VmNumaViewProvider;
import com.redhat.thermostat.vm.numa.common.NumaMemoryLocations;
import com.redhat.thermostat.vm.numa.common.VmNumaDAO;
import com.redhat.thermostat.vm.numa.common.VmNumaNodeStat;
import com.redhat.thermostat.vm.numa.common.VmNumaStat;

public class VmNumaControllerTest {

    private VmNumaController vmNumaController;
    private Timer timer;
    private VmNumaView view;
    @SuppressWarnings("rawtypes")
    private ArgumentCaptor<ActionListener> actionListener;
    private ArgumentCaptor<Runnable> timerAction;

    private static final long TIMESTAMP = 1000;
    private static final double DATA = 50;
    private static final int NUM_NODES = 2;

    @Before
    public void setup() {
        ApplicationService appSvc = mock(ApplicationService.class);
        TimerFactory timerFactory = mock(TimerFactory.class);
        timer = mock(Timer.class);
        timerAction = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerAction.capture());
        when(timerFactory.createTimer()).thenReturn(timer);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);

        AgentId agentId = new AgentId("agent");
        VmId vmId = new VmId("vm");

        NumaDAO numaDAO = mock(NumaDAO.class);
        when(numaDAO.getNumberOfNumaNodes(any(HostRef.class))).thenReturn(NUM_NODES);
        VmNumaDAO vmNumaDAO = mock(VmNumaDAO.class);
        when(vmNumaDAO.getNumaStats(eq(agentId), eq(vmId), anyLong(), anyLong())).thenReturn(createStats());

        VmNumaViewProvider viewProvider = mock(VmNumaViewProvider.class);
        view = mock(VmNumaView.class);
        when(viewProvider.createView()).thenReturn(view);

        actionListener = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(actionListener.capture());

        vmNumaController = new VmNumaController(appSvc, numaDAO, vmNumaDAO, vmId, agentId, viewProvider);
    }

    private List<VmNumaStat> createStats() {
        List<VmNumaStat> stats = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            VmNumaStat stat = new VmNumaStat("agent");
            stat.setTimeStamp(i * TIMESTAMP);
            stat.setVmId("vm");

            List<VmNumaNodeStat> nodeStats = new ArrayList<>();
            for (int j = 0; j < NUM_NODES; j++) {
                VmNumaNodeStat nodeStat = new VmNumaNodeStat();
                nodeStat.setNode(j);
                nodeStat.setHeapMemory(i * DATA);
                nodeStat.setPrivateMemory(i * DATA);
                nodeStat.setStackMemory(i * DATA);
                nodeStat.setHugeMemory(i * DATA);
                nodeStats.add(nodeStat);
            }

            stat.setVmNodeStats(nodeStats.toArray(new VmNumaNodeStat[0]));
            stats.add(stat);
        }

        return stats;
    }

    @Test
    public void verifyChartsAdded() {
        verify(view).addChart(eq(NUM_NODES), eq("Huge"));
        verify(view).addChart(eq(NUM_NODES), eq("Heap"));
        verify(view).addChart(eq(NUM_NODES), eq("Stack"));
        verify(view).addChart(eq(NUM_NODES), eq("Private"));
    }

    @Test
    public void verifyTimerSettings() {
        verify(timer).setAction(any(Runnable.class));
        verify(timer).setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
        verify(timer).setInitialDelay(0);
        verify(timer).setDelay(5);
        verifyNoMoreInteractions(timer);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void verifyTimerAction() {
        timerAction.getValue().run();
        for (int i = 0; i < NUM_NODES; i++) {
            for (NumaMemoryLocations v : NumaMemoryLocations.values()) {
                ArgumentCaptor<DiscreteTimeData> dataCaptor = ArgumentCaptor.forClass(DiscreteTimeData.class);
                verify(view, times(5)).addData(eq(v.getName()), eq(i), dataCaptor.capture());

                verifyValues(dataCaptor.getAllValues());
            }
        }
    }

    private void verifyValues(List<DiscreteTimeData> allValues) {
        assertEquals(5, allValues.size());
        for (int i = 0; i < allValues.size(); i++) {
            DiscreteTimeData data = allValues.get(i);
            assertEquals(i * TIMESTAMP, data.getTimeInMillis());
            assertEquals(i * DATA, data.getData());
        }

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void verifyViewActions() {
        actionListener.getValue().actionPerformed(new ActionEvent(view, VmNumaView.Action.VISIBLE));
        verify(timer).start();

        actionListener.getValue().actionPerformed(new ActionEvent(view, VmNumaView.Action.HIDDEN));
        verify(timer).stop();
    }

    @Test
    public void testView() {
        assertSame(view, vmNumaController.getView());
    }

    @Test
    public void testLocalizedName() {
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            assertEquals("NUMA", vmNumaController.getLocalizedName().getContents());
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

}
