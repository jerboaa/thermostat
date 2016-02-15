/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.client.cli.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.model.HostInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;
import com.redhat.thermostat.testutils.StubBundleContext;

public class ListVMsCommandTest {
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private ListVMsCommand cmd;
    private TestCommandContextFactory cmdCtxFactory;
    private HostInfoDAO hostsDAO;
    private AgentInfoDAO agentsDAO;
    private VmInfoDAO vmsDAO;
    private StubBundleContext context;

    @Before
    public void setUp() {
        setupCommandContextFactory();
        context = new StubBundleContext();

        cmd = new ListVMsCommand(context);

        hostsDAO = mock(HostInfoDAO.class);
        vmsDAO = mock(VmInfoDAO.class);
        agentsDAO = mock(AgentInfoDAO.class);
    }

    private void setupCommandContextFactory() {
        cmdCtxFactory = new TestCommandContextFactory();
    }

    @After
    public void tearDown() {
        vmsDAO = null;
        hostsDAO = null;
        cmdCtxFactory = null;
        cmd = null;
    }

    @Test
    public void verifyOutputFormatOneLine() throws CommandException {
        context.registerService(HostInfoDAO.class, hostsDAO, null);
        context.registerService(AgentInfoDAO.class, agentsDAO, null);
        context.registerService(VmInfoDAO.class, vmsDAO, null);

        HostInfo host1 = new HostInfo();
        host1.setHostname("h1");
        host1.setAgentId("123");

        AgentId agentId = new AgentId("123");
        AgentInformation agent1 = new AgentInformation();
        agent1.setAlive(true);
        agent1.setAgentId(agentId.get());

        List<AgentInformation> agentInformationList = new ArrayList<>();
        agentInformationList.add(agent1);

        when(agentsDAO.getAllAgentInformation()).thenReturn(agentInformationList);
        VmId vmId = new VmId("vmId");
        VmInfo vm1Info = new VmInfo("foo", vmId.get(), 1, 0, 1, "", "", "n", "", "", "", "", "", null, null, null, -1, null);
        Set<AgentId> agentIds = new HashSet<>();
        agentIds.add(agentId);
        when(agentsDAO.getAgentIds()).thenReturn(agentIds);

        Set<VmId> vmIds = new HashSet<>();
        vmIds.add(vmId);
        when(vmsDAO.getVmIds(agentId)).thenReturn(vmIds);

        when(agentsDAO.getAgentInformation(agentId)).thenReturn(agent1);
        when(vmsDAO.getVmInfo(vmId)).thenReturn(vm1Info);

        when(hostsDAO.getHostInfo(agentId)).thenReturn(host1);
        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", "fluff");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);

        String output = cmdCtxFactory.getOutput();
        assertEquals("HOST_ID HOST VM_ID VM_PID STATUS VM_NAME\n" +
                     "123     h1   vmId  1      EXITED n\n", output);
    }

