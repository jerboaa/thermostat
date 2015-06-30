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

package com.redhat.thermostat.thread.client.controller.impl;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollector;
import com.redhat.thermostat.thread.client.common.view.VmDeadLockView;
import com.redhat.thermostat.thread.client.common.view.VmDeadLockView.VmDeadLockViewAction;
import com.redhat.thermostat.thread.model.VmDeadLockData;

public class VmDeadLockControllerTest {

    private Timer timer;
    private VmDeadLockView view;
    private ThreadCollector collector;

    private VmDeadLockController controller;

    @Before
    public void setUp() {
        timer = mock(Timer.class);

        view = mock(VmDeadLockView.class);

        collector = mock(ThreadCollector.class);

        controller = new VmDeadLockController(view, collector, timer);
    }

    @Test
    public void verifyInitilizeRegistersActionListener() {
        controller.initialize();

        verify(view).addVmDeadLockViewActionListener(isA(ActionListener.class));
    }

    @Test
    public void verifyRealDeadLockDataIsDisplayedOnViewAction() {
        final String DESCRIPTION = "foo bar";
        VmDeadLockData data = new VmDeadLockData("foo-agent");
        data.setDeadLockDescription(DESCRIPTION);

        controller.initialize();

        ArgumentCaptor<ActionListener> listenerCaptor = (ArgumentCaptor<ActionListener>) ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addVmDeadLockViewActionListener(listenerCaptor.capture());

        ActionListener<VmDeadLockViewAction> listener = (ActionListener<VmDeadLockViewAction>) listenerCaptor.getValue();

        when(collector.getLatestDeadLockData()).thenReturn(data);

        listener.actionPerformed(new ActionEvent<VmDeadLockViewAction>(view, VmDeadLockViewAction.CHECK_FOR_DEADLOCK));

        verify(collector).requestDeadLockCheck();
        verify(view).setDeadLockInformation(null, DESCRIPTION);
    }

    @Test
    public void verifyNoDeadLockDataIsDisplayedOnViewAction() {
        VmDeadLockData data = new VmDeadLockData("foo-agent");
        data.setDeadLockDescription(VmDeadLockData.NO_DEADLOCK);

        controller.initialize();

        ArgumentCaptor<ActionListener> listenerCaptor = (ArgumentCaptor<ActionListener>) ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addVmDeadLockViewActionListener(listenerCaptor.capture());

        ActionListener<VmDeadLockViewAction> listener = (ActionListener<VmDeadLockViewAction>) listenerCaptor.getValue();

        when(collector.getLatestDeadLockData()).thenReturn(data);

        listener.actionPerformed(new ActionEvent<VmDeadLockViewAction>(view, VmDeadLockViewAction.CHECK_FOR_DEADLOCK));

        verify(collector).requestDeadLockCheck();
        verify(view).setDeadLockInformation(null, "No Deadlocks Detected.");
    }

    @Test
    public void verifyInitializeSetsUpTimer() {
        controller.initialize();

        verify(timer).setAction(isA(Runnable.class));
        verify(timer).setDelay(5);
        verify(timer).setInitialDelay(0);
        verify(timer).setSchedulingType(SchedulingType.FIXED_DELAY);
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
    }

    @Test
    public void verifyTimerIsEnabledWhenViewIsVisible() {
        controller.initialize();

        ArgumentCaptor<ActionListener> listenerCaptor = (ArgumentCaptor<ActionListener>) ArgumentCaptor.forClass(ActionListener.class);

        verify(view).addActionListener(listenerCaptor.capture());

        ActionListener<BasicView.Action> visibilityListener = listenerCaptor.getValue();

        visibilityListener.actionPerformed(new ActionEvent<BasicView.Action>(view, Action.VISIBLE));

        verify(timer).start();

        visibilityListener.actionPerformed(new ActionEvent<BasicView.Action>(view, Action.HIDDEN));

        verify(timer).stop();
    }

    @Test
    public void verifyTimerActionRefreshesView() {
        doThrow(new AssertionError()).when(collector).requestDeadLockCheck();

        VmDeadLockData data = new VmDeadLockData("foo-agent");
        data.setDeadLockDescription(VmDeadLockData.NO_DEADLOCK);
        controller.initialize();

        when(collector.getLatestDeadLockData()).thenReturn(data);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(timer).setAction(runnableCaptor.capture());

        Runnable action = runnableCaptor.getValue();

        action.run();

        verify(view).setDeadLockInformation(null, "No Deadlocks Detected.");
    }

    @Test
    public void verifyTimerActionHandlesNoDataCorrectly() {
        doThrow(new AssertionError()).when(collector).requestDeadLockCheck();

        controller.initialize();

        when(collector.getLatestDeadLockData()).thenReturn(null);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(timer).setAction(runnableCaptor.capture());

        Runnable action = runnableCaptor.getValue();

        action.run();

        // pass if no exceptions thrown
    }
}

