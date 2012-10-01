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

package com.redhat.thermostat.common.utils;

import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class OSGIUtils {
    
	// TODO: Maybe we should stick this singleton into an ApplicationContext?
    private static OSGIUtils instance = new OSGIUtils();
    public static OSGIUtils getInstance() {
        return instance;
    }

    // This is only here to be used by test code.
    public static void setInstance(OSGIUtils utils) {
    	instance = utils;
    }

    /**
     * Gets a service. If no matching service is available, throw a NullPointerException.
     *
     * @param serviceType the class for the service
     * @return The best matching service implementation for the requested serviceType.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <E extends Object> E getService(Class<E> serviceType) {
        BundleContext ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference ref = ctx.getServiceReference(serviceType.getName());
        return (E) ctx.getService(ref);
    }
    
    /**
     * Gets a service. If no matching service is available, return {@code null}.
     *
     * @param serviceType the class for the service
     * @return The best matching service implementation for the requested serviceType or null.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <E extends Object> E getServiceAllowNull(Class<E> serviceType) {
        BundleContext ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference ref = ctx.getServiceReference(serviceType.getName());
        if (ref == null) {
            return null;
        }
        return (E) ctx.getService(ref);
    }
    
    public <E extends Object> void ungetService(Object service) {
        BundleContext ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
        Class<?> serviceType = service.getClass();
        // FIXME the reference returned now may be different from a previous invocation
        ServiceReference ref = ctx.getServiceReference(serviceType.getName());
        ctx.ungetService(ref);
    }

    @SuppressWarnings("rawtypes")
    public <E extends Object> ServiceRegistration registerService(Class<? extends E> serviceType, E service) {
        return registerService(serviceType, service, null);
    }
        
    @SuppressWarnings("rawtypes")
    public <E extends Object> ServiceRegistration registerService(Class<? extends E> serviceType, E service, Dictionary<String, ?> properties) {
        BundleContext ctx = FrameworkUtil.getBundle(getClass()).getBundleContext();
        return ctx.registerService(serviceType.getName(), service, properties);
    }

}
