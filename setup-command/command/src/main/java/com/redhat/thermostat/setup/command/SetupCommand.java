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

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.internal.utils.laf.ThemeManager;
import com.redhat.thermostat.setup.command.internal.SetupWindow;
import com.redhat.thermostat.setup.command.internal.ThermostatSetup;
import com.redhat.thermostat.setup.command.internal.ThermostatSetupImpl;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.locale.LocalizedString;
import org.osgi.framework.BundleContext;

import java.awt.EventQueue;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SetupCommand extends AbstractCommand {
    private SetupWindow mainWindow;

    private CommonPaths paths;
    private BundleContext context;
    private CountDownLatch pathsAvailable;
    private ThermostatSetup thermostatSetup;
    private PrintStream out;

    public SetupCommand(BundleContext context) {
        this.context = context;
        pathsAvailable = new CountDownLatch(1);
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        out = ctx.getConsole().getOutput();

        try {
            setLookAndFeel();

            pathsAvailable.await(1000l, TimeUnit.MILLISECONDS);
            requireNonNull(paths, new LocalizedString("CommonPaths dependency not available"));

            createMainWindowAndRun();
        } catch (InterruptedException | InvocationTargetException e) {
            throw new CommandException(new LocalizedString("SetupCommand failed to run"), e);
        }
    }

    public void setPaths(CommonPaths paths) {
        this.paths = paths;
        pathsAvailable.countDown();
    }

    public boolean isStorageRequired() {
        return false;
    }

    //package-private for testing
    void createMainWindowAndRun() {
        thermostatSetup = new ThermostatSetupImpl(context, paths, out);
        mainWindow = new SetupWindow(out, thermostatSetup);
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

