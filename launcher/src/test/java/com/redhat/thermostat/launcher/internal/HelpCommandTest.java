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

package com.redhat.thermostat.launcher.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.internal.test.TestCommandContextFactory;

public class HelpCommandTest {

    static final String GLOBAL_OPTIONS = ""
            + " --version                display the version of the current thermostat installation\n"
            + " --print-osgi-info        print debug information related to the OSGi framework's boot/shutdown process\n"
            + " --ignore-bundle-versions ignore exact bundle versions and use whatever version is available\n"
            + " --boot-delegation        boot delegation string passed on to the OSGi framework\n";

    private TestCommandContextFactory  ctxFactory;
    private CommandInfoSource commandInfoSource;
    private CommandGroupMetadataSource commandGroupMetadataSource;

    @Before
    public void setUp() {
        ctxFactory = new TestCommandContextFactory();

        commandInfoSource = mock(CommandInfoSource.class);
        commandGroupMetadataSource = mock(CommandGroupMetadataSource.class);
        when(commandGroupMetadataSource.getCommandGroupMetadata()).thenReturn(Collections.<String, PluginConfiguration.CommandGroupMetadata>emptyMap());
    }

    @Test
    public void verifyStorageIsNotRequired() {
        HelpCommand cmd = new HelpCommand();
        assertFalse(cmd.isStorageRequired());
    }

    @Test
    public void verifyHelpFailsWithoutCommandInfoSource() {
        HelpCommand cmd = new HelpCommand();

        Arguments args = mock(Arguments.class);
        when(args.getNonOptionArguments()).thenReturn(Arrays.asList("test1"));
        cmd.setCommandGroupMetadataSource(commandGroupMetadataSource);
        cmd.run(ctxFactory.createContext(args));

        assertEquals("no information about commands", ctxFactory.getError());
        assertEquals("", ctxFactory.getOutput());
    }

    @Test
    public void verifyHelpFailsWithoutCommandGroupMetadataSource() {
        HelpCommand cmd = new HelpCommand();

        Arguments args = mock(Arguments.class);
        when(args.getNonOptionArguments()).thenReturn(Arrays.asList("test1"));
        cmd.setCommandInfoSource(commandInfoSource);
        cmd.run(ctxFactory.createContext(args));

        assertEquals("command group metadata source is missing", ctxFactory.getError());
        assertEquals("", ctxFactory.getOutput());
    }

    @Test
    public void verifyHelpNoArgPrintsListOfCommandsNoCommands() {
        HelpCommand cmd = new HelpCommand();
        cmd.setCommandInfoSource(commandInfoSource);
        cmd.setCommandGroupMetadataSource(commandGroupMetadataSource);
        Arguments args = mock(Arguments.class);
        cmd.run(ctxFactory.createContext(args));
        String expected = "list of commands:\n\n";
        String actual = ctxFactory.getOutput();
        assertEquals(expected, actual);
    }

    @Test
    public void verifyHelpNoArgPrintsListOfCommands2Commands() {
        Collection<CommandInfo> infoList = new ArrayList<CommandInfo>();
        
        CommandInfo info1 = mock(CommandInfo.class);
        when(info1.getName()).thenReturn("test1");
        when(info1.getSummary()).thenReturn("test command 1");
        when(info1.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));

        infoList.add(info1);

