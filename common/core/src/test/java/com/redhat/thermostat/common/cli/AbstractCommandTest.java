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

package com.redhat.thermostat.common.cli;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractCommandTest {

    @Test
    public void testHasCommandInfo() {
        AbstractCommand command = createCommandForTest();
        CommandInfo info = mock(CommandInfo.class);
        assertFalse(command.hasCommandInfo());
        command.setCommandInfo(info);
        assertTrue(command.hasCommandInfo());
    }

    @Test
    public void testGettersReturnCorrectly() {
        AbstractCommand command = createCommandForTest();

        CommandInfo info = mock(CommandInfo.class);
        when(info.getDescription()).thenReturn("The description");
        when(info.getUsage()).thenReturn("The usage.");
        Options options = new Options();
        Option option = new Option("option", "description");
        options.addOption(option);
        when(info.getOptions()).thenReturn(options);
        command.setCommandInfo(info);

        // The values that should be returned based on the CommandInfo supplied.
        assertTrue(command.hasCommandInfo());
        assertEquals(options, command.getOptions());
        assertEquals("The description", command.getDescription());
        assertEquals("The usage.", command.getUsage());
    }

    @Test
    public void testDefaultReturnValues() {
        AbstractCommand command = createCommandForTest();

        // The default values used before CommandInfo injected.
        assertEquals("Description not available.", command.getDescription());
        assertEquals("Usage not available.", command.getUsage());
        Options options = command.getOptions();
        assertTrue(options.getOptions().isEmpty());
        assertTrue(command.isStorageRequired());
        assertTrue(command.isAvailableInShell());
        assertTrue(command.isAvailableOutsideShell());
    }

    private AbstractCommand createCommandForTest() {
        return new AbstractCommand() {

            @Override
            public void run(CommandContext ctx) throws CommandException {
                // Do nothing.
            }

            @Override
            public String getName() {
                return "name";
            }
            
        };
    }
}

