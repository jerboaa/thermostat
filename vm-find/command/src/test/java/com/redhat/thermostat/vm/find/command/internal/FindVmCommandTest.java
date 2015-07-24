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

package com.redhat.thermostat.vm.find.command.internal;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.test.TestCommandContextFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FindVmCommandTest {

    private Arguments args;

    @Before
    public void setup() {
        args = mock(Arguments.class);
    }

    @Test
    public void testCommandFailsWhenDaosUnavailable() {
        FindVmCommand command = new FindVmCommand();
        try {
            command.run(new TestCommandContextFactory().createContext(args));
            fail("should have received CommandException");
        } catch (CommandException e) {
            assertTrue(e.getMessage().contains("AgentInfoDAO is unavailable")
                || e.getMessage().contains("HostInfoDAO is unavailable")
                || e.getMessage().contains("VmInfoDAO is unavailable"));
        }
    }

    @Test
    public void testCommandFailsWhenAgentInfoDaoUnavailable() {
        FindVmCommand command = new FindVmCommand();
        command.setVmInfoDAO(mock(VmInfoDAO.class));
        command.setHostInfoDAO(mock(HostInfoDAO.class));
        try {
            command.run(new TestCommandContextFactory().createContext(args));
            fail("should have received CommandException");
        } catch (CommandException e) {
            assertThat(e.getMessage(), containsString("AgentInfoDAO is unavailable"));
        }
    }

    @Test
    public void testCommandFailsWhenHostInfoDaoUnavailable() {
        FindVmCommand command = new FindVmCommand();
        command.setAgentInfoDAO(mock(AgentInfoDAO.class));
        command.setVmInfoDAO(mock(VmInfoDAO.class));
        try {
            command.run(new TestCommandContextFactory().createContext(args));
            fail("should have received CommandException");
        } catch (CommandException e) {
            assertThat(e.getMessage(), containsString("HostInfoDAO is unavailable"));
        }
    }

    @Test
    public void testCommandFailsWhenVmInfoDaoUnavailable() {
        FindVmCommand command = new FindVmCommand();
        command.setAgentInfoDAO(mock(AgentInfoDAO.class));
        command.setHostInfoDAO(mock(HostInfoDAO.class));
        try {
            command.run(new TestCommandContextFactory().createContext(args));
            fail("should have received CommandException");
        } catch (CommandException e) {
            assertThat(e.getMessage(), containsString("VmInfoDAO is unavailable"));
        }
    }

    @Test
    public void testCommandFailsWhenNoCriteriaSupplied() {
        FindVmCommand command = new FindVmCommand();
        command.setAgentInfoDAO(mock(AgentInfoDAO.class));
        command.setHostInfoDAO(mock(HostInfoDAO.class));
        command.setVmInfoDAO(mock(VmInfoDAO.class));
        try {
            command.run(new TestCommandContextFactory().createContext(args));
            fail("should have received CommandException");
        } catch (CommandException e) {
            assertThat(e.getMessage(), containsString("No filtering criteria were specified"));
        }
    }

    @Test
    public void testCommandSucceedsWithSingleHostCriterion() throws CommandException {
        when(args.hasArgument("hostname")).thenReturn(true);
        when(args.getArgument("hostname")).thenReturn("foo-host");

        AgentInformation agent = new AgentInformation("agent");

        VmInfo vmInfo = new VmInfo();
        vmInfo.setVmId("vm-id");
        vmInfo.setAgentId(agent.getAgentId());

        HostInfo hostInfo = new HostInfo();
        hostInfo.setHostname("foo-host");

        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(vmInfoDAO.getVmIds(any(AgentId.class))).thenReturn(Collections.singleton(new VmId(vmInfo.getVmId())));

        HostInfoDAO hostInfoDAO = mock(HostInfoDAO.class);
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);

        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        when(agentInfoDAO.getAliveAgents()).thenReturn(Collections.singletonList(agent));
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(agent);

        TestCommandContextFactory testCommandContextFactory = new TestCommandContextFactory();
        FindVmCommand command = new FindVmCommand();
        command.setVmInfoDAO(vmInfoDAO);
        command.setAgentInfoDAO(agentInfoDAO);
        command.setHostInfoDAO(hostInfoDAO);
        command.run(testCommandContextFactory.createContext(args));

        String output = testCommandContextFactory.getOutput();
        assertThat(output, containsString(vmInfo.getVmId()));
    }

    @Test
    public void testCommandSucceedsWithSingleVmCriterion() throws CommandException {
        when(args.hasArgument("username")).thenReturn(true);
        when(args.getArgument("username")).thenReturn("foo-user");

        AgentInformation agent = new AgentInformation("agent");

        VmInfo vmInfo = new VmInfo();
        vmInfo.setVmId("vm-id");
        vmInfo.setAgentId(agent.getAgentId());
        vmInfo.setUsername("foo-user");

        HostInfo hostInfo = new HostInfo();

        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);
        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(vmInfoDAO.getVmIds(any(AgentId.class))).thenReturn(Collections.singleton(new VmId(vmInfo.getVmId())));

        HostInfoDAO hostInfoDAO = mock(HostInfoDAO.class);
        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);

        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        when(agentInfoDAO.getAliveAgents()).thenReturn(Collections.singletonList(agent));
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(agent);

        TestCommandContextFactory testCommandContextFactory = new TestCommandContextFactory();
        FindVmCommand command = new FindVmCommand();
        command.setVmInfoDAO(vmInfoDAO);
        command.setAgentInfoDAO(agentInfoDAO);
        command.setHostInfoDAO(hostInfoDAO);
        command.run(testCommandContextFactory.createContext(args));

        String output = testCommandContextFactory.getOutput();
        assertThat(output, containsString(vmInfo.getVmId()));
    }

    @Test
    public void testGetAgentsToSearchWithoutAgentIdArg() {
        List<AgentInformation> list = Arrays.asList(mock(AgentInformation.class), mock(AgentInformation.class));
        FindVmCommand command = new FindVmCommand();
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        when(agentInfoDAO.getAliveAgents()).thenReturn(list);
        command.setAgentInfoDAO(agentInfoDAO);
        List<AgentInformation> agentsToSearch = command.getAgentsToSearch(args);
        assertThat(agentsToSearch, is(equalTo(list)));
    }

    @Test
    public void testGetAgentsToSearchWithAgentIdArg() {
        AgentInformation foo = new AgentInformation("foo");
        AgentInformation bar = new AgentInformation("bar");
        List<AgentInformation> list = Arrays.asList(foo, bar);
        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        when(agentInfoDAO.getAliveAgents()).thenReturn(list);
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(foo);
        FindVmCommand command = new FindVmCommand();
        command.setAgentInfoDAO(agentInfoDAO);
        when(args.hasArgument(Arguments.AGENT_ID_ARGUMENT)).thenReturn(true);
        when(args.getArgument(Arguments.AGENT_ID_ARGUMENT)).thenReturn(foo.getAgentId());
        List<AgentInformation> agentsToSearch = command.getAgentsToSearch(args);
        assertThat(agentsToSearch, is(equalTo(Collections.singletonList(foo))));
    }

    @Test
    public void testGetHostCriteria() {
        Map<String, String> empty = FindVmCommand.getHostCriteria(args);
        assertThat(empty, is(equalTo(Collections.<String, String>emptyMap())));

        when(args.hasArgument(HostCriterion.HOSTNAME.getCliSwitch())).thenReturn(true);
        when(args.getArgument(HostCriterion.HOSTNAME.getCliSwitch())).thenReturn("foo");
        when(args.hasArgument(HostCriterion.OS_KERNEL.getCliSwitch())).thenReturn(true);
        when(args.getArgument(HostCriterion.OS_KERNEL.getCliSwitch())).thenReturn("lin");
        when(args.hasArgument(VmCriterion.JAVA_HOME.getCliSwitch())).thenReturn(true);
        when(args.getArgument(VmCriterion.JAVA_HOME.getCliSwitch())).thenReturn("bar");
        Map<String, String> map1 = FindVmCommand.getHostCriteria(args);
        Map<String, String> expected1 = new HashMap<>();
        expected1.put(HostCriterion.HOSTNAME.getCliSwitch(), "foo");
        expected1.put(HostCriterion.OS_KERNEL.getCliSwitch(), "lin");
        assertThat(map1, is(equalTo(expected1)));
    }

    @Test
    public void testGetVmCriteria() {
        Map<String, String> empty = FindVmCommand.getVmCriteria(args);
        assertThat(empty, is(equalTo(Collections.<String, String>emptyMap())));

        when(args.hasArgument(HostCriterion.HOSTNAME.getCliSwitch())).thenReturn(true);
        when(args.getArgument(HostCriterion.HOSTNAME.getCliSwitch())).thenReturn("foo");
        when(args.hasArgument(VmCriterion.JAVA_HOME.getCliSwitch())).thenReturn(true);
        when(args.getArgument(VmCriterion.JAVA_HOME.getCliSwitch())).thenReturn("bar");
        when(args.hasArgument(VmCriterion.USERNAME.getCliSwitch())).thenReturn(true);
        when(args.getArgument(VmCriterion.USERNAME.getCliSwitch())).thenReturn("baz");
        Map<String, String> map1 = FindVmCommand.getVmCriteria(args);
        Map<String, String> expected1 = new HashMap<>();
        expected1.put(VmCriterion.JAVA_HOME.getCliSwitch(), "bar");
        expected1.put(VmCriterion.USERNAME.getCliSwitch(), "baz");
        assertThat(map1, is(equalTo(expected1)));
    }

    @Test
    public void testPerformSearch() {
        AgentInformation agentInfo1 = new AgentInformation("agentInfo1");
        AgentId agentId1 = new AgentId(agentInfo1.getAgentId());
        AgentInformation agentInfo2 = new AgentInformation("agentInfo2");
        AgentId agentId2 = new AgentId(agentInfo2.getAgentId());
        AgentInformation agentInfo3 = new AgentInformation("agentInfo3");
        AgentId agentId3 = new AgentId(agentInfo3.getAgentId());
        List<AgentInformation> agents = Arrays.asList(agentInfo1, agentInfo2, agentInfo3);

        HostInfo host1 = mock(HostInfo.class);
        when(host1.getHostname()).thenReturn("foo-host");
        HostInfo host2 = host1;
        HostInfo host3 = mock(HostInfo.class);
        when(host3.getHostname()).thenReturn("bar-host");

        VmId vmId1 = new VmId("vmId1");
        VmId vmId2 = new VmId("vmId2");
        VmId vmId3 = new VmId("vmId3");
        VmId vmId4 = new VmId("vmId4");
        Set<VmId> vmIds1 = new HashSet<>(Arrays.asList(vmId1, vmId2));
        Set<VmId> vmIds2 = Collections.singleton(vmId3);
        Set<VmId> vmIds3 = Collections.singleton(vmId4);

        VmInfo vm1 = mock(VmInfo.class);
        when(vm1.getVmId()).thenReturn(vmId1.get());
        when(vm1.getJavaVersion()).thenReturn("1.8");
        when(vm1.getMainClass()).thenReturn("foo-class");
        VmInfo vm2 = mock(VmInfo.class);
        when(vm2.getVmId()).thenReturn(vmId2.get());
        when(vm2.getJavaVersion()).thenReturn("1.8");
        when(vm2.getMainClass()).thenReturn("bar-class");
        VmInfo vm3 = mock(VmInfo.class);
        when(vm3.getVmId()).thenReturn(vmId3.get());
        when(vm3.getJavaVersion()).thenReturn("1.7");
        when(vm3.getMainClass()).thenReturn("baz-class");
        VmInfo vm4 = mock(VmInfo.class);
        when(vm4.getVmId()).thenReturn(vmId4.get());
        when(vm4.getJavaVersion()).thenReturn("1.6");
        when(vm4.getMainClass()).thenReturn("boz-class");

        Pair<HostInfo, VmInfo> pair1 = new Pair<>(host1, vm1);
        Pair<HostInfo, VmInfo> pair2 = new Pair<>(host1, vm2);
        Pair<HostInfo, VmInfo> pair3 = new Pair<>(host2, vm3);
        Pair<HostInfo, VmInfo> pair4 = new Pair<>(host3, vm4);

        FindVmCommand command = new FindVmCommand();

        AgentInfoDAO agentInfoDAO = mock(AgentInfoDAO.class);
        when(agentInfoDAO.getAliveAgents()).thenReturn(agents);
        when(agentInfoDAO.getAgentInformation(agentId1)).thenReturn(agentInfo1);
        when(agentInfoDAO.getAgentInformation(agentId2)).thenReturn(agentInfo2);
        when(agentInfoDAO.getAgentInformation(agentId3)).thenReturn(agentInfo3);
        command.setAgentInfoDAO(agentInfoDAO);

        HostInfoDAO hostInfoDAO = mock(HostInfoDAO.class);
        when(hostInfoDAO.getHostInfo(agentId1)).thenReturn(host1);
        when(hostInfoDAO.getHostInfo(agentId2)).thenReturn(host2);
        when(hostInfoDAO.getHostInfo(agentId3)).thenReturn(host3);
        command.setHostInfoDAO(hostInfoDAO);

        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);
        when(vmInfoDAO.getVmIds(agentId1)).thenReturn(vmIds1);
        when(vmInfoDAO.getVmIds(agentId2)).thenReturn(vmIds2);
        when(vmInfoDAO.getVmIds(agentId3)).thenReturn(vmIds3);
        when(vmInfoDAO.getVmInfo(vmId1)).thenReturn(vm1);
        when(vmInfoDAO.getVmInfo(vmId2)).thenReturn(vm2);
        when(vmInfoDAO.getVmInfo(vmId3)).thenReturn(vm3);
        when(vmInfoDAO.getVmInfo(vmId4)).thenReturn(vm4);
        command.setVmInfoDAO(vmInfoDAO);

        Map<String, String> hostMap1 = new HashMap<>();
        HostMatcher hostMatcher1 = new HostMatcher(hostMap1);
        Map<String, String> vmMap1 = new HashMap<>();
        VmMatcher vmMatcher1 = new VmMatcher(vmMap1);
        List<Pair<HostInfo, VmInfo>> result1 = command.performSearch(agents, hostMatcher1, vmMatcher1);
        assertThat(result1.size(), is(4));
        for (Pair<HostInfo, VmInfo> pair : Arrays.asList(pair1, pair2, pair3, pair4)) {
            assertThat(result1.contains(pair), is(true));
        }

        Map<String, String> hostMap2 = new HashMap<>();
        hostMap2.put("hostname", "nosuchhost");
        HostMatcher hostMatcher2 = new HostMatcher(hostMap2);
        Map<String, String> vmMap2 = new HashMap<>();
        vmMap2.put("javaversion", "1.8");
        VmMatcher vmMatcher2 = new VmMatcher(vmMap2);
        List<Pair<HostInfo, VmInfo>> result2 = command.performSearch(agents, hostMatcher2, vmMatcher2);
        assertThat(result2.size(), is(0));

        Map<String, String> hostMap3 = new HashMap<>();
        hostMap3.put("hostname", "foo-host");
        HostMatcher hostMatcher3 = new HostMatcher(hostMap3);
        Map<String, String> vmMap3 = new HashMap<>();
        VmMatcher vmMatcher3 = new VmMatcher(vmMap3);
        List<Pair<HostInfo, VmInfo>> result3 = command.performSearch(agents, hostMatcher3, vmMatcher3);
        assertThat(result3.size(), is(3));
        for (Pair<HostInfo, VmInfo> pair : Arrays.asList(pair1, pair2, pair3)) {
            assertThat(result3.contains(pair), is(true));
        }

        Map<String, String> hostMap4 = new HashMap<>();
        hostMap3.put("hostname", "foo-host");
        HostMatcher hostMatcher4 = new HostMatcher(hostMap4);
        Map<String, String> vmMap4 = new HashMap<>();
        vmMap4.put("javaversion", "1.8");
        VmMatcher vmMatcher4 = new VmMatcher(vmMap4);
        List<Pair<HostInfo, VmInfo>> result4 = command.performSearch(agents, hostMatcher4, vmMatcher4);
        assertThat(result4.size(), is(2));
        for (Pair<HostInfo, VmInfo> pair : Arrays.asList(pair1, pair2)) {
            assertThat(result4.contains(pair), is(true));
        }
    }

    @Test
    public void testFilterAgents() {
        AgentInformation foo = new AgentInformation("foo");
        AgentId fooId = new AgentId(foo.getAgentId());
        AgentInformation bar = new AgentInformation("bar");
        AgentId barId = new AgentId(bar.getAgentId());
        HostInfo hostInfo1 = mock(HostInfo.class);
        when(hostInfo1.getHostname()).thenReturn("foo-host");
        HostInfo hostInfo2 = mock(HostInfo.class);
        when(hostInfo2.getHostname()).thenReturn("bar-host");
        HostInfoDAO hostInfoDAO = mock(HostInfoDAO.class);
        when(hostInfoDAO.getHostInfo(fooId)).thenReturn(hostInfo1);
        when(hostInfoDAO.getHostInfo(barId)).thenReturn(hostInfo2);
        List<AgentInformation> agents = Arrays.asList(foo, bar);

        FindVmCommand command = new FindVmCommand();
        command.setHostInfoDAO(hostInfoDAO);

        HostMatcher allMatcher = new HostMatcher(Collections.<String, String>emptyMap());
        List<AgentInformation> all = command.filterAgents(agents, allMatcher);
        assertThat(all, is(equalTo(agents)));

        Map<String, String> noneMap = new HashMap<>();
        noneMap.put("hostname", "none-host");
        HostMatcher noneMatcher = new HostMatcher(noneMap);
        List<AgentInformation> none = command.filterAgents(agents, noneMatcher);
        assertThat(none, is(equalTo(Collections.<AgentInformation>emptyList())));

        Map<String, String> fooMap = new HashMap<>();
        fooMap.put("hostname", "foo-host");
        HostMatcher fooMatcher = new HostMatcher(fooMap);
        List<AgentInformation> fooList = command.filterAgents(agents, fooMatcher);
        assertThat(fooList, is(equalTo(Collections.singletonList(foo))));
    }

    @Test
    public void testGetMatchingVms() {
        AgentInformation agent = new AgentInformation("agent");
        AgentId agentId = new AgentId(agent.getAgentId());

        VmId vmId1 = new VmId("vm1");
        VmInfo vmInfo1 = new VmInfo();
        vmInfo1.setAgentId(agent.getAgentId());
        vmInfo1.setVmId(vmId1.get());
        vmInfo1.setUsername("foo-user");
        vmInfo1.setJavaVersion("1.8");

        VmId vmId2 = new VmId("vm2");
        VmInfo vmInfo2 = new VmInfo();
        vmInfo2.setAgentId(agent.getAgentId());
        vmInfo2.setVmId(vmId2.get());
        vmInfo2.setUsername("bar-user");
        vmInfo2.setJavaVersion("1.8");

        VmId vmId3 = new VmId("vm3");
        VmInfo vmInfo3 = new VmInfo();
        vmInfo3.setAgentId(agent.getAgentId());
        vmInfo3.setVmId(vmId3.get());
        vmInfo3.setUsername("baz-user");
        vmInfo3.setJavaVersion("1.7");

        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);
        when(vmInfoDAO.getVmIds(agentId)).thenReturn(new HashSet<>(Arrays.asList(vmId1, vmId2, vmId3)));
        when(vmInfoDAO.getVmInfo(vmId1)).thenReturn(vmInfo1);
        when(vmInfoDAO.getVmInfo(vmId2)).thenReturn(vmInfo2);
        when(vmInfoDAO.getVmInfo(vmId3)).thenReturn(vmInfo3);

        FindVmCommand command = new FindVmCommand();
        command.setVmInfoDAO(vmInfoDAO);

        VmMatcher allMatcher = new VmMatcher(Collections.<String, String>emptyMap());
        List<VmInfo> all = command.getMatchingVms(agent, allMatcher);
        assertThat(all, is(equalTo(Arrays.asList(vmInfo1, vmInfo2, vmInfo3))));

        Map<String, String> userMap = new HashMap<>();
        userMap.put("username", "foo-user");
        VmMatcher userMatcher = new VmMatcher(userMap);
        List<VmInfo> users = command.getMatchingVms(agent, userMatcher);
        assertThat(users, is(equalTo(Collections.singletonList(vmInfo1))));

        Map<String, String> versionMap = new HashMap<>();
        versionMap.put("javaversion", "1.8");
        VmMatcher versionMatcher = new VmMatcher(versionMap);
        List<VmInfo> versions = command.getMatchingVms(agent, versionMatcher);
        assertThat(versions, is(equalTo(Arrays.asList(vmInfo1, vmInfo2))));

        Map<String, String> noneMap = new HashMap<>();
        noneMap.put("javaversion", "1.0");
        VmMatcher noneMatcher = new VmMatcher(noneMap);
        List<VmInfo> none = command.getMatchingVms(agent, noneMatcher);
        assertThat(none, is(equalTo(Collections.<VmInfo>emptyList())));
    }

}
