/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.thread.harvester.osgi;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.thread.dao.ThreadDao;
import com.redhat.thermostat.thread.harvester.ThreadBackend;
import com.redhat.thermostat.thread.harvester.ThreadHarvester;

public class Activator implements BundleActivator {
    
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(24);

    private MultipleServiceTracker connectionPoolTracker;
    private ServiceTracker threadDaoTracker;
    private ServiceRegistration backendRegistration;

    private ReceiverRegistry registry;
    private ThreadHarvester harvester;
    private ThreadBackend backend;
    
    @Override
    public void start(final BundleContext context) throws Exception {
        
        Class<?>[] deps = new Class<?>[] {
                MXBeanConnectionPool.class,
                WriterID.class,
        };
        connectionPoolTracker = new MultipleServiceTracker(context, deps, new Action() {
            
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                MXBeanConnectionPool pool = (MXBeanConnectionPool) services.get(MXBeanConnectionPool.class.getName());
                WriterID writerId = (WriterID) services.get(WriterID.class.getName());
                harvester = new ThreadHarvester(executor, pool, writerId);
            }

            @Override
            public void dependenciesUnavailable() {
                harvester = null;
            }
        });
        connectionPoolTracker.open();

        registry = new ReceiverRegistry(context);
        VmStatusListenerRegistrar vmListener = new VmStatusListenerRegistrar(context);

        /*
         * dont register anything just yet, let the backend handle the
         * registration, deregistration it when it's activated or deactivated
         */

        backend = new ThreadBackend(new Version(context.getBundle()), vmListener, registry, harvester);
        backendRegistration = context.registerService(Backend.class, backend, null);

        threadDaoTracker = new ServiceTracker(context, ThreadDao.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                ThreadDao threadDao = (ThreadDao) context.getService(reference);
                harvester.setThreadDao(threadDao);
                return super.addingService(reference);
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                if (harvester != null) {
                    harvester.setThreadDao(null);
                }
                context.ungetService(reference);
                super.removedService(reference, service);
            }
        };
        threadDaoTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (backend.isActive()) {
            backend.deactivate();
        }

        backendRegistration.unregister();

        connectionPoolTracker.close();
        threadDaoTracker.close();

        if (executor != null) {
            executor.shutdown();
        }        
    }
}

