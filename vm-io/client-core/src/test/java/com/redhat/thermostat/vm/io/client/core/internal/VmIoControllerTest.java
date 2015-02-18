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

package com.redhat.thermostat.vm.io.client.core.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.experimental.Duration;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.io.client.core.VmIoView;
import com.redhat.thermostat.vm.io.client.core.VmIoViewProvider;
import com.redhat.thermostat.vm.io.common.VmIoStat;
import com.redhat.thermostat.vm.io.common.VmIoStatDAO;

public class VmIoControllerTest {

    @SuppressWarnings({ "unchecked", "rawtypes" }) // any(List.class)
    @Test
    public void testChartUpdate() {
        VmRef ref = mock(VmRef.class);

        VmIoStat stat1 = new VmIoStat("foo-agent", "vmId", 123, 1, 2, 3, 4);
        List<VmIoStat> stats = new ArrayList<VmIoStat>();
        stats.add(stat1);

        VmIoStatDAO vmCpuStatDAO = mock(VmIoStatDAO.class);
        when(vmCpuStatDAO.getLatestVmIoStats(any(VmRef.class), any(Long.class))).thenThrow(new AssertionError("Unbounded queries are bad!"));
        when(vmCpuStatDAO.getOldest(ref)).thenReturn(stat1);
        when(vmCpuStatDAO.getNewest(ref)).thenReturn(stat1);

        Timer timer = mock(Timer.class);
        ArgumentCaptor<Runnable> timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationService appSvc = mock(ApplicationService.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);

        final VmIoView view = mock(VmIoView.class);
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());

        when(view.getUserDesiredDuration()).thenReturn(new Duration(1, TimeUnit.MINUTES));

        VmIoViewProvider viewProvider = mock(VmIoViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);

        @SuppressWarnings("unused")
        VmIoController controller = new VmIoController(appSvc, vmCpuStatDAO, ref, viewProvider);

        ActionListener<VmIoView.Action> l = viewArgumentCaptor.getValue();

        l.actionPerformed(new ActionEvent<>(view, VmIoView.Action.VISIBLE));

        verify(timer).start();

        timerActionCaptor.getValue().run();

        l.actionPerformed(new ActionEvent<>(view, VmIoView.Action.HIDDEN));

        verify(timer).stop();

        verify(view).addData(any(List.class));
        // We don't verify atMost() since we might increase the update rate in the future.
    }
}
