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

package com.redhat.thermostat.web.endpoint.internal;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ExitStatus;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class WebStorageLauncherCommandTest {

    private static final String[] STORAGE_START_ARGS = { "storage", "--start" };
    private static final String[] STORAGE_STOP_ARGS = { "storage", "--stop" };

    private CommandContext mockCommandContext;
    private CommonPaths mockPaths;
    private ExitStatus mockExitStatus;
    private Launcher mockLauncher;
    private SSLConfiguration mockSslConfig;
    private JettyContainerLauncher mockJettyLauncher;
    private CountDownLatch shutdownLatch;

    @Before
    public void setup() {
        mockCommandContext = mock(CommandContext.class);
        mockExitStatus = mock(ExitStatus.class);
        mockPaths = mock(CommonPaths.class);
        mockLauncher = mock(Launcher.class);
        mockSslConfig = mock(SSLConfiguration.class);

        shutdownLatch = new CountDownLatch(1);

        final ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                AbstractStateNotifyingCommand cmd = mock(AbstractStateNotifyingCommand.class);
                for (Object l :  captor.getValue()) {
                    ActionEvent<ApplicationState> fakeEvent = new ActionEvent<>(cmd, ApplicationState.START);
                    ((ActionListener<ApplicationState>)l).actionPerformed(fakeEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), captor.capture(), eq(false));


        mockJettyLauncher = mock(JettyContainerLauncher.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                CountDownLatch webStartedLatch = (CountDownLatch) invocation.getArguments()[0];
                webStartedLatch.countDown();
                return null;
            }
        }).when(mockJettyLauncher).startContainer(isA(CountDownLatch.class));
        when(mockJettyLauncher.isStartupSuccessFul()).thenReturn(true);
        doNothing().when(mockJettyLauncher).stopContainer();
    }

    private void bind(WebStorageLauncherCommand command) {
        command.bindCommonPaths(mockPaths);
        command.bindExitStatus(mockExitStatus);
        command.bindLauncher(mockLauncher);
        command.bindSslConfig(mockSslConfig);
    }

    @Test
    public void testIsStorageRequired() {
        WebStorageLauncherCommand command = new WebStorageLauncherCommand();

        assertFalse(command.isStorageRequired());
    }

    @Test(expected = CommandException.class)
    public void testRunCommandWithNoConfigListenAddressSpecified() throws Exception {
        Properties systemConfig = new Properties();
        Properties userConfig = new Properties();
        final EmbeddedServletContainerConfiguration testConfig = new EmbeddedServletContainerConfiguration(mockPaths, systemConfig, userConfig);

        WebStorageLauncherCommand command = new WebStorageLauncherCommand() {
            @Override
            EmbeddedServletContainerConfiguration getConfiguration(CommonPaths paths) {
                return testConfig;
            }
        };

        bind(command);
        command.run(mockCommandContext);
    }

    @Test(expected = CommandException.class)
    public void testRunCommandWithWebArchiveNotExisting() throws Exception {
        CommonPaths paths = mock(CommonPaths.class);

        File thermostatHomeNotExisting = mock(File.class);
        when(paths.getSystemThermostatHome()).thenReturn(thermostatHomeNotExisting);

        Properties systemConfig = new Properties();
        Properties userConfig = new Properties();
        userConfig.put(EmbeddedServletContainerConfiguration.ConfigKeys.SERVLET_CONTAINER_BIND_ADDRESS.name(), "127.0.0.1:8888");
        final EmbeddedServletContainerConfiguration testConfig = new EmbeddedServletContainerConfiguration(mockPaths, systemConfig, userConfig);

        WebStorageLauncherCommand command = new WebStorageLauncherCommand() {
            @Override
            EmbeddedServletContainerConfiguration getConfiguration(CommonPaths paths) {
                return testConfig;
            }
        };
        bind(command);
        command.run(mockCommandContext);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRunOnce() throws CommandException {
        final EmbeddedServletContainerConfiguration mockConfig = mock(EmbeddedServletContainerConfiguration.class);
        when(mockConfig.getConnectionUrl()).thenReturn("Test String");

        WebStorageLauncherCommand command = new WebStorageLauncherCommand(shutdownLatch) {
            @Override
            EmbeddedServletContainerConfiguration getConfiguration(CommonPaths paths) {
                return mockConfig;
            }

            @Override
            JettyContainerLauncher getJettyContainerLauncher(EmbeddedServletContainerConfiguration config, SSLConfiguration sslConfig) {
                return mockJettyLauncher;
            }
        };

        bind(command);

        shutdownLatch.countDown();

        command.run(mockCommandContext);

        verify(mockLauncher, times(1)).run(eq(STORAGE_START_ARGS), isA(Collection.class), eq(false));
    }

    @Test(expected = CommandException.class)
    public void testStorageFailStart()  throws CommandException {
        final EmbeddedServletContainerConfiguration mockConfig = mock(EmbeddedServletContainerConfiguration.class);
        when(mockConfig.getConnectionUrl()).thenReturn("Test String");

        WebStorageLauncherCommand command = new WebStorageLauncherCommand(shutdownLatch) {
            @Override
            EmbeddedServletContainerConfiguration getConfiguration(CommonPaths paths) {
                return mockConfig;
            }

            @Override
            JettyContainerLauncher getJettyContainerLauncher(EmbeddedServletContainerConfiguration config, SSLConfiguration sslConfig) {
                return mockJettyLauncher;
            }
        };

        final ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                AbstractStateNotifyingCommand cmd = mock(AbstractStateNotifyingCommand.class);
                for (Object l :  captor.getValue()) {
                    ActionEvent<ApplicationState> fakeEvent = new ActionEvent<>(cmd, ApplicationState.FAIL);
                    ((ActionListener<ApplicationState>)l).actionPerformed(fakeEvent);
                }
                return null;
            }
        }).when(mockLauncher).run(eq(STORAGE_START_ARGS), captor.capture(), eq(false));

        bind(command);

        shutdownLatch.countDown();

        command.run(mockCommandContext);
    }


    @Test(expected = CommandException.class)
    public void testWebContainerFailStart()  throws CommandException {
        final EmbeddedServletContainerConfiguration mockConfig = mock(EmbeddedServletContainerConfiguration.class);
        when(mockConfig.getConnectionUrl()).thenReturn("Test String");

        WebStorageLauncherCommand command = new WebStorageLauncherCommand(shutdownLatch) {
            @Override
            EmbeddedServletContainerConfiguration getConfiguration(CommonPaths paths) {
                return mockConfig;
            }

            @Override
            JettyContainerLauncher getJettyContainerLauncher(EmbeddedServletContainerConfiguration config, SSLConfiguration sslConfig) {
                return mockJettyLauncher;
            }
        };
        bind(command);

        when(mockJettyLauncher.isStartupSuccessFul()).thenReturn(false);
        shutdownLatch.countDown();

        command.run(mockCommandContext);

        verify(mockLauncher, times(1)).run(eq(STORAGE_START_ARGS), isA(Collection.class), eq(false));
        verify(mockLauncher, times(1)).run(eq(STORAGE_STOP_ARGS), eq(false));
    }
}
