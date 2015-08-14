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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.client.cli.AgentArgument;
import com.redhat.thermostat.client.cli.VmArgument;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.AgentId;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;

public class ListHeapDumpsCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String[] COLUMN_NAMES = {
        translator.localize(LocaleResources.HEADER_AGENT_ID).getContents(),
        translator.localize(LocaleResources.HEADER_VM_ID).getContents(),
        translator.localize(LocaleResources.HEADER_HEAP_ID).getContents(),
        translator.localize(LocaleResources.HEADER_TIMESTAMP).getContents(),
    };

    private final BundleContext context;

    public ListHeapDumpsCommand() {
        this(FrameworkUtil.getBundle(ListHeapDumpsCommand.class).getBundleContext());
    }

    /** For tests only */
    ListHeapDumpsCommand(BundleContext context) {
        this.context = context;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        TableRenderer renderer = new TableRenderer(4);

        renderer.printHeader(COLUMN_NAMES);

        ServiceReference agentDAORef = context.getServiceReference(AgentInfoDAO.class.getName());
        requireNonNull(agentDAORef, translator.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE));
        AgentInfoDAO agentDAO = (AgentInfoDAO) context.getService(agentDAORef);

        ServiceReference vmDAORef = context.getServiceReference(VmInfoDAO.class.getName());
        requireNonNull(vmDAORef, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        VmInfoDAO vmDAO = (VmInfoDAO) context.getService(vmDAORef);

        ServiceReference heapDAORef = context.getServiceReference(HeapDAO.class.getName());
        requireNonNull(heapDAORef, translator.localize(LocaleResources.HEAP_SERVICE_UNAVAILABLE));
        HeapDAO heapDAO = (HeapDAO) context.getService(heapDAORef);

        VmArgument vmArgument = VmArgument.optional(ctx.getArguments());
        VmId vmId = vmArgument.getVmId();
        AgentId agentId = null;

        if (vmId != null) {
            String stringAgentId = vmDAO.getVmInfo(vmId).getAgentId();
            if (stringAgentId != null) {
                agentId = new AgentId(stringAgentId);
            }
        } else {
            AgentArgument agentArgument = AgentArgument.optional(ctx.getArguments());
            agentId = agentArgument.getAgentId();
        }

        Set<AgentId> hosts = agentId != null ? Collections.singleton(agentId) : agentDAO.getAgentIds();
        for (AgentId host : hosts) {
            Set<VmId> vms = vmId != null ? Collections.singleton(vmId) : vmDAO.getVmIds(host);
            for (VmId vm : vms) {
                printDumpsForVm(heapDAO, host, vm, renderer);
            }
        }

        context.ungetService(heapDAORef);
        context.ungetService(vmDAORef);
        context.ungetService(agentDAORef);

        renderer.render(ctx.getConsole().getOutput());
    }

    private void printDumpsForVm(HeapDAO heapDAO, AgentId agentId, VmId vmId, TableRenderer renderer) {
        Collection<HeapInfo> infos = heapDAO.getAllHeapInfo(agentId, vmId);
        for (HeapInfo info : infos) {
            renderer.printLine(agentId.get(),
                               vmId.get(),
                               info.getHeapId(),
                               new Date(info.getTimeStamp()).toString());
        }
    }

}

