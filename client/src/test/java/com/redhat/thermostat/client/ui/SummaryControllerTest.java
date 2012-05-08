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
import com.redhat.thermostat.common.ViewFactory;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.appctx.ApplicationContextUtil;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.MongoDAOFactory;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;

public class SummaryControllerTest {

    private Timer timer;
    private Runnable timerAction;
    private SummaryView view;
    private ActionListener<SummaryView.Action> viewListener;

    @Before
    public void setUp() {
        ApplicationContextUtil.resetApplicationContext();
        ApplicationContext ctx = ApplicationContext.getInstance();

        // Setup timer
        timer = mock(Timer.class);
        TimerFactory timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(actionCaptor.capture());
        ctx.setTimerFactory(timerFactory);

        // setup dao
        HostInfoDAO hDAO = mock(HostInfoDAO.class);
        when(hDAO.getCount()).thenReturn(99l);
        VmInfoDAO vDAO = mock(VmInfoDAO.class);
        when(vDAO.getCount()).thenReturn(42l);

        DAOFactory daoFactory = mock(MongoDAOFactory.class);
        when(daoFactory.getHostInfoDAO()).thenReturn(hDAO);
        when(daoFactory.getVmInfoDAO()).thenReturn(vDAO);

        ctx.setDAOFactory(daoFactory);

        // Setup view
        view = mock(SummaryView.class);
        ArgumentCaptor<ActionListener> viewArgumentCaptor = ArgumentCaptor.forClass(ActionListener.class);
        doNothing().when(view).addActionListener(viewArgumentCaptor.capture());

        ViewFactory viewFactory = mock(ViewFactory.class);
        when(viewFactory.getView(eq(SummaryView.class))).thenReturn(view);
        ApplicationContext.getInstance().setViewFactory(viewFactory);

        SummaryController summaryCtrl = new SummaryController();

        timerAction = actionCaptor.getValue();
        viewListener = viewArgumentCaptor.getValue();
    }

    @After
    public void tearDown() {
        timer = null;
        timerAction = null;
        view = null;
        viewListener = null;
        ApplicationContextUtil.resetApplicationContext();
    }

    @Test
    public void verifyTimerAction() {
        verify(timer).setAction(isNotNull(Runnable.class));
        verify(timer).setDelay(10);
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
        verify(timer).setInitialDelay(0);
        verify(timer).setSchedulingType(SchedulingType.FIXED_RATE);
    }

    @Test
    public void testTimer() {
        viewListener.actionPerformed(new ActionEvent<SummaryView.Action>(view, SummaryView.Action.VISIBLE));

        verify(timer).start();

        timerAction.run();
        verify(view).setTotalHosts(eq("99"));
        verify(view).setTotalVms(eq("42"));

        viewListener.actionPerformed(new ActionEvent<SummaryView.Action>(view, SummaryView.Action.HIDDEN));

        verify(timer).stop();
    }

}
