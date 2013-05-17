/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.vm.jmx.client.core.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsView;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsView.NotificationAction;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsViewProvider;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationStatus;

public class JmxNotificationsViewControllerTest {

    private AgentInfoDAO agentDao;
    private JmxNotificationDAO notificationDao;
    private JmxNotificationsView view;
    private JmxNotificationsViewProvider viewProvider;
    private Timer timer;
    private TimerFactory timerFactory;
    private VmRef vm;
    private RequestQueue queue;
    private JmxNotificationsViewController controller;
    private HostRef host;

    @Before
    public void setUp() {
        agentDao = mock(AgentInfoDAO.class);
        notificationDao = mock(JmxNotificationDAO.class);
        queue = mock(RequestQueue.class);
        view = mock(JmxNotificationsView.class);
        viewProvider = mock(JmxNotificationsViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);
        timer = mock(Timer.class);
        timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);

        host = mock(HostRef.class);
        vm = mock(VmRef.class);
        when(vm.getAgent()).thenReturn(host);

        controller = new JmxNotificationsViewController(agentDao, notificationDao, timerFactory, queue, viewProvider, vm);
    }

    @Test
    public void verifyGetView() {
        assertEquals(view, controller.getView());
    }

    @Test
    public void verifyGetLocalizedName() {
        assertEquals("Notifications", controller.getLocalizedName());
    }

    @Test
    public void verifyTimerIsInitialized() {
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
        verify(timer).setInitialDelay(0);
        verify(timer).setDelay(5);
        verify(timer).setSchedulingType(SchedulingType.FIXED_RATE);
        verify(timer).setTimeUnit(TimeUnit.SECONDS);

        ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(timer).setAction(actionCaptor.capture());

        Runnable timerAction = actionCaptor.getValue();

        JmxNotification data = mock(JmxNotification.class);
        when(notificationDao.getNotifications(vm, Long.MIN_VALUE)).thenReturn(Arrays.asList(data));
        JmxNotificationStatus status = mock(JmxNotificationStatus.class);
        when(notificationDao.getLatestNotificationStatus(vm)).thenReturn(status);

        timerAction.run();

        verify(view).addNotification(data);

    }

    @Test
    public void verifyTimerIsStartedWhenViewIsVisible() {

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);

        verify(view).addActionListener(listenerCaptor.capture());

        ActionListener listener = listenerCaptor.getValue();
        listener.actionPerformed(new ActionEvent<Enum<?>>(view, Action.VISIBLE));

        verify(timer).start();
    }

    @Test
    public void verifyTimerIsStoppedWhenViewisInvisible() {
        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);

        verify(view).addActionListener(listenerCaptor.capture());

        ActionListener listener = listenerCaptor.getValue();
        listener.actionPerformed(new ActionEvent<Enum<?>>(view, Action.VISIBLE));

        verify(timer).start();

        listener.actionPerformed(new ActionEvent<Enum<?>>(view, Action.HIDDEN));

        verify(timer).stop();

    }

    @Test
    public void enableNotificationsWhenViewFiresEvent() {
        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);

        verify(view).addNotificationActionListener(listenerCaptor.capture());

        AgentInformation agentInfo = mock(AgentInformation.class);
        when(agentInfo.getConfigListenAddress()).thenReturn("example.com:0");
        when(agentDao.getAgentInformation(host)).thenReturn(agentInfo);

        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, NotificationAction.TOGGLE_NOTIFICATIONS));

        ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
        verify(queue).putRequest(requestCaptor.capture());

        Request req = requestCaptor.getValue();
        assertEquals(new InetSocketAddress("example.com", 0), req.getTarget());
    }
}
