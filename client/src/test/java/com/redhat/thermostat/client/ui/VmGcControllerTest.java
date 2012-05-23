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

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.ViewFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.VmGcStatDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmGcStat;
import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;

public class VmGcControllerTest {

    private Timer timer;
    private Runnable timerAction;
    private VmGcView view;
    private ActionListener<VmGcView.Action> viewListener;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();

        // Setup Timer
        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> timerActionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(timerActionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        // Set up fake data
        List<VmGcStat> stats = new ArrayList<>();
        VmGcStat stat1 = new VmGcStat(42, 1, "collector1", 1, 10);
        VmGcStat stat2 = new VmGcStat(42, 2, "collector1", 5, 20);
        stats.add(stat1);
        stats.add(stat2);

        Generation gen;
        List<Generation> gens = new ArrayList<>();
        gen = new Generation();
        gen.name = "generation 1";
        gen.collector = "collector1";
        gens.add(gen);
        VmMemoryStat memoryStat = new VmMemoryStat(1, 42, gens);

        // Setup DAO
        VmGcStatDAO vmGcStatDAO = mock(VmGcStatDAO.class);
        when(vmGcStatDAO.getLatestVmGcStats(isA(VmRef.class))).thenReturn(stats);
        VmMemoryStatDAO vmMemoryStatDAO = mock(VmMemoryStatDAO.class);
        when(vmMemoryStatDAO.getLatestMemoryStat(isA(VmRef.class))).thenReturn(memoryStat);

        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getVmGcStatDAO()).thenReturn(vmGcStatDAO);
        when(daoFactory.getVmMemoryStatDAO()).thenReturn(vmMemoryStatDAO);
        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        // Setup View
        view = mock(VmGcView.class);
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());

        ViewFactory viewFactory = mock(ViewFactory.class);
        when(viewFactory.getView(eq(VmGcView.class))).thenReturn(view);

        ApplicationContext.getInstance().setViewFactory(viewFactory);

        // Now start the controller
        VmRef ref = mock(VmRef.class);

        new VmGcController(ref);

        // Extract relevant objects
        viewListener = viewArgumentCaptor.getValue();
        timerAction = timerActionCaptor.getValue();
    }

    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void verifyTimer() {
        verify(timer).setAction(isNotNull(Runnable.class));
        verify(timer).setAction(isA(Runnable.class));
        verify(timer).setDelay(5);
        verify(timer).setInitialDelay(0);
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
        verify(timer).setSchedulingType(SchedulingType.FIXED_RATE);

    }

    @Test
    public void verifyStartAndStop() {
        viewListener.actionPerformed(new ActionEvent<>(view, VmGcView.Action.VISIBLE));

        verify(timer).start();

        viewListener.actionPerformed(new ActionEvent<>(view, VmGcView.Action.HIDDEN));

        verify(timer).stop();
    }

    @Test
    public void verifyAction() {
        timerAction.run();

        verify(view).addData(isA(String.class), isA(List.class));
    }

}
