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

package com.redhat.thermostat.vm.memory.client.core.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Objects;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.gc.remote.common.GCRequest;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.memory.client.core.MemoryStatsService;
import com.redhat.thermostat.vm.memory.client.core.MemoryStatsViewProvider;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;
import com.redhat.thermostat.vm.memory.common.VmTlabStatDAO;

public class Activator implements BundleActivator {

    private MultipleServiceTracker tracker;
    private ServiceRegistration memoryStatRegistration;

    @Override
    public void start(final BundleContext context) throws Exception {
        Class<?>[] deps = new Class<?>[] {
            ApplicationService.class,
            VmInfoDAO.class,
            VmMemoryStatDAO.class,
            VmTlabStatDAO.class,
            GCRequest.class,
            AgentInfoDAO.class,
            MemoryStatsViewProvider.class
        };

        tracker = new MultipleServiceTracker(context, deps, new Action() {
            
            @Override
            public void dependenciesUnavailable() {
                memoryStatRegistration.unregister();
                memoryStatRegistration = null;
            }

            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                VmInfoDAO vmInfoDao = services.get(VmInfoDAO.class);
                VmMemoryStatDAO memoryStatDao = services.get(VmMemoryStatDAO.class);
                VmTlabStatDAO tlabStatDao = services.get(VmTlabStatDAO.class);
                AgentInfoDAO agentDAO = services.get(AgentInfoDAO.class);
                GCRequest gcRequest = services.get(GCRequest.class);
                ApplicationService appSvc = services.get(ApplicationService.class);
                MemoryStatsViewProvider viewProvider = services.get(MemoryStatsViewProvider.class);

                MemoryStatsService impl = new MemoryStatsServiceImpl(appSvc, vmInfoDao, memoryStatDao, tlabStatDao, agentDAO, gcRequest, viewProvider);
                Dictionary<String, String> properties = new Hashtable<>();
                properties.put(Constants.GENERIC_SERVICE_CLASSNAME, VmRef.class.getName());
                properties.put(InformationService.KEY_SERVICE_ID, MemoryStatsService.SERVICE_ID);
                memoryStatRegistration = context.registerService(InformationService.class.getName(), impl, properties);
            }
        });
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }
}

