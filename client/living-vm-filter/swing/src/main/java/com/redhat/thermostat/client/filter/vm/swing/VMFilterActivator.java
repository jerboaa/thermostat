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

package com.redhat.thermostat.client.filter.vm.swing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.redhat.thermostat.client.filter.host.swing.HostNetworkInterfaceLabelMenuAction;
import com.redhat.thermostat.client.ui.MenuAction;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.filter.host.swing.DeadHostIconDecorator;
import com.redhat.thermostat.client.filter.host.swing.HostIconDecorator;
import com.redhat.thermostat.client.filter.host.swing.HostNetworkInterfaceLabelDecorator;
import com.redhat.thermostat.client.filter.host.swing.HostVmMainLabelDecorator;
import com.redhat.thermostat.client.filter.host.swing.ThermostatVmMainLabelDecorator;
import com.redhat.thermostat.client.swing.ReferenceFieldDecoratorLayout;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.ui.ReferenceFieldIconDecorator;
import com.redhat.thermostat.client.ui.ReferenceFieldLabelDecorator;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.NetworkInterfaceInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;

public class VMFilterActivator implements BundleActivator {

    @SuppressWarnings("rawtypes")
    private final List<ServiceRegistration> registeredServices = Collections.synchronizedList(new ArrayList<ServiceRegistration>());

    private MultipleServiceTracker tracker;

