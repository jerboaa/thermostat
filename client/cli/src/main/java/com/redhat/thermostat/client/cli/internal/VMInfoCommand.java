/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.HostVMArguments;
import com.redhat.thermostat.common.cli.SimpleCommand;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.common.dao.DAOException;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.storage.model.VmInfo;

public class VMInfoCommand extends SimpleCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String NAME = "vm-info";
    private static final String STILL_ALIVE = translator.localize(LocaleResources.VM_STOP_TIME_RUNNING);

    private OSGIUtils serviceProvider;

    public VMInfoCommand() {
        this(OSGIUtils.getInstance());
    }

    /** For tests only */
    VMInfoCommand(OSGIUtils serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        VmInfoDAO vmsDAO = serviceProvider.getServiceAllowNull(VmInfoDAO.class);
        if (vmsDAO == null) {
            throw new CommandException(translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        }

        HostVMArguments hostVMArgs = new HostVMArguments(ctx.getArguments(), true, false);
        HostRef host = hostVMArgs.getHost();
        VmRef vm = hostVMArgs.getVM();
        try {
            if (vm != null) {
                getAndPrintVMInfo(ctx, vmsDAO, vm);
            } else {
                getAndPrintAllVMInfo(ctx, vmsDAO, host);

            }
        } catch (DAOException ex) {
            ctx.getConsole().getError().println(ex.getMessage());
        } finally {
            serviceProvider.ungetService(VmInfoDAO.class, vmsDAO);
        }
    }

    private void getAndPrintAllVMInfo(CommandContext ctx, VmInfoDAO vmsDAO, HostRef host) {
        Collection<VmRef> vms = vmsDAO.getVMs(host);
        for (VmRef vm : vms) {
            getAndPrintVMInfo(ctx, vmsDAO, vm);
        }
    }

    private void getAndPrintVMInfo(CommandContext ctx, VmInfoDAO vmsDAO, VmRef vm) {

        VmInfo vmInfo = vmsDAO.getVmInfo(vm);

        TableRenderer table = new TableRenderer(2);
        table.printLine(translator.localize(LocaleResources.VM_INFO_PROCESS_ID), String.valueOf(vmInfo.getVmPid()));
        table.printLine(translator.localize(LocaleResources.VM_INFO_START_TIME), new Date(vmInfo.getStartTimeStamp()).toString());
        if (vmInfo.isAlive()) {
            table.printLine(translator.localize(LocaleResources.VM_INFO_STOP_TIME), STILL_ALIVE);
        } else {
            table.printLine(translator.localize(LocaleResources.VM_INFO_STOP_TIME), new Date(vmInfo.getStopTimeStamp()).toString());
        }
        table.printLine(translator.localize(LocaleResources.VM_INFO_MAIN_CLASS), vmInfo.getMainClass());
        table.printLine(translator.localize(LocaleResources.VM_INFO_COMMAND_LINE), vmInfo.getJavaCommandLine());
        table.printLine(translator.localize(LocaleResources.VM_INFO_JAVA_VERSION), vmInfo.getJavaVersion());
        table.printLine(translator.localize(LocaleResources.VM_INFO_VIRTUAL_MACHINE), vmInfo.getVmName());
        table.printLine(translator.localize(LocaleResources.VM_INFO_VM_ARGUMENTS), vmInfo.getVmArguments());

        PrintStream out = ctx.getConsole().getOutput();
        table.render(out);
    }

    @Override
    public String getName() {
        return NAME;
    }

}

