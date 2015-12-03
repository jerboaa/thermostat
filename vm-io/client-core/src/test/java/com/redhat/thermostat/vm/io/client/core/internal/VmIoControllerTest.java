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
import com.redhat.thermostat.vm.io.client.core.VmIoView;
import com.redhat.thermostat.vm.io.client.core.VmIoViewProvider;
import com.redhat.thermostat.vm.io.common.VmIoStat;
import com.redhat.thermostat.vm.io.common.VmIoStatDAO;

public class VmIoControllerTest {
    
    private ActionListener<VmIoView.Action> listener;
    private Runnable timerAction;
    private Timer timer;
    private VmIoView view;
    private VmRef ref;
    
    @Before
    public void setup() {
        ref = mock(VmRef.class);
    }
    
    @SuppressWarnings("unchecked")
    private void setupWithVmIOStatDAO(VmIoStatDAO vmIoStatDAO) {
        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());
        
        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationService appSvc = mock(ApplicationService.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);
        
        view = mock(VmIoView.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());
        
        when(view.getUserDesiredDuration()).thenReturn(new Duration(1, TimeUnit.MINUTES));
        
        VmIoViewProvider viewProvider = mock(VmIoViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);
        
        @SuppressWarnings("unused")
        VmIoController controller = new VmIoController(appSvc, vmIoStatDAO, ref, viewProvider);
        
        timerAction = timerActionCaptor.getValue();
        listener = viewArgumentCaptor.getValue();
    }
    
    @Test
    public void verifyNoNPEThrownWithNoData() {
        setupWithVmIOStatDAO(mock(VmIoStatDAO.class));
        timerAction.run();
    }

    @SuppressWarnings({ "unchecked"})
    @Test
    public void testChartUpdate() {
        VmIoStat stat1 = new VmIoStat("foo-agent", "vmId", 123, 1, 2, 3, 4);
        List<VmIoStat> stats = new ArrayList<VmIoStat>();
        stats.add(stat1);

        VmIoStatDAO vmIoStatDAO = mock(VmIoStatDAO.class);
        when(vmIoStatDAO.getLatestVmIoStats(any(VmRef.class), any(Long.class))).thenThrow(new AssertionError("Unbounded queries are bad!"));
        when(vmIoStatDAO.getOldest(ref)).thenReturn(stat1);
        when(vmIoStatDAO.getNewest(ref)).thenReturn(stat1);
        
        setupWithVmIOStatDAO(vmIoStatDAO);

        listener.actionPerformed(new ActionEvent<>(view, VmIoView.Action.VISIBLE));

        verify(timer).start();

        timerAction.run();

        listener.actionPerformed(new ActionEvent<>(view, VmIoView.Action.HIDDEN));

        verify(timer).stop();

        verify(view).addData(any(List.class));
        // We don't verify atMost() since we might increase the update rate in the future.
    }

}