    @SuppressWarnings("rawtypes")
    @Override
    public void start(final BundleContext context) throws Exception {
        
        Class<?> [] services =  new Class<?> [] {
                VmInfoDAO.class,
                HostInfoDAO.class,
                NetworkInterfaceInfoDAO.class,
                UIDefaults.class,
        };
        
        tracker = new MultipleServiceTracker(context, services, new MultipleServiceTracker.Action() {
            
            @Override
            public void dependenciesUnavailable() {
                Iterator<ServiceRegistration> iterator = registeredServices.iterator();
                while(iterator.hasNext()) {
                    ServiceRegistration registration = iterator.next();
                    registration.unregister();
                    iterator.remove();
                }
            }
            
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                ServiceRegistration registration = null;

                UIDefaults uiDefaults = (UIDefaults) services.get(UIDefaults.class.getName());
                
                VmInfoDAO vmDao = (VmInfoDAO) services.get(VmInfoDAO.class.getName());
                HostInfoDAO hostDao = (HostInfoDAO) services.get(HostInfoDAO.class.getName());

                Dictionary<String, String> decoratorProperties = new Hashtable<>();
                
                VMPidLabelDecorator vmPidLabelDecorator = new VMPidLabelDecorator(vmDao);
                decoratorProperties = new Hashtable<>();
                decoratorProperties.put(ReferenceFieldLabelDecorator.ID,
                                        ReferenceFieldDecoratorLayout.LABEL_INFO.name());

                registration = context.registerService(ReferenceFieldLabelDecorator.class.getName(),
                        vmPidLabelDecorator, decoratorProperties);

                registeredServices.add(registration);

                VMPidLabelMenuAction vmPidMenuAction = new VMPidLabelMenuAction(vmPidLabelDecorator);
                registration = context.registerService(MenuAction.class.getName(),
                        vmPidMenuAction, null);

                registeredServices.add(registration);

                VMStartTimeLabelDecorator vmStartTimeLabelDecorator = new VMStartTimeLabelDecorator(vmDao);
                decoratorProperties = new Hashtable<>();
                decoratorProperties.put(ReferenceFieldLabelDecorator.ID,
                        ReferenceFieldDecoratorLayout.LABEL_INFO.name());

                registration = context.registerService(ReferenceFieldLabelDecorator.class.getName(),
                        vmStartTimeLabelDecorator, decoratorProperties);

                registeredServices.add(registration);

                VMStartTimeLabelMenuAction vmStartTimeLabelMenuAction = new VMStartTimeLabelMenuAction(vmStartTimeLabelDecorator);
                registration = context.registerService(MenuAction.class.getName(),
                        vmStartTimeLabelMenuAction, null);

                registeredServices.add(registration);

                NetworkInterfaceInfoDAO networkDao = (NetworkInterfaceInfoDAO)
                            services.get(NetworkInterfaceInfoDAO.class.getName());

                HostNetworkInterfaceLabelDecorator hostLabelDecorator =
                            new HostNetworkInterfaceLabelDecorator(networkDao);
                decoratorProperties = new Hashtable<>();
                decoratorProperties.put(ReferenceFieldLabelDecorator.ID,
                                        ReferenceFieldDecoratorLayout.LABEL_INFO.name());
                
                registration = context.registerService(ReferenceFieldLabelDecorator.class.getName(),
                                                       hostLabelDecorator, decoratorProperties);
                registeredServices.add(registration);

                HostNetworkInterfaceLabelMenuAction hostNetworkMenuAction = new HostNetworkInterfaceLabelMenuAction(hostLabelDecorator);
                registration = context.registerService(MenuAction.class.getName(), hostNetworkMenuAction, null);

                registeredServices.add(registration);
                
                HostIconDecorator hostIconDecorator =
                            HostIconDecorator.createInstance(uiDefaults);
                DeadHostIconDecorator deadHostIconDecorator =
                            DeadHostIconDecorator.createInstance(hostDao, uiDefaults, hostIconDecorator);
                decoratorProperties = new Hashtable<>();
                decoratorProperties.put(ReferenceFieldIconDecorator.ID,
                                        ReferenceFieldDecoratorLayout.ICON_MAIN.name());
                
                registration = context.registerService(ReferenceFieldIconDecorator.class.getName(),
                                                       deadHostIconDecorator, decoratorProperties);
                registeredServices.add(registration);
                
                VMIconDecorator livingVMIconDecorator = VMIconDecorator.getInstance(uiDefaults);
                decoratorProperties = new Hashtable<>();
                decoratorProperties.put(ReferenceFieldIconDecorator.ID,
                                        ReferenceFieldDecoratorLayout.ICON_MAIN.name());
                
                registration = context.registerService(ReferenceFieldIconDecorator.class.getName(),
                                                       livingVMIconDecorator, decoratorProperties);
                registeredServices.add(registration);
                
                DeadVMIconDecorator deadVMIconDecorator =
                            DeadVMIconDecorator.getInstance(vmDao, hostDao, uiDefaults);
                decoratorProperties = new Hashtable<>();
                decoratorProperties.put(ReferenceFieldIconDecorator.ID,
                                        ReferenceFieldDecoratorLayout.ICON_MAIN.name());
                
                registration = context.registerService(ReferenceFieldIconDecorator.class.getName(),
                                                       deadVMIconDecorator, decoratorProperties);
                registeredServices.add(registration);
                
                HostVmMainLabelDecorator mainDecorator = new HostVmMainLabelDecorator();
                decoratorProperties = new Hashtable<>();
                decoratorProperties.put(ReferenceFieldLabelDecorator.ID,
                                        ReferenceFieldDecoratorLayout.LABEL_MAIN.name());
                
                registration = context.registerService(ReferenceFieldLabelDecorator.class.getName(),
                                                       mainDecorator, decoratorProperties);
                registeredServices.add(registration);
                
                ThermostatVmMainLabelDecorator thermostatDecorator = new ThermostatVmMainLabelDecorator(vmDao);
                decoratorProperties = new Hashtable<>();
                decoratorProperties.put(ReferenceFieldLabelDecorator.ID,
                        ReferenceFieldDecoratorLayout.LABEL_MAIN.name());
                
                registration = context.registerService(ReferenceFieldLabelDecorator.class.getName(),
                        thermostatDecorator, decoratorProperties);
                
                registeredServices.add(registration);

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

