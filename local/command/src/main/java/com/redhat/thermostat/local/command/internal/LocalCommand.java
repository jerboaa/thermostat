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

package com.redhat.thermostat.local.command.internal;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.launcher.Launcher;

import java.lang.ProcessBuilder.Redirect;
import java.io.IOException;

public class LocalCommand extends AbstractCommand {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private final DependencyServices dependentServices = new DependencyServices();
    private Launcher launcher;
    private CommonPaths paths;

    public void run(CommandContext ctx) throws CommandException {
        this.paths = dependentServices.getService(CommonPaths.class);
        requireNonNull(paths, t.localize(LocaleResources.SERVICE_UNAVAILABLE_MESSAGE, "CommonPaths"));
        this.launcher = dependentServices.getService(Launcher.class);
        requireNonNull(launcher, t.localize(LocaleResources.SERVICE_UNAVAILABLE_MESSAGE, "Launcher"));

        ServiceLauncher serviceLauncher = createServiceLauncher();
        serviceLauncher.start();
        try {
            // this blocks
            runGui();
        } finally {
            serviceLauncher.stop();
        }
    }

    // package-private for testing
    ServiceLauncher createServiceLauncher() {
        return new ServiceLauncher(launcher, paths);
    }

    private void runGui() throws CommandException {
        String thermostat = paths.getSystemBinRoot() + "/thermostat";
        Process gui;

        try {
            gui = execProcess(thermostat, "gui");
        } catch (IOException e) {
            throw new CommandException(t.localize(LocaleResources.ERROR_STARTING_GUI));
        }

        int exitStatus;
        try {
            exitStatus = gui.waitFor();
        } catch (InterruptedException e) {
            throw new CommandException(t.localize(LocaleResources.RUNNING_GUI_INTERRUPTED));
        }

        if (exitStatus != 0) {
            throw new CommandException(t.localize(LocaleResources.ERROR_RUNNING_GUI));
        }
    }

    // package-private for testing
    Process execProcess(String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectOutput(Redirect.INHERIT);
        pb.redirectError(Redirect.INHERIT);
        return pb.start();
    }

    public void setPaths(CommonPaths paths) {
        dependentServices.addService(CommonPaths.class, paths);
    }

    public void setLauncher(Launcher launcher) {
        dependentServices.addService(Launcher.class, launcher);
    }

    public void setServicesUnavailable() {
        dependentServices.removeService(Launcher.class);
        dependentServices.removeService(CommonPaths.class);
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }
}