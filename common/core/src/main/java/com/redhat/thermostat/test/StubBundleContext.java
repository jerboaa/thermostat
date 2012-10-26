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

package com.redhat.thermostat.test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.test.StubBundleContext.ServiceInformation;

public class StubBundleContext implements BundleContext {

    static class ServiceInformation {
        public String serviceInterface;
        public Object implementation;
        public Dictionary properties;
        public int exportedReferences;

        public ServiceInformation(String className, Object impl, Dictionary props) {
            this.serviceInterface = className;
            this.implementation = impl;
            this.properties = props;
        }
    }

    static class ListenerSpec {
        public ServiceListener listener;
        public Filter filter;

        public ListenerSpec(ServiceListener listener, Filter filter) {
            this.listener = listener;
            this.filter = filter;
        }
    }

    private List<Bundle> bundles = new ArrayList<>();
    private List<ServiceInformation> registeredServices = new ArrayList<>();
    private List<ListenerSpec> registeredListeners = new ArrayList<>();

    /*
     * Interface methods
     */

    @Override
    public String getProperty(String key) {
        throw new NotImplementedException();
    }

    @Override
    public Bundle getBundle() {
        throw new NotImplementedException();
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
        return bundles.get((int) id);
    }

    @Override
    public Bundle[] getBundles() {
        throw new NotImplementedException();
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        ListenerSpec spec = new ListenerSpec(listener, createFilter(filter));
        registeredListeners.add(spec);
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        throw new NotImplementedException();
    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        Iterator<ListenerSpec> iter = registeredListeners.iterator();
        while (iter.hasNext()) {
            ListenerSpec item = iter.next();
            if (item.listener == listener) {
                iter.remove();
            }
        }
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
    public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
        throw new NotImplementedException();
    }

    @Override
    public ServiceRegistration registerService(String className, Object service, Dictionary properties) {
        ServiceInformation info = new ServiceInformation(className, service, properties);
        registeredServices.add(info);
        return new StubServiceRegistration(this, info);
    }

    @Override
    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        Filter toMatch = createFilter(filter);
        List<ServiceReference> toReturn = new ArrayList<>();
        for (ServiceInformation info : registeredServices) {
            // how does filter matching in OSGI work again?
            if (info.serviceInterface.equals(clazz) && toMatch.match(info.properties)) {
                toReturn.add(new StubServiceReference(info));
            }
        }
        return toReturn.toArray(new ServiceReference[0]);
    }

    @Override
    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        throw new NotImplementedException();
    }

    @Override
    public ServiceReference getServiceReference(String clazz) {
        throw new NotImplementedException();
    }

    @Override
    public ServiceReference getServiceReference(Class clazz) {
        for (ServiceInformation info : registeredServices) {
            if (info.serviceInterface.equals(clazz.getName())) {
                return new StubServiceReference(info);
            }
        }
        return null;
    }

    @Override
    public Collection getServiceReferences(Class clazz, String filter) throws InvalidSyntaxException {
        throw new NotImplementedException();
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
        info.exportedReferences--;
        return true;
    }

    @Override
    public File getDataFile(String filename) {
        throw new NotImplementedException();
    }

    @Override
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        return new StubFilter(filter);
    }

    @Override
    public Bundle getBundle(String location) {
        throw new NotImplementedException();
    }

    /*
     * Our custom methods
     */

    public void setBundle(int i, Bundle bundle) {
        bundles.add(i, bundle);
    }

    public boolean isServiceRegistered(String serviceName, Class<?> implementationClass) {
        for (ServiceInformation info : registeredServices) {
            if (info.serviceInterface.equals(serviceName) && info.implementation.getClass().equals(implementationClass)) {
                return true;
            }
        }
        return false;
    }

    public Collection<ServiceInformation> getAllServices() {
        return registeredServices;
    }

    public Collection<ListenerSpec> getServiceListeners() {
        return registeredListeners;
    }

    public void removeService(ServiceInformation info) {
        if (!registeredServices.contains(info)) {
            throw new IllegalStateException("service not registered");
        }
        registeredServices.remove(info);

    }

    public int getExportedServiceCount(ServiceRegistration registration) {
        StubServiceRegistration reg = (StubServiceRegistration) registration;
        return reg.getInfo().exportedReferences;
    }


}
