/*
 * Copyright 2012-2016 Red Hat, Inc.
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

import com.redhat.thermostat.vm.heap.analysis.common.HeapDAO;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

public class Activator implements BundleActivator {

    private CommandRegistry reg;
    private MultipleServiceTracker serviceTracker;
    private DumpHeapCommand dumpHeapCommand = new DumpHeapCommand();

    @Override
    public void start(final BundleContext context) throws Exception {
        reg = new CommandRegistryImpl(context);

        registerCommand("dump-heap", dumpHeapCommand);
        registerCommand("list-heap-dumps", new ListHeapDumpsCommand());
        registerCommand("save-heap-dump-to-file", new SaveHeapDumpToFileCommand());
        registerCommand("show-heap-histogram", new ShowHeapHistogramCommand());
        registerCommand("find-objects", new FindObjectsCommand());
        registerCommand("object-info", new ObjectInfoCommand());
        registerCommand("find-root", new FindRootCommand());

        Class<?>[] serviceDeps = new Class<?>[] {
                AgentInfoDAO.class,
                VmInfoDAO.class,
                HeapDAO.class,
                RequestQueue.class,
        };

        serviceTracker = new MultipleServiceTracker(context, serviceDeps, new MultipleServiceTracker.Action() {
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                VmInfoDAO vmDao = services.get(VmInfoDAO.class);
                AgentInfoDAO agentDao = services.get(AgentInfoDAO.class);
                HeapDAO heapDao = services.get(HeapDAO.class);
                RequestQueue queue = services.get(RequestQueue.class);

                dumpHeapCommand.setAgentInfoDAO(agentDao);
                dumpHeapCommand.setVmInfoDAO(vmDao);
                dumpHeapCommand.setHeapDAO(heapDao);
                dumpHeapCommand.setRequestQueue(queue);
            }

            @Override
            public void dependenciesUnavailable() {
                dumpHeapCommand.setAgentInfoDAO(null);
                dumpHeapCommand.setVmInfoDAO(null);
                dumpHeapCommand.setHeapDAO(null);
                dumpHeapCommand.setRequestQueue(null);
            }
        });

        serviceTracker.open();
    }

    private void registerCommand(String name, Command command) {
        reg.registerCommand(name, command);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        serviceTracker.close();
        reg.unregisterCommands();
    }
    
}

