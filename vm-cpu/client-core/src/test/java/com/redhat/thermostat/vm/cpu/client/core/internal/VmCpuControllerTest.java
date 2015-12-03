/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.cpu.client.core.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.cpu.client.core.VmCpuView;
import com.redhat.thermostat.vm.cpu.client.core.VmCpuViewProvider;
import com.redhat.thermostat.vm.cpu.common.VmCpuStatDAO;
import com.redhat.thermostat.vm.cpu.common.model.VmCpuStat;

public class VmCpuControllerTest {
    
    private ActionListener<VmCpuView.Action> listener;
    private Timer timer;
    private Runnable timerAction;
    private VmCpuView view;
    private VmRef ref;
    
    @Before
    public void setup() {
        ref = mock(VmRef.class);
    }

    @SuppressWarnings({ "unchecked"}) // any(List.class)
    @Test
    public void testChartUpdate() {
        VmCpuStat stat1 = new VmCpuStat("foo-agent", 123, "vmId", 50.5);
        List<VmCpuStat> stats = new ArrayList<VmCpuStat>();
        stats.add(stat1);

        VmCpuStatDAO vmCpuStatDAO = mock(VmCpuStatDAO.class);
        when(vmCpuStatDAO.getLatestVmCpuStats(any(VmRef.class), any(Long.class))).thenThrow(new AssertionError("Unbounded queries are bad!"));
        when(vmCpuStatDAO.getOldest(ref)).thenReturn(stat1);
        when(vmCpuStatDAO.getNewest(ref)).thenReturn(stat1);
        
        setupWithVmCPUStatDAO(vmCpuStatDAO);

        listener.actionPerformed(new ActionEvent<>(view, VmCpuView.Action.VISIBLE));

        verify(timer).start();

        timerAction.run();

        listener.actionPerformed(new ActionEvent<>(view, VmCpuView.Action.HIDDEN));

        verify(timer).stop();

        verify(view).addData(any(List.class));
        // We don't verify atMost() since we might increase the update rate in the future.
    }
    
    @Test
    public void verifyNoNPEWhenNoCpuData() {
        setupWithVmCPUStatDAO(mock(VmCpuStatDAO.class));
        timerAction.run();
    }

    @SuppressWarnings("unchecked")
    private void setupWithVmCPUStatDAO(VmCpuStatDAO dao) {
        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationService appSvc = mock(ApplicationService.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);

        view = mock(VmCpuView.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());

        when(view.getUserDesiredDuration()).thenReturn(new Duration(1, TimeUnit.MINUTES));
        
        VmCpuViewProvider viewProvider = mock(VmCpuViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);

        @SuppressWarnings("unused")
        VmCpuController controller = new VmCpuController(appSvc, dao, ref, viewProvider);
        listener = viewArgumentCaptor.getValue();
        timerAction = timerActionCaptor.getValue();
    }
}