        CommandInfo info2 = mock(CommandInfo.class);
        when(info2.getName()).thenReturn("test2longname");
        when(info2.getSummary()).thenReturn("test command 2");
        when(info2.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));
        infoList.add(info2);

        when(commandInfoSource.getCommandInfos()).thenReturn(infoList);

        HelpCommand cmd = new HelpCommand();
        cmd.setEnvironment(Environment.CLI);
        cmd.setCommandInfoSource(commandInfoSource);
        cmd.setCommandGroupMetadataSource(commandGroupMetadataSource);

        Arguments args = mock(Arguments.class);
        cmd.run(ctxFactory.createContext(args));
        String expected = "list of global options:\n\n"
                + GLOBAL_OPTIONS
                + "\n"
                + "list of commands:\n\n"
                + " test1         test command 1\n"
                + " test2longname test command 2\n";
        String actual = ctxFactory.getOutput();
        assertEquals(expected, actual);
    }


    @Test
    public void verifyHelpKnownCmdPrintsCommandUsage() {
        CommandInfo testCommandInfo = mock(CommandInfo.class);
        when(testCommandInfo.getName()).thenReturn("test1");
        when(testCommandInfo.getUsage()).thenReturn("usage of test command");
        when(testCommandInfo.getDescription()).thenReturn("description of test command");
        when(testCommandInfo.getOptions()).thenReturn(new Options());
        when(testCommandInfo.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI));

        PluginConfiguration.Subcommand subcommand = mock(PluginConfiguration.Subcommand.class);
        when(subcommand.getName()).thenReturn("subcommand");
        when(subcommand.getDescription()).thenReturn("subcommand description");
        Options subOptions = new Options();
        subOptions.addOption(new Option("f", "foo argument"));
        when(subcommand.getOptions()).thenReturn(subOptions);
        when(testCommandInfo.getSubcommands()).thenReturn(Collections.singletonList(subcommand));

        when(commandInfoSource.getCommandInfo("test1")).thenReturn(testCommandInfo);

        HelpCommand cmd = new HelpCommand();
        cmd.setCommandInfoSource(commandInfoSource);
        cmd.setCommandGroupMetadataSource(commandGroupMetadataSource);
        Arguments args = mock(Arguments.class);
        when(args.getNonOptionArguments()).thenReturn(Arrays.asList("test1"));
        cmd.run(ctxFactory.createContext(args));

        String actual = ctxFactory.getOutput();
        assertEquals("usage: thermostat usage of test command\n" +
                     "                  description of test command\n" +
                     "Note: this command is only supported outside the shell\n" +
                     "thermostat test1\n" +
                     "     --help    show usage of command\n\n" +
                     "Subcommands:\n" +
                     "\n" +
                     "subcommand:\n" +
                     "subcommand description\n" +
                     "  -f    foo argument\n\n", actual);
    }

    @Test
    public void verifyHelpKnownCmdPrintsCommandUsageSorted() {
        CommandInfo helpInfo = mock(CommandInfo.class);
        when(helpInfo.getName()).thenReturn("help");
        when(helpInfo.getSummary()).thenReturn("show help");
        when(helpInfo.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));

        CommandInfo info1 = mock(CommandInfo.class);
        when(info1.getName()).thenReturn("test1");
        when(info1.getSummary()).thenReturn("test command 1");
        when(info1.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));

        CommandInfo info2 = mock(CommandInfo.class);
        when(info2.getName()).thenReturn("test2");
        when(info2.getSummary()).thenReturn("test command 2");
        when(info2.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));

        CommandInfo info3 = mock(CommandInfo.class);
        when(info3.getName()).thenReturn("test3");
        when(info3.getSummary()).thenReturn("test command 3");
        when(info3.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));

        CommandInfo info4 = mock(CommandInfo.class);
        when(info4.getName()).thenReturn("test4");
        when(info4.getSummary()).thenReturn("test command 4");
        when(info4.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));

        when(commandInfoSource.getCommandInfos()).thenReturn(Arrays.asList(info2, helpInfo, info4, info3, info1));

        HelpCommand cmd = new HelpCommand();
        cmd.setEnvironment(Environment.CLI);
        cmd.setCommandInfoSource(commandInfoSource);
        cmd.setCommandGroupMetadataSource(commandGroupMetadataSource);
        Arguments args = mock(Arguments.class);
        when(args.getNonOptionArguments()).thenReturn(new ArrayList<String>());
        cmd.run(ctxFactory.createContext(args));

        String actual = ctxFactory.getOutput();
        String expected = "list of global options:\n\n"
                + GLOBAL_OPTIONS
                + "\n"
                + "list of commands:\n\n"
                + " help          show help\n"
                + " test1         test command 1\n"
                + " test2         test command 2\n"
                + " test3         test command 3\n"
                + " test4         test command 4\n";
        assertEquals(expected, actual);
    }

    @Test
    public void verifyHelpFiltersCommands() {
        CommandInfo helpInfo = mock(CommandInfo.class);
        when(helpInfo.getName()).thenReturn("help");
        when(helpInfo.getSummary()).thenReturn("show help");
        when(helpInfo.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI));

        CommandInfo info1 = mock(CommandInfo.class);
        when(info1.getName()).thenReturn("test1");
        when(info1.getSummary()).thenReturn("test command 1");
        when(info1.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));

        CommandInfo info2 = mock(CommandInfo.class);
        when(info2.getName()).thenReturn("test2");
        when(info2.getSummary()).thenReturn("test command 2");
        when(info2.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI));

        CommandInfo info3 = mock(CommandInfo.class);
        when(info3.getName()).thenReturn("test3");
        when(info3.getSummary()).thenReturn("test command 3");
        when(info3.getEnvironments()).thenReturn(EnumSet.of(Environment.SHELL));

        CommandInfo info4 = mock(CommandInfo.class);
        when(info4.getName()).thenReturn("test4");
        when(info4.getSummary()).thenReturn("test command 4");
        when(info4.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI));

        when(commandInfoSource.getCommandInfos()).thenReturn(Arrays.asList(info2, helpInfo, info4, info3, info1));

        HelpCommand cmd = new HelpCommand();
        cmd.setEnvironment(Environment.CLI);
        cmd.setCommandInfoSource(commandInfoSource);
        cmd.setCommandGroupMetadataSource(commandGroupMetadataSource);
        Arguments args = mock(Arguments.class);
        when(args.getNonOptionArguments()).thenReturn(new ArrayList<String>());
        cmd.run(ctxFactory.createContext(args));

        String actual = ctxFactory.getOutput();
        String expected = "list of global options:\n\n"
                + GLOBAL_OPTIONS
                + "\n"
                + "list of commands:\n\n"
                + " help          show help\n"
                + " test1         test command 1\n"
                + " test2         test command 2\n"
                + " test4         test command 4\n";
        assertEquals(expected, actual);
    }

    @Test
    public void verifyHelpFiltersOptions() {
        CommandInfo helpInfo = mock(CommandInfo.class);
        when(helpInfo.getName()).thenReturn("help");
        when(helpInfo.getSummary()).thenReturn("show help");
        when(helpInfo.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));

        CommandInfo info1 = mock(CommandInfo.class);
        when(info1.getName()).thenReturn("test1");
        when(info1.getSummary()).thenReturn("test command 1");
        when(info1.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));

        CommandInfo info2 = mock(CommandInfo.class);
        when(info2.getName()).thenReturn("test2");
        when(info2.getSummary()).thenReturn("test command 2");
        when(info2.getEnvironments()).thenReturn(EnumSet.of(Environment.SHELL));

        when(commandInfoSource.getCommandInfos()).thenReturn(Arrays.asList(info2, helpInfo, info1));

        HelpCommand cmd = new HelpCommand();
        cmd.setEnvironment(Environment.SHELL);
        cmd.setCommandInfoSource(commandInfoSource);
        cmd.setCommandGroupMetadataSource(commandGroupMetadataSource);
        Arguments args = mock(Arguments.class);
        when(args.getNonOptionArguments()).thenReturn(new ArrayList<String>());
        cmd.run(ctxFactory.createContext(args));

        String actual = ctxFactory.getOutput();
        String expected = "list of commands:\n\n"
                + " help          show help\n"
                + " test1         test command 1\n"
                + " test2         test command 2\n";
        assertEquals(expected, actual);
    }

    @Test
    public void verifyHelpUnknownCmdPrintsSummaries() {
        when(commandInfoSource.getCommandInfo("test1")).thenThrow(new CommandInfoNotFoundException("test1"));

        HelpCommand cmd = new HelpCommand();
        cmd.setCommandInfoSource(commandInfoSource);
        cmd.setCommandGroupMetadataSource(commandGroupMetadataSource);
        SimpleArguments args = new SimpleArguments();
        args.addNonOptionArgument("test1");
        cmd.run(ctxFactory.createContext(args));

        String expected = "unknown command 'test1'\n"
                        + "list of commands:\n\n";

        String actual = ctxFactory.getOutput();
        assertEquals(expected, actual);
    }

    @Test
    public void verifyHelpKnownCmdPrintsCommandGroupSeeAlso() {
        CommandInfo info1 = mock(CommandInfo.class);
        when(info1.getName()).thenReturn("test1");
        when(info1.getUsage()).thenReturn("usage of test1 command");
        when(info1.getDescription()).thenReturn("description of test1 command");
        when(info1.getCommandGroups()).thenReturn(Collections.singletonList("group"));
        when(info1.getOptions()).thenReturn(new Options());
        when(info1.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI));

        CommandInfo info2 = mock(CommandInfo.class);
        when(info2.getName()).thenReturn("test2");
        when(info2.getUsage()).thenReturn("usage of test2 command");
        when(info2.getDescription()).thenReturn("description of test2 command");
        when(info2.getCommandGroups()).thenReturn(Collections.singletonList("group"));
        when(info2.getOptions()).thenReturn(new Options());
        when(info2.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI));

        CommandInfo info3 = mock(CommandInfo.class);
        when(info3.getName()).thenReturn("test3");
        when(info3.getUsage()).thenReturn("usage of test3 command");
        when(info3.getDescription()).thenReturn("description of test3 command");
        when(info3.getCommandGroups()).thenReturn(Collections.<String>emptyList());
        when(info3.getOptions()).thenReturn(new Options());
        when(info3.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI));

        when(commandInfoSource.getCommandInfo("test1")).thenReturn(info1);
        when(commandInfoSource.getCommandInfo("test2")).thenReturn(info2);
        when(commandInfoSource.getCommandInfo("test3")).thenReturn(info3);
        when(commandInfoSource.getCommandInfos()).thenReturn(Arrays.asList(info1, info2, info3));

        HelpCommand cmd = new HelpCommand();
        cmd.setEnvironment(Environment.CLI);
        cmd.setCommandInfoSource(commandInfoSource);
        cmd.setCommandGroupMetadataSource(commandGroupMetadataSource);
        Arguments args = mock(Arguments.class);
        when(args.getNonOptionArguments()).thenReturn(Collections.singletonList("test1"));
        cmd.run(ctxFactory.createContext(args));

        String actual = ctxFactory.getOutput();
        assertEquals("usage: thermostat usage of test1 command\n" +
                "                  description of test1 command\n" +
                "\n" +
                "thermostat test1\n" +
                "     --help    show usage of command\n" +
                "\n" +
                "See also:\n" +
                "  test2\n", actual);
    }

    @Test
    public void testCommandGroupMetadataDescriptions() {
        when(commandGroupMetadataSource.getCommandGroupMetadata()).thenReturn(Collections.singletonMap(
                "group1", new PluginConfiguration.CommandGroupMetadata("group1", "Group Name", 10)
        ));

        CommandInfo commandInfo = mock(CommandInfo.class);
        when(commandInfo.getName()).thenReturn("test1");
        when(commandInfo.getUsage()).thenReturn("usage of test1 command");
        when(commandInfo.getDescription()).thenReturn("description of test1 command");
        when(commandInfo.getSummary()).thenReturn("summary of test1 command");
        when(commandInfo.getCommandGroups()).thenReturn(Arrays.asList("group1", "group2"));
        when(commandInfo.getOptions()).thenReturn(new Options());
        when(commandInfo.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI));

        when(commandInfoSource.getCommandInfo("test1")).thenReturn(commandInfo);
        when(commandInfoSource.getCommandInfos()).thenReturn(Collections.singleton(commandInfo));

        HelpCommand cmd = new HelpCommand();
        cmd.setEnvironment(Environment.CLI);
        cmd.setCommandInfoSource(commandInfoSource);
        cmd.setCommandGroupMetadataSource(commandGroupMetadataSource);
        Arguments args = mock(Arguments.class);
        cmd.run(ctxFactory.createContext(args));

        String output = ctxFactory.getOutput();
        assertEquals("list of global options:\n" +
                "\n" +
                " --version                display the version of the current thermostat installation\n" +
                " --print-osgi-info        print debug information related to the OSGi framework's boot/shutdown process\n" +
                " --ignore-bundle-versions ignore exact bundle versions and use whatever version is available\n" +
                " --boot-delegation        boot delegation string passed on to the OSGi framework\n" +
                "\n" +
                "list of commands:\n" +
                "\n" +
                "Group Name:    \n" +
                " test1         summary of test1 command\n" +
                "               \n" +
                "group2:        \n" +
                " test1         summary of test1 command\n" +
                "               \n", output);
    }

}

