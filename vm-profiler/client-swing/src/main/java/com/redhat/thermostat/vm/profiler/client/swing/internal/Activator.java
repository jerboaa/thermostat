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

package com.redhat.thermostat.vm.profiler.client.swing.internal;

import java.util.Hashtable;
import java.util.Map;

import com.redhat.thermostat.client.swing.UIDefaults;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.core.progress.ProgressNotifier;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.AgentInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;

public class Activator implements BundleActivator {

    private ServiceRegistration<InformationService> registration;
    private MultipleServiceTracker tracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        VmProfileTreeMapViewProvider vmProfileTreeMapViewProvider = new SwingVmProfileTreeMapViewProvider();
        context.registerService(VmProfileTreeMapViewProvider.class.getName(),
                vmProfileTreeMapViewProvider, null);

        Class<?>[] deps = new Class<?>[] {
                ApplicationService.class,
                ProgressNotifier.class,
                AgentInfoDAO.class,
                VmInfoDAO.class,
                ProfileDAO.class,
                RequestQueue.class,
                VmProfileTreeMapViewProvider.class,
                UIDefaults.class,
        };

        tracker = new MultipleServiceTracker(context, deps, new MultipleServiceTracker.Action() {
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                ApplicationService service = (ApplicationService) services.get(ApplicationService.class.getName());
                ProgressNotifier notifier = (ProgressNotifier) services.get(ProgressNotifier.class.getName());
                AgentInfoDAO agentInfoDao = (AgentInfoDAO) services.get(AgentInfoDAO.class.getName());
                VmInfoDAO vmInfoDao = (VmInfoDAO) services.get(VmInfoDAO.class.getName());
                ProfileDAO profileDao = (ProfileDAO) services.get(ProfileDAO.class.getName());
                RequestQueue queue = (RequestQueue) services.get(RequestQueue.class.getName());
                VmProfileTreeMapViewProvider treeMapViewProvider = (VmProfileTreeMapViewProvider) services
                        .get(VmProfileTreeMapViewProvider.class.getName());

                UIDefaults uiDefaults = (UIDefaults) services.get(UIDefaults.class.getName());

                InformationService<VmRef> profileService = new VmProfileService(service, notifier,
                        agentInfoDao, vmInfoDao, profileDao, queue, treeMapViewProvider, uiDefaults);

                Hashtable<String,String> properties = new Hashtable<>();
                properties.put(Constants.GENERIC_SERVICE_CLASSNAME, VmRef.class.getName());
                properties.put(InformationService.KEY_SERVICE_ID, profileService.getClass().getName());

                registration = context.registerService(InformationService.class, profileService, properties);
            }

            @Override
            public void dependenciesUnavailable() {
                registration.unregister();
                registration = null;
            }
        });
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }

}
