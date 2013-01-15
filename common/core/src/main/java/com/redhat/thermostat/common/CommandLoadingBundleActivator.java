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

package com.redhat.thermostat.common;

import java.util.ServiceLoader;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.cli.CommandRegistryImpl;

/**
 * Superclass for activators that need to register commands.  The bundle for this
 * activator should contain a META-INF/services/com.redhat.thermostat.common.cli.Command
 * file containing the class names that should be loaded as commands.  If this activator
 * also needs to create/destroy other resources during start() and stop(), be sure to
 * call super()
 */
public abstract class CommandLoadingBundleActivator implements BundleActivator {

    private CommandRegistry registry;
    private ServiceLoader<Command> commands;

    @Override
    public void start(BundleContext context) throws Exception {
        registry = new CommandRegistryImpl(context);
        commands = ServiceLoader.load(Command.class, getClass().getClassLoader());
        registry.registerCommands(commands);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (registry != null && commands != null) {
            registry.unregisterCommands();
        }
    }

}
