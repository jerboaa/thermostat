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

package com.redhat.thermostat.vm.jmx.client.cli;

import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Clock;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.Timers;
import com.redhat.thermostat.common.cli.AbstractCompleterCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.jmx.client.cli.internal.NotificationsSinceParser;
import com.redhat.thermostat.vm.jmx.client.cli.locale.LocaleResources;
import com.redhat.thermostat.vm.jmx.client.core.JmxToggleNotificationRequest;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationStatus;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class NotificationsCommand extends AbstractCompleterCommand {

    public static final String COMMAND_NAME = "notifications";

    private static final String STATUS_SUBCOMMAND = "status";
    private static final String SHOW_SUBCOMMAND = "show";
    private static final String ENABLE_SUBCOMMAND = "enable";
    private static final String DISABLE_SUBCOMMAND = "disable";
    private static final String FOLLOW_SUBCOMMAND = "follow";

    private static final String ENABLE_FOLLOW_OPTION = "follow";
    private static final String SHOW_SINCE_OPTION = "since";

    // in milliseconds
    private static final long FOLLOW_LOOP_INPUT_PERIOD = 500L;
    private static final long ENABLE_TO_FOLLOW_TRANSITION_DELAY = 500L;

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private static final NotificationsSinceParser sinceParser = new NotificationsSinceParser(new SystemClock());

    private final DependencyServices dependencyServices = new DependencyServices();

    @Override
    public void run(CommandContext ctx) throws CommandException {
        Arguments args = ctx.getArguments();
        String subcommand = args.getSubcommand();

        ApplicationService applicationService = dependencyServices.getRequiredService(ApplicationService.class);
        Clock clock = dependencyServices.getRequiredService(Clock.class);
        RequestQueue queue = dependencyServices.getRequiredService(RequestQueue.class);
        HostInfoDAO hostInfoDAO = dependencyServices.getRequiredService(HostInfoDAO.class);
        AgentInfoDAO agentInfoDAO = dependencyServices.getRequiredService(AgentInfoDAO.class);
        VmInfoDAO vmInfoDAO = dependencyServices.getRequiredService(VmInfoDAO.class);
        JmxNotificationDAO jmxNotificationDAO = dependencyServices.getRequiredService(JmxNotificationDAO.class);

        VmRef vmRef = getVmRef(args, vmInfoDAO, hostInfoDAO);

        switch (subcommand) {
            case STATUS_SUBCOMMAND:
                handleStatusRequest(ctx, jmxNotificationDAO, vmRef);
                break;
            case DISABLE_SUBCOMMAND:
                handleJmxStatusChange(subcommand, ctx, queue, jmxNotificationDAO, agentInfoDAO, vmRef);
                break;
            case ENABLE_SUBCOMMAND:
                handleJmxStatusChange(subcommand, ctx, queue, jmxNotificationDAO, agentInfoDAO, vmRef);
                if (args.hasArgument(ENABLE_FOLLOW_OPTION)) {
                    try {
                        Thread.sleep(ENABLE_TO_FOLLOW_TRANSITION_DELAY);
                    } catch (InterruptedException e) {
                        break;
                    }
                    // fall through to FOLLOW_SUBCOMMAND
                } else {
                    break;
                }
            case FOLLOW_SUBCOMMAND:
                if (!isMonitoringEnabled(jmxNotificationDAO, vmRef)) {
                    ctx.getConsole().getOutput().println(t.localize(LocaleResources.JMX_NOTIFICATION_MONITORING_FOLLOW_NOT_ENABLED).getContents());
                    break;
                }
                ctx.getConsole().getOutput().println(t.localize(LocaleResources.EXIT_FOLLOW_MODE_HINT).getContents());
                followNotificationsLoop(ctx, clock, applicationService.getTimerFactory(), jmxNotificationDAO, vmRef);
                break;
            case SHOW_SUBCOMMAND:
                printNotifications(ctx, jmxNotificationDAO, vmRef, parseSinceOption(args));
                break;
        }
    }

    private VmRef getVmRef(Arguments arguments, VmInfoDAO vmInfoDAO, HostInfoDAO hostInfoDAO) throws CommandException {
        VmId vmId = VmArgument.required(arguments).getVmId();
        VmInfo vmInfo = vmInfoDAO.getVmInfo(vmId);
        if (vmInfo == null) {
            throw new CommandException(t.localize(LocaleResources.UNKNOWN_VMID, vmId.get()));
        }
        AgentId agentId = new AgentId(vmInfo.getAgentId());
        HostInfo hostInfo = hostInfoDAO.getHostInfo(agentId);
        if (hostInfo == null) {
            throw new CommandException(t.localize(LocaleResources.UNKNOWN_HOST));
        }

        HostRef hostRef = new HostRef(agentId.get(), hostInfo.getHostname());
        return new VmRef(hostRef, vmInfo);
    }

    private void handleStatusRequest(CommandContext ctx, JmxNotificationDAO jmxNotificationDAO, VmRef vmRef) {
        boolean monitoringEnabled = isMonitoringEnabled(jmxNotificationDAO, vmRef);
        LocalizedString message = t.localize(monitoringEnabled ? LocaleResources.JMX_NOTIFICATION_MONITORING_STATUS_ENABLED
                                                                : LocaleResources.JMX_NOTIFICATION_MONITORING_STATUS_DISABLED);
        ctx.getConsole().getOutput().println(message.getContents());
    }

    private boolean isMonitoringEnabled(JmxNotificationDAO jmxNotificationDAO, VmRef vmRef) {
        JmxNotificationStatus status = jmxNotificationDAO.getLatestNotificationStatus(vmRef);
        return status != null && status.isEnabled();
    }

    private void handleJmxStatusChange(String subcommand, CommandContext ctx, RequestQueue queue,
                                       JmxNotificationDAO jmxNotificationDAO, AgentInfoDAO agentInfoDAO, VmRef vmRef) {
        if (!ENABLE_SUBCOMMAND.equals(subcommand) && !DISABLE_SUBCOMMAND.equals(subcommand)) {
            throw new AssertionError("Invalid subcommand: " + subcommand);
        }

        boolean enable = ENABLE_SUBCOMMAND.equals(subcommand);
        boolean currentlyEnabled = isMonitoringEnabled(jmxNotificationDAO, vmRef);
        if (enable == currentlyEnabled) {
            LocalizedString message = t.localize(enable ? LocaleResources.ALREADY_ENABLED : LocaleResources.ALREADY_DISABLED);
            ctx.getConsole().getOutput().println(message.getContents());
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        RequestCompleteAction action = new RequestCompleteAction(ctx, latch, enable);
        JmxToggleNotificationRequest enableRequest =
                new JmxToggleNotificationRequest(queue, agentInfoDAO, action.getSuccessAction(), action.getFailureAction());
        enableRequest.sendEnableNotificationsRequestToAgent(vmRef, enable);
        try {
            latch.await();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private long parseSinceOption(Arguments args) throws CommandException {
        if (!args.hasArgument(SHOW_SINCE_OPTION)) {
            return NotificationsSinceParser.NO_SINCE_OPTION;
        }
        String sinceArg = args.getArgument(SHOW_SINCE_OPTION);
        return sinceParser.parse(sinceArg);
    }

    private void followNotificationsLoop(final CommandContext ctx, final Clock clock, TimerFactory timerFactory,
                                         final JmxNotificationDAO jmxNotificationDAO, final VmRef vmRef) {

        Timer pollTimer = null;
        try {
            // this timer asynchronously performs the DAO polling and results printing
            pollTimer = createFollowModeTimer(ctx, clock, timerFactory, jmxNotificationDAO, vmRef);
            pollTimer.start();

            // loop the main thread waiting for user key input, which tells us to exit follow mode and stop the timer
            while (continueFollowing(ctx, jmxNotificationDAO, vmRef)) {
                try {
                    Thread.sleep(FOLLOW_LOOP_INPUT_PERIOD);
                } catch (InterruptedException e) {
                    break;
                }
            }
        } finally {
            if (pollTimer != null) {
                pollTimer.stop();
            }
        }
    }

    private Timer createFollowModeTimer(final CommandContext ctx, final Clock clock, TimerFactory timerFactory,
                                        final JmxNotificationDAO jmxNotificationDAO, final VmRef vmRef) {
        return Timers.createDataRefreshTimer(timerFactory, new Runnable() {
            long lastUpdate = clock.getRealTimeMillis();

            @Override
            public void run() {
                printNotifications(ctx, jmxNotificationDAO, vmRef, lastUpdate);
                lastUpdate = clock.getRealTimeMillis();
            }
        });
    }

    private boolean continueFollowing(CommandContext ctx, JmxNotificationDAO jmxNotificationDAO, VmRef vmRef) {
        try {
            if (ctx.getConsole().getInput().available() > 0) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        boolean monitoringActive = isMonitoringEnabled(jmxNotificationDAO, vmRef);
        if (!monitoringActive) {
            ctx.getConsole().getOutput().println(t.localize(LocaleResources.JMX_NOTIFICATION_MONITORING_FOLLOW_INTERRUPTED).getContents());
        }
        return monitoringActive;
    }

    private void printNotifications(CommandContext ctx, JmxNotificationDAO jmxNotificationDAO, VmRef vmRef, long since) {
        List<JmxNotification> notifications = jmxNotificationDAO.getNotifications(vmRef, since);
        for (JmxNotification notification : notifications) {
            String timestamp = Clock.DEFAULT_DATE_FORMAT.format(new Date(notification.getTimeStamp()));
            String details = notification.getSourceDetails();
            String contents = notification.getContents();
            ctx.getConsole().getOutput().println(
                    t.localize(LocaleResources.PRINT_NOTIFICATIONS_FORMAT, timestamp, details, contents).getContents());
        }
    }

    public void bindApplicationService(ApplicationService applicationService) {
        dependencyServices.addService(ApplicationService.class, applicationService);
    }

    public void bindClock(Clock clock) {
        dependencyServices.addService(Clock.class, clock);
    }

    public void bindRequestQueue(RequestQueue requestQueue) {
        dependencyServices.addService(RequestQueue.class, requestQueue);
    }

    public void bindHostInfoDao(HostInfoDAO hostInfoDAO) {
        dependencyServices.addService(HostInfoDAO.class, hostInfoDAO);
    }

    public void bindAgentInfoDao(AgentInfoDAO agentInfoDAO) {
        dependencyServices.addService(AgentInfoDAO.class, agentInfoDAO);
    }

    public void bindVmInfoDao(VmInfoDAO vmInfoDAO) {
        dependencyServices.addService(VmInfoDAO.class, vmInfoDAO);
    }

    public void bindJmxNotificationDao(JmxNotificationDAO jmxNotificationDAO) {
        dependencyServices.addService(JmxNotificationDAO.class, jmxNotificationDAO);
    }

    public void dependenciesUnavailable() {
        dependencyServices.removeService(ApplicationService.class);
        dependencyServices.removeService(Clock.class);
        dependencyServices.removeService(RequestQueue.class);
        dependencyServices.removeService(HostInfoDAO.class);
        dependencyServices.removeService(AgentInfoDAO.class);
        dependencyServices.removeService(VmInfoDAO.class);
        dependencyServices.removeService(JmxNotificationDAO.class);
    }

    private static class RequestCompleteAction {

        private final CommandContext ctx;
        private final CountDownLatch latch;
        private final boolean enableRequest;

        RequestCompleteAction(CommandContext ctx, CountDownLatch latch, boolean enableRequest) {
            this.ctx = ctx;
            this.latch = latch;
            this.enableRequest = enableRequest;
        }

        Runnable getSuccessAction() {
            return new Runnable() {
                @Override
                public void run() {
                    LocaleResources message = enableRequest ? LocaleResources.ENABLE_SUCCESS : LocaleResources.DISABLE_SUCCESS;
                    ctx.getConsole().getOutput().println(t.localize(message).getContents());
                    latch.countDown();
                }
            };
        }

        Runnable getFailureAction() {
            return new Runnable() {
                @Override
                public void run() {
                    LocaleResources message = enableRequest ? LocaleResources.ENABLE_FAILURE : LocaleResources.DISABLE_FAILURE;
                    ctx.getConsole().getError().println(t.localize(message).getContents());
                    latch.countDown();
                }
            };
        }

    }

}
