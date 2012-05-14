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
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.ViewFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmMemoryStat;
import com.redhat.thermostat.common.model.VmMemoryStat.Generation;
import com.redhat.thermostat.common.model.VmMemoryStat.Space;

public class VmMemoryControllerTest {

    private final long TIMESTAMP = 1;
    private final int VM_ID = 99;

    private List<Generation> generations = new ArrayList<>();

    private Timer timer;
    private Space space;
    private Generation gen;
    private VmMemoryController controller;
    private VmMemoryView view;
    private ActionListener<VmMemoryView.Action> viewListener;
    private Runnable timerAction;


    @Before()
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();

        // Setup timer.
        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(actionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        space = new Space();
        space.name = "space";
        space.index = 0;
        space.used = 10;
        space.capacity = 100;
        space.maxCapacity = 1000;

        gen = new Generation();
        gen.spaces = new ArrayList<>();
        gen.spaces.add(space);

        generations.add(gen);

        // Setup dao
        VmMemoryStat vmMemory = new VmMemoryStat(TIMESTAMP, VM_ID, generations);
        VmMemoryStatDAO memoryStatDao = mock(VmMemoryStatDAO.class);
        when(memoryStatDao.getLatestMemoryStat(any(VmRef.class))).thenReturn(vmMemory);

        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getVmMemoryStatDAO()).thenReturn(memoryStatDao);

        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        // Setup view
        view = mock(VmMemoryView.class);
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());

        ViewFactory viewFactory = mock(ViewFactory.class);
        when(viewFactory.getView(eq(VmMemoryView.class))).thenReturn(view);
        ApplicationContext.getInstance().setViewFactory(viewFactory);


        VmRef ref = mock(VmRef.class);

        controller = new VmMemoryController(ref);
        timerAction = actionCaptor.getValue();
        viewListener = viewArgumentCaptor.getValue();

    }

    @After
    public void tearDown() {
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void testTimer() {

        viewListener.actionPerformed(new ActionEvent<>(view, VmMemoryView.Action.VISIBLE));

        verify(timer).start();
        verify(timer).setSchedulingType(SchedulingType.FIXED_RATE);

        viewListener.actionPerformed(new ActionEvent<>(view, VmMemoryView.Action.HIDDEN));

        verify(timer).stop();
    }


    @Test
    public void testControllerUpdatesView() {

        timerAction.run();

        verify(view).addRegion(eq("space"));
        verify(view).updateRegionSize(eq("space"), eq(10), contains("10"), contains("90"), contains("900"));
    }
}
