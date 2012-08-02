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

package com.redhat.thermostat.launcher.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.launcher.internal.HelpCommand;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.test.cli.TestCommand;

public class HelpCommandTest {

    private TestCommandContextFactory  ctxFactory;

    @Before
    public void setUp() {

        ctxFactory = new TestCommandContextFactory();


    }

    @After
    public void tearDown() {
        ctxFactory = null;
    }

    @Test
    public void testName() {
        HelpCommand cmd = new HelpCommand();
        assertEquals("help", cmd.getName());
    }

    @Test
    public void verifyHelpNoArgPrintsListOfCommandsNoCommands() {

        HelpCommand cmd = new HelpCommand();
        Arguments args = mock(Arguments.class);
        cmd.run(ctxFactory.createContext(args));
        String expected = "list of commands:\n\n";
        String actual = ctxFactory.getOutput();
        assertEquals(expected, actual);
    }

    @Test
    public void verifyHelpNoArgPrintsListOfCommands2Commands() {

        TestCommand cmd1 = new TestCommand("test1");
        cmd1.setDescription("test command 1");
        TestCommand cmd2 = new TestCommand("test2longname");
        cmd2.setDescription("test command 2");
        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(cmd1, cmd2));

        HelpCommand cmd = new HelpCommand();
        Arguments args = mock(Arguments.class);
        cmd.run(ctxFactory.createContext(args));
        String expected = "list of commands:\n\n"
                        + " test1         test command 1\n"
                        + " test2longname test command 2\n";
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
        Arguments args = mock(Arguments.class);
        when(args.getNonOptionArguments()).thenReturn(Arrays.asList("test1"));
        cmd.run(ctxFactory.createContext(args));

        String actual = ctxFactory.getOutput();
        assertEquals("usage: test1 [--logLevel <arg>] [--password <arg>] [--username <arg>]\n" +
                     "test usage command 1\n" +
                     "     --logLevel <arg>    log level\n" +
                     "     --password <arg>    the password to use for authentication\n" +
                     "     --username <arg>    the username to use for authentication\n", actual);
    }

    @Test
    public void verifyHelpKnownCmdPrintsCommandUsageSorted() {
        TestCommand cmd1 = new TestCommand("test1");
        String description1 = "test command 1";
        cmd1.setDescription(description1);

        TestCommand cmd2 = new TestCommand("test2");
        String description2 = "test command 2";
        cmd2.setDescription(description2);

        TestCommand cmd3 = new TestCommand("test3");
        String description3 = "test command 3";
        cmd3.setDescription(description3);

        TestCommand cmd4 = new TestCommand("test4");
        String description4 = "test command 4";
        cmd4.setDescription(description4);

        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(cmd3, cmd1, cmd2, cmd4));

        HelpCommand cmd = new HelpCommand();
        Arguments args = mock(Arguments.class);
        when(args.getNonOptionArguments()).thenReturn(new ArrayList<String>());
        cmd.run(ctxFactory.createContext(args));

        String actual = ctxFactory.getOutput();
        String expected = "list of commands:\n\n"
                + " test1         test command 1\n"
                + " test2         test command 2\n"
                + " test3         test command 3\n"
                + " test4         test command 4\n";
        assertEquals(expected, actual);
    }

    @Test
    public void verifyHelpKnownStorageCmdPrintsCommandUsageWithDbUrl() {
        TestCommand cmd1 = new TestCommand("test1");
        String usage = "test usage command 1";
        cmd1.setUsage(usage);
        cmd1.setStorageRequired(true);
        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(cmd1));

        HelpCommand cmd = new HelpCommand();
        Arguments args = mock(Arguments.class);
        when(args.getNonOptionArguments()).thenReturn(Arrays.asList("test1"));
        cmd.run(ctxFactory.createContext(args));

        String actual = ctxFactory.getOutput();
        assertEquals("usage: test1 [-d <arg>] [--logLevel <arg>] [--password <arg>] [--username <arg>]\n" +
                     "test usage command 1\n" +
                     "  -d,--dbUrl <arg>       the URL of the storage to connect to\n" +
                     "     --logLevel <arg>    log level\n" +
                     "     --password <arg>    the password to use for authentication\n" +
                     "     --username <arg>    the username to use for authentication\n", actual);
    }

    @Test
    public void verifyHelpUnknownCmdPrintsSummaries() {
        TestCommand cmd1 = new TestCommand("test1");
        cmd1.setUsage("test usage command 1");
        cmd1.setDescription("test command 1");
        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(cmd1));

        HelpCommand cmd = new HelpCommand();
        Arguments args = mock(Arguments.class);
        cmd.run(ctxFactory.createContext(args));

        String expected = "list of commands:\n\n"
                        + " test1         test command 1\n";
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
        String expected = "show help for a given command or help overview";

        assertEquals(expected, cmd.getUsage());
    }
}
