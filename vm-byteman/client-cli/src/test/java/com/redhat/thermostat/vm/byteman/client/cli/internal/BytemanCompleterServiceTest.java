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

package com.redhat.thermostat.vm.byteman.client.cli.internal;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.FileNameTabCompleter;
import com.redhat.thermostat.common.cli.TabCompleter;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.redhat.thermostat.vm.byteman.client.cli.internal.BytemanCompleterService.RULES_OPTION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class BytemanCompleterServiceTest {

    private BytemanCompleterService completerService;

    @Before
    public void setup() {
        completerService = new BytemanCompleterService();
        completerService.setFileNameTabCompleter(mock(FileNameTabCompleter.class));
    }

    @Test
    public void testOnlyProvidesCompletionForBytemanCommand() {
        assertThat(completerService.getCommands(), is(equalTo(Collections.singleton(BytemanControlCommand.COMMAND_NAME))));
    }

    @Test
    public void testProvidesNoTopLevelOptionCompletions() {
        assertThat(completerService.getOptionCompleters().size(), is(0));
    }

    @Test
    public void testProvidesLoadSubcommandCompletionForRulesArgument() {
        Map<String, Map<CliCommandOption, ? extends TabCompleter>> map = completerService.getSubcommandCompleters();
        assertThat(map.keySet(), is(equalTo(Collections.singleton(BytemanControlCommand.INJECT_RULE_ACTION))));
        Collection<Map<CliCommandOption, ? extends TabCompleter>> values = map.values();
        assertThat(values.size(), is(1));
        Map<CliCommandOption, ? extends TabCompleter> submap = (Map<CliCommandOption, ? extends TabCompleter>) values.toArray(new Object[1])[0];
        assertThat(submap.keySet(), is(equalTo(Collections.singleton(BytemanCompleterService.RULES_OPTION))));
        assertThat(RULES_OPTION.getLongOpt(), is("rules"));
        assertThat(RULES_OPTION.getOpt(), is("r"));
        assertThat(RULES_OPTION.isRequired(), is(false));
        assertThat(RULES_OPTION.hasArg(), is(true));
    }

    @Test
    public void testProvidesOnlyLoadSubcommandCompletion() {
        Map<String, Map<CliCommandOption, ? extends TabCompleter>> map = completerService.getSubcommandCompleters();
        assertThat(map.size(), is(1));
        for (String key : map.keySet()) {
            assertThat(key, is(equalTo(BytemanControlCommand.INJECT_RULE_ACTION)));
        }
    }

    @Test
    public void testCompleterIsNotNull() {
        Map<String, Map<CliCommandOption, ? extends TabCompleter>> map = completerService.getSubcommandCompleters();
        assertThat(map.get(BytemanControlCommand.INJECT_RULE_ACTION).get(BytemanCompleterService.RULES_OPTION),
                is(not(equalTo(null))));
    }

    @Test
    public void testProvidesNoCompletionWhenFileNameTabCompleterNotAvailable() {
        completerService.setFileNameTabCompleter(null);
        Map<String, Map<CliCommandOption, ? extends TabCompleter>> map = completerService.getSubcommandCompleters();
        assertThat(map.isEmpty(), is(true));
    }

}
