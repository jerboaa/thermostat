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

package com.redhat.thermostat.vm.find.command.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.client.cli.AgentArgument;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;

public class FindVmCommand extends AbstractCommand {

    static final String REGISTER_NAME = "find-vm";

    private static final Translate<LocaleResources> translator = LocaleResources.createTranslator();
    static final String ALIVE_AGENTS_ONLY_ARGUMENT = "alive-agents-only";

    private DependencyServices services = new DependencyServices();
    private AgentInfoDAO agentInfoDAO;
    private HostInfoDAO hostInfoDAO;
    private VmInfoDAO vmInfoDAO;

    private final Map<VmId, VmInfo> vmInfoMap = new HashMap<>();
    private final Map<AgentId, List<VmInfo>> agentVmMap = new HashMap<>();
    private final Map<AgentId, HostInfo> hostInfoMap = new HashMap<>();

    @Override
    public void run(CommandContext ctx) throws CommandException {
        setServices();
        initDaoData();

        Arguments arguments = ctx.getArguments();
        List<AgentInformation> agentsToSearch = getAgentsToSearch(arguments);

        Map<String, String> hostCriteria = getHostCriteria(arguments);
        Map<String, String> vmCriteria = getVmCriteria(arguments);
        assertCriteriaGiven(hostCriteria, vmCriteria);

        HostMatcher hostMatcher = new HostMatcher(hostCriteria);
        VmMatcher vmMatcher = new VmMatcher(vmCriteria);

        List<MatchContext> results = performSearch(agentsToSearch, hostMatcher, vmMatcher);

        ResultsRenderer resultsRenderer = new ResultsRenderer(arguments);
        resultsRenderer.print(ctx.getConsole().getOutput(), results);
    }

    void setServices() throws CommandException {
        agentInfoDAO = services.getService(AgentInfoDAO.class);
        requireNonNull(agentInfoDAO, translator.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE));
        hostInfoDAO = services.getService(HostInfoDAO.class);
        requireNonNull(hostInfoDAO, translator.localize(LocaleResources.HOST_SERVICE_UNAVAILABLE));
        vmInfoDAO = services.getService(VmInfoDAO.class);
        requireNonNull(vmInfoDAO, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
    }

    void initDaoData() {
        vmInfoMap.clear();
        agentVmMap.clear();
        hostInfoMap.clear();

        List<VmInfo> allVms = vmInfoDAO.getAllVmInfos();
        List<HostInfo> allHosts = hostInfoDAO.getAllHostInfos();

        for (VmInfo vm : allVms) {
            vmInfoMap.put(new VmId(vm.getVmId()), vm);
            AgentId agentId = new AgentId(vm.getAgentId());
            if (!agentVmMap.containsKey(agentId)) {
                agentVmMap.put(agentId, new ArrayList<VmInfo>());
            }
            agentVmMap.get(agentId).add(vm);
        }

        for (HostInfo host : allHosts) {
            hostInfoMap.put(new AgentId(host.getAgentId()), host);
        }
    }

    List<AgentInformation> getAgentsToSearch(Arguments arguments) throws CommandException {
        validateAgentStatusArguments(arguments);
        List<AgentInformation> aliveAgents;
        AgentArgument agentArgument = AgentArgument.optional(arguments);
        AgentId agentId = agentArgument.getAgentId();

        if (agentId != null) {
            aliveAgents = Collections.singletonList(agentInfoDAO.getAgentInformation(agentId));
        } else if (arguments.hasArgument(ALIVE_AGENTS_ONLY_ARGUMENT)) {
            aliveAgents = agentInfoDAO.getAliveAgents();
        } else {
            aliveAgents = agentInfoDAO.getAllAgentInformation();
        }
        return aliveAgents;
    }

    static void validateAgentStatusArguments(Arguments arguments) throws CommandException {
        boolean hasAgentIdArgument = arguments.hasArgument(AgentArgument.ARGUMENT_NAME);
        boolean hasAliveAgentsOnlyArgument = arguments.hasArgument(ALIVE_AGENTS_ONLY_ARGUMENT);
        if (hasAgentIdArgument && hasAliveAgentsOnlyArgument) {
            throw new CommandException(translator.localize(LocaleResources.AGENT_FLAGS_CLASH, AgentArgument.ARGUMENT_NAME, ALIVE_AGENTS_ONLY_ARGUMENT));
        }
    }

