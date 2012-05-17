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

package com.redhat.thermostat.client.killvm;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.client.osgi.service.ContextAction;
import com.redhat.thermostat.client.osgi.service.VMContextAction;
import com.redhat.thermostat.service.process.UNIXProcessHandler;

public class Activator implements BundleActivator {

    private static final Logger logger = Logger.getLogger(Activator.class.getSimpleName());
    
    @Override
    public void start(final BundleContext context) throws Exception {

        // FIXME: there should be a better way than this
        // also, we need to be prepared that the unix service may disappear...
        ServiceListener listener = new ServiceListener() {
            
            private UNIXProcessHandler unixService;
            private boolean[] loaded = new boolean[2];
            
            @Override
            public void serviceChanged(ServiceEvent event) {
                
                ServiceReference reference = event.getServiceReference();
                Object service = context.getService(reference);
                switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                    if (service instanceof ContextAction) {
                        loaded[0] = true;
                    } else if (service instanceof UNIXProcessHandler) {
                        loaded[1] = true;
                        unixService = (UNIXProcessHandler) service;
                    }
                    break;

                default:
                    break;
                }
                
                if (loaded[0] && loaded[1]) {
                    context.registerService(VMContextAction.class.getName(),
                                            new KillVMAction(unixService), null);
                }
            }
        };
        
        try {
            String filter = "(|(objectClass=" + ContextAction.class.getName() + 
                            ")(objectClass=" + UNIXProcessHandler.class.getName() + "))";
                    
            context.addServiceListener(listener, filter);
            ServiceReference[] services = context.getServiceReferences(null, null);
            if (services != null) {
                for(int i = 0; i < services.length; i++) {
                    listener.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, services[i]));
                }
            }
            
        } catch (Exception e) {
           logger.log(Level.WARNING, "Failed to set up listener for http", e);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        /* nothing to do here */
    }
}
