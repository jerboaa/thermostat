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

package com.redhat.thermostat.client.cli.internal;

import java.util.List;
import java.util.Set;

import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.model.HostInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;

public class ListVMsCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private final BundleContext context;

    public ListVMsCommand() {
        this(FrameworkUtil.getBundle(ListVMsCommand.class).getBundleContext());
    }

    ListVMsCommand(BundleContext context) {
        this.context = context;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        String agentIdArg = ctx.getArguments().getArgument("agentId");

        ServiceReference hostsDAORef = context.getServiceReference(HostInfoDAO.class.getName());
        requireNonNull(hostsDAORef, translator.localize(LocaleResources.HOST_SERVICE_UNAVAILABLE));
        HostInfoDAO hostsDAO = (HostInfoDAO) context.getService(hostsDAORef);

        ServiceReference agentInfoDAORef = context.getServiceReference(AgentInfoDAO.class.getName());
        AgentInfoDAO agentInfoDAO = (AgentInfoDAO) context.getService(agentInfoDAORef);

        ServiceReference vmsDAORef = context.getServiceReference(VmInfoDAO.class.getName());
        requireNonNull(vmsDAORef, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        VmInfoDAO vmsDAO = (VmInfoDAO) context.getService(vmsDAORef);

        VMListFormatter formatter = new VMListFormatter();
        formatter.addHeader();
        if (agentIdArg == null) {
            List<AgentInformation> agentInfos = agentInfoDAO.getAllAgentInformation();
            for (AgentInformation agentInfo : agentInfos) {
                addVMsToFormatter(hostsDAO, vmsDAO, formatter, agentInfo, ctx);
            }
        } else {
            AgentId agentId = new AgentId(agentIdArg);
            AgentInformation agentInfo = agentInfoDAO.getAgentInformation(agentId);

            if (agentInfo != null) {
                addVMsToFormatter(hostsDAO, vmsDAO, formatter, agentInfo, ctx);
            } else {
                throw new CommandException(translator.localize(LocaleResources.AGENT_NOT_FOUND, agentId.get()));
            }
        }
        formatter.format(ctx.getConsole().getOutput());
        context.ungetService(vmsDAORef);
        context.ungetService(hostsDAORef);

    }

    private void addVMsToFormatter(final HostInfoDAO hostsDAO, final VmInfoDAO vmsDAO, final VMListFormatter formatter, final AgentInformation agentInfo, final CommandContext ctx) {
        Set<VmId> vmIds = vmsDAO.getVmIds(new AgentId(agentInfo.getAgentId()));
        for (VmId vmId : vmIds) {
            addVMToFormatter(hostsDAO, vmsDAO, formatter, agentInfo, vmId);
        }
    }

    private void addVMToFormatter(final HostInfoDAO hostsDAO, final VmInfoDAO vmsDAO, final VMListFormatter formatter,  final AgentInformation agentInfo, final VmId vmId) {
        VmInfo vmInfo = vmsDAO.getVmInfo(vmId);
        AgentId agentId = new AgentId(agentInfo.getAgentId());
        HostInfo hostInfo = hostsDAO.getHostInfo(agentId);
        formatter.addVM(hostInfo.getHostname(), vmId.get(), agentInfo, vmInfo);
    }
}

