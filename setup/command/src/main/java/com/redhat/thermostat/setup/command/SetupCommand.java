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

package com.redhat.thermostat.setup.command;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.internal.utils.laf.ThemeManager;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.setup.command.internal.SetupWindow;
import com.redhat.thermostat.setup.command.internal.model.ThermostatSetup;
import com.redhat.thermostat.setup.command.locale.LocaleResources;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.utils.keyring.Keyring;

public class SetupCommand extends AbstractCommand {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final String ORIG_CMD_ARGUMENT_NAME = "origArgs";
    private static final Logger logger = LoggingUtils.getLogger(SetupCommand.class);
    private final DependencyServices dependentServices = new DependencyServices();
    private SetupWindow mainWindow;
    private CommonPaths paths;
    private Launcher launcher;
    private Keyring keyring;
    private ThermostatSetup thermostatSetup;
    private String[] origArgsList;

    @Override
    public void run(CommandContext ctx) throws CommandException {
        Arguments args = ctx.getArguments();
        if (args.hasArgument(ORIG_CMD_ARGUMENT_NAME)) {
            String origArgs = args.getArgument(ORIG_CMD_ARGUMENT_NAME);
            origArgsList = origArgs.split("\\|\\|\\|");
        }
        
        try {
            setLookAndFeel();

            this.paths = dependentServices.getService(CommonPaths.class);
            requireNonNull(paths, t.localize(LocaleResources.SERVICE_UNAVAILABLE_MESSAGE, "CommonPaths"));
            this.launcher = dependentServices.getService(Launcher.class);
            requireNonNull(launcher, t.localize(LocaleResources.SERVICE_UNAVAILABLE_MESSAGE, "Launcher"));
            this.keyring = dependentServices.getService(Keyring.class);
            requireNonNull(keyring, t.localize(LocaleResources.SERVICE_UNAVAILABLE_MESSAGE, "Keyring"));

            createMainWindowAndRun();
        } catch (InterruptedException | InvocationTargetException e) {
            throw new CommandException(t.localize(LocaleResources.SETUP_FAILED), e);
        }
        runOriginalCommand(origArgsList);
    }

    private void runOriginalCommand(String[] args) {
        if (args == null) {
            return;
        }
        if (args.length == 0) {
            throw new AssertionError("Original command args were empty!");
        }
        if (args[0].equals("setup")) {
            // Do not run setup recursively
            return;
        }
        logger.log(Level.FINE, "Running intercepted command '" + args[0] + "' after setup.");
        launcher.run(args, false);
    }

    public void setPaths(CommonPaths paths) {
        dependentServices.addService(CommonPaths.class, paths);
    }
    
    public void setLauncher(Launcher launcher) {
        dependentServices.addService(Launcher.class, launcher);
    }
    
    public void setKeyring(Keyring keyring) {
        dependentServices.addService(Keyring.class, keyring);
    }
    
    public void setServicesUnavailable() {
        dependentServices.removeService(Launcher.class);
        dependentServices.removeService(CommonPaths.class);
        dependentServices.removeService(Keyring.class);
    }

    public boolean isStorageRequired() {
        return false;
    }

    //package-private for testing
    void createMainWindowAndRun() throws CommandException {
        thermostatSetup = ThermostatSetup.create(launcher, paths, keyring);
        mainWindow = new SetupWindow(thermostatSetup);
        mainWindow.run();
    }

    //package-private for testing
    void setLookAndFeel() throws InvocationTargetException, InterruptedException {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ThemeManager themeManager = ThemeManager.getInstance();
                themeManager.setLAF();
            }
        });
    }
}

