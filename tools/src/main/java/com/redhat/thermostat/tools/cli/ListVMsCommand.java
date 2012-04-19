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

import java.util.Arrays;
import java.util.Collection;

import com.redhat.thermostat.cli.ArgumentSpec;
import com.redhat.thermostat.cli.Command;
import com.redhat.thermostat.cli.CommandContext;
import com.redhat.thermostat.cli.CommandException;
import com.redhat.thermostat.cli.SimpleArgumentSpec;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;

public class ListVMsCommand implements Command {

    private static final String NAME = "list-vms";

    // TODO: Localize.
    private static final String DESCRIPTION = "lists all currently monitored VMs";

    private static final String USAGE = DESCRIPTION;

    private static final String DB_URL_ARG = "dbUrl";

    private static final String DB_URL_DESC = "the URL of the storage to connect to";

    @Override
    public void run(CommandContext ctx) throws CommandException {

        String dbUrl = ctx.getArguments().getArgument(DB_URL_ARG);

        ctx.getAppContextSetup().setupAppContext(dbUrl);

        DAOFactory daoFactory = ApplicationContext.getInstance().getDAOFactory();
        HostInfoDAO hostsDAO = daoFactory.getHostInfoDAO();
        Collection<HostRef> hosts = hostsDAO.getHosts();
        VmInfoDAO vmsDAO = daoFactory.getVmInfoDAO();
        VMListFormatter formatter = new VMListFormatter();
        for (HostRef host : hosts) {
            Collection<VmRef> vms = vmsDAO.getVMs(host);
            for (VmRef vm : vms) {
                formatter.addVM(vm);
            }
        }
        formatter.format(ctx.getConsole().getOutput());
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
        return USAGE;
    }

    @Override
    public Collection<ArgumentSpec> getAcceptedArguments() {
        ArgumentSpec dbUrl = new SimpleArgumentSpec(DB_URL_ARG, DB_URL_DESC, true, true);
        return Arrays.asList(dbUrl);
    }

}
