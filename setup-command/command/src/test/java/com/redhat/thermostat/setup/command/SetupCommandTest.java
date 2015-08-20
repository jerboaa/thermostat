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

package com.redhat.thermostat.setup.command;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;

public class SetupCommandTest {

    private SetupCommand cmd;
    private CommandContext ctxt;
    private Arguments mockArgs;
    private Console console;
    private List<String> list = new ArrayList<>();
    private ByteArrayOutputStream outputBaos, errorBaos;
    private PrintStream output, error;
    private CommonPaths paths;
    private Launcher launcher;

    @Before
    public void setUp() {
        paths = mock(CommonPaths.class);
        ctxt = mock(CommandContext.class);
        mockArgs = mock(Arguments.class);
        console = mock(Console.class);

        outputBaos = new ByteArrayOutputStream();
        output = new PrintStream(outputBaos);

        errorBaos = new ByteArrayOutputStream();
        error = new PrintStream(errorBaos);
        launcher = mock(Launcher.class);

        when(ctxt.getArguments()).thenReturn(mockArgs);
        when(ctxt.getConsole()).thenReturn(console);
        when(console.getError()).thenReturn(error);
        when(console.getOutput()).thenReturn(output);
        when(mockArgs.getNonOptionArguments()).thenReturn(list);
    }

    @Test
    public void testLookAndFeelIsSet() throws CommandException {
        final boolean[] isSet = {false};
        cmd = new SetupCommand() {
            @Override
            void setLookAndFeel() throws InvocationTargetException, InterruptedException {
                isSet[0] = true;
            }

            @Override
            void createMainWindowAndRun() {
                //do nothing
            }
        };

        setServices();
        cmd.run(ctxt);

        assertTrue(isSet[0]);
    }

    @Test
    public void testCreateMainWindowIsCalled() throws CommandException {
        final boolean[] isSet = {false};
        cmd = new SetupCommand() {
            @Override
            void setLookAndFeel() throws InvocationTargetException, InterruptedException {
                //do nothing
            }

            @Override
            void createMainWindowAndRun() {
                isSet[0] = true;
            }
        };

        setServices();
        cmd.run(ctxt);

        assertTrue(isSet[0]);
    }


    @Test
    public void testPathsNotSetFailure() {
        cmd = createSetupCommand();

        try {
            cmd.run(ctxt);
            fail();
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("CommonPaths dependency not available"));
        }
    }
    
    @Test
    public void testLauncherNotSetFailure() {
        cmd = createSetupCommand();
        
        cmd.setPaths(mock(CommonPaths.class));
        try {
            cmd.run(ctxt);
            fail();
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("Launcher dependency not available"));
        }
    }
    
    private SetupCommand createSetupCommand() {
        return new SetupCommand() {
            @Override
            void setLookAndFeel() throws InvocationTargetException, InterruptedException {
                //do nothing
            }

            @Override
            void createMainWindowAndRun() {
                //do nothing
            }
        };
    }
    
    private void setServices() {
        cmd.setPaths(paths);
        cmd.setLauncher(launcher);
    }

}

