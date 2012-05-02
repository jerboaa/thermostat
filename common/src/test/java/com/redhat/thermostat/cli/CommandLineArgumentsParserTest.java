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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CommandLineArgumentsParserTest {

    private CommandLineArgumentsParser parser;

    private ArgumentSpec arg3;

    @Before
    public void setUp() {
        parser = new CommandLineArgumentsParser();

        ArgumentSpec arg1 = mock(ArgumentSpec.class);
        when(arg1.getName()).thenReturn("test1");
        when(arg1.isRequired()).thenReturn(true);

        ArgumentSpec arg2 = mock(ArgumentSpec.class);
        when(arg2.getName()).thenReturn("test2");
        when(arg2.isRequired()).thenReturn(false);

        arg3 = mock(ArgumentSpec.class);
        when(arg3.getName()).thenReturn("test3");
        when(arg3.isRequired()).thenReturn(false);
        when(arg3.isUsingAdditionalArgument()).thenReturn(true);

        parser.addArguments(Arrays.asList(arg1, arg2, arg3));
    }

    @After
    public void tearDown() {
       parser = null; 
       arg3 = null;
    }

    @Test
    public void testSimpleArgs() throws CommandLineArgumentParseException {
        Arguments args = parser.parse(new String[] { "--test1", "--test2" });

        assertTrue(args.hasArgument("test1"));
        assertEquals(null, args.getArgument("test1"));
        assertTrue(args.hasArgument("test2"));
        assertEquals(null, args.getArgument("test2"));
    }

    @Test(expected=CommandLineArgumentParseException.class)
    public void testNoMatchingArgs() throws CommandLineArgumentParseException {
        parser.parse(new String[] { "--test1", "--no-match" });
    }

    @Test
    public void testMissingRequiredArgument() throws CommandLineArgumentParseException {
        try {
          parser.parse(new String[] { "--test2" });
          fail();
        } catch (CommandLineArgumentParseException ex) {
            String msg = ex.getMessage();
            assertEquals("Missing required option: --test1", msg);
        }
    }

    @Test
    public void testMissingRequiredArguments() throws CommandLineArgumentParseException {
        ArgumentSpec arg4 = mock(ArgumentSpec.class);
        when(arg4.getName()).thenReturn("test4");
        when(arg4.isRequired()).thenReturn(true);
        parser.addArguments(Arrays.asList(arg4));

        try {
          parser.parse(new String[] { "--test2" });
          fail();
        } catch (CommandLineArgumentParseException ex) {
            String msg = ex.getMessage();
            assertEquals("Missing required options: --test1, --test4", msg);
        }
    }

    @Test
    public void testArgumentWithAdditionalArgument() throws CommandLineArgumentParseException {
        Arguments args = parser.parse(new String[] { "--test3", "parameter", "--test1" } );
        assertTrue(args.hasArgument("test3"));
        assertEquals("parameter", args.getArgument("test3"));
        assertTrue(args.hasArgument("test1"));
    }

    @Test(expected=CommandLineArgumentParseException.class)
    public void testMissingAdditionalArgument() throws CommandLineArgumentParseException {
        parser.parse(new String[] { "--test3" });
    }
}
