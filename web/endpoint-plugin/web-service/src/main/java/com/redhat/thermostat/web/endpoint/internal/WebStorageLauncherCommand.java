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

package com.redhat.thermostat.web.endpoint.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ExitStatus;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.SSLConfiguration;
import com.redhat.thermostat.shared.locale.Translate;
import sun.misc.Signal;
import sun.misc.SignalHandler;

@Component
@Service(Command.class)
@Property(name=Command.NAME, value="web-storage")
public class WebStorageLauncherCommand extends AbstractStateNotifyingCommand {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final Logger logger = LoggingUtils.getLogger(WebappLauncherCommand.class);

    private final List<ActionListener<ApplicationState>> listeners = new ArrayList<>();
    private final CustomSignalHandler handler = new CustomSignalHandler();
    private final CountDownLatch shutdownLatch;
    private JettyContainerLauncher jettyLauncher;

    @Reference
    private CommonPaths commonPaths;

    @Reference
    private Launcher launcher;

    @Reference
    private SSLConfiguration sslConfig;

    @Reference
    private ExitStatus exitStatus;

    //for declarative services instantiation
    public WebStorageLauncherCommand() {
        this(new CountDownLatch(1));
    }

    //package private for testing
    WebStorageLauncherCommand(CountDownLatch shutdownLatch) {
        this.shutdownLatch = shutdownLatch;
    }

    protected void bindCommonPaths(CommonPaths commonPaths) {
        this.commonPaths = commonPaths;
    }

    protected void unbindCommonPaths(CommonPaths commonPaths) {
        this.commonPaths = null;
    }

    protected void bindLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    protected void unbindLauncher(Launcher launcher) {
        this.launcher = null;
    }

    protected void bindSslConfig(SSLConfiguration sslConfig) {
        this.sslConfig = sslConfig;
    }

    protected void unbindSslConfig(SSLConfiguration sslConfig) {
        this.sslConfig = null;
    }

    protected void bindExitStatus(ExitStatus exitStatus) {
        this.exitStatus = exitStatus;
    }

    protected void unbindExitStatus(ExitStatus exitStatus) {
        this.exitStatus = null;
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        // start storage
        final CountDownLatch storageLatch = new CountDownLatch(1);
        StorageStartedListener storageListener = new StorageStartedListener(storageLatch);
        listeners.add(storageListener);
        String[] storageArgs = new String[] {
                "storage", "--start"
        };
        launcher.run(storageArgs, listeners, false);
        listeners.clear();
        try {
            storageLatch.await();
        } catch (InterruptedException e) {
            getNotifier().fireAction(ApplicationState.FAIL, e);
            throw new CommandException(translator.localize(LocaleResources.STORAGE_WAIT_INTERRUPTED));
        }
        if (!storageListener.isStartupSuccessful()) {
            getNotifier().fireAction(ApplicationState.FAIL);
            throw new CommandException(translator.localize(LocaleResources.ERROR_STARTING_STORAGE));
        }

        EmbeddedServletContainerConfiguration config = getConfiguration(commonPaths);
        jettyLauncher = getJettyContainerLauncher(config, sslConfig);
        CountDownLatch webStartedLatch = new CountDownLatch(1);
        // start web container with the web archive deployed
        jettyLauncher.startContainer(webStartedLatch);
        try {
            webStartedLatch.await();
        } catch (InterruptedException e) {
            // ignore
        }
        if (!jettyLauncher.isStartupSuccessFul()) {
            stopStorage(launcher);
            getNotifier().fireAction(ApplicationState.FAIL);
            throw new CommandException(translator.localize(LocaleResources.ERROR_STARTING_JETTY));
        }

        Signal.handle(new Signal("INT"), handler);
        Signal.handle(new Signal("TERM"), handler);

        //Wait for SIGINT/SIGTERM
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            handler.handle(new Signal("INT"));
        }
    }

    // testing hook
    EmbeddedServletContainerConfiguration getConfiguration(CommonPaths paths) {
        return new EmbeddedServletContainerConfiguration(paths);
    }

    // testing hook
    JettyContainerLauncher getJettyContainerLauncher(EmbeddedServletContainerConfiguration config, SSLConfiguration sslConfig) {
        return new JettyContainerLauncher(config, sslConfig);
    }

    // This fires up the web endpoint. No need to automatically firing up
    // storage.
    @Override
    public boolean isStorageRequired() {
        return false;
    }

    private void stopStorage(Launcher launcher) {
        String[] storageStopArgs = new String[] {
                "storage", "--stop"
        };
        launcher.run(storageStopArgs, false);
    }

    private class CustomSignalHandler implements SignalHandler {

        @Override
        public void handle(Signal arg0) {
            try {
                jettyLauncher.stopContainer();
                stopStorage(launcher);
            } catch (Exception ex) {
                // We don't want any exception to hold back the signal handler, otherwise
                // there will be no way to actually stop Thermostat.
                ex.printStackTrace();
            }
            logger.fine("Web storage stopped.");
            // Hook for integration tests. Print a well known message to stdout
            // if verbose mode is turned on via the system property.
            shutdown(ExitStatus.EXIT_SUCCESS);
        }
    }

    private void shutdown(int shutDownStatus) {
        // Exit application
        if (shutdownLatch != null) {
            shutdownLatch.countDown();
        }

        this.exitStatus.setExitStatus(shutDownStatus);
        if (shutDownStatus == ExitStatus.EXIT_SUCCESS) {
            getNotifier().fireAction(ApplicationState.STOP);
        } else {
            getNotifier().fireAction(ApplicationState.FAIL);
        }
    }
}
