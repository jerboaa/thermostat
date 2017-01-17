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

package com.redhat.thermostat.thread.harvester.internal;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendService;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.thread.dao.LockInfoDao;
import com.redhat.thermostat.thread.dao.ThreadDao;

public class Activator implements BundleActivator {
    private MultipleServiceTracker threadBackendTracker;
    private MultipleServiceTracker threadCountTracker;
    private MultipleServiceTracker lockInfoTracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        final Version VERSION = new Version(context.getBundle());
        final VmStatusListenerRegistrar VM_STATUS_REGISTRAR
            = new VmStatusListenerRegistrar(context);

        final Class<?>[] threadCountDeps = new Class<?>[] {
                BackendService.class,
                WriterID.class,
                ThreadDao.class,
        };
        threadCountTracker = new MultipleServiceTracker(context, threadCountDeps, new Action() {

            private ServiceRegistration<Backend> registration;
            private ThreadCountBackend threadCountBackend;

            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                WriterID writerId = services.get(WriterID.class);
                ThreadDao dao = services.get(ThreadDao.class);
                threadCountBackend = new ThreadCountBackend(dao, VERSION, VM_STATUS_REGISTRAR, writerId);
                registration = context.registerService(Backend.class, threadCountBackend, null);
            }

            @Override
            public void dependenciesUnavailable() {
                if (threadCountBackend.isActive()) {
                    threadCountBackend.deactivate();
                }
                registration.unregister();
            }
        });
        threadCountTracker.open();

        Class<?>[] lockInfoDeps = new Class<?>[] {
                BackendService.class,
                WriterID.class,
                LockInfoDao.class,
        };
        lockInfoTracker = new MultipleServiceTracker(context, lockInfoDeps, new Action() {

            private ServiceRegistration<Backend> registration;
            private LockInfoBackend lockInfoBackend;

            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                WriterID writerId = services.get(WriterID.class);
                LockInfoDao dao = services.get(LockInfoDao.class);
                lockInfoBackend = new LockInfoBackend(dao, VERSION, VM_STATUS_REGISTRAR, writerId);
                registration = context.registerService(Backend.class, lockInfoBackend, null);
            }

            @Override
            public void dependenciesUnavailable() {
                if (lockInfoBackend.isActive()) {
                    lockInfoBackend.deactivate();
                }
                if (registration != null) {
                    registration.unregister();
                }
            }
        });
        lockInfoTracker.open();

        Class<?>[] deps = new Class<?>[] {
                BackendService.class,
                MXBeanConnectionPool.class,
                WriterID.class,
                ThreadDao.class,
        };
        threadBackendTracker = new MultipleServiceTracker(context, deps, new Action() {

            private ServiceRegistration<Backend> registration;
            private ThreadBackend threadBackend;
            private ScheduledExecutorService executor;

            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                MXBeanConnectionPool pool = services.get(MXBeanConnectionPool.class);
                WriterID writerId = services.get(WriterID.class);
                ThreadDao threadDao = services.get(ThreadDao.class);

                executor = Executors.newScheduledThreadPool(24);
                ThreadHarvester harvester = new ThreadHarvester(executor, pool, writerId);
                harvester.setThreadDao(threadDao);

                ReceiverRegistry registry = new ReceiverRegistry(context);
                threadBackend = new ThreadBackend(VERSION, VM_STATUS_REGISTRAR, registry, harvester);
                registration = context.registerService(Backend.class, threadBackend, null);
            }

            @Override
            public void dependenciesUnavailable() {
                if (executor != null) {
                    executor.shutdown();
                }

                if (threadBackend.isActive()) {
                    threadBackend.deactivate();
                }
                if (registration != null) {
                    registration.unregister();
                }
            }
        });
        threadBackendTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        threadCountTracker.close();
        lockInfoTracker.close();
        threadBackendTracker.close();
    }
}

