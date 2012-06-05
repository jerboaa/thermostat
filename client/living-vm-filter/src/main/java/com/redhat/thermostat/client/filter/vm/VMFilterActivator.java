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

package com.redhat.thermostat.client.filter.vm;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.client.osgi.service.Filter;
import com.redhat.thermostat.client.osgi.service.MenuAction;

public class VMFilterActivator implements BundleActivator {
    
    @Override
    public void start(BundleContext context) throws Exception {
        
        ServiceTracker tracker = new ServiceTracker(context, ApplicationService.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                Hashtable<String, String> props = new Hashtable<>();
                props.put(MenuAction.PARENT_MENU, LivingVMFilterMenuAction.PARENT_MENU);
                
                ApplicationService service = (ApplicationService) context.getService(reference);
                LivingVMFilter filter = new LivingVMFilter(service.getDAOFactory());
                LivingVMFilterMenuAction menu = new LivingVMFilterMenuAction(filter);
                
                context.registerService(Filter.class.getName(), filter, null);
                context.registerService(MenuAction.class.getName(), menu, props);
                return super.addingService(reference);
            }
        };
        tracker.open();
    }
    
    @Override
    public void stop(BundleContext context) throws Exception {
        
    }
}
