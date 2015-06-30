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

package com.redhat.thermostat.vm.gc.command.internal;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.gc.remote.common.GCRequest;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;

public class GCCommandTest {

    private GCCommand command;
    private TestCommandContextFactory cmdCtxFactory;

    private VmInfoDAO vmInfoDAO;
    private AgentInfoDAO agentInfoDAO;
    private GCRequest gcRequest;
    private GCCommandListener listener;

    @Before
    public void setup() {
        cmdCtxFactory = new TestCommandContextFactory();

        vmInfoDAO = mock(VmInfoDAO.class);
        agentInfoDAO = mock(AgentInfoDAO.class);
        gcRequest = mock(GCRequest.class);

        listener = mock(GCCommandListener.class);

        command = new GCCommand(listener);
    }

    @Test
    public void testPerformGC() throws Exception {
        String vmId = "liveVM";

        VmRef vmRef = new VmRef(new HostRef(null, "dummy"), "liveVM", -1, "dummy");

        VmInfo vmInfo = mock(VmInfo.class);
        when(vmInfo.getVmPid()).thenReturn(-1);
        when(vmInfo.getVmId()).thenReturn("liveVM");
        when(vmInfo.getVmName()).thenReturn("dummy");

        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);

        final boolean[] complete = {false};
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                complete[0] = true;
                return null;
            }
        }).when(gcRequest).sendGCRequestToAgent(vmRef, agentInfoDAO, listener);

        CommandContext context = createVmIdArgs(vmId);

        setServices();

        command.run(context);

        assertTrue(complete[0]);
    }

    @Test(expected = CommandException.class)
    public void testPerformGCOnMissingVM() throws Exception {
        String vmId = "nonexistentVM";

        CommandContext context = createVmIdArgs(vmId);

        setServices();

        command.run(context);
    }

    @Test(expected = CommandException.class)
    public void testGCWithoutServices() throws Exception {
        String vmId = "liveVM";

        CommandContext context = createVmIdArgs(vmId);

        command.run(context);
    }

    private CommandContext createVmIdArgs(String vmId) {
        SimpleArguments args = new SimpleArguments();
        args.addArgument(Arguments.VM_ID_ARGUMENT, vmId);
        return cmdCtxFactory.createContext(args);
    }

    private void setServices() {
        command.setServices(gcRequest, agentInfoDAO, vmInfoDAO);
    }

}
