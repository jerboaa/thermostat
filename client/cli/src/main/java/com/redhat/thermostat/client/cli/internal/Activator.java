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

package com.redhat.thermostat.client.cli.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.BackendInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.utils.keyring.Keyring;

public class Activator implements BundleActivator {

    private CommandRegistry reg = null;
    private MultipleServiceTracker connectTracker;

    private MultipleServiceTracker vmStatTracker;
    private final VMStatCommand vmStatCommand = new VMStatCommand();

    private MultipleServiceTracker listAgentTracker;
    private final ListAgentsCommand listAgentsCommand = new ListAgentsCommand();

    private MultipleServiceTracker agentInfoTracker;
    private final AgentInfoCommand agentInfoCommand = new AgentInfoCommand();

    @Override
    public void start(final BundleContext context) throws Exception {
        reg = new CommandRegistryImpl(context);

        reg.registerCommand("list-vms", new ListVMsCommand());
        reg.registerCommand("vm-info", new VMInfoCommand());
        reg.registerCommand("vm-stat", vmStatCommand);
        reg.registerCommand("disconnect", new DisconnectCommand());
        reg.registerCommand("clean-data", new CleanDataCommand(context));

        Class<?>[] classes = new Class[] {
            Keyring.class,
            CommonPaths.class,
            SSLConfiguration.class,
        };
        connectTracker = new MultipleServiceTracker(context, classes, new Action() {

            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                Keyring keyring = services.get(Keyring.class);
                CommonPaths paths = services.get(CommonPaths.class);
                ClientPreferences prefs = new ClientPreferences(paths);
                SSLConfiguration sslConf = services.get(SSLConfiguration.class);
                reg.registerCommand("connect", new ConnectCommand(context, prefs, keyring, sslConf));
            }

            @Override
            public void dependenciesUnavailable() {
                reg.unregisterCommand("connect");
            }
            
        });
        connectTracker.open();

        Class<?>[] vmStatClasses = new Class<?>[] {
                VmInfoDAO.class,
        };

        vmStatTracker = new MultipleServiceTracker(context, vmStatClasses, new Action() {
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                VmInfoDAO vmInfoDAO = services.get(VmInfoDAO.class);
                vmStatCommand.setVmInfoDAO(vmInfoDAO);
            }

            @Override
            public void dependenciesUnavailable() {
                vmStatCommand.setVmInfoDAO(null);
            }
        });
        vmStatTracker.open();

        Class<?>[] listAgentClasses = new Class[] {
                AgentInfoDAO.class,
        };
        listAgentTracker = new MultipleServiceTracker(context, listAgentClasses, new Action() {
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                AgentInfoDAO agentInfoDAO = services.get(AgentInfoDAO.class);
                listAgentsCommand.setAgentInfoDAO(agentInfoDAO);
            }

            @Override
            public void dependenciesUnavailable() {
                listAgentsCommand.setAgentInfoDAO(null);
            }
        });
        listAgentTracker.open();

        reg.registerCommand("list-agents", listAgentsCommand);

        Class<?>[] agentInfoClasses = new Class[] {
                AgentInfoDAO.class,
                BackendInfoDAO.class,
        };
        agentInfoTracker = new MultipleServiceTracker(context, agentInfoClasses, new Action() {
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                AgentInfoDAO agentInfoDAO = services.get(AgentInfoDAO.class);
                BackendInfoDAO backendInfoDAO = services.get(BackendInfoDAO.class);

                agentInfoCommand.setServices(agentInfoDAO, backendInfoDAO);
            }

            @Override
            public void dependenciesUnavailable() {
                agentInfoCommand.setServices(null, null);
            }
        });
        agentInfoTracker.open();

        reg.registerCommand("agent-info", agentInfoCommand);

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        connectTracker.close();
        listAgentTracker.close();
        agentInfoTracker.close();
        reg.unregisterCommands();
    }

}

