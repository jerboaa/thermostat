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

package com.redhat.thermostat.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * 
 *
 * @param <T> The type of service which this registry works with.
 */
public class ServiceRegistry<T> {

    // Commonly used property keys.
    public static final String SERVICE_NAME = "servicename";
    
    private final BundleContext context;
    private final String registrationClass;
    @SuppressWarnings("rawtypes")
    private final List<ServiceRegistration> ourRegistrations;

    private static final Logger log = LoggingUtils.getLogger(ServiceRegistry.class);

    public ServiceRegistry(BundleContext ctx, String registrationClassName) {
        context = ctx;
        registrationClass = registrationClassName;
        ourRegistrations = new ArrayList<>();
    }

    /**
     * Register the implementation of T to the OSGi context, with the given properties.
     * @param service The implementation to be registered.
     * @param props The additional properties which the service should have.
     */
    public void registerService(T service, Dictionary<String, ?> props) {
        ServiceRegistration registration = context.registerService(registrationClass, service, props);
        ourRegistrations.add(registration);
    }

    /**
     * Equivalent to calling registerService(service, props) with a dictionary containing
     * an entry for SERVICE_NAME with value as name, in adddition to the contents of props
     * 
     * @param service The implementation to be registered.
     * @param name The name to be added as SERVICE_NAME property.
     * @param props Additional properties which the service should be registered with.
     */
    public void registerService(T service, String name, Dictionary<String, String> props) {
        if (props == null) {
            props = new Hashtable<String, String>();
        }
        props.put(SERVICE_NAME, name);
        registerService(service, props);
    }

    /**
     * Equivalent to calling registerService(service, name, props) with null or empty props.
     * 
     * @param service The implementation to be registered.
     * @param name The name to be added as SERVICE_NAME property.
     */
    public void registerService(T service, String name) {
        Hashtable<String, String> props = new Hashtable<>();
        props.put(SERVICE_NAME, name);
        registerService(service, props);
    }

    /**
     * Unregister all of the services that have been registered through this registry.
     */
    @SuppressWarnings("rawtypes")
    public void unregisterAll() {
        Iterator<ServiceRegistration> iter = ourRegistrations.iterator();
        while (iter.hasNext()) {
            ServiceRegistration registration = iter.next();
            registration.unregister();
            iter.remove();
        }
    }

    /**
     * Get a implementation of T matching the properties.
     * 
     * @param props The properties which the returned T implementation must match.
     * @return A matching T implementation, if any.  Otherwise, null.
     */
    public T getService(Dictionary<String, String> props) {
        ServiceReference[] refs = getServiceRefs(filterFromProps(props));
        if (refs == null || refs.length == 0) {
            return null;
        } else if (refs.length > 1) {
            log.info("More than one matching service implementation found.");
        }
        ServiceReference ref = refs[0];
        return (T) context.getService(ref);
    }

    /**
     * Equivalent to calling getService(props) with a dictionary containing an entry for
     * SERVICE_NAME with value as name.
     * 
     * @param name The name for the service which should be returned.
     * @return An implementation of T matching the supplied name, if any.  Otherwise, null.
     */
    public T getService(String name) {
        Hashtable<String, String> props = new Hashtable<>();
        props.put(SERVICE_NAME, name);
        return getService(props);
    }

    private String filterFromProps(Dictionary<String, String> props) {
        StringBuilder builder = new StringBuilder();
        builder.append("(&(objectclass=*)");
        if (props != null) {
            for (Enumeration<String> e = props.keys(); e.hasMoreElements();) {
                String key = e.nextElement();
                String value = props.get(key);
                builder.append("(");
                builder.append(key);
                builder.append("=");
                builder.append(value);
                builder.append(")");
            }
        }
        builder.append(")");
        return builder.toString();
    }

    /**
     * Get all implementations of T which have been registered.  This may include ones not
     * registered through this registry instance.
     * 
     * @return A collection of all registered T implementations.
     */
    public Collection<T> getRegisteredServices() {
        ServiceReference[] refs = getServiceRefs(null);
        List<T> services = new ArrayList<>();
        for (ServiceReference ref : refs) {
            T service = (T) context.getService(ref);
            services.add(service);
        }
        return services;
    }

    @SuppressWarnings("unchecked")
    private ServiceReference[] getServiceRefs(String filter) {
        ServiceReference[] refs;
        try {
            refs = context.getServiceReferences(registrationClass, filter);
        } catch (InvalidSyntaxException e) {
            throw (InternalError) new InternalError().initCause(e);
        }
        return refs;
    }

}

