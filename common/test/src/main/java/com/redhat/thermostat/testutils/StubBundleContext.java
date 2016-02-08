/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.testutils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * An implementation of BundleContext that's useful for writing unit tests.
 * <p>
 * WARNING: if you static mock {@link FrameworkUtil#createFilter(String)}, you
 * are going to have a bad time.
 */
public class StubBundleContext implements BundleContext {

    static class ServiceInformation {
        public Object implementation;
        public Dictionary properties;
        public int exportedReferences;

        public ServiceInformation(Object impl, Dictionary props) {
            this.implementation = impl;
            this.properties = props;
        }
    }

    private int nextServiceId = 0;

    private Map<String, String> frameworkProperties = new HashMap<>();
    private List<Bundle> bundles = new ArrayList<>();
    private List<ServiceInformation> registeredServices = new ArrayList<>();
    private Map<ServiceListener, Filter> registeredListeners = new HashMap<>();
    private Bundle contextBundle = null;

    /*
     * Interface methods
     */

    @Override
    public String getProperty(String key) {
        String result = null;
        result = frameworkProperties.get(key);
        if (result == null) {
            result = System.getProperty(key);
        }
        return result;
    }

    @Override
    public Bundle getBundle() {
        return contextBundle;
    }

    @Override
    public Bundle installBundle(String location, InputStream input) throws BundleException {
        throw new NotImplementedException();
    }

    @Override
    public Bundle installBundle(String location) throws BundleException {
        throw new NotImplementedException();
    }

    @Override
    public Bundle getBundle(long id) {
        if (id > Integer.MAX_VALUE) {
            throw new NotImplementedException();
        }
        if (id >= bundles.size()) {
            return null;
        }

        return bundles.get((int) id);
    }

    @Override
    public Bundle getBundle(String location) {
        throw new NotImplementedException();
    }

    @Override
    public Bundle[] getBundles() {
        return bundles.toArray(new Bundle[bundles.size()]);
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        registeredListeners.put(listener, filter == null ? null : createFilter(filter));
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        try {
            addServiceListener(listener, null);
        } catch (InvalidSyntaxException e) {
            throw new AssertionError("a null filter can not have invalid systax");
        }
    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        registeredListeners.remove(listener);
    }

    @Override
    public void addBundleListener(BundleListener listener) {
        throw new NotImplementedException();
    }

    @Override
    public void removeBundleListener(BundleListener listener) {
        throw new NotImplementedException();
    }

    @Override
    public void addFrameworkListener(FrameworkListener listener) {
        throw new NotImplementedException();
    }

    @Override
    public void removeFrameworkListener(FrameworkListener listener) {
        throw new NotImplementedException();
    }

    @Override
    public ServiceRegistration registerService(Class clazz, Object service, Dictionary properties) {
        return registerService(clazz.getName(), service, properties);
    }

    @Override
    public ServiceRegistration registerService(String className, Object service, Dictionary properties) {
        return registerService(new String[] { className }, service, properties);
    }

    @Override
    public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
        if (service instanceof ServiceFactory) {
            throw new NotImplementedException("support for service factories is not implemented");
        }

        for (String className : clazzes) {
            try {
                Class<?> clazz = Class.forName(className);
                if (!clazz.isAssignableFrom(service.getClass())) {
                    throw new IllegalArgumentException("service is not a subclass of " + className);
                }
            } catch (ClassNotFoundException classNotFound) {
                throw new IllegalArgumentException("not a valid class: " + className);
            }
        }

        Object specifiedRanking = null;
        Hashtable<String, Object> newProperties = new Hashtable<>();
        if (properties != null) {
            Enumeration<?> enumeration = properties.keys();
            while (enumeration.hasMoreElements()) {
                Object key = enumeration.nextElement();
                newProperties.put((String)key, properties.get(key));
            }
            specifiedRanking = properties.get(Constants.SERVICE_RANKING);
        }

        newProperties.put(Constants.OBJECTCLASS, clazzes);
        newProperties.put(Constants.SERVICE_ID, nextServiceId);
        nextServiceId++;
        if (specifiedRanking == null || !(specifiedRanking instanceof Integer)) {
            specifiedRanking = 0;
        }
        newProperties.put(Constants.SERVICE_RANKING, (Integer) specifiedRanking);

        ServiceInformation info = new ServiceInformation(service, newProperties);
        registeredServices.add(info);

        notifyServiceChange(new StubServiceReference(info, contextBundle), true);

        return new StubServiceRegistration(this, info);
    }

    @Override
    public ServiceReference getServiceReference(Class clazz) {
        return getServiceReference(clazz.getName());
    }

    @Override
    public ServiceReference getServiceReference(String clazz) {
        try {
            ServiceReference[] initial = getServiceReferences(clazz, null);
            if (initial == null) {
                return null;
            }

            Arrays.sort(initial);
            return initial[initial.length-1];
        } catch (InvalidSyntaxException invalidFilterSyntax) {
            throw new AssertionError("a null filter can not have an invalid syntax");
        }
    }

    @Override
    public Collection getServiceReferences(Class clazz, String filter) throws InvalidSyntaxException {
        return Arrays.asList(getServiceReferences(clazz.getName(), filter));
    }

    @Override
    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        ServiceReference[] allRefs = getAllServiceReferences(clazz, filter);
        if (allRefs == null) {
            return null;
        }

        List<ServiceReference> result = new ArrayList<>();
        for (ServiceReference ref : allRefs) {
            if (ref.isAssignableTo(contextBundle, clazz)) {
                result.add(ref);
            }
        }
        return result.toArray(new ServiceReference[0]);
    }

    @Override
    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        List<ServiceReference> toReturn = new ArrayList<>();

        Filter toMatch = (filter == null) ? null : createFilter(filter);

        for (ServiceInformation info : registeredServices) {
            for (String serviceInterface : (String[]) info.properties.get(Constants.OBJECTCLASS)) {
                if (clazz == null || serviceInterface.equals(clazz)) {
                    if (toMatch == null || toMatch.match(info.properties)) {
                        toReturn.add(new StubServiceReference(info, contextBundle));
                    }
                }
            }
        }

        if (toReturn.size() == 0) {
            return null;
        }
        return toReturn.toArray(new ServiceReference[0]);
    }


    @Override
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        Filter result = FrameworkUtil.createFilter(filter);
        if (result == null) {
            throw new AssertionError("FrameworkUtil created a null filter. Is it mocked incompletely?");
        }
        return result;
    }

    @Override
    public Object getService(ServiceReference reference) {
        StubServiceReference ref = (StubServiceReference) reference;
        ServiceInformation info = ref.getInformation();
        info.exportedReferences++;
        return info.implementation;
    }

    @Override
    public boolean ungetService(ServiceReference reference) {
        StubServiceReference ref = (StubServiceReference) reference;
        ServiceInformation info = ref.getInformation();
        if (info.exportedReferences == 0) {
            return false;
        }
        if (!registeredServices.contains(info)) {
            return false;
        }
        info.exportedReferences--;
        return true;
    }

    @Override
    public File getDataFile(String filename) {
        throw new NotImplementedException();
    }

    /*
     * Our custom methods
     */
    public void setProperty(String key, String value) {
        frameworkProperties.put(key, value);
    }

    /** Set the context bundle */
    public void setBundle(Bundle bundle) {
        this.contextBundle = bundle;
    }

    /** Set the bundle associated with an id */
    public void setBundle(int i, Bundle bundle) {
        bundles.add(i, bundle);
    }

    public boolean isServiceRegistered(String serviceName, Class<?> implementationClass) {
        for (ServiceInformation info : registeredServices) {
            for (String serviceInterface : (String[]) info.properties.get(Constants.OBJECTCLASS)) {
                if (serviceInterface.equals(serviceName)
                        && info.implementation.getClass().equals(implementationClass)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @returns true if an instance of implementationClass is registered as
     * service serviceName with properties that is a superset of props
     */
    public boolean isServiceRegistered(String serviceName, Class<?> implementationClass, Dictionary<String,?> props) {
        for (ServiceInformation info : registeredServices) {
            for (String serviceInterface : (String[]) info.properties.get(Constants.OBJECTCLASS)) {
                if (serviceInterface.equals(serviceName)
                        && info.implementation.getClass().equals(implementationClass)) {
                    boolean propsMatch = true;
                    Enumeration<String> keys = props.keys();
                    while (keys.hasMoreElements()) {
                        String key = keys.nextElement();
                        if (!Objects.equals(props.get(key), info.properties.get(key))) {
                            propsMatch = false;
                            break;
                        }
                    }
                    if (propsMatch) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public Collection<ServiceInformation> getAllServices() {
        return registeredServices;
    }

    public Collection<ServiceListener> getServiceListeners() {
        return registeredListeners.keySet();
    }

    public void removeService(ServiceInformation info) {
        if (!registeredServices.contains(info)) {
            throw new IllegalArgumentException("service not registered");
        }
        registeredServices.remove(info);
        notifyServiceChange(new StubServiceReference(info, contextBundle), false);
    }

    private void notifyServiceChange(ServiceReference serviceReference, boolean registered) {
        int eventType = registered ? ServiceEvent.REGISTERED : ServiceEvent.UNREGISTERING;
        ServiceEvent event = new ServiceEvent(eventType, serviceReference);
        for (Entry<ServiceListener, Filter> entry : registeredListeners.entrySet()) {
            ServiceListener listener = entry.getKey();
            Filter filter = entry.getValue();
            if (filter == null || filter.match(serviceReference)) {
                listener.serviceChanged(event);
            }
        }
    }

    public int getExportedServiceCount(ServiceRegistration registration) {
        StubServiceRegistration reg = (StubServiceRegistration) registration;
        return reg.getInfo().exportedReferences;
    }


}

