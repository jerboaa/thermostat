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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.launcher.CommandLineArgumentParseException;

public class CommandLineArgumentsParserTest {

    private CommandLineArgumentsParser parser;

    @Before
    public void setUp() {
        parser = new CommandLineArgumentsParser();

        Options options = new Options();

        Option option1 = new Option("t", "test1", false, null);
        option1.setArgName("test1");
        option1.setRequired(true);
        options.addOption(option1);

        Option option2 = new Option("r", "test2", false, null);
        option2.setArgName("test2");
        option2.setRequired(false);
        options.addOption(option2);

        Option option3 = new Option("s", "test3", true, null);
        option3.setArgName("test3");
        option3.setRequired(false);
        options.addOption(option3);

        parser.addOptions(options);
    }

    @After
    public void tearDown() {
       parser = null;
    }

    @Test
    public void testSimpleArgs() throws CommandLineArgumentParseException {
        Arguments args = parser.parse(new String[] { "--test1", "--test2" });

        assertTrue(args.hasArgument("test1"));
        assertEquals(null, args.getArgument("test1"));
        assertTrue(args.hasArgument("test2"));
        assertEquals(null, args.getArgument("test2"));
    }

    @Test
    public void testShortArgs() throws CommandLineArgumentParseException {
        Arguments args = parser.parse(new String[] { "-t", "--test2" });

        assertTrue(args.hasArgument("test1"));
        assertTrue(args.hasArgument("t"));
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
            assertEquals("Missing required option: -t", msg);
        }
    }

    @Test
    public void testMissingRequiredArguments() throws CommandLineArgumentParseException {
        Options options = new Options();
        Option option4 = new Option(null, "test4", false, null);
        option4.setArgName("test4");
        option4.setRequired(true);
        options.addOption(option4);
        parser.addOptions(options);

        try {
          parser.parse(new String[] { "--test2" });
          fail();
        } catch (CommandLineArgumentParseException ex) {
            String msg = ex.getMessage();
            assertEquals("Missing required options: -t, --test4", msg);
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

    @Test(expected = CommandLineArgumentParseException.class)
    public void testRequiredArgumentEscaped() throws CommandLineArgumentParseException {
        String[] rawArgs = new String[] { "--", "-t" };
        parser.parse(rawArgs);
    }

    @Test
    public void testEscapedArguments() throws CommandLineArgumentParseException {
        String[] rawArgs = new String[] { "-t", "--", "--this-is-not-an-option" };
        Arguments args = parser.parse(rawArgs);

        assertTrue(args.hasArgument("t"));
        assertFalse(args.hasArgument("--this-is-not-an-option"));
        assertEquals("--this-is-not-an-option", args.getNonOptionArguments().get(0));
    }

}

