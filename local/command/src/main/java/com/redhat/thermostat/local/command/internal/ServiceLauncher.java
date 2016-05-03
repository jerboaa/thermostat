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

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.locale.Translate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class ServiceLauncher {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final Logger logger = LoggingUtils.getLogger(ServiceLauncher.class);
    private File webApp;
    private String serviceArg;
    private Launcher launcher;
    private Thread serviceThread;

    public ServiceLauncher(Launcher launcher, CommonPaths paths) {
        this(launcher, new File(paths.getSystemThermostatHome(), "webapp"));
    }

    //package-private for testing
    ServiceLauncher(Launcher launcher, File webApp) {
        this.launcher = launcher;
        this.webApp = webApp;
        serviceArg = getServiceArg();
    }

    public void start() throws CommandException {
        logger.fine("running thermostat " + serviceArg);

        CountDownLatch serviceLatch = new CountDownLatch(1);
        ServiceStartedListener serviceListener = new ServiceStartedListener(serviceLatch);
        final List<ActionListener<ApplicationState>> listeners = new ArrayList<>();
        listeners.add(serviceListener);

        serviceThread = new Thread(new Runnable() {
            @Override
            public void run() {
                String[] serviceArgs = new String[] {serviceArg};
                launcher.run(serviceArgs, listeners, false);
            }
        });
        serviceThread.start();

        try {
            serviceLatch.await();
        } catch (InterruptedException e) {
            throw new CommandException(t.localize(LocaleResources.SERVICE_WAIT_INTERRUPTED, serviceArg), e);
        }
        if (serviceListener.startupFailed) {
            throw new CommandException(t.localize(LocaleResources.ERROR_STARTING_SERVICE, serviceArg));
        }
    }

    public void stop() throws CommandException {
        if (serviceThread != null && serviceThread.isAlive()) {
            serviceThread.interrupt();
            try {
                // wait for service to finish handling interruption
                serviceThread.join();
            } catch (InterruptedException e) {
                throw new CommandException(t.localize(LocaleResources.STOPPING_SERVICE_INTERRUPTED, serviceArg), e);
            }
        }
    }

    private String getServiceArg() {
        if (isWebAppInstalled()) {
            return "web-storage-service";
        } else {
            return "service";
        }
    }
    private boolean isWebAppInstalled() {
        return webApp.exists();
    }

    private static class ServiceStartedListener implements ActionListener<ApplicationState> {

        private final CountDownLatch serviceStarted;
        private boolean startupFailed = true;

        private ServiceStartedListener(CountDownLatch latch) {
            serviceStarted = latch;
        }

        @Override
        public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
            if (actionEvent.getSource() instanceof AbstractStateNotifyingCommand) {
                ApplicationState state = (ApplicationState) actionEvent.getActionId();
                switch(state) {
                    case START:
                        startupFailed = false;
                        // fall-through
                    default:
                        serviceStarted.countDown();
                        break;
                }
            }
        }
    }

}
