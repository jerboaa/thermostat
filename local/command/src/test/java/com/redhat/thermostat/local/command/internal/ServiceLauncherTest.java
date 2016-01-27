/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.local.command.internal;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.local.command.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class ServiceLauncherTest {
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private ServiceLauncher serviceLauncher;
    private String thermostat;
    private File webApp;
    private File servicePid;

    @Before
    public void setup() throws IOException {
        thermostat = "thermostat-test";
        webApp = mock(File.class);
        servicePid = mock(File.class);
        when(servicePid.getCanonicalPath()).thenReturn("test-pid");
    }

    @Test
    public void testThermostatServiceStartWebAppInstalled() throws CommandException {
        when(webApp.exists()).thenReturn(true);
        testThermostatServiceStart("web-storage-service");
    }

    @Test
    public void testThermostatServiceStartWebAppNotInstalled() throws CommandException {
        when(webApp.exists()).thenReturn(false);
        testThermostatServiceStart("service");
    }

    private void testThermostatServiceStart(final String expectedServiceArg) throws CommandException {
        final String[] expectedArgs = {expectedServiceArg};
        Launcher launcher = new Launcher() {
            @Override
            public void run(String[] args, boolean inShell) {
                // shouldn't be called
                fail("Incorrect Launcher.run() used.");
            }

            @Override
            public void run(String[] args, Collection<ActionListener<ApplicationState>> listeners, boolean inShell) {
                assertArrayEquals("Incorrect thermostat service args", expectedArgs, args);
                assertFalse(inShell);
                assertTrue(listeners.size() == 1);
                AbstractStateNotifyingCommand cmd = mock(AbstractStateNotifyingCommand.class);

                // return START state
                ActionEvent<ApplicationState> fakeEvent = new ActionEvent<>(cmd, ApplicationState.START);
                listeners.iterator().next().actionPerformed(fakeEvent);
            }
        };

        serviceLauncher = new ServiceLauncher(launcher, webApp);
        serviceLauncher.start();
    }

    @Test
    public void testThermostatServiceStartFail() {
        String serviceArg = "web-storage-service";
        final String[] expectedArgs = {serviceArg};
        when(webApp.exists()).thenReturn(true);
        Launcher launcher = new Launcher() {
            @Override
            public void run(String[] args, boolean inShell) {
                // shouldn't be called
                fail("Incorrect Launcher.run() used.");
            }

            @Override
            public void run(String[] args, Collection<ActionListener<ApplicationState>> listeners, boolean inShell) {
                assertArrayEquals("Incorrect thermostat service args", expectedArgs, args);
                assertFalse(inShell);
                assertTrue(listeners.size() == 1);

                // return FAIL state
                AbstractStateNotifyingCommand cmd = mock(AbstractStateNotifyingCommand.class);
                ActionEvent<ApplicationState> fakeEvent = new ActionEvent<>(cmd, ApplicationState.FAIL);
                listeners.iterator().next().actionPerformed(fakeEvent);
            }
        };

        serviceLauncher = new ServiceLauncher(launcher, webApp);
        try {
            serviceLauncher.start();
        } catch (CommandException e) {
            assertTrue(e.getMessage().equals(t.localize(LocaleResources.ERROR_STARTING_SERVICE, serviceArg).getContents()));
        }
    }

    @Test
    public void testThermostatServiceStopServiceStarted() throws CommandException {
        final boolean[] isShutdown = {false};
        Launcher launcher = new Launcher() {
            @Override
            public void run(String[] args, boolean inShell) {
                // shouldn't be called
                fail("Incorrect Launcher.run() used.");
            }

            @Override
            public void run(String[] args, Collection<ActionListener<ApplicationState>> listeners, boolean inShell) {
                // return START state
                AbstractStateNotifyingCommand cmd = mock(AbstractStateNotifyingCommand.class);
                ActionEvent<ApplicationState> fakeEvent = new ActionEvent<>(cmd, ApplicationState.START);
                listeners.iterator().next().actionPerformed(fakeEvent);

                try {
                    block();
                } catch (InterruptedException e) {
                    isShutdown[0] = true;
                }
            }

            private void block() throws InterruptedException {
                while(!Thread.currentThread().isInterrupted()) {
                    //spin until interrupted
                }
                throw new InterruptedException();
            }
        };

        serviceLauncher = new ServiceLauncher(launcher, webApp);
        serviceLauncher.start();
        serviceLauncher.stop();
        assertTrue(isShutdown[0]);
    }

    @Test
    public void testThermostatServiceStopWhenServiceNotStarted() throws CommandException {
        Launcher launcher = new Launcher() {
            @Override
            public void run(String[] args, boolean inShell) {
                // shouldn't be called
                fail("Incorrect Launcher.run() used.");
            }

            @Override
            public void run(String[] args, Collection<ActionListener<ApplicationState>> listeners, boolean inShell) {
                fail("Launcher.run() should not be when service not already started.");
            }
        };

        serviceLauncher = new ServiceLauncher(launcher, webApp);
        serviceLauncher.stop();
    }

    @Test
    public void testThermostatServiceStopWhenServiceStartedButNotRunning() throws CommandException {
        Launcher launcher = new Launcher() {
            @Override
            public void run(String[] args, boolean inShell) {
                // shouldn't be called
                fail("Incorrect Launcher.run() used.");
            }

            @Override
            public void run(String[] args, Collection<ActionListener<ApplicationState>> listeners, boolean inShell) {
                // return START state and end
                AbstractStateNotifyingCommand cmd = mock(AbstractStateNotifyingCommand.class);
                ActionEvent<ApplicationState> fakeEvent = new ActionEvent<>(cmd, ApplicationState.START);
                listeners.iterator().next().actionPerformed(fakeEvent);
            }
        };

        serviceLauncher = new ServiceLauncher(launcher, webApp);
        serviceLauncher.start();
        serviceLauncher.stop();
    }

}
