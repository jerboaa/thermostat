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

package com.redhat.thermostat.common;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A helper class to make it easier to implement registeries for osgi-services.
 */
public class ThermostatExtensionRegistry<E> {

    public enum Action {
        SERVICE_ADDED,
        SERVICE_REMOVED
    }
    
    private ActionNotifier<Action> actionNotifier = new ActionNotifier<>(this);
    
    @SuppressWarnings("rawtypes")
    private ServiceTracker tracker;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ThermostatExtensionRegistry(BundleContext context, String filter, final Class<E> classType) throws InvalidSyntaxException {

        tracker = new ServiceTracker(context, FrameworkUtil.createFilter(filter), null) {
            
            @Override
            public Object addingService(ServiceReference reference) {
                E service = (E) super.addingService(reference);
                
                actionNotifier.fireAction(Action.SERVICE_ADDED, service);
                return service;
            }
            
            @Override
            public void removedService(ServiceReference reference, Object service) {
                if (!classType.isAssignableFrom(service.getClass())) {
                    throw new AssertionError("removing a service of not matching type.");
                }
                
                actionNotifier.fireAction(Action.SERVICE_REMOVED, service);
                super.removedService(reference, service);
            }
        };
    }
    
    public void start() {
        tracker.open();
    }

    public void stop() {
        tracker.close();
    }
    
    public void addActionListener(ActionListener<Action> l) {
        actionNotifier.addActionListener(l);
    }

    public void removeActionListener(ActionListener<Action> l) {
        actionNotifier.removeActionListener(l);
    }
}

