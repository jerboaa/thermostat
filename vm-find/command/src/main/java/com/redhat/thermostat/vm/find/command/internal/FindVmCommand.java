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
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.HostInfo;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.vm.find.command.locale.LocaleResources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FindVmCommand extends AbstractCommand {

    static final String REGISTER_NAME = "find-vm";

    private static final Translate<LocaleResources> translator = LocaleResources.createTranslator();

    private AgentInfoDAO agentInfoDAO;
    private HostInfoDAO hostInfoDAO;
    private VmInfoDAO vmInfoDAO;
    private CountDownLatch servicesLatch = new CountDownLatch(3);
    
    @Override
    public void run(CommandContext ctx) throws CommandException {
        try {
            servicesLatch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new CommandException(translator.localize(LocaleResources.COMMAND_INTERRUPTED));
        }

        requireNonNull(agentInfoDAO, translator.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE));
        requireNonNull(hostInfoDAO, translator.localize(LocaleResources.HOST_SERVICE_UNAVAILABLE));
        requireNonNull(vmInfoDAO, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));

        List<AgentInformation> agentsToSearch = getAgentsToSearch(ctx.getArguments());

        Map<String, String> hostCriteria = getHostCriteria(ctx.getArguments());
        Map<String, String> vmCriteria = getVmCriteria(ctx.getArguments());

        if (hostCriteria.isEmpty() && vmCriteria.isEmpty()) {
            throw new CommandException(translator.localize(LocaleResources.NO_CRITERIA_GIVEN));
        }

        HostMatcher hostMatcher = new HostMatcher(hostCriteria);
        VmMatcher vmMatcher = new VmMatcher(vmCriteria);

        List<Pair<HostInfo, VmInfo>> results = performSearch(agentsToSearch, hostMatcher, vmMatcher);

        ResultsRenderer resultsRenderer = new ResultsRenderer(ctx.getArguments());
        resultsRenderer.print(ctx.getConsole().getOutput(), results);
    }

    List<AgentInformation> getAgentsToSearch(Arguments arguments) {
        List<AgentInformation> aliveAgents;
        if (arguments.hasArgument(Arguments.AGENT_ID_ARGUMENT)) {
            AgentId agentId = new AgentId(arguments.getArgument(Arguments.AGENT_ID_ARGUMENT));
            aliveAgents = Collections.singletonList(agentInfoDAO.getAgentInformation(agentId));
        } else {
            aliveAgents = agentInfoDAO.getAliveAgents();
        }
        return aliveAgents;
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

    List<Pair<HostInfo, VmInfo>> performSearch(Iterable<AgentInformation> agents, HostMatcher hostMatcher, VmMatcher vmMatcher) {
        List<Pair<HostInfo, VmInfo>> pairs = new ArrayList<>();
        for (AgentInformation agentInformation : filterAgents(agents, hostMatcher)) {
            HostInfo hostInfo = getHostInfo(agentInformation);
            List<VmInfo> matchingVms = getMatchingVms(agentInformation, vmMatcher);
            for (VmInfo vm : matchingVms) {
                pairs.add(new Pair<>(hostInfo, vm));
            }
        }
        return pairs;
    }

    List<AgentInformation> filterAgents(Iterable<AgentInformation> agents, HostMatcher hostMatcher) {
        List<AgentInformation> list = new ArrayList<>();
        for (AgentInformation agent : agents) {
            HostInfo hostInfo = hostInfoDAO.getHostInfo(new AgentId(agent.getAgentId()));
            if (hostMatcher.match(hostInfo)) {
                list.add(agent);
            }
        }
        return list;
    }

    HostInfo getHostInfo(AgentInformation agentInformation) {
        return hostInfoDAO.getHostInfo(new AgentId(agentInformation.getAgentId()));
    }

    List<VmInfo> getMatchingVms(AgentInformation agent, VmMatcher vmMatcher) {
        List<VmInfo> list = new ArrayList<>();
        for (VmId vmId : vmInfoDAO.getVmIds(new AgentId(agent.getAgentId()))) {
            VmInfo vmInfo = vmInfoDAO.getVmInfo(vmId);
            if (vmMatcher.match(vmInfo)) {
                list.add(vmInfo);
            }
        }
        return list;
    }

    public void setAgentInfoDAO(AgentInfoDAO agentInfoDAO) {
        this.agentInfoDAO = agentInfoDAO;
        servicesLatch.countDown();
    }

    public void setHostInfoDAO(HostInfoDAO hostInfoDAO) {
        this.hostInfoDAO = hostInfoDAO;
        servicesLatch.countDown();
    }

    public void setVmInfoDAO(VmInfoDAO vmInfoDAO) {
        this.vmInfoDAO = vmInfoDAO;
        servicesLatch.countDown();
    }

    public void servicesUnavailable() {
        setAgentInfoDAO(null);
        setHostInfoDAO(null);
        setVmInfoDAO(null);
        servicesLatch = new CountDownLatch(3);
    }
}
