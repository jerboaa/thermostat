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

package com.redhat.thermostat.setup.command.internal;

import java.util.Map;

import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.service.process.UNIXProcessHandler;
import com.redhat.thermostat.shared.config.CommonPaths;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.utils.keyring.Keyring;

public class Activator implements BundleActivator {

    private CommandRegistry reg;
    private MultipleServiceTracker tracker;
    private SetupCommand setupCommand;

    @Override
    public void start(final BundleContext context) throws Exception {
        reg = new CommandRegistryImpl(context);
        setupCommand = new SetupCommand();
        reg.registerCommand("setup", setupCommand);

        Class<?>[] deps = new Class<?>[] {
                CommonPaths.class,
                Launcher.class,
                Keyring.class,
                UNIXProcessHandler.class,
        };
        tracker = new MultipleServiceTracker(context, deps, new MultipleServiceTracker.Action() {
            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                CommonPaths paths = (CommonPaths) services.get(CommonPaths.class.getName());
                Launcher launcher = (Launcher) services.get(Launcher.class.getName());
                Keyring keyring = (Keyring) services.get(Keyring.class.getName());
                UNIXProcessHandler processHandler = (UNIXProcessHandler) services.get(UNIXProcessHandler.class.getName());
                setupCommand.setPaths(paths);
                setupCommand.setLauncher(launcher);
                setupCommand.setKeyring(keyring);
                setupCommand.setProcessHandler(processHandler);
            }

            @Override
            public void dependenciesUnavailable() {
                setupCommand.setServicesUnavailable();
                reg.unregisterCommands();
            }
        });
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        reg.unregisterCommands();
        tracker.close();
        setupCommand = null;
    }
}