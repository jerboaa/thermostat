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

package com.redhat.thermostat.launcher.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.redhat.thermostat.common.CommandLoadingBundleActivator;
import com.redhat.thermostat.common.Configuration;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandInfoSource;
import com.redhat.thermostat.launcher.BundleManager;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.utils.keyring.Keyring;

public class Activator extends CommandLoadingBundleActivator {
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    class RegisterLauncherCustomizer implements ServiceTrackerCustomizer {

        private ServiceRegistration launcherReg;
        private ServiceRegistration bundleManReg;
        private ServiceRegistration cmdInfoReg;
        private BundleContext context;
        private BundleManager bundleService;

        RegisterLauncherCustomizer(BundleContext context, BundleManager bundleService) {
            this.context = context;
            this.bundleService = bundleService;
        }

        @Override
        public Object addingService(ServiceReference reference) {
            // keyring is now ready
            Keyring keyring = (Keyring)context.getService(reference);
            // Register Launcher service since FrameworkProvider is waiting for it blockingly.
            CommandInfoSourceImpl commands = new CommandInfoSourceImpl(bundleService.getConfiguration().getThermostatHome());
            cmdInfoReg = context.registerService(CommandInfoSource.class, commands, null);
            bundleService.setCommandInfoSource(commands);
            LauncherImpl launcher = new LauncherImpl(context,
                    new CommandContextFactory(context), bundleService);
            launcherReg = context.registerService(Launcher.class.getName(), launcher, null);
            bundleManReg = context.registerService(BundleManager.class, bundleService, null);
            return keyring;
        }

        @Override
        public void modifiedService(ServiceReference reference, Object service) {
            // nothing
        }

        @Override
        public void removedService(ServiceReference reference, Object service) {
            // Keyring is gone, remove launcher, et. al. as well
            launcherReg.unregister();
            bundleManReg.unregister();
            cmdInfoReg.unregister();
        }

    }

    @SuppressWarnings("rawtypes")
    private ServiceTracker serviceTracker;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        BundleManager bundleService = new BundleManagerImpl(new Configuration());
        ServiceTrackerCustomizer customizer = new RegisterLauncherCustomizer(context, bundleService);
        serviceTracker = new ServiceTracker(context, Keyring.class, customizer);
        // Track for Keyring service.
        serviceTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        if (serviceTracker != null) {
            serviceTracker.close();
        }
    }
}
