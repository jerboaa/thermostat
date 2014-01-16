/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.killvm.client.internal;

import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.ui.ReferenceContextAction;

import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

public class Activator implements BundleActivator {

    private MultipleServiceTracker killVmActionTracker;
    private ServiceRegistration killActionRegistration;

    @Override
    public void start(final BundleContext context) throws Exception {
        Class<?>[] serviceDeps = new Class<?>[] {
            AgentInfoDAO.class,
            VmInfoDAO.class,
            RequestQueue.class,
        };

        killVmActionTracker = new MultipleServiceTracker(context, serviceDeps, new Action() {
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                AgentInfoDAO agentDao = (AgentInfoDAO) services.get(AgentInfoDAO.class.getName());
                VmInfoDAO vmDao = (VmInfoDAO) services.get(VmInfoDAO.class.getName());
                RequestQueue queue = (RequestQueue) services.get(RequestQueue.class.getName());
                KillVMAction service = new KillVMAction(agentDao, vmDao, queue, new SwingVMKilledListener());
                killActionRegistration = context.registerService(ReferenceContextAction.class.getName(), service, null);
            }

            @Override
            public void dependenciesUnavailable() {
                killActionRegistration.unregister();
                killActionRegistration = null;
            }
        });

        killVmActionTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        killVmActionTracker.close();
        // service automatically unregistered on bundle stop
    }

}

