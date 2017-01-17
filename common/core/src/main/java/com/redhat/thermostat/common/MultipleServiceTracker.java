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

package com.redhat.thermostat.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * A {@link ServiceTracker} for multiple services. This class is intended to be
 * used within BundleActivator implementations that require some action be taken
 * only after certain services have appeared.
 */
public class MultipleServiceTracker {

    public static class DependencyProvider {

        private Map<String, Object> deps;

        public DependencyProvider(Map<String, Object> deps) {
            this.deps = deps;
        }

        public <T> T get(Class<T> klass) {
            T dep = (T) deps.get(klass.getName());
            return Objects.requireNonNull(dep);
        }

        // for testing only
        Map<String, Object> getDependencies() {
            return deps;
        }
    }

    public interface Action {
        public void dependenciesAvailable(DependencyProvider services);
        public void dependenciesUnavailable();
    }

    class InternalServiceTrackerCustomizer implements ServiceTrackerCustomizer {

        @Override
        public Object addingService(ServiceReference reference) {
            Object service = context.getService(reference);
            services.put(getServiceClassName(reference), context.getService(reference));
            if (allServicesReady()) {
                action.dependenciesAvailable(new DependencyProvider(services));
            }
            return service;
        }

        @Override
        public void modifiedService(ServiceReference reference, Object service) {
            // We don't actually need to do anything here.
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            if (servicesWillBecomeNotReady(getServiceClassName(reference))) {
                action.dependenciesUnavailable();
            }
            services.put(getServiceClassName(reference), null);
            context.ungetService(reference);
        }

        private String getServiceClassName(ServiceReference reference) {
            return ((String[]) reference.getProperty(org.osgi.framework.Constants.OBJECTCLASS))[0];
        }
    }

    private Map<String, Object> services;
    private Collection<ServiceTracker> trackers;
    private Action action;
    private BundleContext context;

    public MultipleServiceTracker(BundleContext context, Class<?>[] classes, Action action) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(classes);
        Objects.requireNonNull(action);
        this.context = context;
        services = new HashMap<>();
        trackers = new ArrayList<>();
        for (Class<?> clazz: classes) {
            InternalServiceTrackerCustomizer tc = new InternalServiceTrackerCustomizer();
            ServiceTracker tracker = new ServiceTracker(context, clazz.getName(), tc);
            trackers.add(tracker);
            services.put(clazz.getName(), null);
        }
        this.action = action;
    }

    public void open() {
        for (ServiceTracker tracker : trackers) {
            tracker.open();
        }
    }

    public void close() {
        for (ServiceTracker tracker: trackers) {
            tracker.close();
        }
    }

    private boolean allServicesReady() {
        for (Entry<String, Object> entry: services.entrySet()) {
            if (entry.getValue() == null) {
                return false;
            }
        }
        return true;
    }

    private boolean servicesWillBecomeNotReady(String serviceName) {
        return (allServicesReady() && services.containsKey(serviceName));
    }
}

