/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.client.command.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.redhat.thermostat.client.command.RequestQueue;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class Activator implements BundleActivator {

    private RequestQueueImpl queue;
    private ServiceRegistration queueRegistration;
    private ConfigurationRequestContext configContext;
    private CommandRegistryImpl reg;
    private ServiceTracker queueDepsTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        queueDepsTracker = new ServiceTracker(context, SSLConfiguration.class, new DepsCustomizer(context));
        queueDepsTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (queueDepsTracker != null) {
            queueDepsTracker.close();
            queueDepsTracker = null;
        }
        deactivate();
    }

    private class DepsCustomizer implements ServiceTrackerCustomizer {

        private BundleContext context;

        DepsCustomizer(BundleContext context) {
            this.context = context;
        }

        @Override
        public Object addingService(ServiceReference reference) {
            SSLConfiguration sslConf = (SSLConfiguration) context.getService(reference);
            activate(sslConf, context);
            return sslConf;
        }

        @Override
        public void modifiedService(ServiceReference reference, Object service) {
            // Do nothing
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            deactivate();
            context.ungetService(reference);
        }
    }

    private synchronized void activate(SSLConfiguration sslConf, BundleContext context) {
        configContext = new ConfigurationRequestContext(sslConf);
        queue = new RequestQueueImpl(configContext);
        queueRegistration = context.registerService(RequestQueue.class.getName(), queue, null);
        queue.startProcessingRequests();
        reg = new CommandRegistryImpl(context);
        reg.registerCommand("ping", new PingCommand());
    }

    private synchronized void deactivate() {
        if (reg != null) {
            reg.unregisterCommands();
            reg = null;
        }
        if (queue != null) {
            queue.stopProcessingRequests();
            queue = null;
        }
        if (queueRegistration != null) {
            queueRegistration.unregister();
            queueRegistration = null;
        }
        configContext.getBootstrap().group().shutdownGracefully();
    }
}

