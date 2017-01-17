/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.platform.internal.mvc.lifecycle;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ThermostatExtensionRegistry;
import com.redhat.thermostat.common.ThermostatExtensionRegistry.Action;
import com.redhat.thermostat.platform.mvc.MVCProvider;

public class MVCRegistry {
    
    MVCRegistryHandler handler;
    
    public MVCRegistry() {
        this(createRegistryHandler());
    }
    
    // Testing hook
    MVCRegistry(MVCRegistryHandler handler) {
        this.handler = handler;
    }
    
    private static MVCRegistryHandler createRegistryHandler() {
        BundleContext context =
                FrameworkUtil.getBundle(MVCRegistry.class).getBundleContext();

        try {
            return new MVCRegistryHandler(context);
            
        } catch (InvalidSyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    public void start() {
        handler.start();
    }
    
    public void stop() {
        handler.stop();
    }
    
    public void addMVCRegistryListener(ActionListener<Action> l) {
        this.handler.addActionListener(l);
    }
    
    public void removeActionListener(ActionListener<Action> l) {
        this.handler.removeActionListener(l);
    }
    
    static class MVCRegistryHandler extends ThermostatExtensionRegistry<MVCProvider> {
        private static final String FILTER =
                "(" + Constants.OBJECTCLASS + "=" +
                        MVCProvider.class.getName() + ")";
        
        public MVCRegistryHandler(BundleContext context) throws InvalidSyntaxException {
            super(context, FILTER, MVCProvider.class);
        }
    }
}
