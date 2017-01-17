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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.BasicView.Action;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsView;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsView.NotificationAction;
import com.redhat.thermostat.vm.jmx.client.core.JmxNotificationsViewProvider;
import com.redhat.thermostat.vm.jmx.client.core.LocaleResources;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationStatus;
import com.redhat.thermostat.vm.jmx.client.core.JmxToggleNotificationRequest;

public class JmxNotificationsViewController implements InformationServiceController<VmRef> {

    private final JmxNotificationsView view;
    private final Timer timer;
    private final JmxNotificationDAO dao;
    private final VmInfoDAO vmInfo;
    private final AgentInfoDAO agentInfo;
    private final VmRef vm;

    private final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private final JmxToggleNotificationRequest toggleReq;
    private final AtomicBoolean notificationsEnabled = new AtomicBoolean(false);

    public JmxNotificationsViewController(ApplicationService appSvc,
            AgentInfoDAO agent, VmInfoDAO vmInfo, JmxNotificationDAO notification,
            TimerFactory timerFactory, RequestQueue queue,
            JmxNotificationsViewProvider viewProvider, VmRef vmId) {
        this(appSvc, agent, vmInfo, notification, timerFactory, queue, viewProvider, vmId,
                new JmxToggleNotificationRequestFactory());
    }
    
    JmxNotificationsViewController(final ApplicationService appSvc,
            AgentInfoDAO agent, VmInfoDAO vmInfoDAO, JmxNotificationDAO notification,
            TimerFactory timerFactory, RequestQueue queue,
            JmxNotificationsViewProvider viewProvider, VmRef vmId,
            JmxToggleNotificationRequestFactory reqFactory) {
        this.dao = notification;
        this.agentInfo = agent;
        this.vmInfo = vmInfoDAO;
        this.view = viewProvider.createView();
        this.timer = timerFactory.createTimer();
        this.vm = vmId;
        
        // Callbacks for toggle notifications
        final Runnable successAction = new Runnable() {
            @Override
            public void run() {
                boolean val = notificationsEnabled.get();
                notificationsEnabled.set(!val);
                if (val) {
                    view.setMonitoringState(JmxNotificationsView.MonitoringState.STOPPED);
                } else {
                    view.setMonitoringState(JmxNotificationsView.MonitoringState.STARTED);
                }
            }
        };

        final Runnable failureAction = new Runnable() {

            @Override
            public void run() {
                LocalizedString warning;
                if (notificationsEnabled.get()) {
                    warning = t.localize(LocaleResources.NOTIFICATIONS_CANNOT_DISABLE);
                } else {
                    warning = t.localize(LocaleResources.NOTIFICATIONS_CANNOT_ENABLE);
                }
                view.displayWarning(warning);
                view.setViewControlsEnabled(false);
            }
        };
        
        this.toggleReq = reqFactory.createRequest(queue, agent, successAction, failureAction);

        initializeTimer();

        view.addActionListener(new ActionListener<BasicView.Action>() {

            @Override
            public void actionPerformed(ActionEvent<Action> actionEvent) {
                switch (actionEvent.getActionId()) {
                case HIDDEN:
                    stopUpdatingView();
                    break;
                case VISIBLE:
                    if (notificationsEnabled.get() && isVmAlive()) {
                        view.setMonitoringState(JmxNotificationsView.MonitoringState.STARTED);
                    } else {
                        view.setMonitoringState(JmxNotificationsView.MonitoringState.STOPPED);
                    }
                    view.setViewControlsEnabled(isVmAlive());
                    startUpdatingView();
                    break;
                }
            }
        });

        view.addNotificationActionListener(new ActionListener<JmxNotificationsView.NotificationAction>() {
            @Override
            public void actionPerformed(ActionEvent<NotificationAction> actionEvent) {
                if (actionEvent.getActionId() == NotificationAction.TOGGLE_NOTIFICATIONS) {
                    final boolean enabled = notificationsEnabled.get();
                    if (!enabled) {
                        view.setMonitoringState(JmxNotificationsView.MonitoringState.STARTING);
                    } else {
                        view.setMonitoringState(JmxNotificationsView.MonitoringState.STOPPING);
                    }
                    // This can block on network, do outside EDT/UI thread
                    appSvc.getApplicationExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            toggleReq.sendEnableNotificationsRequestToAgent(vm, !notificationsEnabled.get());
                        }
                    });
                }
            }
        });

        view.setViewControlsEnabled(isVmAlive());
    }

    private void initializeTimer() {
        timer.setTimeUnit(TimeUnit.SECONDS);
        timer.setInitialDelay(0);
        timer.setDelay(5);
        timer.setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        timer.setAction(new Runnable() {

            private long lastTimeStamp = Long.MIN_VALUE;

            @Override
            public void run() {
                JmxNotificationStatus status = dao.getLatestNotificationStatus(vm);
                if (status == null) {
                    return;
                }

                boolean monitoring = status.isEnabled();
                notificationsEnabled.set(monitoring);
                if (monitoring) {
                    view.setMonitoringState(JmxNotificationsView.MonitoringState.STARTED);
                } else {
                    view.setMonitoringState(JmxNotificationsView.MonitoringState.STOPPED);
                }

                List<JmxNotification> notifications = dao.getNotifications(vm, lastTimeStamp);
                for (JmxNotification notification : notifications) {
                    lastTimeStamp = Math.max(lastTimeStamp, notification.getTimeStamp());
                    view.addNotification(notification);
                }
            }
        });

        view.setViewControlsEnabled(true);

    }

    private boolean isVmAlive() {
        AgentId agentId = new AgentId(vm.getHostRef().getAgentId());
        AgentInformation agentInformation = agentInfo.getAgentInformation(agentId);
        VmInfo.AliveStatus status = vmInfo.getVmInfo(vm).isAlive(agentInformation);
        return status.equals(VmInfo.AliveStatus.RUNNING);
    }

    private void startUpdatingView() {
        timer.start();
    }

    private void stopUpdatingView() {
        timer.stop();
    }

    @Override
    public UIComponent getView() {
        return view;
    }

    @Override
    public LocalizedString getLocalizedName() {
        return t.localize(LocaleResources.NOTIFICATIONS_TITLE);
    }
    
    static class JmxToggleNotificationRequestFactory {
        
        JmxToggleNotificationRequest createRequest(RequestQueue queue, AgentInfoDAO agentDAO, 
                Runnable successAction, Runnable failureAction) {
            return new JmxToggleNotificationRequest(queue, agentDAO, successAction, failureAction);
        }
        
    }

}

