/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.killvm.command;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.command.Request;
import com.redhat.thermostat.killvm.command.internal.ShellVMKilledListener;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class KillVmCommandTest {

    private TestCommandContextFactory cmdCtxFactory;
    private KillVMCommand cmd;

    private HostInfoDAO hostInfoDAO;
    private VmInfoDAO vmInfoDAO;
    private AgentInfoDAO agentInfoDAO;

    private ShellVMKilledListener listener;
    private RequestQueue requestQueue;

    private HostRef hostRef;
    private VmRef vmRef;


    @Before
    public void setup() {
        cmdCtxFactory = new TestCommandContextFactory();

        hostInfoDAO = mock(HostInfoDAO.class);
        vmInfoDAO = mock(VmInfoDAO.class);
        agentInfoDAO = mock(AgentInfoDAO.class);

        listener = mock(ShellVMKilledListener.class);
        requestQueue = mock(RequestQueue.class);

        cmd = new KillVMCommand(listener);
        cmd.setVmInfoDAO(vmInfoDAO);
        cmd.setAgentInfoDAO(agentInfoDAO);
        cmd.setHostInfoDAO(hostInfoDAO);
        cmd.setRequestQueue(requestQueue);

        hostRef = new HostRef("10", "dummy");
        vmRef = new VmRef(hostRef, "liveVM", -1, "dummy");

        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfo.getVmPid()).thenReturn(-1);

        when(vmInfoDAO.getVmInfo(vmRef)).thenReturn(vmInfo);
    }

    @Test
    public void testKillLiveVM() throws CommandException, InterruptedException {
        String vmId = "liveVM";

        AgentInformation agent = mock(AgentInformation.class);
        when(agent.getConfigListenAddress()).thenReturn("addr:10");
        when(agentInfoDAO.getAgentInformation(hostRef)).thenReturn(agent);

        CommandContext ctx = createContext(vmId, hostRef.getAgentId());

        final boolean[] complete = {false};

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                complete[0] = true;
                return null;
            }
        }).when(requestQueue).putRequest(any(Request.class));

        cmd.run(ctx);

        assertTrue(complete[0]);
    }

    @Test(expected = CommandException.class)
    public void testKillNonexistentVM() throws CommandException {
        String vmId = "nonexistentVM";
        CommandContext ctx = createContext(vmId, hostRef.getAgentId());
        cmd.run(ctx);
    }

    @Test(expected = CommandException.class)
    public void testKillNonexistentHost() throws  CommandException {
        String vmId = "liveVM";
        CommandContext ctx = createContext(vmId, "nonexistentHost");
        cmd.run(ctx);
    }

    @Test(expected = CommandException.class)
    public void testNoVMArgument() throws CommandException {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("hostId", "hostId");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);
    }

    @Test(expected =  CommandException.class)
    public void testNoHostArgument() throws CommandException {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("vmId", "vmId");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);
    }

    public CommandContext createContext(String vmId, String hostId) {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("vmId", vmId);
        args.addArgument("hostId", hostId);
        return cmdCtxFactory.createContext(args);
    }
}