    @Test
    public void verifyOutputFormatMultiLines() throws CommandException {
        context.registerService(HostInfoDAO.class, hostsDAO, null);
        context.registerService(AgentInfoDAO.class, agentsDAO, null);
        context.registerService(VmInfoDAO.class, vmsDAO, null);

        HostInfo host1 = new HostInfo();
        host1.setHostname("h1");
        host1.setAgentId("123");

        HostInfo host2 = new HostInfo();
        host2.setHostname("longhostname");
        host2.setAgentId("456");
        AgentId agentId1 = new AgentId("123");
        AgentId agentId2 = new AgentId("456");

        AgentInformation agent1 = new AgentInformation();
        agent1.setAlive(true);
        agent1.setAgentId(agentId1.get());
        AgentInformation agent2 = new AgentInformation();
        agent2.setAlive(true);
        agent2.setAgentId(agentId2.get());

        VmId vmId1 = new VmId("vm1");
        VmId vmId2 = new VmId("vm2");
        VmId vmId3 = new VmId("vm3");
        VmInfo vm1Info = new VmInfo("foo", vmId1.get(), 1, 0, 1, "", "", "n", "", "", "", "", "", null, null, null, -1, null);
        VmInfo vm2Info = new VmInfo("foo", vmId2.get(), 2, 0, 1, "", "", "n1", "", "", "", "", "", null, null, null, -1, null);
        VmInfo vm3Info = new VmInfo("foo", vmId3.get(), 123456, 0, 1, "", "", "longvmname", "", "", "", "", "", null, null, null, -1, null);

        Set<AgentId> agentIds = new HashSet<>();
        agentIds.add(agentId1);
        agentIds.add(agentId2);

        when(agentsDAO.getAgentIds()).thenReturn(agentIds);
        List<AgentInformation> agentInformationList = new ArrayList<>();
        agentInformationList.add(agent1);
        agentInformationList.add(agent2);
        when(agentsDAO.getAllAgentInformation()).thenReturn(agentInformationList);

        Set<VmId> vmIds1 = new HashSet<>();
        vmIds1.add(vmId1);
        vmIds1.add(vmId2);
        Set<VmId> vmIds2 = new HashSet<>();
        vmIds2.add(vmId3);
        when(vmsDAO.getVmIds(agentId1)).thenReturn(vmIds1);
        when(vmsDAO.getVmIds(agentId2)).thenReturn(vmIds2);

        when(agentsDAO.getAgentInformation(agentId1)).thenReturn(agent1);
        when(agentsDAO.getAgentInformation(agentId2)).thenReturn(agent2);
        when(vmsDAO.getVmInfo(vmId1)).thenReturn(vm1Info);
        when(vmsDAO.getVmInfo(vmId2)).thenReturn(vm2Info);
        when(vmsDAO.getVmInfo(vmId3)).thenReturn(vm3Info);

        when(hostsDAO.getHostInfo(agentId1)).thenReturn(host1);
        when(hostsDAO.getHostInfo(agentId2)).thenReturn(host2);
        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", "fluff");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);

        String output = cmdCtxFactory.getOutput();
        assertTrue(output.startsWith("HOST_ID HOST         VM_ID VM_PID STATUS VM_NAME\n"));
        assertTrue(output.contains("123     h1           vm1   1      EXITED n\n"));
        assertTrue(output.contains("123     h1           vm2   2      EXITED n1\n"));
        assertTrue(output.contains("456     longhostname vm3   123456 EXITED longvmname\n"));
    }

    @Test
    public void verifyUnknownStatusIfAgentExited() throws CommandException {
        context.registerService(HostInfoDAO.class, hostsDAO, null);
        context.registerService(AgentInfoDAO.class, agentsDAO, null);
        context.registerService(VmInfoDAO.class, vmsDAO, null);

        HostInfo host1 = new HostInfo();
        host1.setHostname("h1");
        host1.setAgentId("123");

        AgentId agentId = new AgentId("123");

        AgentInformation agent1 = new AgentInformation();
        agent1.setAlive(false);
        agent1.setAgentId(agentId.get());
        VmId vmId1 = new VmId("vm1");
        VmId vmId2 = new VmId("vm2");

        VmInfo vm1Info = new VmInfo("foo", "vm1", 1, 0, 1, "", "", "n", "", "", "", "", "", null, null, null, -1, null);
        VmInfo vm2Info = new VmInfo("foo", "vm1", 2, 0, Long.MIN_VALUE, "", "", "n1", "", "", "", "", "", null, null, null, -1, null);

        Set<AgentId> agentIds = new HashSet<>();
        agentIds.add(agentId);
        when(agentsDAO.getAgentIds()).thenReturn(agentIds);

        Set<VmId> vmIds = new HashSet<>();
        vmIds.add(vmId1);
        vmIds.add(vmId2);

        List<AgentInformation> agentInformationList = new ArrayList<>();
        agentInformationList.add(agent1);
        when(agentsDAO.getAllAgentInformation()).thenReturn(agentInformationList);
        when(vmsDAO.getVmIds(agentId)).thenReturn(vmIds);

        when(agentsDAO.getAgentInformation(agentId)).thenReturn(agent1);
        when(vmsDAO.getVmInfo(vmId1)).thenReturn(vm1Info);
        when(vmsDAO.getVmInfo(vmId2)).thenReturn(vm2Info);

        when(hostsDAO.getHostInfo(agentId)).thenReturn(host1);
        SimpleArguments args = new SimpleArguments();
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);

