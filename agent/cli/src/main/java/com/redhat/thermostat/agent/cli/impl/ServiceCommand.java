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

package com.redhat.thermostat.agent.cli.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.redhat.thermostat.agent.cli.db.StorageAlreadyRunningException;
import com.redhat.thermostat.agent.cli.impl.locale.LocaleResources;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.SimpleCommand;
import com.redhat.thermostat.common.locale.Translate;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.launcher.Launcher;

/**
 * Simple service that allows starting Agent and DB Backend
 * in a single step.
 */
public class ServiceCommand extends SimpleCommand implements ActionListener<ApplicationState> {
    
    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final String NAME = "service";

    private List<ActionListener<ApplicationState>> listeners;

    private Semaphore agentBarrier = new Semaphore(0);

    public ServiceCommand() {
        listeners = new ArrayList<>();
        listeners.add(this);
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        Launcher launcher = getLauncher();
        String[] storageStartArgs = new String[] { "storage", "--start" };
        launcher.setArgs(storageStartArgs);
        launcher.run(listeners);
        agentBarrier.acquireUninterruptibly();
        String[] storageStopArgs = new String[] { "storage", "--stop" };
        launcher.setArgs(storageStopArgs);
        launcher.run();
    }

    @Override
    public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
        if (actionEvent.getSource() instanceof StorageCommand) {
            StorageCommand storage = (StorageCommand) actionEvent.getSource();
            // Implementation detail: there is a single StorageCommand instance registered
            // as an OSGi service.  We remove ourselves as listener so that we don't get
            // notified in the case that the command is invoked by some other means later.
            storage.getNotifier().removeActionListener(this);
            switch (actionEvent.getActionId()) {
            case START:
                String dbUrl = storage.getConfiguration().getDBConnectionString();
                Launcher launcher = getLauncher();
                String[] agentArgs =  new String[] {"agent", "-d", dbUrl};
                System.err.println(translator.localize(LocaleResources.STARTING_AGENT));
                launcher.setArgs(agentArgs);
                launcher.run();
                agentBarrier.release();
                break;
            case FAIL:
                System.err.println(translator.localize(LocaleResources.ERROR_STARTING_DB));
                Object payload = actionEvent.getPayload();
                if (payload instanceof StorageAlreadyRunningException) {
                    System.err.println(translator.localize(LocaleResources.STORAGE_ALREADY_RUNNING));
                }
                break;
            }
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isAvailableInShell() {
        return false;
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }

    private Launcher getLauncher() {
        OSGIUtils osgi = OSGIUtils.getInstance();
        return osgi.getService(Launcher.class);
    }

}
