/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.killvm.command.internal;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.killvm.common.KillVMRequest;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

public class Activator implements BundleActivator {

    private ServiceRegistration killCommandRegistration;
    private MultipleServiceTracker serviceTracker;
    private KillVMCommand command = new KillVMCommand(new ShellVMKilledListener());

    @Override
    public void start(final BundleContext context) throws Exception {
        Class<?>[] serviceDeps = new Class<?>[] {
                AgentInfoDAO.class,
                VmInfoDAO.class,
                KillVMRequest.class,
                RequestQueue.class,
        };

        serviceTracker = new MultipleServiceTracker(context, serviceDeps, new MultipleServiceTracker.Action() {
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                AgentInfoDAO agentDao = services.get(AgentInfoDAO.class);
                VmInfoDAO vmDao = services.get(VmInfoDAO.class);
                KillVMRequest request = services.get(KillVMRequest.class);

                command.setAgentInfoDAO(agentDao);
                command.setVmInfoDAO(vmDao);
                command.setKillVMRequest(request);
            }

            @Override
            public void dependenciesUnavailable() {
                command.setAgentInfoDAO(null);
                command.setVmInfoDAO(null);
                command.setKillVMRequest(null);
            }
        });

        serviceTracker.open();

        Hashtable<String,String> properties = new Hashtable<>();
        properties.put(Command.NAME, "kill-vm");
        killCommandRegistration = context.registerService(Command.class.getName(), command, properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        serviceTracker.close();
        killCommandRegistration.unregister();
    }
}
