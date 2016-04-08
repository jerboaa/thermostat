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

package com.redhat.thermostat.launcher.internal;

import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.CompleterService;
import com.redhat.thermostat.common.cli.TabCompleter;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.launcher.BundleInformation;
import jline.console.ConsoleReader;
import org.apache.commons.cli.Options;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TabCompletionTest {

    private TabCompletion tabCompletion;
    private Logger log;
    private TreeCompleter treeCompleter;
    private Map<String, TreeCompleter.Node> commandMap;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        log = mock(Logger.class);
        treeCompleter = mock(TreeCompleter.class);
        commandMap = new HashMap<>();
        tabCompletion = new TabCompletion(treeCompleter, commandMap);
    }

    @Test
    public void verifyAddCompleterServiceDoesNothingWhenCommandMapEmpty() {
        CompleterService service = mock(CompleterService.class);
        tabCompletion.addCompleterService(service);
        verify(treeCompleter).setAlphabeticalCompletions(true);
        verifyNoMoreInteractions(treeCompleter);
        assertThat(commandMap.isEmpty(), is(true));
    }

    @Test
    public void verifyRemoveCompleterServiceDoesNothingWhenCommandMapEmpty() {
        CompleterService service = mock(CompleterService.class);
        tabCompletion.removeCompleterService(service);
        verify(treeCompleter).setAlphabeticalCompletions(true);
        verifyNoMoreInteractions(treeCompleter);
        assertThat(commandMap.isEmpty(), is(true));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAddCompleterServiceWorks() {
        doSetupTabCompletion();
        CompleterService service = mock(CompleterService.class);
        when(service.getCommands()).thenReturn(Collections.singleton("mock-command"));
        Map completerMap = new HashMap();
        TabCompleter completer = mock(TabCompleter.class);
        completerMap.put(new CliCommandOption("l", "long", true, "long option", false), completer);
        when(service.getOptionCompleters()).thenReturn(completerMap);

        tabCompletion.addCompleterService(service);

        TreeCompleter.Node node = commandMap.get("mock-command");
        assertThat(node, is(not(equalTo(null))));
        for (TreeCompleter.Node branch : node.getBranches()) {
            if (branch.getTag().equals("--long")) {
                assertThat(branch.getBranches().isEmpty(), is(false));
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRemoveCompleterServiceWorks() {
        doSetupTabCompletion();
        CompleterService service = mock(CompleterService.class);
        when(service.getCommands()).thenReturn(Collections.singleton("mock-command"));
        Map completerMap = new HashMap();
        TabCompleter completer = mock(TabCompleter.class);
        completerMap.put(new CliCommandOption("l", "long", true, "long option", false), completer);
        when(service.getOptionCompleters()).thenReturn(completerMap);

        tabCompletion.addCompleterService(service);
        tabCompletion.removeCompleterService(service);

        TreeCompleter.Node node = commandMap.get("mock-command");
        assertThat(node, is(not(equalTo(null))));
        for (TreeCompleter.Node branch : node.getBranches()) {
            if (branch.getTag().equals("--long")) {
                assertThat(branch.getBranches().isEmpty(), is(true));
            }
        }
    }

    private void doSetupTabCompletion() {
        ConsoleReader reader = mock(ConsoleReader.class);

        Options options = new Options();
        options.addOption("s", "short", true, "short option");
        options.addOption("l", "long", true, "long option");

        CommandInfo mockCommand = mock(CommandInfo.class);
        when(mockCommand.getName()).thenReturn("mock-command");
        when(mockCommand.getBundles()).thenReturn(Collections.<BundleInformation>emptyList());
        when(mockCommand.getDescription()).thenReturn("description");
        when(mockCommand.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));
        when(mockCommand.getOptions()).thenReturn(options);

        CommandInfo fakeCommand = mock(CommandInfo.class);
        when(fakeCommand.getName()).thenReturn("fake-command");
        when(fakeCommand.getBundles()).thenReturn(Collections.<BundleInformation>emptyList());
        when(fakeCommand.getDescription()).thenReturn("description");
        when(fakeCommand.getEnvironments()).thenReturn(EnumSet.of(Environment.CLI, Environment.SHELL));
        when(fakeCommand.getOptions()).thenReturn(options);

        CommandInfoSource infoSource = mock(CommandInfoSource.class);
        when(infoSource.getCommandInfos()).thenReturn(Arrays.asList(mockCommand, fakeCommand));
        when(infoSource.getCommandInfo(mockCommand.getName())).thenReturn(mockCommand);
        when(infoSource.getCommandInfo(fakeCommand.getName())).thenReturn(fakeCommand);

        BundleContext context = mock(BundleContext.class);

        ClientPreferences prefs = mock(ClientPreferences.class);

        tabCompletion.setupTabCompletion(reader, infoSource, context, prefs);

        assertThat(commandMap.containsKey("mock-command"), is(true));
        assertThat(commandMap.containsKey("fake-command"), is(true));
        assertThat(commandMap.size(), is(2));
        verify(treeCompleter, times(2)).addBranch(isA(TreeCompleter.Node.class));
    }

}
