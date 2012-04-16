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

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import com.redhat.thermostat.cli.Command;
import com.redhat.thermostat.cli.CommandContext;
import com.redhat.thermostat.cli.CommandException;
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

    private static final String USAGE = "list-vms --dbUrl URL\n\n"
                                        + DESCRIPTION + "\n\n\t"
                                        + "Options:\n\n"
                                        + "--dbUrl URL  the URL of the storage to connect to.\n";

    @Override
    public void run(CommandContext ctx) throws CommandException {

        OptionSet options = parseArguments(ctx);
        String dbUrl = getDBURL(options);

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

    private OptionSet parseArguments(CommandContext ctx) throws CommandException {
        OptionParser parser = new OptionParser();
        parser.accepts("dbUrl").withRequiredArg();
        try {
            OptionSet options = parser.parse(ctx.getArguments());
            if (! options.nonOptionArguments().isEmpty()) {
                throw new CommandException("Unknown arguments: " + options.nonOptionArguments());
            }
            return options;
        } catch (OptionException ex) {
            throw new CommandException(ex);
        }
    }

    private String getDBURL(OptionSet options) {
        String dbUrl = (String) options.valueOf("dbUrl");
        return dbUrl;
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

}
