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

package com.redhat.thermostat.client.cli.internal;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Date;

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
import com.redhat.thermostat.storage.dao.DAOException;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.AgentInformation;
import com.redhat.thermostat.storage.model.VmInfo;

public class VMInfoCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String STATUS_RUNNING = translator.localize(LocaleResources.VM_STATUS_RUNNING).getContents();
    private static final String STATUS_UNKNOWN = translator.localize(LocaleResources.VM_STATUS_UNKNOWN).getContents();

    private final BundleContext context;

    public VMInfoCommand() {
        this(FrameworkUtil.getBundle(VMInfoCommand.class).getBundleContext());
    }

    /** For tests only */
    VMInfoCommand(BundleContext context) {
        this.context = context;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        ServiceReference agentsDAORef = context.getServiceReference(AgentInfoDAO.class.getName());
        requireNonNull(agentsDAORef, translator.localize(LocaleResources.AGENT_SERVICE_UNAVAILABLE));
        AgentInfoDAO agentsDAO = (AgentInfoDAO) context.getService(agentsDAORef);

        ServiceReference vmsDAORef = context.getServiceReference(VmInfoDAO.class.getName());
        requireNonNull(vmsDAORef, translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        VmInfoDAO vmsDAO = (VmInfoDAO) context.getService(vmsDAORef);

        try {
            VmArgument vmArgument = VmArgument.optional(ctx.getArguments());
            AgentArgument agentArgument = AgentArgument.optional(ctx.getArguments());
            VmId vmId = vmArgument.getVmId();
            AgentId agentId = agentArgument.getAgentId();

            if (vmId != null) {
                final VmInfo vmInfo = vmsDAO.getVmInfo(vmId);
                agentId = new AgentId(vmInfo.getAgentId());
                AgentInformation agentInfo = agentsDAO.getAgentInformation(agentId);
                if (agentInfo == null) {
                    throw new CommandException(translator.localize(LocaleResources.AGENT_NOT_FOUND, VmArgument.ARGUMENT_NAME, vmId.get()));
                }
                getAndPrintVMInfo(ctx, agentInfo, vmsDAO, vmId);
            } else if (agentId != null){
                AgentInformation agentInfo = agentsDAO.getAgentInformation(agentId);
                if (agentInfo == null) {
                    throw new CommandException(translator.localize(LocaleResources.AGENT_NOT_FOUND, AgentArgument.ARGUMENT_NAME, agentId.get()));
                }
                getAndPrintAllVMInfo(ctx, agentInfo, vmsDAO, agentId);
            } else {
                throw new CommandException(translator.localize(LocaleResources.ONE_ID_REQUIRED));
            }
        } catch (DAOException ex) {
            ctx.getConsole().getError().println(ex.getMessage());
        } finally {
            context.ungetService(vmsDAORef);
            context.ungetService(agentsDAORef);
        }
    }

    private void getAndPrintAllVMInfo(CommandContext ctx, AgentInformation agentInfo, VmInfoDAO vmsDAO, AgentId agentId) {
        Collection<VmId> vms = vmsDAO.getVmIds(agentId);
        for (VmId vm : vms) {
            getAndPrintVMInfo(ctx, agentInfo, vmsDAO, vm);
        }
    }

    private void getAndPrintVMInfo(CommandContext ctx, AgentInformation agentInfo, VmInfoDAO vmsDAO, VmId vmId) {
        VmInfo vmInfo = vmsDAO.getVmInfo(vmId);

        TableRenderer table = new TableRenderer(2);
        table.printLine(translator.localize(LocaleResources.VM_INFO_VM_ID).getContents(), vmInfo.getVmId());
        table.printLine(translator.localize(LocaleResources.VM_INFO_PROCESS_ID).getContents(), String.valueOf(vmInfo.getVmPid()));
        table.printLine(translator.localize(LocaleResources.VM_INFO_START_TIME).getContents(), new Date(vmInfo.getStartTimeStamp()).toString());
        table.printLine(translator.localize(LocaleResources.VM_INFO_STOP_TIME).getContents(), getVmStopTimeForDisplay(agentInfo, vmInfo));
        printUserInfo(vmInfo, table);
        table.printLine(translator.localize(LocaleResources.VM_INFO_MAIN_CLASS).getContents(), vmInfo.getMainClass());
        table.printLine(translator.localize(LocaleResources.VM_INFO_COMMAND_LINE).getContents(), vmInfo.getJavaCommandLine());
        table.printLine(translator.localize(LocaleResources.VM_INFO_JAVA_VERSION).getContents(), vmInfo.getJavaVersion());
        table.printLine(translator.localize(LocaleResources.VM_INFO_VIRTUAL_MACHINE).getContents(), vmInfo.getVmName());
        table.printLine(translator.localize(LocaleResources.VM_INFO_VM_ARGUMENTS).getContents(), vmInfo.getVmArguments());
        
        PrintStream out = ctx.getConsole().getOutput();
        table.render(out);
    }

    private String getVmStopTimeForDisplay(AgentInformation agentInfo, VmInfo vmInfo) {
        switch (vmInfo.isAlive(agentInfo)) {
        case RUNNING:
            return STATUS_RUNNING;
        case EXITED:
            return new Date(vmInfo.getStopTimeStamp()).toString();
        case UNKNOWN:
            return STATUS_UNKNOWN;
        default:
            throw new AssertionError("Unknown VM status");
        }
    }

    private void printUserInfo(VmInfo vmInfo, TableRenderer table) {
        // Check if we have valid user info
        long uid = vmInfo.getUid();
        String user;
        if (uid >= 0) {
            user = String.valueOf(uid);
            String username = vmInfo.getUsername();
            if (username != null) {
                user += "(" + username + ")";
            }
        }
        else {
            user = translator.localize(LocaleResources.VM_INFO_USER_UNKNOWN).getContents();
        }
        table.printLine(translator.localize(LocaleResources.VM_INFO_USER).getContents(), user);
    }

}

