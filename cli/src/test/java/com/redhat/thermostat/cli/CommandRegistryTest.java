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

package com.redhat.thermostat.cli;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class CommandRegistryTest {

    private CommandRegistry registry;
    private Command cmd1;
    private Command cmd2;

    @Before
    public void setUp() {
        registry = new CommandRegistry();
        cmd1 = createCommand("test1");
        cmd2 = createCommand("test2");
        registry.registerCommands(Arrays.asList(cmd1, cmd2));
    }

    @After
    public void tearDown() {
        cmd2 = null;
        cmd1 = null;
        registry = null;
    }

    private Command createCommand(String name) {
        Command cmd = mock(Command.class);
        when(cmd.getName()).thenReturn(name);
        return cmd;
    }

    @Test
    public void testRegisterCommands() {
        runAndVerifyCommand("test1", cmd1);
        runAndVerifyCommand("test2", cmd2);
    }

    private void runAndVerifyCommand(String name, Command cmd) {
        Command actualCmd = registry.getCommand(name);
        TestCommandContextFactory cf = new TestCommandContextFactory();
        CommandContext ctx = cf.createContext(new String[0]);
        actualCmd.run(ctx);
        verify(cmd).run(ctx);
    }
}
