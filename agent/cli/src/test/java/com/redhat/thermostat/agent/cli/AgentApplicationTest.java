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

package com.redhat.thermostat.agent.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.cli.AgentApplication;

public class AgentApplicationTest {

    // TODO: Test i18nized versions when they come.

    private AgentApplication agent;

    @Before
    public void setUp() {
        agent = new AgentApplication();
    }

    @After
    public void tearDown() {
        agent = null;
    }

    @Test
    public void testName() {
        String name = agent.getName();
        assertEquals("agent", name);
    }

    @Test
    public void testUsage() {
        String usage = agent.getUsage();
        assertEquals("thermostat agent -d <url> [-u <user> -p <password>] [-s] [--debug]", usage);
    }

    @Test
    public void testOptions() {
        Options options = agent.getOptions();
        assertNotNull(options);
        assertEquals(5, options.getOptions().size());

        assertTrue(options.hasOption("saveOnExit"));
        Option save = options.getOption("saveOnExit");
        assertEquals("s", save.getOpt());
        assertEquals("save the data on exit", save.getDescription());
        assertFalse(save.isRequired());
        assertFalse(save.hasArg());

        assertTrue(options.hasOption("debug"));
        Option debug = options.getOption("debug");
        assertEquals("launch with debug console enabled", debug.getDescription());
        assertFalse(debug.isRequired());
        assertFalse(debug.hasArg());

        assertTrue(options.hasOption("dbUrl"));
        Option db = options.getOption("dbUrl");
        assertEquals("d", db.getOpt());
        assertEquals("connect to the given url", db.getDescription());
        assertTrue(db.isRequired());
        assertTrue(db.hasArg());

        assertTrue(options.hasOption("username"));
        Option user = options.getOption("username");
        assertEquals("the username to use for authentication", user.getDescription());
        assertFalse(user.isRequired());
        assertTrue(user.hasArg());

        assertTrue(options.hasOption("password"));
        Option pass = options.getOption("password");
        assertEquals("the password to use for authentication", pass.getDescription());
        assertFalse(pass.isRequired());
        assertTrue(pass.hasArg());
    }
}
