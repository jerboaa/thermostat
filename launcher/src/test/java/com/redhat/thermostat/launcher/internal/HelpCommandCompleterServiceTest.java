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
import com.redhat.thermostat.common.cli.CompletionInfo;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.cli.TabCompleter;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class HelpCommandCompleterServiceTest {

    public static class CompleterServiceTest {

        private CommandInfoSource commandInfoSource;
        private HelpCommandCompleterService service;

        @Before
        public void setup() {
            this.commandInfoSource = mock(CommandInfoSource.class);

            this.service = new HelpCommandCompleterService();
            this.service.bindCommandInfoSource(commandInfoSource);
        }

        @Test
        public void testOnlyProvidesCompletionForHelpCommand() {
            assertThat(service.getCommands(), is(equalTo(Collections.singleton(HelpCommand.COMMAND_NAME))));
        }

        @Test
        public void testOnlyProvidesPositionalArgumentCompletions() {
            Map<CliCommandOption, ? extends TabCompleter> completerMap = service.getOptionCompleters();
            assertThat(completerMap.keySet(), is(equalTo(Collections.singleton(CliCommandOption.POSITIONAL_ARG_COMPLETION))));
            assertThat(completerMap.get(CliCommandOption.POSITIONAL_ARG_COMPLETION), is(not(equalTo(null))));
        }

    }

    public static class HelpCommandCompletionFinderTest {

        private CommandInfoSource commandInfoSource;
        private DependencyServices dependencyServices;
        private HelpCommandCompleterService.HelpCommandCompletionFinder finder;

        @Before
        public void setup() {
            List<CommandInfo> commandInfos = new ArrayList<>();
            CommandInfo info1 = mock(CommandInfo.class);
            when(info1.getName()).thenReturn("foo-command");
            commandInfos.add(info1);
            CommandInfo info2 = mock(CommandInfo.class);
            when(info2.getName()).thenReturn("bar-command");
            commandInfos.add(info2);

            this.commandInfoSource = mock(CommandInfoSource.class);
            when(commandInfoSource.getCommandInfos()).thenReturn(commandInfos);

            this.dependencyServices = mock(DependencyServices.class);
            when(dependencyServices.hasService(CommandInfoSource.class)).thenReturn(true);
            when(dependencyServices.getService(CommandInfoSource.class)).thenReturn(commandInfoSource);

            this.finder = new HelpCommandCompleterService.HelpCommandCompletionFinder(dependencyServices);
        }

        @Test
        public void testGetRequiredDependencies() {
            assertThat(finder.getRequiredDependencies(), is(equalTo(new Class<?>[]{CommandInfoSource.class})));
        }

        @Test
        public void testReturnsEmptyWhenCommandInfoSourceUnavailable() {
            when(dependencyServices.hasService(CommandInfoSource.class)).thenReturn(false);
            when(dependencyServices.getService(CommandInfoSource.class)).thenReturn(null);

            assertThat(finder.findCompletions(), is(equalTo(Collections.<CompletionInfo>emptyList())));
        }

        @Test
        public void testReturnsSimpleCompletionInfosWithCommandNames() {
            List<CompletionInfo> completionInfos = finder.findCompletions();

            List<CompletionInfo> expected =
                    Arrays.asList(new CompletionInfo("foo-command"), new CompletionInfo("bar-command"));

            assertThat(completionInfos, is(equalTo(expected)));
        }

    }

}
