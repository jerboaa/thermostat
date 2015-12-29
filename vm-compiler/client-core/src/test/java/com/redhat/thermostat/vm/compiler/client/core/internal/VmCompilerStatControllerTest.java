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

package com.redhat.thermostat.vm.compiler.client.core.internal;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.compiler.client.core.VmCompilerStatView;
import com.redhat.thermostat.vm.compiler.client.core.VmCompilerStatView.ViewData;
import com.redhat.thermostat.vm.compiler.client.core.VmCompilerStatViewProvider;
import com.redhat.thermostat.vm.compiler.common.VmCompilerStat;
import com.redhat.thermostat.vm.compiler.common.VmCompilerStatDao;

public class VmCompilerStatControllerTest {

    private Timer timer;
    private VmCompilerStatView view;
    private VmRef ref;
    private Runnable timerAction;
    private ActionListener<Action> listener;

    @Before
    public void setup() {
        ref = mock(VmRef.class);
    }

    @Test
    public void testChartUpdate() {
        final long SOME_TIMESTAMP = 12345;
        final int SOME_VALUE = 1234;
        final int LAST_TYPE = 2;

        VmCompilerStat stat1 = new VmCompilerStat("foo-agent", "vmId", SOME_TIMESTAMP,
                SOME_VALUE, SOME_VALUE, SOME_VALUE,
                SOME_VALUE,
                SOME_VALUE, LAST_TYPE, "Foo lastMethod",
                LAST_TYPE, "Bar lastFailedMethod");

        List<VmCompilerStat> stats = new ArrayList<VmCompilerStat>();
        stats.add(stat1);

        VmCompilerStatDao vmCompilerStatDAO = mock(VmCompilerStatDao.class);

        when(vmCompilerStatDAO.getNewest(ref)).thenReturn(stat1);

        setupWithVmCompilerStatDAO(vmCompilerStatDAO);

        listener.actionPerformed(new ActionEvent<>(view, VmCompilerStatView.Action.VISIBLE));

        verify(timer).start();
        timerAction.run();
        verify(view).setData(isA(ViewData.class));

        listener.actionPerformed(new ActionEvent<>(view, VmCompilerStatView.Action.HIDDEN));

        verify(timer).stop();
    }

    @Test
    public void verifyNoCompilerStatDataDoesNotNPE() {
        setupWithVmCompilerStatDAO(mock(VmCompilerStatDao.class));
        timerAction.run();
    }

    @SuppressWarnings("unchecked")
    private void setupWithVmCompilerStatDAO(VmCompilerStatDao vmCompilerStatDao) {

        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationService appSvc = mock(ApplicationService.class);
        when(appSvc.getTimerFactory()).thenReturn(timerFactory);

        view = mock(VmCompilerStatView.class);
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());

        VmCompilerStatViewProvider viewProvider = mock(VmCompilerStatViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);

        @SuppressWarnings("unused")
        VmCompilerStatController controller = new VmCompilerStatController(appSvc, vmCompilerStatDao, ref, viewProvider);

        listener = viewArgumentCaptor.getValue();
        timerAction = timerActionCaptor.getValue();
    }

}