    static Map<String, String> getHostCriteria(Arguments arguments) {
        Map<String, String> hostCriteria = new HashMap<>();
        for (HostCriterion criterion : HostCriterion.values()) {
            if (arguments.hasArgument(criterion.getCliSwitch())) {
                hostCriteria.put(criterion.getCliSwitch(), arguments.getArgument(criterion.getCliSwitch()));
            }
        }
        return hostCriteria;
    }

    static Map<String, String> getVmCriteria(Arguments arguments) {
        Map<String, String> vmCriteria = new HashMap<>();
        for (VmCriterion criterion : VmCriterion.values()) {
            if (arguments.hasArgument(criterion.getCliSwitch())) {
                vmCriteria.put(criterion.getCliSwitch(), arguments.getArgument(criterion.getCliSwitch()));
            }
        }
        return vmCriteria;
    }

    static void assertCriteriaGiven(Map<String, String> hostCriteria, Map<String, String> vmCriteria) throws CommandException {
        if (hostCriteria.isEmpty() && vmCriteria.isEmpty()) {
            throw new CommandException(translator.localize(LocaleResources.NO_CRITERIA_GIVEN));
        }
    }

    List<MatchContext> performSearch(Iterable<AgentInformation> agents, HostMatcher hostMatcher, VmMatcher vmMatcher) throws UnrecognizedArgumentException {
        List<MatchContext> matchContexts = new ArrayList<>();
        for (AgentInformation agentInformation : filterAgents(agents, hostMatcher)) {
            HostInfo hostInfo = getHostInfo(agentInformation.getAgentId());
            List<VmInfo> matchingVms = getMatchingVms(agentInformation, hostInfo, vmMatcher);
            for (VmInfo vm : matchingVms) {
                MatchContext context = MatchContext.builder()
                        .agentInfo(agentInformation)
                        .hostInfo(hostInfo)
                        .vmInfo(vm)
                        .build();
                matchContexts.add(context);
            }
        }
        return matchContexts;
    }

    List<AgentInformation> filterAgents(Iterable<AgentInformation> agents, HostMatcher hostMatcher) throws UnrecognizedArgumentException {
        List<AgentInformation> list = new ArrayList<>();
        for (AgentInformation agent : agents) {
            HostInfo hostInfo = getHostInfo(agent.getAgentId());
            MatchContext context = MatchContext.builder()
                    .agentInfo(agent)
                    .hostInfo(hostInfo)
                    .build();
            if (hostMatcher.match(context)) {
                list.add(agent);
            }
        }
        return list;
    }

    HostInfo getHostInfo(String agentId) {
        return getHostInfo(new AgentId(agentId));
    }

    HostInfo getHostInfo(AgentId agentId) {
        return hostInfoMap.get(agentId);
    }

    List<VmInfo> getMatchingVms(AgentInformation agent, HostInfo hostInfo, VmMatcher vmMatcher) throws UnrecognizedArgumentException {
        List<VmInfo> list = new ArrayList<>();
        for (VmInfo vmInfo : getVmInfos(agent.getAgentId())) {
            MatchContext context = MatchContext.builder()
                    .agentInfo(agent)
                    .hostInfo(hostInfo)
                    .vmInfo(vmInfo)
                    .build();
            if (vmMatcher.match(context)) {
                list.add(vmInfo);
            }
        }
        return list;
    }

    List<VmInfo> getVmInfos(String agentId) {
        return getVmInfos(new AgentId(agentId));
    }

    List<VmInfo> getVmInfos(AgentId agentId) {
        return agentVmMap.get(agentId);
    }

    public void setAgentInfoDAO(AgentInfoDAO agentInfoDAO) {
        services.addService(AgentInfoDAO.class, agentInfoDAO);
    }

    public void setHostInfoDAO(HostInfoDAO hostInfoDAO) {
        services.addService(HostInfoDAO.class, hostInfoDAO);
    }

    public void setVmInfoDAO(VmInfoDAO vmInfoDAO) {
        services.addService(VmInfoDAO.class, vmInfoDAO);
    }

    public void servicesUnavailable() {
        services.removeService(AgentInfoDAO.class);
        services.removeService(HostInfoDAO.class);
        services.removeService(VmInfoDAO.class);
    }
}
