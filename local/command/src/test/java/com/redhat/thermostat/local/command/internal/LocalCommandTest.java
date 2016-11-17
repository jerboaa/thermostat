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

package com.redhat.thermostat.local.command.internal;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

public class LocalCommandTest {
    private LocalCommand cmd;
    private CommandContext ctxt;
    private CommonPaths paths;
    private Launcher launcher;
    private ServiceLauncher service;

    @Before
    public void setup() {
        paths = mock(CommonPaths.class);
        when(paths.getSystemBinRoot()).thenReturn(new File(""));
        launcher = mock(Launcher.class);
        ctxt = mock(CommandContext.class);
    }

    @Test
    public void testPathsNotSetFailure() throws CommandException {
        cmd = createLocalCommand();
        try {
            cmd.run(ctxt);
            fail();
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("Required service CommonPaths is unavailable"));
        }
    }

    @Test
    public void testLauncherNotSetFailure() throws CommandException {
        cmd = createLocalCommand();
        cmd.setPaths(paths);
        try {
            cmd.run(ctxt);
            fail();
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("Required service Launcher is unavailable"));
        }
    }

    private LocalCommand createLocalCommand() throws CommandException {
        service = mock(ServiceLauncher.class);
        doNothing().when(service).start();
        doNothing().when(service).stop();
        return new LocalCommand() {
            @Override
            ServiceLauncher createServiceLauncher() {
                return service;
            }
        };
    }

    @Test
    public void testRunLocalCommand() throws CommandException, InterruptedException {
        service = mock(ServiceLauncher.class);
        doNothing().when(service).start();
        doNothing().when(service).stop();

        final Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor()).thenReturn(0);

        cmd = new LocalCommand() {
            @Override
            ServiceLauncher createServiceLauncher() {
                return service;
            }

            @Override
            Process execProcess(String... command) throws IOException {
                String[] expectedArgs = {"/thermostat", "gui"};
                assertArrayEquals("Incorrect thermostat service args", expectedArgs, command);
                return mockProcess;
            }
        };

        cmd.setPaths(paths);
        cmd.setLauncher(launcher);
        cmd.run(ctxt);

        String[] args = {"gui"};
        verify(service, times(1)).start();
        verify(service, times(1)).stop();
        verify(mockProcess, times(1)).waitFor();
    }
}

