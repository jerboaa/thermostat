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

package com.redhat.thermostat.client.stats.memory.swing;

import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.core.VmInformationService;
import com.redhat.thermostat.client.stats.memory.core.MemoryStatsService;
import com.redhat.thermostat.client.stats.memory.core.MemoryStatsViewProvider;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.dao.AgentInfoDAO;
import com.redhat.thermostat.common.dao.VmMemoryStatDAO;
import com.redhat.thermostat.gc.remote.common.GCRequest;

public class MemoryStatsPanelActivator implements BundleActivator {

    private MultipleServiceTracker tracker;
    private ServiceRegistration memoryStatRegistration;

    @Override
    public void start(final BundleContext context) throws Exception {
        MemoryStatsViewProvider provider = new SwingMemoryStatsViewProvider();
        context.registerService(MemoryStatsViewProvider.class.getName(), provider, null);
        
        Class<?>[] deps = new Class<?>[] {
            ApplicationService.class,
            VmMemoryStatDAO.class,
            GCRequest.class,
            AgentInfoDAO.class,
        };

        tracker = new MultipleServiceTracker(context, deps, new Action() {
            
            @Override
            public void dependenciesUnavailable() {
                memoryStatRegistration.unregister();
                memoryStatRegistration = null;
            }

            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                VmMemoryStatDAO memoryStatDao = (VmMemoryStatDAO) services.get(VmMemoryStatDAO.class.getName());
                AgentInfoDAO agentDAO = (AgentInfoDAO) services.get(AgentInfoDAO.class.getName());
                GCRequest gcRequest = (GCRequest) services.get(GCRequest.class.getName());
                ApplicationService appSvc = (ApplicationService) services.get(ApplicationService.class.getName());

                MemoryStatsService impl = new MemoryStatsService(appSvc, memoryStatDao, agentDAO, gcRequest);
                memoryStatRegistration = context.registerService(VmInformationService.class.getName(), impl , null);
            }
        });
        tracker.open();

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }
}
