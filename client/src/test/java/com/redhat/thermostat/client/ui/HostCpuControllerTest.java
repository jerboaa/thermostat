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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.ui.HostCpuController;
import com.redhat.thermostat.client.ui.HostCpuView;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.ViewFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.CpuStatDAO;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.model.CpuStat;
import com.redhat.thermostat.common.model.DiscreteTimeData;
import com.redhat.thermostat.common.model.HostInfo;

public class HostCpuControllerTest {

    private HostCpuController controller;

    private HostCpuView view;

    private Timer timer;

    private Runnable timerAction;

    @Before
    public void setUp() {
        // Setup timer.
        ApplicationContextUtil.resetApplicationContext();
        timer = mock(Timer.class);
        ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(actionCaptor.capture());

        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ApplicationContext.getInstance().setTimerFactory(timerFactory);

        // Setup DAOs.
        HostInfo hostInfo = new HostInfo("fluffhost1", "fluffOs1", "fluffKernel1", "fluffCpu1", 12345, 98765);
        HostInfoDAO hostInfoDAO = mock(HostInfoDAO.class);
        when(hostInfoDAO.getHostInfo(any(HostRef.class))).thenReturn(hostInfo);

        CpuStat cpuStat1 = new CpuStat(1l, 10.0, 20.0, 30.0);
        CpuStat cpuStat2 = new CpuStat(2l, 15.0, 25.0, 35.0);
        CpuStatDAO cpuStatDAO = mock(CpuStatDAO.class);
        when(cpuStatDAO.getLatestCpuStats(any(HostRef.class))).thenReturn(Arrays.asList(cpuStat1, cpuStat2));

        DAOFactory daoFactory = mock(DAOFactory.class);
        when(daoFactory.getHostInfoDAO()).thenReturn(hostInfoDAO);
        when(daoFactory.getCpuStatDAO()).thenReturn(cpuStatDAO);

        ApplicationContext.getInstance().setDAOFactory(daoFactory);

        view = mock(HostCpuView.class);
        ViewFactory viewFactory = mock(ViewFactory.class);
        when(viewFactory.getView(eq(HostCpuView.class))).thenReturn(view);

        ApplicationContext.getInstance().setViewFactory(viewFactory);

        HostRef host = new HostRef("123", "fluffhost");
        controller = new HostCpuController(host);

        timerAction = actionCaptor.getValue();
    }

    @After
    public void tearDown() {
        timerAction = null;
        controller = null;
        view = null;
        timer = null;
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void testTimer() {
        controller.start();

        verify(timer).setAction(isNotNull(Runnable.class));
        verify(timer).setDelay(5);
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
        verify(timer).setInitialDelay(0);
        verify(timer).setSchedulingType(SchedulingType.FIXED_RATE);
        verify(timer).start();

        controller.stop();

        verify(timer).stop();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testTimerAction() {
        timerAction.run();
        verify(view).setCpuModel("fluffCpu1");
        verify(view).setCpuCount("12345");
        @SuppressWarnings("rawtypes")
        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(view).addCpuLoadData(captor.capture());
        List<DiscreteTimeData<Double>> cpuLoadData = captor.getValue();
        assertEquals(1, cpuLoadData.get(0).getTimeInMillis());
        assertEquals(10.0, cpuLoadData.get(0).getData().doubleValue(), 0.0001);
        assertEquals(2, cpuLoadData.get(1).getTimeInMillis());
        assertEquals(15.0, cpuLoadData.get(1).getData().doubleValue(), 0.0001);
    }
}
