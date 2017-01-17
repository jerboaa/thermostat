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

package com.redhat.thermostat.vm.find.command.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.client.cli.AgentArgument;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.internal.test.TestCommandContextFactory;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;

public class FindVmCommandTest {

    private Arguments args;
    private VmInfoDAO vmInfoDAO;
    private HostInfoDAO hostInfoDAO;
    private AgentInfoDAO agentInfoDAO;
    private FindVmCommand command;

    @Before
    public void setup() {
        args = mock(Arguments.class);
        vmInfoDAO = mock(VmInfoDAO.class);
        when(vmInfoDAO.getAllVmInfos()).thenReturn(Collections.<VmInfo>emptyList());
        hostInfoDAO = mock(HostInfoDAO.class);
        when(hostInfoDAO.getAllHostInfos()).thenReturn(Collections.<HostInfo>emptyList());
        agentInfoDAO = mock(AgentInfoDAO.class);
        command = new FindVmCommand();
        command.setHostInfoDAO(hostInfoDAO);
        command.setVmInfoDAO(vmInfoDAO);
        command.setAgentInfoDAO(agentInfoDAO);
    }

    @Test
    public void testCommandFailsWhenDaosUnavailable() {
        command.servicesUnavailable();
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
        command.servicesUnavailable();
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
        command.servicesUnavailable();
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
        command.servicesUnavailable();
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
        hostInfo.setAgentId(agent.getAgentId());
        hostInfo.setHostname("foo-host");

        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(vmInfoDAO.getAllVmInfos()).thenReturn(Collections.singletonList(vmInfo));
        when(vmInfoDAO.getVmIds(any(AgentId.class))).thenReturn(Collections.singleton(new VmId(vmInfo.getVmId())));

        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        when(hostInfoDAO.getAllHostInfos()).thenReturn(Collections.singletonList(hostInfo));

        when(agentInfoDAO.getAllAgentInformation()).thenReturn(Collections.singletonList(agent));
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(agent);

        TestCommandContextFactory testCommandContextFactory = new TestCommandContextFactory();
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
        hostInfo.setAgentId(agent.getAgentId());
        hostInfo.setHostname("foo-host");

        when(vmInfoDAO.getVmInfo(any(VmId.class))).thenReturn(vmInfo);
        when(vmInfoDAO.getAllVmInfos()).thenReturn(Collections.singletonList(vmInfo));
        when(vmInfoDAO.getVmIds(any(AgentId.class))).thenReturn(Collections.singleton(new VmId(vmInfo.getVmId())));

        when(hostInfoDAO.getHostInfo(any(AgentId.class))).thenReturn(hostInfo);
        when(hostInfoDAO.getAllHostInfos()).thenReturn(Collections.singletonList(hostInfo));

        when(agentInfoDAO.getAllAgentInformation()).thenReturn(Collections.singletonList(agent));
        when(agentInfoDAO.getAgentInformation(any(AgentId.class))).thenReturn(agent);

        TestCommandContextFactory testCommandContextFactory = new TestCommandContextFactory();
        command.run(testCommandContextFactory.createContext(args));

        String output = testCommandContextFactory.getOutput();
        assertThat(output, containsString(vmInfo.getVmId()));
    }

    @Test
    public void testGetAgentsToSearchWithoutAgentArgs() throws CommandException {
        List<AgentInformation> list = Arrays.asList(mock(AgentInformation.class), mock(AgentInformation.class));
        when(agentInfoDAO.getAllAgentInformation()).thenReturn(list);
        command.setServices();
        command.initDaoData();
        List<AgentInformation> agentsToSearch = command.getAgentsToSearch(args);
        assertThat(agentsToSearch, is(equalTo(list)));
    }

    @Test
    public void testGetAgentsToSearchWithAgentIdArg() throws CommandException {
        AgentInformation foo = new AgentInformation("foo");
        foo.setAlive(false);
        AgentInformation bar = new AgentInformation("bar");
        bar.setAlive(true);
        List<AgentInformation> list = Arrays.asList(foo, bar);
        when(agentInfoDAO.getAllAgentInformation()).thenReturn(list);
        when(agentInfoDAO.getAgentInformation(new AgentId("foo"))).thenReturn(foo);
        when(agentInfoDAO.getAgentInformation(new AgentId("bar"))).thenReturn(bar);
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.getArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(foo.getAgentId());
        command.setServices();
        command.initDaoData();
        List<AgentInformation> agentsToSearch = command.getAgentsToSearch(args);
        assertThat(agentsToSearch, is(equalTo(Collections.singletonList(foo))));
    }

