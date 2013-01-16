/*
 * Copyright 2012, 2013 Red Hat, Inc.
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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandInfo;
import com.redhat.thermostat.common.cli.CommandInfoNotFoundException;
import com.redhat.thermostat.common.cli.CommandInfoSource;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.launcher.TestCommand;
import com.redhat.thermostat.launcher.internal.HelpCommand;
import com.redhat.thermostat.test.TestCommandContextFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HelpCommand.class, FrameworkUtil.class})
public class HelpCommandTest {

    private TestCommandContextFactory  ctxFactory;

    private void mockCommandInfoSourceService(CommandInfoSource infos) {
        PowerMockito.mockStatic(FrameworkUtil.class);
        Bundle bundle = mock(Bundle.class);
        BundleContext bCtx = mock(BundleContext.class);
        when(bundle.getBundleContext()).thenReturn(bCtx);
        ServiceReference infosRef = mock(ServiceReference.class);
        when(bCtx.getServiceReference(CommandInfoSource.class)).thenReturn(infosRef);
        when(bCtx.getService(infosRef)).thenReturn(infos);
        when(FrameworkUtil.getBundle(isA(HelpCommand.class.getClass()))).thenReturn(bundle);
    }

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
        CommandInfoSource infos = mock(CommandInfoSource.class);
        mockCommandInfoSourceService(infos);

        HelpCommand cmd = new HelpCommand();
        Arguments args = mock(Arguments.class);
        cmd.run(ctxFactory.createContext(args));
        String expected = "list of commands:\n\n";
        String actual = ctxFactory.getOutput();
        assertEquals(expected, actual);
    }

    @Test
    public void verifyHelpNoArgPrintsListOfCommands2Commands() {

        CommandInfoSource infos = mock(CommandInfoSource.class);
        Collection<CommandInfo> infoList = new ArrayList<CommandInfo>();
        
        CommandInfo info1 = mock(CommandInfo.class);
        when(info1.getName()).thenReturn("test1");
        when(info1.getDescription()).thenReturn("test command 1");
        infoList.add(info1);

        CommandInfo info2 = mock(CommandInfo.class);
        when(info2.getName()).thenReturn("test2longname");
        when(info2.getDescription()).thenReturn("test command 2");
        infoList.add(info2);

        when(infos.getCommandInfos()).thenReturn(infoList);
        mockCommandInfoSourceService(infos);

        HelpCommand cmd = new HelpCommand();
        Arguments args = mock(Arguments.class);
        cmd.run(ctxFactory.createContext(args));
        String expected = "list of commands:\n\n"
                        + " test1         test command 1\n"
                        + " test2longname test command 2\n";
        String actual = ctxFactory.getOutput();
        assertEquals(expected, actual);
    }

    // TODO bug wrt CommonCommandOptions makes this test fail.  Commenting out until that is resolved
    @Ignore
    @Test
    public void verifyHelpKnownCmdPrintsCommandUsage() {
        CommandInfoSource infos = mock(CommandInfoSource.class);
        Collection<CommandInfo> infoList = new ArrayList<CommandInfo>();
        
        CommandInfo info1 = mock(CommandInfo.class);
        when(info1.getName()).thenReturn("test1");
        when(info1.getDescription()).thenReturn("test command 1");
        infoList.add(info1);

        when(infos.getCommandInfos()).thenReturn(infoList);
        mockCommandInfoSourceService(infos);

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

        CommandInfoSource infos = mock(CommandInfoSource.class);
        Collection<CommandInfo> infoList = new ArrayList<CommandInfo>();
        
        CommandInfo info1 = mock(CommandInfo.class);
        when(info1.getName()).thenReturn("test1");
        when(info1.getDescription()).thenReturn("test command 1");
        infoList.add(info1);

        CommandInfo info2 = mock(CommandInfo.class);
        when(info2.getName()).thenReturn("test2");
        when(info2.getDescription()).thenReturn("test command 2");
        infoList.add(info2);

        CommandInfo info3 = mock(CommandInfo.class);
        when(info3.getName()).thenReturn("test3");
        when(info3.getDescription()).thenReturn("test command 3");
        infoList.add(info3);

        CommandInfo info4 = mock(CommandInfo.class);
        when(info4.getName()).thenReturn("test4");
        when(info4.getDescription()).thenReturn("test command 4");
        infoList.add(info4);

        when(infos.getCommandInfos()).thenReturn(infoList);
        mockCommandInfoSourceService(infos);

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

    // TODO bug wrt CommonCommandOptions makes this test fail.
    @Ignore
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
        assertEquals("usage: test1 [--logLevel <arg>] [--password <arg>] [--username <arg>]\n" +
                     "test usage command 1\n" +
                     "  -d,--dbUrl <arg>       the URL of the storage to connect to\n" +
                     "     --logLevel <arg>    log level\n" +
                     "     --password <arg>    the password to use for authentication\n" +
                     "     --username <arg>    the username to use for authentication\n", actual);
    }

    @Test
    public void verifyHelpUnknownCmdPrintsSummaries() {

        CommandInfoSource infos = mock(CommandInfoSource.class);
        
        when(infos.getCommandInfo("test1")).thenThrow(new CommandInfoNotFoundException("test1"));
        mockCommandInfoSourceService(infos);

        HelpCommand cmd = new HelpCommand();
        SimpleArguments args = new SimpleArguments();
        args.addNonOptionArgument("test1");
        cmd.run(ctxFactory.createContext(args));

        String expected = "unknown command 'test1'\n"
                        + "list of commands:\n\n";

        String actual = ctxFactory.getOutput();
        assertEquals(expected, actual);
    }

    @Test
    public void testDescAndUsage() {
        HelpCommand cmd = new HelpCommand();
        assertNotNull(cmd.getDescription());
        assertNotNull(cmd.getUsage());
    }
}

