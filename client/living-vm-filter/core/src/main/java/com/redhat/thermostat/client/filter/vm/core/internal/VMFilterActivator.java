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

package com.redhat.thermostat.client.filter.vm.core.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.filter.vm.core.LivingHostFilter;
import com.redhat.thermostat.client.filter.vm.core.LivingVMFilter;
import com.redhat.thermostat.client.ui.MenuAction;
import com.redhat.thermostat.client.ui.ReferenceFilter;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

public class VMFilterActivator implements BundleActivator {

    @SuppressWarnings("rawtypes")
    private final List<ServiceRegistration> registeredServices =
        Collections.synchronizedList(new ArrayList<ServiceRegistration>());

    private MultipleServiceTracker tracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        
        Class<?> [] services =  new Class<?> [] {
            VmInfoDAO.class,
            HostInfoDAO.class,
        };
        
        tracker = new MultipleServiceTracker(context, services, new MultipleServiceTracker.Action() {
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                @SuppressWarnings("rawtypes")
                ServiceRegistration registration = null;
                
                VmInfoDAO vmDao = (VmInfoDAO) services.get(VmInfoDAO.class.getName());
                HostInfoDAO hostDao = (HostInfoDAO) services.get(HostInfoDAO.class.getName());

                LivingHostFilter hostFilter = new LivingHostFilter(hostDao);
                registration = context.registerService(ReferenceFilter.class.getName(), hostFilter, null);
                registeredServices.add(registration);

                LivingVMFilter vmFilter = new LivingVMFilter(vmDao, hostDao);
                registration = context.registerService(ReferenceFilter.class.getName(), vmFilter, null);
                registeredServices.add(registration);

                LivingVMFilterMenuAction vmMenu = new LivingVMFilterMenuAction(vmFilter);
                registration = context.registerService(MenuAction.class.getName(), vmMenu, null);                
                registeredServices.add(registration);
                
                LivingHostFilterMenuAction hostMenu = new LivingHostFilterMenuAction(hostFilter);
                registration = context.registerService(MenuAction.class.getName(), hostMenu, null);                
                registeredServices.add(registration);
            }
            
            @Override
            public void dependenciesUnavailable() {
                @SuppressWarnings("rawtypes")
                Iterator<ServiceRegistration> iterator = registeredServices.iterator();
                while(iterator.hasNext()) {
                    
                    @SuppressWarnings("rawtypes")
                    ServiceRegistration registration = iterator.next();
                    registration.unregister();
                    iterator.remove();
                }
            }
        });
        tracker.open();
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
        for (ServiceRegistration registration : registeredServices) {
            registration.unregister();
        }
    }
    
}

