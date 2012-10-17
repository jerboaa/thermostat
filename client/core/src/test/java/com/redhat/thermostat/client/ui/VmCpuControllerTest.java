/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.client.ui;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.views.VmCpuView;
import com.redhat.thermostat.client.core.views.VmCpuViewProvider;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.VmCpuStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmCpuStat;


public class VmCpuControllerTest {

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" }) // any(List.class)
    @Test
    public void testChartUpdate() {

        VmCpuStat stat1 = new VmCpuStat(123, 12345, 50.5);
        List<VmCpuStat> stats = new ArrayList<VmCpuStat>();
        stats.add(stat1);

        VmCpuStatDAO vmCpuStatDAO = mock(VmCpuStatDAO.class);
        when(vmCpuStatDAO.getLatestVmCpuStats(any(VmRef.class), eq(Long.MIN_VALUE))).thenReturn(stats).thenReturn(new ArrayList<VmCpuStat>());

        VmRef ref = mock(VmRef.class);

        Timer timer = mock(Timer.class);
        ArgumentCaptor<Runnable> timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        final VmCpuView view = mock(VmCpuView.class);
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());
        
        VmCpuViewProvider viewProvider = mock(VmCpuViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);

        @SuppressWarnings("unused")
        VmCpuController controller = new VmCpuController(vmCpuStatDAO, ref, viewProvider);

        ActionListener<VmCpuView.Action> l = viewArgumentCaptor.getValue();

        l.actionPerformed(new ActionEvent<>(view, VmCpuView.Action.VISIBLE));

        verify(timer).start();

        timerActionCaptor.getValue().run();

        l.actionPerformed(new ActionEvent<>(view, VmCpuView.Action.HIDDEN));

        verify(timer).stop();

        verify(view).addData(any(List.class));
        // We don't verify atMost() since we might increase the update rate in the future.
    }
}
