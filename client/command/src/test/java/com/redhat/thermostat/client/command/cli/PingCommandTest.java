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

package com.redhat.thermostat.client.command.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

import org.junit.Test;

import com.redhat.thermostat.client.command.cli.PingCommand.PongListener;
import com.redhat.thermostat.client.command.internal.LocaleResources;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.common.command.Response;
import com.redhat.thermostat.common.command.Response.ResponseType;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.testutils.StubBundleContext;

public class PingCommandTest {

    private static final String KNOWN_AGENT_ID = "some-agent-id";

    @Test
    public void testCommandNeedsAgentId() throws CommandException {
        StubBundleContext context = new StubBundleContext();

        PingCommand command = new PingCommand(context);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();

        command.run(factory.createContext(args));

        // TODO why doesn't ping throw an exception?
        assertEquals("Ping command accepts one and only one argument.\n", factory.getOutput());
    }

    @Test
    public void testCommandWithoutHostDao() throws CommandException {
        StubBundleContext context = new StubBundleContext();

        PingCommand command = new PingCommand(context);

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

        StubBundleContext context = new StubBundleContext();
        context.registerService(HostInfoDAO.class, hostInfoDao, null);

        PingCommand command = new PingCommand(context);

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
    
    @Test
    public void testCommandWithoutRequestQueue() throws CommandException {
        HostRef host1 = mock(HostRef.class);
        when(host1.getAgentId()).thenReturn(KNOWN_AGENT_ID);

        HostInfoDAO hostInfoDao = mock(HostInfoDAO.class);
        when(hostInfoDao.getAliveHosts()).thenReturn(Arrays.asList(host1));
        
        AgentInfoDAO agentInfoDao = mock(AgentInfoDAO.class);
        AgentInformation info = mock(AgentInformation.class);
        when(info.getConfigListenAddress()).thenReturn("myHost:9001");
        when(agentInfoDao.getAgentInformation(any(HostRef.class))).thenReturn(info);

        StubBundleContext context = new StubBundleContext();
        context.registerService(HostInfoDAO.class, hostInfoDao, null);
        context.registerService(AgentInfoDAO.class, agentInfoDao, null);

        PingCommand command = new PingCommand(context);

        TestCommandContextFactory factory = new TestCommandContextFactory();

        SimpleArguments args = new SimpleArguments();
        args.addNonOptionArgument(KNOWN_AGENT_ID);

        try {
            command.run(factory.createContext(args));
            fail("did not throw expected exception");
        } catch (CommandException e) {
            assertEquals("Unable to access command request queue: service not available", e.getMessage());
        }
    }
    
    /*
     * Tests whether getContents() gets called on auth fail responses.
     */
    @Test
    public void testAuthFailStringMessage() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bout);
        Semaphore responseBarrier = new Semaphore(1);
        InetSocketAddress addr = InetSocketAddress.createUnresolved("foo", 1234);
        @SuppressWarnings("unchecked")
        Translate<LocaleResources> t = mock(Translate.class);
        when(t.localize(LocaleResources.COMMAND_PING_RESPONSE_AUTH_FAILED, addr.toString())).thenReturn(new LocalizedString("auth_fail"));
        PongListener listener = new PongListener(out, responseBarrier, t);
        responseBarrier.release();
        Request request = mock(Request.class);
        when(request.getTarget()).thenReturn(addr);
        listener.fireComplete(request, new Response(ResponseType.AUTH_FAILED));
        assertEquals("auth_fail\n", bout.toString());
    }

    // TODO add more tests that check the actual behaviour under valid input
}

