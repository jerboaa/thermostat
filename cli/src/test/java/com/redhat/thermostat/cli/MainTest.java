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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MainTest {

    private static class TestCmd1 implements TestCommand.Handle {

        @Override
        public void run(CommandContext ctx) {
            ctx.getConsole().getOutput().print(ctx.getArguments()[0] + ", " + ctx.getArguments()[1]);
        }

    }

    private static class TestCmd2 implements TestCommand.Handle {
        @Override
        public void run(CommandContext ctx) {
            ctx.getConsole().getOutput().print(ctx.getArguments()[1] + ": " + ctx.getArguments()[0]);
        }
    }

    private TestCommandContextFactory  ctxFactory;

    @Before
    public void setUp() {

        CLITestEnvironment.setUp();
        ctxFactory = new TestCommandContextFactory();
        CommandContextFactory.setInstance(ctxFactory);

        TestCommand cmd1 = new TestCommand("test1", new TestCmd1());
        cmd1.setDescription("description 1");
        TestCommand cmd2 = new TestCommand("test2", new TestCmd2());
        cmd2.setDescription("description 2");
        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(cmd1, cmd2));

    }

    @After
    public void tearDown() {
        CLITestEnvironment.tearDown();
    }

    @Test
    public void testMain() {
        runAndVerifyCommand(new String[] {"test1", "Hello", "World"}, "Hello, World");

        ctxFactory.reset();

        runAndVerifyCommand(new String[] {"test2", "Hello", "World"}, "World: Hello");
    }

    @Test
    public void testMainNoArgs() {
        String expected = "list of commands:\n\n"
                          + " help\t\tshow help for a given command or help overview\n"
                          + " test1\t\tdescription 1\n"
                          + " test2\t\tdescription 2\n";
        runAndVerifyCommand(new String[0], expected);
    }

    private void runAndVerifyCommand(String[] args, String expected) {
        Main.main(args);
        assertEquals(expected, ctxFactory.getOutput());
    }
}
