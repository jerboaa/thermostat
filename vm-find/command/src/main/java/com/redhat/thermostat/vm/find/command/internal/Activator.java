/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.vm.find.command.internal;

import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Hashtable;
import java.util.Map;

/**
 * Registers the {@link FindVmCommand} with Thermostat.
 */
public class Activator implements BundleActivator {

    private final FindVmCommand findVmCommand = new FindVmCommand();

    private MultipleServiceTracker serviceTracker;
    private ServiceRegistration serviceRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        Class<?>[] deps = new Class<?>[] {
            AgentInfoDAO.class,
            HostInfoDAO.class,
            VmInfoDAO.class,
        };

        serviceTracker = new MultipleServiceTracker(context, deps, new MultipleServiceTracker.Action() {
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                AgentInfoDAO agentInfoDAO = (AgentInfoDAO) services.get(AgentInfoDAO.class.getName());
                HostInfoDAO hostInfoDAO = (HostInfoDAO) services.get(HostInfoDAO.class.getName());
                VmInfoDAO vmInfoDAO = (VmInfoDAO) services.get(VmInfoDAO.class.getName());

                findVmCommand.setAgentInfoDAO(agentInfoDAO);
                findVmCommand.setHostInfoDAO(hostInfoDAO);
                findVmCommand.setVmInfoDAO(vmInfoDAO);
            }

            @Override
            public void dependenciesUnavailable() {
                findVmCommand.servicesUnavailable();
            }
        });

        serviceTracker.open();

        Hashtable<String,String> properties = new Hashtable<>();
        properties.put(Command.NAME, FindVmCommand.REGISTER_NAME);
        serviceRegistration = context.registerService(Command.class.getName(), findVmCommand, properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        serviceTracker.close();
        serviceRegistration.unregister();
    }
}
