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

package com.redhat.thermostat.vm.profiler.agent.internal;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.profiler.common.ProfileDAO;

public class Activator implements BundleActivator {

    private MultipleServiceTracker tracker;

    @Override
    public void start(final BundleContext context) throws Exception {
        Class<?>[] deps = new Class<?>[] {
                CommonPaths.class,
                MXBeanConnectionPool.class,
                ProfileDAO.class,
                WriterID.class,
        };
        tracker = new MultipleServiceTracker(context, deps, new MultipleServiceTracker.Action() {
            private ReceiverRegistry requestHandlerRegisteration;
            private VmStatusListenerRegistrar vmStatusRegistrar;
            private ProfilerRequestReceiver profileRequestHandler;
            private ProfilerVmStatusListener livenessListener;

            @Override
            public void dependenciesUnavailable() {
                requestHandlerRegisteration.unregisterReceivers();
                requestHandlerRegisteration = null;
                profileRequestHandler = null;

                vmStatusRegistrar.unregister(livenessListener);
                livenessListener = null;

            }

            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                CommonPaths path = services.get(CommonPaths.class);
                MXBeanConnectionPool pool = services.get(MXBeanConnectionPool.class);
                WriterID writerIdProvider = services.get(WriterID.class);
                ProfileDAO dao = services.get(ProfileDAO.class);
                String writerId = writerIdProvider.getWriterID();

                final Properties configuration = new Properties();
                try {
                    configuration.load(this.getClass().getResourceAsStream("settings.properties"));
                    prefixJarsWithThermostatHome(path, configuration);
                } catch (IOException e) {
                    throw new AssertionError("Missing resource:", e);
                }

                VmProfiler profiler = new VmProfiler(writerId, configuration, dao, pool);
                profileRequestHandler = new ProfilerRequestReceiver(profiler);

                requestHandlerRegisteration = new ReceiverRegistry(context);
                requestHandlerRegisteration.registerReceiver(profileRequestHandler);

                livenessListener = new ProfilerVmStatusListener(profiler);
                vmStatusRegistrar = new VmStatusListenerRegistrar(context);
                vmStatusRegistrar.register(livenessListener);
            }

            private void prefixJarsWithThermostatHome(CommonPaths path, final Properties configuration) {
                for (String name : configuration.stringPropertyNames()) {
                    String currentValue = configuration.getProperty(name);
                    if (currentValue.endsWith(".jar")) {
                        configuration.setProperty(name,
                                path.getSystemThermostatHome().getAbsolutePath() + File.separator + currentValue);
                    }
                }
            }

        });

        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }
}

