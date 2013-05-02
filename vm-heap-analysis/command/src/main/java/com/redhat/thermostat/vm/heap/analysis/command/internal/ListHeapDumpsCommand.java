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

package com.redhat.thermostat.vm.heap.analysis.command.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.client.cli.HostVMArguments;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.heap.analysis.command.locale.LocaleResources;
import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import com.redhat.thermostat.vm.heap.analysis.common.model.HeapInfo;

public class ListHeapDumpsCommand extends AbstractCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String[] COLUMN_NAMES = {
        translator.localize(LocaleResources.HEADER_HOST_ID).getContents(),
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
        HostVMArguments args = new HostVMArguments(ctx.getArguments(), false, false);

        TableRenderer renderer = new TableRenderer(4);

        renderer.printLine(COLUMN_NAMES);

        ServiceReference hostDAORef = context.getServiceReference(HostInfoDAO.class.getName());
        if (hostDAORef == null) {
            throw new CommandException(translator.localize(LocaleResources.HOST_SERVICE_UNAVAILABLE));
        }
        HostInfoDAO hostDAO = (HostInfoDAO) context.getService(hostDAORef);

        ServiceReference vmDAORef = context.getServiceReference(VmInfoDAO.class.getName());
        if (vmDAORef == null) {
            throw new CommandException(translator.localize(LocaleResources.VM_SERVICE_UNAVAILABLE));
        }
        VmInfoDAO vmDAO = (VmInfoDAO) context.getService(vmDAORef);

        ServiceReference heapDAORef = context.getServiceReference(HeapDAO.class.getName());
        if (heapDAORef == null) {
            throw new CommandException(translator.localize(LocaleResources.HEAP_SERVICE_UNAVAILABLE));
        }
        HeapDAO heapDAO = (HeapDAO) context.getService(heapDAORef);

        Collection<HostRef> hosts = args.getHost() != null ? Arrays.asList(args.getHost()) : hostDAO.getHosts();
        for (HostRef hostRef : hosts) {
            Collection<VmRef> vms = args.getVM() != null ? Arrays.asList(args.getVM()) : vmDAO.getVMs(hostRef);
            for (VmRef vmRef : vms) {
                printDumpsForVm(heapDAO, hostRef, vmRef, renderer);
            }
        }

        context.ungetService(heapDAORef);
        context.ungetService(vmDAORef);
        context.ungetService(hostDAORef);

        renderer.render(ctx.getConsole().getOutput());
    }

    private void printDumpsForVm(HeapDAO heapDAO, HostRef hostRef, VmRef vmRef, TableRenderer renderer) {
        Collection<HeapInfo> infos = heapDAO.getAllHeapInfo(vmRef);
        for (HeapInfo info : infos) {
            renderer.printLine(hostRef.getStringID(),
                               vmRef.getStringID(),
                               info.getHeapId(),
                               new Date(info.getTimeStamp()).toString());
        }
    }

}

