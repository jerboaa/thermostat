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

public class HelpCommandTest {

    private TestCommandContextFactory  ctxFactory;

    @Before
    public void setUp() {

        CLITestEnvironment.setUp();
        ctxFactory = new TestCommandContextFactory();
        CommandContextFactory.setInstance(ctxFactory);


    }

    @After
    public void tearDown() {
        CLITestEnvironment.tearDown();
    }

    @Test
    public void testName() {
        HelpCommand cmd = new HelpCommand();
        assertEquals("help", cmd.getName());
    }

    @Test
    public void verifyHelpNoArgPrintsListOfCommandsNoCommands() {

        HelpCommand cmd = new HelpCommand();
        cmd.run(ctxFactory.createContext(new String[0]));
        String expected = "list of commands:\n\n";
        String actual = ctxFactory.getOutput();
        assertEquals(expected, actual);
    }

    @Test
    public void verifyHelpNoArgPrintsListOfCommands2Commands() {

        TestCommand cmd1 = new TestCommand("test1");
        cmd1.setDescription("test command 1");
        TestCommand cmd2 = new TestCommand("test2");
        cmd2.setDescription("test command 2");
        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(cmd1, cmd2));

        HelpCommand cmd = new HelpCommand();
        cmd.run(ctxFactory.createContext(new String[0]));
        String expected = "list of commands:\n\n"
                          + " test1\t\ttest command 1\n"
                          + " test2\t\ttest command 2\n";
        String actual = ctxFactory.getOutput();
        assertEquals(expected, actual);
    }

    @Test
    public void verifyHelpKnownCmdPrintsCommandUsage() {
        TestCommand cmd1 = new TestCommand("test1");
        String usage = "test usage command 1";
        cmd1.setUsage(usage);
        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(cmd1));

        HelpCommand cmd = new HelpCommand();
        cmd.run(ctxFactory.createContext(new String[] { "test1" }));

        String actual = ctxFactory.getOutput();
        assertEquals(usage, actual);
    }

    @Test
    public void verifyHelpUnknownCmdPrintsSummaries() {
        TestCommand cmd1 = new TestCommand("test1");
        cmd1.setUsage("test usage command 1");
        cmd1.setDescription("test command 1");
        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(cmd1));

        HelpCommand cmd = new HelpCommand();
        cmd.run(ctxFactory.createContext(new String[] { "test12" }));

        String expected = "list of commands:\n\n"
                + " test1\t\ttest command 1\n";
        String actual = ctxFactory.getOutput();
        assertEquals(expected, actual);
    }

    @Test
    public void testDescription() {
        HelpCommand cmd = new HelpCommand();
        assertEquals("show help for a given command or help overview", cmd.getDescription());
    }

    @Test
    public void testUsage() {
        HelpCommand cmd = new HelpCommand();
        String expected = "help [COMMAND]\n\n"
                          + "show help for a given command or help overview\n\n\t"
                          + "With no arguments, print a list of commands with short help messages.\n\n\t"
                          + "Given a command, print help for that command.";

        assertEquals(expected, cmd.getUsage());
    }
}
