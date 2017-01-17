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

package com.redhat.thermostat.vm.jmx.client.cli.internal;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.jmx.client.cli.NotificationsCommand;
import com.redhat.thermostat.vm.jmx.common.JmxNotificationDAO;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.Hashtable;

public class Activator implements BundleActivator {

    private MultipleServiceTracker depsTracker;
    private ServiceRegistration registration;

    @Override
    public void start(final BundleContext context) throws Exception {
        final Class<?>[] deps = new Class<?>[] {
                ApplicationService.class,
                RequestQueue.class,
                HostInfoDAO.class,
                AgentInfoDAO.class,
                VmInfoDAO.class,
                JmxNotificationDAO.class,
        };

        final NotificationsCommand cmd = new NotificationsCommand();
        depsTracker = new MultipleServiceTracker(context, deps, new MultipleServiceTracker.Action() {
            @Override
            public void dependenciesAvailable(MultipleServiceTracker.DependencyProvider services) {
                ApplicationService applicationService = services.get(ApplicationService.class);
                RequestQueue queue = services.get(RequestQueue.class);
                HostInfoDAO hostDao = services.get(HostInfoDAO.class);
                AgentInfoDAO agentDao = services.get(AgentInfoDAO.class);
                VmInfoDAO vmDao = services.get(VmInfoDAO.class);
                JmxNotificationDAO jmxDao = services.get(JmxNotificationDAO.class);

                cmd.bindApplicationService(applicationService);
                cmd.bindClock(new SystemClock());
                cmd.bindRequestQueue(queue);
                cmd.bindHostInfoDao(hostDao);
                cmd.bindAgentInfoDao(agentDao);
                cmd.bindVmInfoDao(vmDao);
                cmd.bindJmxNotificationDao(jmxDao);
            }

            @Override
            public void dependenciesUnavailable() {
                cmd.dependenciesUnavailable();
            }

        });
        depsTracker.open();

        Dictionary<String, String> props = new Hashtable<>();
        props.put(Command.NAME, NotificationsCommand.COMMAND_NAME);
        registration = context.registerService(Command.class.getName(), cmd, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (depsTracker != null) {
            depsTracker.close();
        }
        if (registration != null) {
            registration.unregister();
        }
    }

}
