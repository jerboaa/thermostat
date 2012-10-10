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

package com.redhat.thermostat.client.command.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class PingCommandTest {

    private static final String KNOWN_AGENT_ID = "some-agent-id";

    @Test
    public void testCommandName() {
        PingCommand command = new PingCommand();

        assertEquals("ping", command.getName());
    }

    @Test
    public void testCommandNeedsAgentId() throws CommandException {
        OSGIUtils serviceProvider = mock(OSGIUtils.class);

        PingCommand command = new PingCommand(serviceProvider);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();

        command.run(factory.createContext(args));

        // TODO why doesn't ping throw an exception?
        assertEquals("Ping command accepts one and only one argument.\nUsage not available.\n", factory.getOutput());
    }

    @Test
    public void testCommandWithoutHostDao() throws CommandException {
        OSGIUtils serviceProvider = mock(OSGIUtils.class);

        PingCommand command = new PingCommand(serviceProvider);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addNonOptionArgument(KNOWN_AGENT_ID);

        try {
            command.run(factory.createContext(args));
            fail("did not throw expected exception");
        } catch (CommandException agentDaoServiceMissing) {
            assertEquals("Unable to access host information: service not available", agentDaoServiceMissing.getMessage());
        }
    }

    @Test
    public void testCommandWithoutAgentDao() throws CommandException {
        HostRef host1 = mock(HostRef.class);
        when(host1.getAgentId()).thenReturn(KNOWN_AGENT_ID);

        HostInfoDAO hostInfoDao = mock(HostInfoDAO.class);
        when(hostInfoDao.getAliveHosts()).thenReturn(Arrays.asList(host1));

        OSGIUtils serviceProvider = mock(OSGIUtils.class);
        when(serviceProvider.getServiceAllowNull(HostInfoDAO.class)).thenReturn(hostInfoDao);

        PingCommand command = new PingCommand(serviceProvider);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addNonOptionArgument(KNOWN_AGENT_ID);

        try {
            command.run(factory.createContext(args));
            fail("did not throw expected exception");
        } catch (CommandException agentDaoServiceMissing) {
            assertEquals("Unable to access agent information: service not available", agentDaoServiceMissing.getMessage());
        }
    }

    // TODO add more tests that check the actual behaviour under valid input
}
