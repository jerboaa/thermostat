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

package com.redhat.thermostat.client.heap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HeapDAO;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmInfoDAO;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.model.HeapInfo;

public class ListHeapDumpsCommand implements Command {

    private static final String NAME = "list-heap-dumps";
    private static final String DESCRIPTION = "list all heap dumps";
    private static final String USAGE = DESCRIPTION;

    // TODO localize
    private static final String[] COLUMN_NAMES = {"HOST ID", "VM ID", "HEAP ID", "TIMESTAMP"};

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
        return new ArrayList<>();
    }

    @Override
    public boolean isStorageRequired() {
        return true;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        TableRenderer renderer = new TableRenderer(4);

        renderer.printLine(COLUMN_NAMES);

        DAOFactory daoFactory = ApplicationContext.getInstance().getDAOFactory();
        HostInfoDAO hostDAO = daoFactory.getHostInfoDAO();
        VmInfoDAO vmDAO = daoFactory.getVmInfoDAO();
        HeapDAO heapDAO = daoFactory.getHeapDAO();

        for (HostRef hostRef : hostDAO.getHosts()) {
            for (VmRef vmRef : vmDAO.getVMs(hostRef)) {
                Collection<HeapInfo> infos = heapDAO.getAllHeapInfo(vmRef);
                for (HeapInfo info : infos) {
                    renderer.printLine(hostRef.getStringID(),
                                       vmRef.getStringID(),
                                       info.getHeapDumpId(),
                                       new Date(info.getTimestamp()).toString());
                }
            }
        }

        renderer.render(ctx.getConsole().getOutput());
    }

    @Override
    public void disable() {
        /* NO-OP */
    }

}
