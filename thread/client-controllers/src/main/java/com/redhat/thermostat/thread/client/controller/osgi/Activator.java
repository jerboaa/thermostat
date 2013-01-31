/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.thread.client.controller.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.thread.client.common.ThreadViewProvider;
import com.redhat.thermostat.thread.client.common.collector.ThreadCollectorFactory;
import com.redhat.thermostat.thread.client.controller.ThreadInformationService;
import com.redhat.thermostat.thread.client.controller.impl.ThreadInformationServiceImpl;

public class Activator implements BundleActivator {

    @SuppressWarnings({ "rawtypes" })
    @Override
    public void start(final BundleContext context) throws Exception {
        
        Class[] classes = new Class[] {
                ThreadCollectorFactory.class,
                ApplicationService.class,
                VmInfoDAO.class,
                ThreadViewProvider.class
        };
        
        Action action = new Action() {

            private ServiceRegistration registration;

            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                ThreadCollectorFactory collectorFactory = (ThreadCollectorFactory) services.get(ThreadCollectorFactory.class.getName());
                ApplicationService applicationService = (ApplicationService) services.get(ApplicationService.class.getName());
                VmInfoDAO vmInfoDao = Objects.requireNonNull((VmInfoDAO) services.get(VmInfoDAO.class.getName()));
                ThreadViewProvider viewFactory = (ThreadViewProvider) services.get(ThreadViewProvider.class.getName());
                
                ThreadInformationService vmInfoService = new ThreadInformationServiceImpl(applicationService, vmInfoDao, collectorFactory, viewFactory);
                Dictionary<String, String> properties = new Hashtable<>();
                properties.put(Constants.GENERIC_SERVICE_CLASSNAME, VmRef.class.getName());
                properties.put(InformationService.KEY_SERVICE_ID, ThreadInformationService.SERVICE_ID);
                registration = context.registerService(InformationService.class.getName(), vmInfoService, properties);
            }

            @Override
            public void dependenciesUnavailable() {
                registration.unregister();
            }
        };
        
        MultipleServiceTracker tracker = new MultipleServiceTracker(context, classes, action);
        tracker.open();
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {}
}

