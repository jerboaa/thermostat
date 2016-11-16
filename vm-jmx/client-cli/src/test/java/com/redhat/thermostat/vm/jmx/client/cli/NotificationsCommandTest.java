/*
 * Copyright 2012-2016 Red Hat, Inc.
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
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.internal.test.TestTimerFactory;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.jmx.common.JmxNotification;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class NotificationsCommandTest {

    private static final long TIMESTAMP = 110L;
    private static final long FUTURE_TIMESTAMP = Long.MAX_VALUE;

    private static final String NOTIFICATION_OUTPUT;
    private static final String FAR_FUTURE_NOTIFICATION_OUTPUT;

    static {
        String timestampString = Clock.DEFAULT_DATE_FORMAT.format(new Date(TIMESTAMP));
        NOTIFICATION_OUTPUT = "[" + timestampString + "] (notification source details) : notification contents";
        String futureTimestampString = Clock.DEFAULT_DATE_FORMAT.format(new Date(FUTURE_TIMESTAMP));
        FAR_FUTURE_NOTIFICATION_OUTPUT = "[" + futureTimestampString + "] (notification source details) : notification contents";
    }

    private static final String JMX_NOTIFICATION_MONITORING_ENABLED = "JMX notification monitoring enabled";
    private static final String PRESS_ANY_KEY_TO_EXIT_FOLLOW_MODE = "Press any key to exit follow mode...";
    private static final String JMX_NOTIFICATION_MONITORING_INTERRUPTED = "JMX notification monitoring interrupted";
    private static final String JMX_NOTIFICATION_MONITORING_IS_ALREADY_ENABLED = "JMX notification monitoring is already enabled";
    private static final String JMX_NOTIFICATION_MONITORING_IS_NOT_ENABLED = "JMX notification monitoring is not enabled";
    private static final String JMX_NOTIFICATION_MONITORING_DISABLED = "JMX notification monitoring disabled";
    private static final String JMX_NOTIFICATION_MONITORING_IS_DISABLED = "JMX notification monitoring is disabled";
    private static final String JMX_NOTIFICATION_MONITORING_IS_ENABLED = "JMX notification monitoring is enabled";

    private static final String FOO_AGENTID = "foo-agentid";
    private static final String FOO_HOSTNAME = "foo-hostname";
    private static final String FOO_VMID = "foo-vmid";

    private static final String STATUS_SUBCOMMAND = "status";
    private static final String DISABLE_SUBCOMMAND = "disable";
    private static final String ENABLE_SUBCOMMAND = "enable";
    private static final String FOLLOW_SUBCOMMAND = "follow";
    private static final String SHOW_SUBCOMMAND = "show";

    private static final String FOLLOW_OPTION = "follow";
    private static final String SINCE_OPTION = "since";

    private ApplicationService applicationService;
    private Clock clock;
    private RequestQueue requestQueue;
    private HostInfoDAO hostInfoDAO;
    private AgentInfoDAO agentInfoDAO;
    private VmInfoDAO vmInfoDAO;
    private JmxNotificationDAO jmxNotificationDAO;

    private JmxNotificationStatus jmxNotificationStatus;
    private JmxNotification jmxNotification;

    private Arguments args;
    private PrintStream outStream;
    private InputStream inStream;
    private ArgumentCaptor<String> outCaptor;
    private CommandContext ctx;

    private NotificationsCommand cmd;
    private TestTimerFactory timerFactory;

    @Before
    public void setup() {
        timerFactory = new TestTimerFactory();
        applicationService = mock(ApplicationService.class);
        clock = mock(Clock.class);
        requestQueue = mock(RequestQueue.class);
        hostInfoDAO = mock(HostInfoDAO.class);
        agentInfoDAO = mock(AgentInfoDAO.class);
        vmInfoDAO = mock(VmInfoDAO.class);
        jmxNotificationDAO = mock(JmxNotificationDAO.class);

        Console console = mock(Console.class);
        outStream = mock(PrintStream.class);
        inStream = mock(InputStream.class);
        outCaptor = ArgumentCaptor.forClass(String.class);
        when(console.getOutput()).thenReturn(outStream);
        when(console.getInput()).thenReturn(inStream);

        args = mock(Arguments.class);
        when(args.hasArgument(VmArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(VmArgument.ARGUMENT_NAME)).thenReturn(FOO_VMID);
        ctx = mock(CommandContext.class);
        when(ctx.getArguments()).thenReturn(args);
        when(ctx.getConsole()).thenReturn(console);

        cmd = new NotificationsCommand();
        cmd.bindApplicationService(applicationService);
        cmd.bindClock(clock);
        cmd.bindRequestQueue(requestQueue);
        cmd.bindHostInfoDao(hostInfoDAO);
        cmd.bindAgentInfoDao(agentInfoDAO);
        cmd.bindVmInfoDao(vmInfoDAO);
        cmd.bindJmxNotificationDao(jmxNotificationDAO);

        setupMocks();
    }

    private void setupMocks() {
        when(applicationService.getTimerFactory()).thenReturn(timerFactory);
        when(clock.getRealTimeMillis()).thenReturn(1L);

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                Request request = (Request) invocationOnMock.getArguments()[0];
                Response response = new Response(Response.ResponseType.OK);
                for (RequestResponseListener listener : request.getListeners()) {
                    listener.fireComplete(request, response);
                }
                return null;
            }
        }).when(requestQueue).putRequest(any(Request.class));

        HostInfo hostInfo = new HostInfo();
        hostInfo.setAgentId(FOO_AGENTID);
        hostInfo.setHostname(FOO_HOSTNAME);
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        when(hostInfoDAO.getAllHostInfos()).thenReturn(Collections.singletonList(hostInfo));

        AgentInformation agentInfo = new AgentInformation();
        agentInfo.setAgentId(FOO_AGENTID);
        agentInfo.setConfigListenAddress("127.0.0.1:22");
        when(agentInfoDAO.getAgentIds()).thenReturn(Collections.singleton(new AgentId(FOO_AGENTID)));
        when(agentInfoDAO.getAliveAgentIds()).thenReturn(Collections.singleton(new AgentId(FOO_AGENTID)));
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(agentInfo);
        when(agentInfoDAO.getAliveAgents()).thenReturn(Collections.singletonList(agentInfo));
        when(agentInfoDAO.getAllAgentInformation()).thenReturn(Collections.singletonList(agentInfo));

        VmInfo vmInfo = new VmInfo();
        vmInfo.setAgentId(FOO_AGENTID);
        vmInfo.setVmId(FOO_VMID);
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(vmInfoDAO.getAllVmInfos()).thenReturn(Collections.singletonList(vmInfo));
        when(vmInfoDAO.getAllVmInfosForAgent(any(AgentId.class))).thenReturn(Collections.singletonList(vmInfo));
        when(vmInfoDAO.getVmIds(any(AgentId.class))).thenReturn(Collections.singleton(new VmId(FOO_VMID)));
        when(vmInfoDAO.getVmInfo(any(VmRef.class))).thenReturn(vmInfo);

        jmxNotificationStatus = new JmxNotificationStatus(FOO_AGENTID);
        jmxNotificationStatus.setVmId(FOO_VMID);
        jmxNotificationStatus.setTimeStamp(100L);
        jmxNotificationStatus.setEnabled(true);
        jmxNotification = new JmxNotification(FOO_AGENTID);
        jmxNotification.setVmId(FOO_VMID);
        jmxNotification.setTimeStamp(TIMESTAMP);
        jmxNotification.setContents("notification contents");
        jmxNotification.setSourceBackend("jmxBackend");
        jmxNotification.setSourceDetails("notification source details");
        when(jmxNotificationDAO.getLatestNotificationStatus(any(VmRef.class))).thenReturn(jmxNotificationStatus);
        when(jmxNotificationDAO.getNotifications(any(VmRef.class), anyLong())).thenAnswer(new Answer<List<JmxNotification>>() {
            @Override
            public List<JmxNotification> answer(InvocationOnMock invocationOnMock) throws Throwable {
                long timestamp = (Long) invocationOnMock.getArguments()[1];
                if (timestamp < jmxNotification.getTimeStamp()) {
                    return Collections.singletonList(jmxNotification);
                } else {
                    return Collections.emptyList();
                }
            }
        });
    }

    @Test
    public void testStatusWhenMonitoringIsEnabled() throws CommandException {
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(STATUS_SUBCOMMAND));
        assertThat(runCommandForOutput(), is(JMX_NOTIFICATION_MONITORING_IS_ENABLED));
    }

    @Test
    public void testStatusWhenMonitoringIsDisabled() throws CommandException {
        jmxNotificationStatus.setEnabled(false);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(STATUS_SUBCOMMAND));
        assertThat(runCommandForOutput(), is(JMX_NOTIFICATION_MONITORING_IS_DISABLED));
    }

    @Test
    public void testStatusWhenMonitoringStatusIsUnknown() throws CommandException {
        when(jmxNotificationDAO.getLatestNotificationStatus(any(VmRef.class))).thenReturn(null);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(STATUS_SUBCOMMAND));
        assertThat(runCommandForOutput(), is(JMX_NOTIFICATION_MONITORING_IS_DISABLED));
    }

    @Test
    public void testDisableWhenMonitoringIsEnabled() throws CommandException {
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(DISABLE_SUBCOMMAND));
        assertThat(runCommandForOutput(), is(JMX_NOTIFICATION_MONITORING_DISABLED));
    }

    @Test
    public void testDisableWhenMonitoringIsDisabled() throws CommandException {
        jmxNotificationStatus.setEnabled(false);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(DISABLE_SUBCOMMAND));
        assertThat(runCommandForOutput(), is(JMX_NOTIFICATION_MONITORING_IS_NOT_ENABLED));
    }

    @Test
    public void testDisableWhenMonitoringStatusIsUnknown() throws CommandException {
        when(jmxNotificationDAO.getLatestNotificationStatus(any(VmRef.class))).thenReturn(null);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(DISABLE_SUBCOMMAND));
        assertThat(runCommandForOutput(), is(JMX_NOTIFICATION_MONITORING_IS_NOT_ENABLED));
    }

    @Test
    public void testEnableWhenMonitoringIsEnabled() throws CommandException {
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(ENABLE_SUBCOMMAND));
        assertThat(runCommandForOutput(), is(JMX_NOTIFICATION_MONITORING_IS_ALREADY_ENABLED));
    }

    @Test
    public void testEnableWhenMonitoringIsDisabled() throws CommandException {
        jmxNotificationStatus.setEnabled(false);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(ENABLE_SUBCOMMAND));
        assertThat(runCommandForOutput(), is(JMX_NOTIFICATION_MONITORING_ENABLED));
    }

    @Test
    public void testEnableWhenMonitoringStatusIsUnknown() throws CommandException {
        when(jmxNotificationDAO.getLatestNotificationStatus(any(VmRef.class))).thenReturn(null);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(ENABLE_SUBCOMMAND));
        assertThat(runCommandForOutput(), is(JMX_NOTIFICATION_MONITORING_ENABLED));
    }

    @Test
    public void testEnableWithFollowOption() throws CommandException, IOException {
        jmxNotificationStatus.setEnabled(false);
        jmxNotification.setTimeStamp(FUTURE_TIMESTAMP);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(ENABLE_SUBCOMMAND));
        when(args.hasArgument(FOLLOW_OPTION)).thenReturn(true);

        doFollowTestWithKeyboardInterrupt();

        verify(outStream, times(3)).println(outCaptor.capture());
        String status = outCaptor.getAllValues().get(0);
        String hint = outCaptor.getAllValues().get(1);
        String firstNotification = outCaptor.getAllValues().get(2);
        assertThat(status, is(JMX_NOTIFICATION_MONITORING_ENABLED));
        assertThat(hint, is(PRESS_ANY_KEY_TO_EXIT_FOLLOW_MODE));
        assertThat(firstNotification, is(FAR_FUTURE_NOTIFICATION_OUTPUT));
    }

    @Test
    public void testEnableWithFollowOptionWhenAlreadyEnabled() throws CommandException, IOException {
        jmxNotification.setTimeStamp(FUTURE_TIMESTAMP);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(ENABLE_SUBCOMMAND));
        when(args.hasArgument(FOLLOW_OPTION)).thenReturn(true);

        doFollowTestWithKeyboardInterrupt();

        verify(outStream, times(3)).println(outCaptor.capture());
        String status = outCaptor.getAllValues().get(0);
        String hint = outCaptor.getAllValues().get(1);
        String firstNotification = outCaptor.getAllValues().get(2);
        assertThat(status, is(JMX_NOTIFICATION_MONITORING_IS_ALREADY_ENABLED));
        assertThat(hint, is(PRESS_ANY_KEY_TO_EXIT_FOLLOW_MODE));
        assertThat(firstNotification, is(FAR_FUTURE_NOTIFICATION_OUTPUT));
    }

    @Test
    public void testEnableWithFollowOptionWithExternalInterrupt() throws CommandException, IOException {
        jmxNotificationStatus.setEnabled(false);
        jmxNotification.setTimeStamp(FUTURE_TIMESTAMP);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(ENABLE_SUBCOMMAND));
        when(args.hasArgument(FOLLOW_OPTION)).thenReturn(true);

        doFollowTestWithExternalMonitoringInterrupt();

        verify(outStream, times(4)).println(outCaptor.capture());
        String status = outCaptor.getAllValues().get(0);
        String hint = outCaptor.getAllValues().get(1);
        String firstNotification = outCaptor.getAllValues().get(2);
        String interruptNotice = outCaptor.getAllValues().get(3);
        assertThat(status, is(JMX_NOTIFICATION_MONITORING_ENABLED));
        assertThat(hint, is(PRESS_ANY_KEY_TO_EXIT_FOLLOW_MODE));
        assertThat(firstNotification, is(FAR_FUTURE_NOTIFICATION_OUTPUT));
        assertThat(interruptNotice, is(JMX_NOTIFICATION_MONITORING_INTERRUPTED));
    }

    @Test
    public void testFollowWhenDisabled() throws CommandException {
        jmxNotificationStatus.setEnabled(false);
        jmxNotification.setTimeStamp(FUTURE_TIMESTAMP);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(FOLLOW_SUBCOMMAND));

        assertThat(runCommandForOutput(), is("JMX notification monitoring is not enabled for this JVM - notifications cannot be followed"));
    }

    @Test
    public void testFollowWhenEnabled() throws CommandException, IOException {
        jmxNotification.setTimeStamp(FUTURE_TIMESTAMP);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(FOLLOW_SUBCOMMAND));

        doFollowTestWithKeyboardInterrupt();

        verify(outStream, times(2)).println(outCaptor.capture());
        String hint = outCaptor.getAllValues().get(0);
        String firstNotification = outCaptor.getAllValues().get(1);
        assertThat(hint, is(PRESS_ANY_KEY_TO_EXIT_FOLLOW_MODE));
        assertThat(firstNotification, is(FAR_FUTURE_NOTIFICATION_OUTPUT));
    }

    @Test
    public void testFollowWithExternalInterrupt() throws CommandException, IOException {
        jmxNotification.setTimeStamp(FUTURE_TIMESTAMP);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(FOLLOW_SUBCOMMAND));

        doFollowTestWithExternalMonitoringInterrupt();

        verify(outStream, times(3)).println(outCaptor.capture());
        String hint = outCaptor.getAllValues().get(0);
        String firstNotification = outCaptor.getAllValues().get(1);
        String interruptNotice = outCaptor.getAllValues().get(2);
        assertThat(hint, is(PRESS_ANY_KEY_TO_EXIT_FOLLOW_MODE));
        assertThat(firstNotification, is(FAR_FUTURE_NOTIFICATION_OUTPUT));
        assertThat(interruptNotice, is(JMX_NOTIFICATION_MONITORING_INTERRUPTED));
    }

    private void doFollowTestWithKeyboardInterrupt() throws IOException {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                jmxNotificationStatus.setEnabled(true);
                Request request = (Request) invocationOnMock.getArguments()[0];
                Response response = new Response(Response.ResponseType.OK);
                for (RequestResponseListener listener : request.getListeners()) {
                    listener.fireComplete(request, response);
                }
                return null;
            }
        }).when(requestQueue).putRequest(any(Request.class));

        Thread helper = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cmd.run(ctx);
                } catch (CommandException e) {
                    fail();
                }
            }
        });
        helper.start();
        try {
            Thread.sleep(750);
            timerFactory.getAction().run();
        } catch (InterruptedException ignored) {
        }
        when(inStream.available()).thenReturn(1);
        try {
            helper.join();
        } catch (InterruptedException ignored) {
        }
    }

    private void doFollowTestWithExternalMonitoringInterrupt() throws IOException {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                jmxNotificationStatus.setEnabled(true);
                Request request = (Request) invocationOnMock.getArguments()[0];
                Response response = new Response(Response.ResponseType.OK);
                for (RequestResponseListener listener : request.getListeners()) {
                    listener.fireComplete(request, response);
                }
                return null;
            }
        }).when(requestQueue).putRequest(any(Request.class));

        Thread helper = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    cmd.run(ctx);
                } catch (CommandException e) {
                    fail();
                }
            }
        });
        helper.start();
        try {
            Thread.sleep(750);
            timerFactory.getAction().run();
        } catch (InterruptedException ignored) {
        }
        jmxNotificationStatus.setEnabled(false);
        try {
            helper.join();
        } catch (InterruptedException ignored) {
        }
    }

    @Test
    public void testShow() throws CommandException {
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(SHOW_SUBCOMMAND));
        assertThat(runCommandForOutput(), is(NOTIFICATION_OUTPUT));
    }

    @Test
    public void testShowSinceWithTimestampNewerThanData() throws CommandException {
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(SHOW_SUBCOMMAND));
        when(args.hasArgument(SINCE_OPTION)).thenReturn(true);
        when(args.getArgument(SINCE_OPTION)).thenReturn("500");
        cmd.run(ctx);
        verifyZeroInteractions(outStream);
    }

    @Test
    public void testShowSinceWithTimestampOlderThanData() throws CommandException {
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(SHOW_SUBCOMMAND));
        when(args.hasArgument(SINCE_OPTION)).thenReturn(true);
        when(args.getArgument(SINCE_OPTION)).thenReturn("1");
        assertThat(runCommandForOutput(), is(NOTIFICATION_OUTPUT));
    }

    private String runCommandForOutput() throws CommandException {
        cmd.run(ctx);
        verify(outStream).println(outCaptor.capture());
        return outCaptor.getValue();
    }

    @Test(expected = CommandException.class)
    public void testRequiresRequestQueue() throws CommandException {
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(STATUS_SUBCOMMAND));
        cmd.dependenciesUnavailable();
//        cmd.bindRequestQueue(requestQueue);
        cmd.bindHostInfoDao(hostInfoDAO);
        cmd.bindAgentInfoDao(agentInfoDAO);
        cmd.bindVmInfoDao(vmInfoDAO);
        cmd.bindJmxNotificationDao(jmxNotificationDAO);
        cmd.run(ctx);
    }

    @Test(expected = CommandException.class)
    public void testRequiresHostInfoDao() throws CommandException {
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(STATUS_SUBCOMMAND));
        cmd.dependenciesUnavailable();
        cmd.bindRequestQueue(requestQueue);
//        cmd.bindHostInfoDao(hostInfoDAO);
        cmd.bindAgentInfoDao(agentInfoDAO);
        cmd.bindVmInfoDao(vmInfoDAO);
        cmd.bindJmxNotificationDao(jmxNotificationDAO);
        cmd.run(ctx);
    }

    @Test(expected = CommandException.class)
    public void testRequiresAgentInfoDao() throws CommandException {
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(STATUS_SUBCOMMAND));
        cmd.dependenciesUnavailable();
        cmd.bindRequestQueue(requestQueue);
        cmd.bindHostInfoDao(hostInfoDAO);
//        cmd.bindAgentInfoDao(agentInfoDAO);
        cmd.bindVmInfoDao(vmInfoDAO);
        cmd.bindJmxNotificationDao(jmxNotificationDAO);
        cmd.run(ctx);
    }

    @Test(expected = CommandException.class)
    public void testRequiresVmInfoDao() throws CommandException {
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(STATUS_SUBCOMMAND));
        cmd.dependenciesUnavailable();
        cmd.bindRequestQueue(requestQueue);
        cmd.bindHostInfoDao(hostInfoDAO);
        cmd.bindAgentInfoDao(agentInfoDAO);
//        cmd.bindVmInfoDao(vmInfoDAO);
        cmd.bindJmxNotificationDao(jmxNotificationDAO);
        cmd.run(ctx);
    }

    @Test(expected = CommandException.class)
    public void testRequiresJmxNotificationDao() throws CommandException {
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList(STATUS_SUBCOMMAND));
        cmd.dependenciesUnavailable();
        cmd.bindRequestQueue(requestQueue);
        cmd.bindHostInfoDao(hostInfoDAO);
        cmd.bindAgentInfoDao(agentInfoDAO);
        cmd.bindVmInfoDao(vmInfoDAO);
//        cmd.bindJmxNotificationDao(jmxNotificationDAO);
        cmd.run(ctx);
    }

}
