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
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.Before;
import org.junit.Test;

public class UsageStringBuilderTest {

    private UsageStringBuilder builder;

    @Before
    public void setUp() {
        builder = new UsageStringBuilder();
    }

    @Test
    public void verifyUsageWithNoOptions() {
        Options options = new Options();
        String usage = builder.getUsage("test", false, options);
        assertEquals("test", usage);
    }

    @Test
    public void verifyUsageWithSingleShortOption() {
        Option a = new Option("a", "something");
        a.setRequired(false);
        Options options = new Options();
        options.addOption(a);

        String usage = builder.getUsage("test", false, options);
        assertEquals("test [-a]", usage);
    }

    @Test
    public void verifyUsageWithShortAndLongOptions() {
        Options options = new Options();

        Option a = new Option("a", "something");
        options.addOption(a);

        Option b = new Option("b", "bee", false, "another thing");
        options.addOption(b);

        String usage = builder.getUsage("test", false, options);
        String[] parts = usage.split(" ");
        assertEquals(3, parts.length);
        assertEquals("test", parts[0]);
        assertTrue(Arrays.asList(parts).contains("[-a]"));
        assertTrue(Arrays.asList(parts).contains("[--bee]"));
    }

    @Test
    public void verifyOptionWithArgument() {
        Options options = new Options();

        Option a = new Option("a", true, "something");
        a.setArgName("aaah");
        options.addOption(a);

        String usage = builder.getUsage("test", false, options);
        assertEquals("test [-a <aaah>]", usage);
    }

    @Test
    public void verifyRequiredSingleShortOption() {
        Option a = new Option("a", "something");
        a.setRequired(true);
        Options options = new Options();
        options.addOption(a);

        String usage = builder.getUsage("test", false, options);
        assertEquals("test -a", usage);
    }

    @Test
    public void verifyRequiredOptionsBeforeOptionalOnes() {
        Options options = new Options();

        Option a = new Option("a", "something");
        a.setRequired(true);
        options.addOption(a);

        Option b = new Option("b", "something");
        b.setRequired(false);
        options.addOption(b);

        Option c = new Option("c", "something");
        c.setRequired(false);
        options.addOption(c);

        String usage = builder.getUsage("test", false, options);
        assertEquals("test -a [-b] [-c]", usage);
    }

    @Test
    public void verifyPositionArgumentsAreIncluded() {
        Options options = new Options();

        String usage = builder.getUsage("test", false, options, "agent-id", "vm-id");
        assertEquals("test agent-id vm-id", usage);
    }

    @Test
    public void verifyPositionArgumentsAreDisplayedLast() {
        Options options = new Options();

        Option a = new Option("a", "something");
        a.setRequired(true);
        options.addOption(a);

        String usage = builder.getUsage("test", false, options, "agent-id", "vm-id");
        assertEquals("test -a agent-id vm-id", usage);
    }

    @Test
    public void verifySubcommandsPlaceholderIsShownIfApplicable() {
        Options options = new Options();

        String usage = builder.getUsage("test", true, options);
        assertEquals("test <subcommand>", usage);
    }

    @Test
    public void verifySubcommandsPlaceholderIsNotShownIfNotApplicable() {
        Options options = new Options();

        String usage = builder.getUsage("test", false, options);
        assertEquals("test", usage);
    }

    @Test
    public void verifySubcommandsPlaceholderIsShownBetweenCommandAndOptions() {
        Options options = new Options();

        Option a = new Option("a", "something");
        a.setRequired(true);
        a.setArgName("agentId");
        a.setArgs(1);
        options.addOption(a);

        String usage = builder.getUsage("test", true, options, "fileName");
        assertEquals("test <subcommand> -a <agentId> fileName", usage);
    }
}

