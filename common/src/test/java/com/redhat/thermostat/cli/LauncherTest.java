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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.test.TestCommandContextFactory;

public class LauncherTest {

    private static class TestCmd1 implements TestCommand.Handle {

        @Override
        public void run(CommandContext ctx) {
            Arguments args = ctx.getArguments();
            ctx.getConsole().getOutput().print(args.getArgument("arg1") + ", " + args.getArgument("arg2"));
        }

    }

    private static class TestCmd2 implements TestCommand.Handle {
        @Override
        public void run(CommandContext ctx) {
            Arguments args = ctx.getArguments();
            ctx.getConsole().getOutput().print(args.getArgument("arg4") + ": " + args.getArgument("arg3"));
        }
    }

    private TestCommandContextFactory  ctxFactory;
    private AppContextSetup appContextSetup;

    @Before
    public void setUp() {

        CLITestEnvironment.setUp();
        setupCommandContextFactory();
        CommandContextFactory.setInstance(ctxFactory);

        TestCommand cmd1 = new TestCommand("test1", new TestCmd1());
        SimpleArgumentSpec arg1 = new SimpleArgumentSpec();
        arg1.setName("arg1");
        arg1.setUsingAdditionalArgument(true);
        SimpleArgumentSpec arg2 = new SimpleArgumentSpec();
        arg2.setName("arg2");
        arg2.setUsingAdditionalArgument(true);
        cmd1.addArguments(arg1, arg2);
        cmd1.setDescription("description 1");
        TestCommand cmd2 = new TestCommand("test2", new TestCmd2());
        SimpleArgumentSpec arg3 = new SimpleArgumentSpec();
        arg3.setName("arg3");
        arg3.setUsingAdditionalArgument(true);
        SimpleArgumentSpec arg4 = new SimpleArgumentSpec();
        arg4.setName("arg4");
        arg4.setUsingAdditionalArgument(true);
        cmd2.addArguments(arg3, arg4);
        cmd2.setDescription("description 2");

        TestCommand cmd3 = new TestCommand("test3");
        cmd3.setStorageRequired(true);
        cmd3.setDescription("description 3");

        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(cmd1, cmd2, cmd3, new HelpCommand()));

    }

    private void setupCommandContextFactory() {
        appContextSetup = mock(AppContextSetup.class);
        ctxFactory = new TestCommandContextFactory() {
            @Override
            protected AppContextSetup getAppContextSetup() {
                return appContextSetup;
            }
        };
        CommandContextFactory.setInstance(ctxFactory);
    }


    @After
    public void tearDown() {
        CLITestEnvironment.tearDown();
    }

    @Test
    public void testMain() {
        runAndVerifyCommand(new String[] {"test1", "--arg1", "Hello", "--arg2", "World"}, "Hello, World");

        ctxFactory.reset();

        runAndVerifyCommand(new String[] {"test2", "--arg3", "Hello", "--arg4", "World"}, "World: Hello");
    }

    @Test
    public void testMainNoArgs() {
        String expected = "list of commands:\n\n"
                        + " help          show help for a given command or help overview\n"
                        + " test1         description 1\n"
                        + " test2         description 2\n"
                        + " test3         description 3\n";
        runAndVerifyCommand(new String[0], expected);
    }

    @Test
    public void verifySetLogLevel() {
        runAndVerifyCommand(new String[] {"test1", "--logLevel", "WARNING", "--arg1", "Hello", "--arg2", "World"}, "Hello, World");
        Logger globalLogger = Logger.getLogger("com.redhat.thermostat");
        assertEquals(Level.WARNING, globalLogger.getLevel());
    }

    @Test
    public void testMainBadCommand1() {
        String expected = "list of commands:\n\n"
            + " help          show help for a given command or help overview\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"--help"}, expected);
    }

    @Test
    public void testMainBadCommand2() {
        String expected = "list of commands:\n\n"
            + " help          show help for a given command or help overview\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"-help"}, expected);
    }

    @Test
    public void testMainBadCommand3() {
        String expected = "list of commands:\n\n"
            + " help          show help for a given command or help overview\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"foobarbaz"}, expected);
    }

    @Test
    public void testMainBadCommand4() {
        String expected = "list of commands:\n\n"
            + " help          show help for a given command or help overview\n"
            + " test1         description 1\n"
            + " test2         description 2\n"
            + " test3         description 3\n";
        runAndVerifyCommand(new String[] {"foo",  "--bar", "baz"}, expected);
    }

    @Test
    public void testMainExceptionInCommand() {
        TestCommand errorCmd = new TestCommand("error", new TestCommand.Handle() {

            @Override
            public void run(CommandContext ctx) throws CommandException {
                throw new CommandException("test error");
            }
        });
        ctxFactory.getCommandRegistry().registerCommands(Arrays.asList(errorCmd));

        new Launcher().run(new String[] { "error" });
        assertEquals("test error\n", ctxFactory.getError());

    }

    private void runAndVerifyCommand(String[] args, String expected) {
        new Launcher().run(args);
        assertEquals(expected, ctxFactory.getOutput());
    }

    @Test
    public void verifyStorageCommandSetsUpDAOFactory() {
        new Launcher().run(new String[] { "test3" , "--dbUrl", "mongo://fluff:12345" });
        verify(appContextSetup).setupAppContext("mongo://fluff:12345");
    }

    @Test
    public void verifyStorageCommandRequiresDbUrl() {
        new Launcher().run(new String[] { "test3" });
        assertEquals("Missing required option: --dbUrl\n", ctxFactory.getError());
    }
}