        String output = cmdCtxFactory.getOutput();
        assertTrue(output.startsWith("HOST_ID HOST VM_ID VM_PID STATUS  VM_NAME\n"));
        assertTrue(output.contains("123     h1   vm1   1      EXITED  n\n"));
        assertTrue(output.contains("123     h1   vm2   2      UNKNOWN n1\n"));
    }

    @Test
    public void testAgentIdOnlyReturnsVmsFromSpecifiedAgent() throws CommandException {
        context.registerService(HostInfoDAO.class, hostsDAO, null);
        context.registerService(AgentInfoDAO.class, agentsDAO, null);
        context.registerService(VmInfoDAO.class, vmsDAO, null);

        HostInfo host1 = new HostInfo();
        host1.setHostname("h2");
        host1.setAgentId("456");

        AgentId agentId1 = new AgentId("123");
        AgentId agentId2 = new AgentId("456");

        AgentInformation agent1 = new AgentInformation();
        agent1.setAlive(true);
        agent1.setAgentId(agentId1.get());
        when(agentsDAO.getAgentInformation(agentId1)).thenReturn(agent1);

        VmInfo vm1Info = new VmInfo("foo", "vm1", 1, 0, 1, "", "", "", "", "", "", "", "", null, null, null, -1, null);
        VmInfo vm2Info = new VmInfo("foo", "vm1", 1, 0, Long.MIN_VALUE, "", "", "", "", "", "", "", "", null, null, null, -1, null);

        VmId vmId1 = new VmId("vm1");
        VmId vmId2 = new VmId("vm2");
        Set<VmId> vmIds1 = new HashSet<>();
        vmIds1.add(vmId1);
        vmIds1.add(vmId2);

        VmId vmId3 = new VmId("vm3");
        VmId vmId4 = new VmId("vm4");
        Set<VmId> vmIds2 = new HashSet<>();
        vmIds2.add(vmId3);
        vmIds2.add(vmId4);

        when(vmsDAO.getVmIds(agentId1)).thenReturn(vmIds1);
        when(vmsDAO.getVmIds(agentId2)).thenReturn(vmIds2);

        HostInfo host2 = new HostInfo();
        host2.setHostname("h2");
        host2.setAgentId("456");

        AgentInformation agent2 = new AgentInformation();
        agent2.setAlive(true);
        agent2.setAgentId(agentId2.get());
        when(agentsDAO.getAgentInformation(agentId2)).thenReturn(agent2);

        VmInfo vm3Info = new VmInfo("foo", "vm2", 2, 0, 1, "", "", "n2", "", "", "", "", "", null, null, null, -1, null);
        VmInfo vm4Info = new VmInfo("foo", "vm2", 3, 5, 0, "", "", "n3", "", "", "", "", "", null, null, null, -1, null);

        when(vmsDAO.getVmInfo(vmId1)).thenReturn(vm1Info);
        when(vmsDAO.getVmInfo(vmId2)).thenReturn(vm2Info);
        when(vmsDAO.getVmInfo(vmId3)).thenReturn(vm3Info);
        when(vmsDAO.getVmInfo(vmId4)).thenReturn(vm4Info);

        when(hostsDAO.getHostInfo(agentId1)).thenReturn(host1);
        when(hostsDAO.getHostInfo(agentId2)).thenReturn(host2);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("agentId", "456");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);

        String output = cmdCtxFactory.getOutput();
        assertTrue(output.startsWith("HOST_ID HOST VM_ID VM_PID STATUS  VM_NAME\n"));
        assertTrue(output.contains("456     h2   vm3   2      EXITED  n2\n"));
        assertTrue(output.contains("456     h2   vm4   3      RUNNING n3\n"));
    }

    @Test(expected=CommandException.class)
    public void testAgentIdArgDoesNotExist() throws CommandException {
        context.registerService(HostInfoDAO.class, hostsDAO, null);
        context.registerService(AgentInfoDAO.class, agentsDAO, null);
        context.registerService(VmInfoDAO.class, vmsDAO, null);

        AgentInformation agent1 = new AgentInformation();
        when(agentsDAO.getAgentInformation(new AgentId("123-456-789"))).thenReturn(agent1);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("agentId", "not-real-id");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        cmd.run(ctx);
    }

    @Test
    public void testNeedHostInfoDAO() throws CommandException {
        context.registerService(VmInfoDAO.class, vmsDAO, null);
        context.registerService(AgentInfoDAO.class, agentsDAO, null);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", "fluff");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        try {
            cmd.run(ctx);
            fail();
        } catch (CommandException e) {
            assertEquals(translator.localize(LocaleResources.HOST_SERVICE_UNAVAILABLE).getContents(), e.getMessage());
        }
    }
    
    @Test
    public void testNeedVmInfoDAO() throws CommandException {
        context.registerService(HostInfoDAO.class, hostsDAO, null);
        context.registerService(AgentInfoDAO.class, agentsDAO, null);

        SimpleArguments args = new SimpleArguments();
        args.addArgument("--dbUrl", "fluff");
        CommandContext ctx = cmdCtxFactory.createContext(args);

        try {
            cmd.run(ctx);
            fail();
        } catch (CommandException e) {
            assertEquals(translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE).getContents(), e.getMessage());
        }
    }

}

