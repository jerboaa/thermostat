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

package com.redhat.thermostat.storage.populator.internal;

import java.util.Hashtable;
import java.util.Map;

import com.redhat.thermostat.common.cli.CompleterService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.populator.StoragePopulatorCommand;
import com.redhat.thermostat.thread.dao.ThreadDao;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator implements BundleActivator {

    private MultipleServiceTracker cmdDepsTracker;
    private ServiceTracker completerDepsTracker;
    private ServiceRegistration completerRegistration;

    @Override
    @SuppressWarnings("unchecked")
    public void start(final BundleContext context) throws Exception {

        final StoragePopulatorCommand command = new StoragePopulatorCommand();
        Hashtable<String,String> properties = new Hashtable<>();
        properties.put(Command.NAME, "storage-populator");
        context.registerService(Command.class, command, properties);

        Class<?>[] deps = new Class<?>[] {
                CommonPaths.class,
                HostInfoDAO.class,
                AgentInfoDAO.class,
                VmInfoDAO.class,
                NetworkInterfaceInfoDAO.class,
                ThreadDao.class,
        };

        cmdDepsTracker = new MultipleServiceTracker(context, deps, new MultipleServiceTracker.Action() {

            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                CommonPaths paths = (CommonPaths) services.get(CommonPaths.class.getName());
                HostInfoDAO hostInfoDAO = (HostInfoDAO) services.get(HostInfoDAO.class.getName());
                AgentInfoDAO agentInfoDAO = (AgentInfoDAO) services.get(AgentInfoDAO.class.getName());
                VmInfoDAO vmInfoDAO = (VmInfoDAO) services.get(VmInfoDAO.class.getName());
                NetworkInterfaceInfoDAO networkInfoDAO = (NetworkInterfaceInfoDAO)
                        services.get(NetworkInterfaceInfoDAO.class.getName());
                ThreadDao threadDao = (ThreadDao) services.get(ThreadDao.class.getName());

                command.setPaths(paths);
                command.setHostInfoDAO(hostInfoDAO);
                command.setAgentInfoDAO(agentInfoDAO);
                command.setVmInfoDAO(vmInfoDAO);
                command.setNetworkInfoDAO(networkInfoDAO);
                command.setThreadDao(threadDao);
            }

            @Override
            public void dependenciesUnavailable() {
                command.setServicesUnavailable();
            }
        });
        cmdDepsTracker.open();

        final StoragePopulatorCompleterService completerService = new StoragePopulatorCompleterService();
        completerDepsTracker = new ServiceTracker(context, CommonPaths.class, new ServiceTrackerCustomizer() {
            @Override
            public Object addingService(ServiceReference serviceReference) {
                CommonPaths paths = (CommonPaths) context.getService(serviceReference);
                completerService.setCommonPaths(paths);
                completerRegistration = context.registerService(CompleterService.class.getName(), completerService, null);
                return context.getService(serviceReference);
            }

            @Override
            public void modifiedService(ServiceReference serviceReference, Object o) {
            }

            @Override
            public void removedService(ServiceReference serviceReference, Object o) {
                completerService.setCommonPaths(null);
            }
        });
        completerDepsTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        cmdDepsTracker.close();
        completerDepsTracker.close();
        if (completerRegistration != null) {
            completerRegistration.unregister();
        }
    }

}
