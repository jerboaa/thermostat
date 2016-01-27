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

package com.redhat.thermostat.local.command.internal;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StreamUtils;
import com.redhat.thermostat.local.command.locale.LocaleResources;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.launcher.Launcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class LocalCommand extends AbstractCommand {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final Logger logger = LoggingUtils.getLogger(LocalCommand.class);
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

        ProcOutErrReader reader = createProcessReader(gui);
        reader.start();

        int exitStatus;
        try {
            exitStatus = gui.waitFor();
        } catch (InterruptedException e) {
            throw new CommandException(t.localize(LocaleResources.RUNNING_GUI_INTERRUPTED));
        }

        if (exitStatus != 0) {
            throw new CommandException(t.localize(LocaleResources.ERROR_RUNNING_GUI));
        }

        reader.join();
        String stdOutput = reader.getOutput();
        String errOutput = reader.getErrOutput();
        logger.fine("GUI stdout >>> " + stdOutput);
        logger.fine("GUI stderr >>> " + errOutput);
    }

    // package-private for testing
    Process execProcess(String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectInput(ProcessBuilder.Redirect.PIPE);
        return pb.start();
    }

    //package-private for testing
    ProcOutErrReader createProcessReader(Process process) {
        return  new ProcOutErrReader(process);
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

    // package-private for testing
    static class ProcOutErrReader {
        private final Thread errReaderThread;
        private final Thread outReaderThread;
        private final ProcStreamReader outReader;
        private final ProcStreamReader errReader;

        private ProcOutErrReader(Process process) {
            outReader = new ProcStreamReader(process.getInputStream());
            errReader = new ProcStreamReader(process.getErrorStream());
            Runnable outRunnable = new Runnable() {

                @Override
                public void run() {
                    outReader.readAll();
                }

            };
            Runnable errRunnable = new Runnable() {

                @Override
                public void run() {
                    errReader.readAll();
                }

            };
            errReaderThread = new Thread(errRunnable);
            outReaderThread = new Thread(outRunnable);
        }

        public void start() {
            errReaderThread.start();
            outReaderThread.start();
        }

        public void join() {
            try {
                errReaderThread.join();
            } catch (InterruptedException e) {
                // ignore
            }
            try {
                outReaderThread.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }

        public String getOutput() {
            return outReader.getOutput();
        }
        public String getErrOutput() {
            return errReader.getOutput();
        }

    }

    private static class ProcStreamReader {
        private final InputStream in;

        private byte[] contents;

        private ProcStreamReader(InputStream in) {
            this.in = in;
        }

        public void readAll() {
            try {
                contents = StreamUtils.readAll(in);
            } catch (IOException e) {
                // ignore
            }
        }
        public String getOutput() {
            return new String(contents);
        }

    }
}