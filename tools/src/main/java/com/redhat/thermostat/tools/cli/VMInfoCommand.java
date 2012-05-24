/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.tools.cli;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleArgumentSpec;
import com.redhat.thermostat.common.dao.DAOException;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmInfo;

public class VMInfoCommand implements Command {

    private static final String NAME = "vm-info";
    private static final String DESCRIPTION = "shows basic information about a VM";
    private static final String HOST_ID_ARGUMENT = "hostId";
    private static final String VM_ID_ARGUMENT = "vmId";

    @Override
    public void run(CommandContext ctx) throws CommandException {
        DAOFactory daoFactory = ApplicationContext.getInstance().getDAOFactory();
        VmInfoDAO vmsDAO = daoFactory.getVmInfoDAO();
        String hostId = ctx.getArguments().getArgument(HOST_ID_ARGUMENT);
        String vmId = ctx.getArguments().getArgument(VM_ID_ARGUMENT);
        HostRef host = new HostRef(hostId, "dummy");
        int parsedVmId = parseVmId(vmId);
        VmRef vm = new VmRef(host, parsedVmId, "dummy");
        try {
            getAndPrintVMInfo(ctx, vmsDAO, vm);
        } catch (DAOException ex) {
            ctx.getConsole().getError().println(ex.getMessage());
        }
    }

    private int parseVmId(String vmId) throws CommandException {
        try {
            return Integer.parseInt(vmId);
        } catch (NumberFormatException ex) {
            throw new CommandException("Invalid VM ID: " + vmId, ex);
        }
    }

    private void getAndPrintVMInfo(CommandContext ctx, VmInfoDAO vmsDAO, VmRef vm) {

        VmInfo vmInfo = vmsDAO.getVmInfo(vm);

        TableRenderer table = new TableRenderer(2);
        table.printLine("Process ID:", String.valueOf(vmInfo.getVmPid()));
        table.printLine("Start time:", new Date(vmInfo.getStartTimeStamp()).toString());
        table.printLine("Stop time:", new Date(vmInfo.getStopTimeStamp()).toString());
        table.printLine("Main class:", vmInfo.getMainClass());
        table.printLine("Command line:", vmInfo.getJavaCommandLine());
        table.printLine("Java version:", vmInfo.getJavaVersion());
        table.printLine("Virtual machine:", vmInfo.getVmName());
        table.printLine("VM arguments:", vmInfo.getVmArguments());

        PrintStream out = ctx.getConsole().getOutput();
        table.render(out);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getUsage() {
        return DESCRIPTION;
    }

    @Override
    public Collection<ArgumentSpec> getAcceptedArguments() {
        ArgumentSpec vmId = new SimpleArgumentSpec(VM_ID_ARGUMENT, "the ID of the VM to monitor", true, true);
        ArgumentSpec hostId = new SimpleArgumentSpec(HOST_ID_ARGUMENT, "the ID of the host to monitor", true, true);
        return Arrays.asList(vmId, hostId);
    }

    @Override
    public boolean isStorageRequired() {
        return true;
    }

}
