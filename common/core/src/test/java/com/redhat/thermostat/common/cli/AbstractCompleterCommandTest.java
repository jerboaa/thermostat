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

package com.redhat.thermostat.common.cli;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osgi.service.component.ComponentContext;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractCompleterCommandTest {

    public static final String COMMAND_NAME = "StubCompleterCommand";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private AbstractCompleterCommand command;
    private ComponentContext componentContext;

    @Before
    public void setup() {
        command = new StubCompleterCommand();
        componentContext = mock(ComponentContext.class);
        Dictionary dict = mock(Dictionary.class);
        when(dict.get(Command.NAME)).thenReturn(COMMAND_NAME);
        when(componentContext.getProperties()).thenReturn(dict);
    }

    @Test
    public void testActivationSetsCommandName() {
        command.activate(componentContext);
        assertThat(command.getCommands(), is(equalTo(Collections.singleton(COMMAND_NAME))));
    }

    @Test
    public void testThrowsExceptionIfGetCommandsCalledBeforeActivation() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The implementation class " +
                "com.redhat.thermostat.common.cli.AbstractCompleterCommandTest$StubCompleterCommand does not define an" +
                " OSGi property for COMMAND_NAME, which is required.");
        command.getCommands();
    }

    @Test
    public void testActivationFailsIfCommandNamePropertyNotSet() {
        Dictionary dict = mock(Dictionary.class);
        when(componentContext.getProperties()).thenReturn(dict);
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("The implementation class " +
                "com.redhat.thermostat.common.cli.AbstractCompleterCommandTest$StubCompleterCommand does not define an" +
                " OSGi property for COMMAND_NAME, which is required.");
        command.activate(componentContext);
    }

    private static class StubCompleterCommand extends AbstractCompleterCommand {
        @Override
        public void run(CommandContext ctx) throws CommandException {
            // no-op
        }

        @Override
        public Map<CliCommandOption, ? extends TabCompleter> getOptionCompleters() {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, Map<CliCommandOption, ? extends TabCompleter>> getSubcommandCompleters() {
            return Collections.emptyMap();
        }
    }

}
