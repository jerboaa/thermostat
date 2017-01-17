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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.testutils.StubBundleContext;

public class CommandSourceTest {

    private CommandSource commandSource;

    private StubBundleContext bundleContext;
    private CommandInfoSource infoSource;

    @Before
    public void setUp() {
        bundleContext = new StubBundleContext();
        commandSource = new CommandSource(bundleContext);

        infoSource = mock(CommandInfoSource.class);

        bundleContext.registerService(CommandInfoSource.class, infoSource, null);
    }

    @Test
    public void testGetNotRegisteredCommand() throws InvalidSyntaxException {
        Command result = commandSource.getCommand("test1");

        assertNull(result);
    }

    @Test
    public void testGetCommandAndInfo() throws InvalidSyntaxException {
        Command cmd = mock(Command.class);
        registerCommand("test", cmd);

        Command result = commandSource.getCommand("test1");

        assertSame(cmd, result);
    }

    @Test
    public void testDoubleRegisteredCommand() throws InvalidSyntaxException {
        Command cmd1 = mock(Command.class);
        Command cmd2 = mock(Command.class);

        registerCommand("test1", cmd1);
        registerCommand("test1", cmd2);

        Command cmd = commandSource.getCommand("test1");

        assertSame(cmd1, cmd);
    }

    private ServiceRegistration registerCommand(String name, Command cmd) {
        Hashtable<String,String> props = new Hashtable<>();
        props.put(Command.NAME, "test1");
        return bundleContext.registerService(Command.class, cmd, props);
    }

 }
