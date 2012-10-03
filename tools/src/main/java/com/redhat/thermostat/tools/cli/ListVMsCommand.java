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

import java.util.Collection;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleCommand;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.VmInfo;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.tools.LocaleResources;
import com.redhat.thermostat.tools.Translate;

public class ListVMsCommand extends SimpleCommand {

    private static final String NAME = "list-vms";

    private final OSGIUtils serviceProvider;

    public ListVMsCommand() {
        this(OSGIUtils.getInstance());
    }

    ListVMsCommand(OSGIUtils serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {

        DAOFactory daoFactory = ApplicationContext.getInstance().getDAOFactory();

        HostInfoDAO hostsDAO = serviceProvider.getServiceAllowNull(HostInfoDAO.class);
        if (hostsDAO == null) {
            throw new CommandException(Translate.localize(LocaleResources.HOST_SERVICE_UNAVAILABLE));
        }
        Collection<HostRef> hosts = hostsDAO.getHosts();
        serviceProvider.ungetService(HostInfoDAO.class, hostsDAO);

        VmInfoDAO vmsDAO = daoFactory.getVmInfoDAO();
        VMListFormatter formatter = new VMListFormatter();
        for (HostRef host : hosts) {
            Collection<VmRef> vms = vmsDAO.getVMs(host);
            for (VmRef vm : vms) {
                VmInfo info = vmsDAO.getVmInfo(vm);
                formatter.addVM(vm, info);
            }
        }
        formatter.format(ctx.getConsole().getOutput());
    }

    @Override
    public String getName() {
        return NAME;
    }

}