    @Test
    public void testGetAgentsToSearchWithAliveAgentsOnlyArg() throws CommandException {
        AgentInformation foo = new AgentInformation("foo");
        foo.setAlive(false);
        AgentInformation bar = new AgentInformation("bar");
        bar.setAlive(true);
        when(agentInfoDAO.getAliveAgents()).thenReturn(Collections.singletonList(bar));
        when(agentInfoDAO.getAgentInformation(new AgentId("foo"))).thenReturn(foo);
        when(agentInfoDAO.getAgentInformation(new AgentId("bar"))).thenReturn(bar);
        when(args.hasArgument(FindVmCommand.ALIVE_AGENTS_ONLY_ARGUMENT)).thenReturn(true);
        command.setServices();
        command.initDaoData();
        List<AgentInformation> agentsToSearch = command.getAgentsToSearch(args);
        assertThat(agentsToSearch, is(equalTo(Collections.singletonList(bar))));
    }

    @Test(expected = CommandException.class)
    public void testBothAgentIdAndAliveAgentsOnlyArgs() throws CommandException {
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.hasArgument(FindVmCommand.ALIVE_AGENTS_ONLY_ARGUMENT)).thenReturn(true);

        TestCommandContextFactory testCommandContextFactory = new TestCommandContextFactory();
        command.run(testCommandContextFactory.createContext(args));
    }

    @Test(expected = CommandException.class)
    public void testValidateAgentStatusArgumentsThrowsExceptionWhenBothFlagsGiven() throws CommandException {
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.hasArgument(FindVmCommand.ALIVE_AGENTS_ONLY_ARGUMENT)).thenReturn(true);
        FindVmCommand.validateAgentStatusArguments(args);
    }

    @Test
    public void testValidateAgentStatusArgumentsShouldNotThrowExceptionOnValidInput() {
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(false);
        when(args.hasArgument(FindVmCommand.ALIVE_AGENTS_ONLY_ARGUMENT)).thenReturn(true);
        try {
            FindVmCommand.validateAgentStatusArguments(args);
        } catch (CommandException ce) {
            fail("Exception should not be thrown when only " + FindVmCommand.ALIVE_AGENTS_ONLY_ARGUMENT + " is given. Exception: " + ce.getLocalizedMessage());
        }
    }

    @Test
    public void testValidateAgentStatusArgumentsShouldNotThrowExceptionOnValidInput2() {
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(true);
        when(args.hasArgument(FindVmCommand.ALIVE_AGENTS_ONLY_ARGUMENT)).thenReturn(false);
        try {
            FindVmCommand.validateAgentStatusArguments(args);
        } catch (CommandException ce) {
            fail("Exception should not be thrown when only " + AgentArgument.ARGUMENT_NAME + " is given. Exception: " + ce.getLocalizedMessage());
        }
    }

    @Test
    public void testValidateAgentStatusArgumentsShouldNotThrowExceptionOnValidInput3() {
        when(args.hasArgument(AgentArgument.ARGUMENT_NAME)).thenReturn(false);
        when(args.hasArgument(FindVmCommand.ALIVE_AGENTS_ONLY_ARGUMENT)).thenReturn(false);
        try {
            FindVmCommand.validateAgentStatusArguments(args);
        } catch (CommandException ce) {
            fail("Exception should not be thrown when neither agent flag is given. Exception: " + ce.getLocalizedMessage());
        }
    }

    @Test(expected = CommandException.class)
    public void testAssertCriteriaGivenThrowsExceptionWhenInputsEmpty() throws CommandException {
        Map<String, String> hostCriteria = new HashMap<>();
        Map<String, String> vmCriteria = new HashMap<>();
        FindVmCommand.assertCriteriaGiven(hostCriteria, vmCriteria);
    }

    @Test
    public void testAssertCriteriaGivenShouldNotThrowExceptionOnValidInput() {
        Map<String, String> hostCriteria = new HashMap<>();
        hostCriteria.put("foo", "bar");
        Map<String, String> vmCriteria = new HashMap<>();
        try {
            FindVmCommand.assertCriteriaGiven(hostCriteria, vmCriteria);
        } catch (CommandException ce) {
            fail("Exception should not be thrown when host criteria are given. Exception: " + ce.getLocalizedMessage());
        }
    }

    @Test
    public void testAssertCriteriaGivenShouldNotThrowExceptionOnValidInput2() {
        Map<String, String> hostCriteria = new HashMap<>();
        Map<String, String> vmCriteria = new HashMap<>();
        vmCriteria.put("foo", "bar");
        try {
            FindVmCommand.assertCriteriaGiven(hostCriteria, vmCriteria);
        } catch (CommandException ce) {
            fail("Exception should not be thrown when vm criteria are given. Exception: " + ce.getLocalizedMessage());
        }
    }

    @Test
    public void testAssertCriteriaGivenShouldNotThrowExceptionOnValidInput3() {
        Map<String, String> hostCriteria = new HashMap<>();
        hostCriteria.put("foo", "bar");
        Map<String, String> vmCriteria = new HashMap<>();
        vmCriteria.put("foo2", "bar2");
        try {
            FindVmCommand.assertCriteriaGiven(hostCriteria, vmCriteria);
        } catch (CommandException ce) {
            fail("Exception should not be thrown when host and vm criteria are given. Exception: " + ce.getLocalizedMessage());
        }
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
    public void testPerformSearch() throws CommandException {
        AgentInformation agentInfo1 = new AgentInformation("agentInfo1");
        AgentId agentId1 = new AgentId(agentInfo1.getAgentId());
        AgentInformation agentInfo2 = new AgentInformation("agentInfo2");
        AgentId agentId2 = new AgentId(agentInfo2.getAgentId());
        AgentInformation agentInfo3 = new AgentInformation("agentInfo3");
        AgentId agentId3 = new AgentId(agentInfo3.getAgentId());
        List<AgentInformation> agents = Arrays.asList(agentInfo1, agentInfo2, agentInfo3);

        HostInfo host1 = new HostInfo();
        host1.setHostname("foo-host");
        host1.setAgentId(agentId1.get());
        HostInfo host2 = new HostInfo();
        host2.setHostname("foo-host");
        host2.setAgentId(agentId2.get());
        HostInfo host3 = new HostInfo();
        host3.setHostname("baz-host");
        host3.setAgentId(agentId3.get());

        VmId vmId1 = new VmId("vmId1");
        VmId vmId2 = new VmId("vmId2");
        VmId vmId3 = new VmId("vmId3");
        VmId vmId4 = new VmId("vmId4");
        Set<VmId> vmIds1 = new HashSet<>(Arrays.asList(vmId1, vmId2));
        Set<VmId> vmIds2 = Collections.singleton(vmId3);
        Set<VmId> vmIds3 = Collections.singleton(vmId4);

        VmInfo vm1 = new VmInfo();
        vm1.setAgentId(agentId1.get());
        vm1.setVmId(vmId1.get());
        vm1.setJavaVersion("1.8");
        vm1.setMainClass("foo-class");
        VmInfo vm2 = new VmInfo();
        vm2.setAgentId(agentId1.get());
        vm2.setVmId(vmId2.get());
        vm2.setJavaVersion("1.8");
        vm2.setMainClass("bar-class");
        VmInfo vm3 = new VmInfo();
        vm3.setAgentId(agentId2.get());
        vm3.setVmId(vmId3.get());
        vm3.setJavaVersion("1.7");
        vm3.setMainClass("baz-class");
        VmInfo vm4 = new VmInfo();
        vm4.setAgentId(agentId3.get());
        vm4.setVmId(vmId4.get());
        vm4.setJavaVersion("1.6");
        vm4.setMainClass("boz-class");

        MatchContext context1 = MatchContext.builder()
                .hostInfo(host1)
                .agentInfo(agentInfo1)
                .vmInfo(vm1)
                .build();
        MatchContext context2 = MatchContext.builder()
                .hostInfo(host1)
                .agentInfo(agentInfo1)
                .vmInfo(vm2)
                .build();
        MatchContext context3 = MatchContext.builder()
                .hostInfo(host2)
                .agentInfo(agentInfo2)
                .vmInfo(vm3)
                .build();
        MatchContext context4 = MatchContext.builder()
                .hostInfo(host3)
                .agentInfo(agentInfo3)
                .vmInfo(vm4)
                .build();

        when(agentInfoDAO.getAliveAgents()).thenReturn(agents);
        when(agentInfoDAO.getAllAgentInformation()).thenReturn(agents);
        when(agentInfoDAO.getAgentInformation(agentId1)).thenReturn(agentInfo1);
        when(agentInfoDAO.getAgentInformation(agentId2)).thenReturn(agentInfo2);
        when(agentInfoDAO.getAgentInformation(agentId3)).thenReturn(agentInfo3);

        when(hostInfoDAO.getHostInfo(agentId1)).thenReturn(host1);
        when(hostInfoDAO.getHostInfo(agentId2)).thenReturn(host2);
        when(hostInfoDAO.getHostInfo(agentId3)).thenReturn(host3);
        when(hostInfoDAO.getAllHostInfos()).thenReturn(Arrays.asList(host1, host2, host3));

        when(vmInfoDAO.getVmIds(agentId1)).thenReturn(vmIds1);
        when(vmInfoDAO.getVmIds(agentId2)).thenReturn(vmIds2);
        when(vmInfoDAO.getVmIds(agentId3)).thenReturn(vmIds3);
        when(vmInfoDAO.getVmInfo(vmId1)).thenReturn(vm1);
        when(vmInfoDAO.getVmInfo(vmId2)).thenReturn(vm2);
        when(vmInfoDAO.getVmInfo(vmId3)).thenReturn(vm3);
        when(vmInfoDAO.getVmInfo(vmId4)).thenReturn(vm4);
        when(vmInfoDAO.getAllVmInfos()).thenReturn(Arrays.asList(vm1, vm2, vm3, vm4));

        Map<String, String> hostMap1 = new HashMap<>();
        HostMatcher hostMatcher1 = new HostMatcher(hostMap1);
        Map<String, String> vmMap1 = new HashMap<>();
        VmMatcher vmMatcher1 = new VmMatcher(vmMap1);
        command.setServices();
        command.initDaoData();
        List<MatchContext> result1 = command.performSearch(agents, hostMatcher1, vmMatcher1);
        assertThat(new HashSet<>(result1), is(equalTo(new HashSet<>(Arrays.asList(context1, context2, context3, context4)))));

        Map<String, String> hostMap2 = new HashMap<>();
        hostMap2.put("hostname", "nosuchhost");
        HostMatcher hostMatcher2 = new HostMatcher(hostMap2);
        Map<String, String> vmMap2 = new HashMap<>();
        vmMap2.put("javaversion", "1.8");
        VmMatcher vmMatcher2 = new VmMatcher(vmMap2);
        List<MatchContext> result2 = command.performSearch(agents, hostMatcher2, vmMatcher2);
        assertThat(result2, is(equalTo(Collections.<MatchContext>emptyList())));

        Map<String, String> hostMap3 = new HashMap<>();
        hostMap3.put("hostname", "foo-host");
        HostMatcher hostMatcher3 = new HostMatcher(hostMap3);
        Map<String, String> vmMap3 = new HashMap<>();
        VmMatcher vmMatcher3 = new VmMatcher(vmMap3);
        List<MatchContext> result3 = command.performSearch(agents, hostMatcher3, vmMatcher3);
        assertThat(new HashSet<>(result3), is(equalTo(new HashSet<>(Arrays.asList(context1, context2, context3)))));

        Map<String, String> hostMap4 = new HashMap<>();
        hostMap3.put("hostname", "foo-host");
        HostMatcher hostMatcher4 = new HostMatcher(hostMap4);
        Map<String, String> vmMap4 = new HashMap<>();
        vmMap4.put("javaversion", "1.8");
        VmMatcher vmMatcher4 = new VmMatcher(vmMap4);
        List<MatchContext> result4 = command.performSearch(agents, hostMatcher4, vmMatcher4);
        assertThat(new HashSet<>(result4), is(equalTo(new HashSet<>(Arrays.asList(context1, context2)))));
    }

    @Test
    public void testFilterAgents() throws CommandException {
        AgentInformation foo = new AgentInformation("foo");
        AgentId fooId = new AgentId(foo.getAgentId());
        AgentInformation bar = new AgentInformation("bar");
        AgentId barId = new AgentId(bar.getAgentId());

        HostInfo hostInfo1 = new HostInfo();
        hostInfo1.setHostname("foo-host");
        hostInfo1.setAgentId(fooId.get());

        HostInfo hostInfo2 = new HostInfo();
        hostInfo2.setHostname("bar-host");
        hostInfo2.setAgentId(barId.get());

        when(hostInfoDAO.getHostInfo(fooId)).thenReturn(hostInfo1);
        when(hostInfoDAO.getHostInfo(barId)).thenReturn(hostInfo2);
        when(hostInfoDAO.getAllHostInfos()).thenReturn(Arrays.asList(hostInfo1, hostInfo2));
        List<AgentInformation> agents = Arrays.asList(foo, bar);

        HostMatcher allMatcher = new HostMatcher(Collections.<String, String>emptyMap());
        command.setServices();
        command.initDaoData();
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
    public void testGetMatchingVms() throws CommandException {
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

        HostInfo hostInfo = new HostInfo();
        hostInfo.setAgentId(agentId.get());
        hostInfo.setHostname("foo-host");
        hostInfo.setOsKernel("linux");
        hostInfo.setAgentId("foo-agent");
        hostInfo.setOsName("foo-linux");
        hostInfo.setCpuCount(2);
        hostInfo.setCpuModel("foo-cpu");
        hostInfo.setTotalMemory(4096l);

        when(vmInfoDAO.getVmIds(agentId)).thenReturn(new HashSet<>(Arrays.asList(vmId1, vmId2, vmId3)));
        when(vmInfoDAO.getVmInfo(vmId1)).thenReturn(vmInfo1);
        when(vmInfoDAO.getVmInfo(vmId2)).thenReturn(vmInfo2);
        when(vmInfoDAO.getVmInfo(vmId3)).thenReturn(vmInfo3);
        when(vmInfoDAO.getAllVmInfos()).thenReturn(Arrays.asList(vmInfo1, vmInfo2, vmInfo3));

        when(hostInfoDAO.getAllHostInfos()).thenReturn(Collections.singletonList(hostInfo));

        when(agentInfoDAO.getAllAgentInformation()).thenReturn(Collections.singletonList(agent));

        VmMatcher allMatcher = new VmMatcher(Collections.<String, String>emptyMap());
        command.setServices();
        command.initDaoData();
        List<VmInfo> all = command.getMatchingVms(agent, hostInfo, allMatcher);
        assertThat(all, is(equalTo(Arrays.asList(vmInfo1, vmInfo2, vmInfo3))));

        Map<String, String> userMap = new HashMap<>();
        userMap.put("username", "foo-user");
        VmMatcher userMatcher = new VmMatcher(userMap);
        List<VmInfo> users = command.getMatchingVms(agent,hostInfo, userMatcher);
        assertThat(users, is(equalTo(Collections.singletonList(vmInfo1))));

        Map<String, String> versionMap = new HashMap<>();
        versionMap.put("javaversion", "1.8");
        VmMatcher versionMatcher = new VmMatcher(versionMap);
        List<VmInfo> versions = command.getMatchingVms(agent, hostInfo, versionMatcher);
        assertThat(versions, is(equalTo(Arrays.asList(vmInfo1, vmInfo2))));

        Map<String, String> noneMap = new HashMap<>();
        noneMap.put("javaversion", "1.0");
        VmMatcher noneMatcher = new VmMatcher(noneMap);
        List<VmInfo> none = command.getMatchingVms(agent, hostInfo, noneMatcher);
        assertThat(none, is(equalTo(Collections.<VmInfo>emptyList())));
    }

}
