/*
 * Copyright 2012-2017 Red Hat, Inc.
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.jmx.client.core.JmxToggleNotificationRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.Timer.SchedulingType;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsView;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsView.NotificationAction;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsViewProvider;
import com.redhat.thermostat.vm.jmx.client.core.LocaleResources;
import com.redhat.thermostat.vm.jmx.client.core.internal.JmxNotificationsViewController.JmxToggleNotificationRequestFactory;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationStatus;

public class JmxNotificationsViewControllerTest {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    
    private JmxNotificationDAO notificationDao;
    private JmxNotificationsView view;
    private JmxNotificationsViewProvider viewProvider;
    private Timer timer;
    private TimerFactory timerFactory;
    private VmRef vm;
    private VmInfo vmInfo;
    private RequestQueue queue;
    private JmxNotificationsViewController controller;
    private HostRef host;

    private JmxToggleNotificationRequest toggleReq;

    private Runnable successAction;
    private Runnable failureAction;

    @Before
    public void setUp() {
        ApplicationService appSvc = mock(ApplicationService.class);
        ExecutorService execSvc = mock(ExecutorService.class);
        when(appSvc.getApplicationExecutor()).thenReturn(execSvc);
        // Run task immediately
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return null;
            }
        }).when(execSvc).execute(any(Runnable.class));
        
        AgentInfoDAO agentDao = mock(AgentInfoDAO.class);
        notificationDao = mock(JmxNotificationDAO.class);
        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);
        queue = mock(RequestQueue.class);
        view = mock(JmxNotificationsView.class);
        viewProvider = mock(JmxNotificationsViewProvider.class);
        when(viewProvider.createView()).thenReturn(view);
        timer = mock(Timer.class);
        timerFactory = mock(TimerFactory.class);
        when(timerFactory.createTimer()).thenReturn(timer);

        vmInfo = mock(VmInfo.class);
        when(vmInfo.isAlive(any(AgentInformation.class))).thenReturn(VmInfo.AliveStatus.RUNNING);
        when(vmInfoDAO.getVmInfo(any(VmRef.class))).thenReturn(vmInfo);

        host = mock(HostRef.class);
        vm = mock(VmRef.class);
        when(vm.getHostRef()).thenReturn(host);
        when(host.getAgentId()).thenReturn("123");
        
        JmxToggleNotificationRequestFactory reqFactory = mock(JmxToggleNotificationRequestFactory.class);
        toggleReq = mock(JmxToggleNotificationRequest.class);
        when(reqFactory.createRequest(eq(queue), eq(agentDao), any(Runnable.class), 
                any(Runnable.class))).thenReturn(toggleReq);

        controller = new JmxNotificationsViewController(appSvc, agentDao, vmInfoDAO, notificationDao, timerFactory,
                queue, viewProvider, vm, reqFactory);
        ArgumentCaptor<Runnable> successCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> failureCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(reqFactory).createRequest(eq(queue), eq(agentDao), successCaptor.capture(), failureCaptor.capture());
        
        successAction = successCaptor.getValue();
        failureAction = failureCaptor.getValue();
    }

    @Test
    public void verifyGetView() {
        assertEquals(view, controller.getView());
    }

    @Test
    public void verifyGetLocalizedName() {
        assertEquals("Notifications", controller.getLocalizedName().getContents());
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
    public void verifyTimerWorksWhenNoStatus() {
        ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(timer).setAction(actionCaptor.capture());

        Runnable timerAction = actionCaptor.getValue();

        JmxNotification data = mock(JmxNotification.class);
        when(notificationDao.getNotifications(vm, Long.MIN_VALUE)).thenReturn(new ArrayList<JmxNotification>());
        JmxNotificationStatus status = mock(JmxNotificationStatus.class);
        when(notificationDao.getLatestNotificationStatus(vm)).thenReturn(null);

        verify(view, never()).setMonitoringState(any(JmxNotificationsView.MonitoringState.class));
        verify(view, never()).addNotification(isA(JmxNotification.class));
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
        answerSuccess(true);

        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, NotificationAction.TOGGLE_NOTIFICATIONS));

        verify(toggleReq).sendEnableNotificationsRequestToAgent(eq(vm), eq(true));
        verify(view).setMonitoringState(JmxNotificationsView.MonitoringState.STARTING);
        verify(view).setMonitoringState(JmxNotificationsView.MonitoringState.STARTED);
    }
    
    @Test
    public void enableNotificationsFails() {
        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);

        verify(view).addNotificationActionListener(listenerCaptor.capture());
        answerFailure(true);

        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, NotificationAction.TOGGLE_NOTIFICATIONS));

        verify(toggleReq).sendEnableNotificationsRequestToAgent(vm, true);
        verify(view).setMonitoringState(JmxNotificationsView.MonitoringState.STARTING);
        verify(view, never()).setMonitoringState(JmxNotificationsView.MonitoringState.STARTED);
        verify(view).setViewControlsEnabled(false);

        ArgumentCaptor<LocalizedString> warningCaptor = ArgumentCaptor.forClass(LocalizedString.class);
        verify(view).displayWarning(warningCaptor.capture());
        assertEquals(translator.localize(LocaleResources.NOTIFICATIONS_CANNOT_ENABLE).getContents(),
                warningCaptor.getValue().getContents());
    }
    
    @Test
    public void disableNotificationsWhenViewFiresEvent() {
        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);

        verify(view).addNotificationActionListener(listenerCaptor.capture());
        answerSuccess(true);
        answerSuccess(false);

        // Enable, then disable
        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, NotificationAction.TOGGLE_NOTIFICATIONS));
        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, NotificationAction.TOGGLE_NOTIFICATIONS));

        verify(toggleReq).sendEnableNotificationsRequestToAgent(vm, false);
        verify(view).setMonitoringState(JmxNotificationsView.MonitoringState.STOPPING);
        verify(view).setMonitoringState(JmxNotificationsView.MonitoringState.STOPPED);
    }

    @Test
    public void verifyControlsEnabled() {
        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addActionListener(listenerCaptor.capture());

        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, Action.VISIBLE));

        verify(view, atLeastOnce()).setViewControlsEnabled(true);
    }
    
    @Test
    public void verifyControlsDisabledWhenVmDead() {
        when(vmInfo.isAlive(any(AgentInformation.class))).thenReturn(VmInfo.AliveStatus.EXITED);

        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);
        verify(view).addActionListener(listenerCaptor.capture());

        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, Action.VISIBLE));

        verify(view, atLeastOnce()).setViewControlsEnabled(false);
    }

    private void answerSuccess(boolean enable) {
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                successAction.run();
                return null;
            }
        }).when(toggleReq).sendEnableNotificationsRequestToAgent(vm, enable);
    }

    private void answerFailure(boolean enable) {
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                failureAction.run();
                return null;
            }
        }).when(toggleReq).sendEnableNotificationsRequestToAgent(vm, enable);
    }

    @Test
    public void disableNotificationsFails() {
        ArgumentCaptor<ActionListener> listenerCaptor = ArgumentCaptor.forClass(ActionListener.class);

        verify(view).addNotificationActionListener(listenerCaptor.capture());
        answerSuccess(true);
        answerFailure(false);
        
        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, NotificationAction.TOGGLE_NOTIFICATIONS));
        listenerCaptor.getValue().actionPerformed(new ActionEvent<>(view, NotificationAction.TOGGLE_NOTIFICATIONS));

        verify(toggleReq).sendEnableNotificationsRequestToAgent(vm, false);
        verify(view, times(1)).setMonitoringState(JmxNotificationsView.MonitoringState.STARTED);
        verify(view, times(1)).setMonitoringState(JmxNotificationsView.MonitoringState.STOPPING);
        verify(view, times(1)).setMonitoringState(JmxNotificationsView.MonitoringState.STARTING);
        verify(view).setViewControlsEnabled(false);
        
        ArgumentCaptor<LocalizedString> warningCaptor = ArgumentCaptor.forClass(LocalizedString.class);
        verify(view).displayWarning(warningCaptor.capture());
        assertEquals(translator.localize(LocaleResources.NOTIFICATIONS_CANNOT_DISABLE).getContents(), 
                warningCaptor.getValue().getContents());
    }

    
}

