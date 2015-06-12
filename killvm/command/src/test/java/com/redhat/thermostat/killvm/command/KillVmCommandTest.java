/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.command.RequestResponseListener;
import com.redhat.thermostat.killvm.command.internal.ShellVMKilledListener;
import com.redhat.thermostat.killvm.common.KillVMRequest;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class KillVmCommandTest {

    private TestCommandContextFactory cmdCtxFactory;
    private KillVMCommand cmd;

    private VmInfoDAO vmInfoDAO;
    private AgentInfoDAO agentInfoDAO;

    private ShellVMKilledListener listener;
    private KillVMRequest request;

    @Before
    public void setup() {
        cmdCtxFactory = new TestCommandContextFactory();

        vmInfoDAO = mock(VmInfoDAO.class);
        agentInfoDAO = mock(AgentInfoDAO.class);

        listener = mock(ShellVMKilledListener.class);
        request = mock(KillVMRequest.class);

        cmd = new KillVMCommand(listener);
    }

    @Test
    public void testKillLiveVM() throws CommandException, InterruptedException {
        String vmId = "liveVM";

        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);

        CommandContext ctx = createContext(vmId);

        setServices();
        cmd.run(ctx);

        verify(request).sendKillVMRequestToAgent(any(AgentId.class), any(int.class), any(AgentInfoDAO.class), any(RequestResponseListener.class));
    }

    @Test(expected = CommandException.class)
    public void testKillNonexistentVM() throws CommandException {
        String vmId = "nonexistentVM";

        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(null);

        CommandContext ctx = createContext(vmId);
        setServices();

        cmd.run(ctx);
    }

    @Test(expected = CommandException.class)
    public void testNoVMArgument() throws CommandException {
        SimpleArguments args = new SimpleArguments();
        CommandContext ctx = cmdCtxFactory.createContext(args);
        setServices();
        cmd.run(ctx);
    }

    public CommandContext createContext(String vmId) {
        SimpleArguments args = new SimpleArguments();
        args.addArgument("vmId", vmId);
        return cmdCtxFactory.createContext(args);
    }

    private void setServices() {
        cmd.setVmInfoDAO(vmInfoDAO);
        cmd.setAgentInfoDAO(agentInfoDAO);
        cmd.setKillVMRequest(request);

    }
}
