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

package com.redhat.thermostat.storage.mongodb.internal;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.locale.Translate;

/*
 * A command which adds a mongdb user by using AddUserCommand. It has been
 * introduced in order to aid boot-strapping of thermostat deployments. In order
 * to set up a user in mongodb, storage needs to be started with the
 * --permitLocalhostException option first. Then the credentials need to be
 * injected and storage stopped again. This command performs all three
 * required steps.
 */
public class StartStopAddUserCommandDecorator extends BaseAddUserCommand {

    public static final String COMMAND_NAME = "admin-mongodb-creds-setup";
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private final BundleContext context;
    private final List<ActionListener<ApplicationState>> listeners;
    private final CountDownLatch setupFinishedBarrier;
    private final BaseAddUserCommand decoratee;
    private boolean setupSuccessful;
    private Launcher launcher;
    private CommandContext cmdCtx;
    
    StartStopAddUserCommandDecorator(BundleContext context, BaseAddUserCommand command) {
        this.context = context;
        this.listeners = new ArrayList<>(1);
        this.setupFinishedBarrier = new CountDownLatch(1);
        this.setupSuccessful = true;
        this.decoratee = command;
    }
    
    @Override
    public void run(CommandContext ctx) throws CommandException {
        cmdCtx = ctx;
        if (!stampFileExists()) {
            startStorageAndRunDecoratee();
            stopStorage();
        }
        // if stamp file exists, there is nothing to do.
        String msg;
        if (setupSuccessful) {
            msg = t.localize(LocaleResources.USER_SETUP_COMPLETE).getContents();
            cmdCtx.getConsole().getOutput().println(msg);
        } else {
            msg = t.localize(LocaleResources.USER_SETUP_FAILED).getContents();
            cmdCtx.getConsole().getOutput().println(msg);
        }
    }

    private void stopStorage() throws CommandException {
        listeners.clear();
        CountDownLatch storageStoppedlatch = new CountDownLatch(1);
        StorageStoppedListener listener = new StorageStoppedListener(storageStoppedlatch);
        String[] storageStopArgs = new String[] {
                "storage", "--stop"
        };
        listeners.add(listener);
        launcher.run(storageStopArgs, listeners, false);
        try {
            storageStoppedlatch.await();
        } catch (InterruptedException e) {
            setupSuccessful = false;
            throw new CommandException(t.localize(LocaleResources.INTERRUPTED_WAITING_FOR_STORAGE_STOP), e);
        }
        if (!listener.storageStopPassed) {
            setupSuccessful = false;
            String msg = t.localize(LocaleResources.STORAGE_STOP_FAILED).getContents();
            cmdCtx.getConsole().getError().println(msg);
        }
    }

    private void startStorageAndRunDecoratee() throws CommandException {
        ServiceReference launcherRef = context.getServiceReference(Launcher.class);
        if (launcherRef == null) {
            throw new CommandException(t.localize(LocaleResources.LAUNCHER_SERVICE_UNAVAILABLE));
        }
        launcher = (Launcher) context.getService(launcherRef);
        StorageStartedListener listener = new StorageStartedListener();
        listeners.add(listener);
        String[] storageStartArgs = new String[] { "storage", "--start", "--permitLocalhostException"};
        launcher.run(storageStartArgs, listeners, false);
        try {
            setupFinishedBarrier.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean stampFileExists() throws CommandException {
        ServiceReference commonPathRef = context.getServiceReference(CommonPaths.class.getName());
        if (commonPathRef == null) {
            throw new CommandException(t.localize(LocaleResources.COMMON_PATHS_SERVICE_UNAVAILABLE));
        }
        CommonPaths commonPath = (CommonPaths)context.getService(commonPathRef);
        File dataDir = commonPath.getUserPersistentDataDirectory();
        // Since this is backing storage specific, it's most likely not a good
        // candidate for CommonPaths
        File mongodbSetupStamp = new File(dataDir, BaseAddUserCommand.MONGODB_STAMP_FILE_NAME);
        if (mongodbSetupStamp.exists()) {
            String msg = t.localize(LocaleResources.MONGODB_SETUP_FILE_EXISTS,
                                    mongodbSetupStamp.getAbsolutePath()).getContents();
            cmdCtx.getConsole().getOutput().println(msg);
            return true;
        } else {
            return false;
        }
    }
    
    private class StorageStartedListener implements ActionListener<ApplicationState> {
     
        @Override
        public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
            if (actionEvent.getSource() instanceof AbstractStateNotifyingCommand) {
                AbstractStateNotifyingCommand storage = (AbstractStateNotifyingCommand) actionEvent.getSource();
                // Implementation detail: there is a single StorageCommand instance registered
                // as an OSGi service.  We remove ourselves as listener so that we don't get
                // notified in the case that the command is invoked by some other means later.
                storage.getNotifier().removeActionListener(this);
                
                try {
                    switch (actionEvent.getActionId()) {
                    case START:
                        // Payload is connection URL
                        Object payload = actionEvent.getPayload();
                        if (payload == null || !(payload instanceof String)) {
                            setupSuccessful = false;
                            throw new CommandException(t.localize(LocaleResources.UNRECOGNIZED_PAYLOAD_FROM_STORAGE_CMD));
                        }
                        final String dbUrl = (String)payload;
                        try {
                            CommandContext ctx = getAddUserCommandContext(dbUrl);
                            decoratee.run(ctx);
                        } catch (CommandException e) {
                            cmdCtx.getConsole().getError().println(e.getMessage());
                            String msg = t.localize(LocaleResources.ADDING_USER_FAILED).getContents();
                            cmdCtx.getConsole().getError().println(msg);
                        }
                        break;
                    case FAIL:
                        // nothing
                        break;
                    default:
                        // nothing
                        break;
                    }
                } catch (CommandException e) {
                    cmdCtx.getConsole().getError().println(e.getMessage());
                } finally {
                    setupFinishedBarrier.countDown();
                }
            }
            
        }
        
        private CommandContext getAddUserCommandContext(final String dbUrl) {
            CommandContextFactory factory = new CommandContextFactory(context);
            CommandContext ctx = factory.createContext(new Arguments() {
                
                @Override
                public boolean hasArgument(String name) {
                    if (name.equals(AddUserCommand.DB_URL_ARG)) {
                        return true;
                    }
                    return false;
                }
                
                @Override
                public List<String> getNonOptionArguments() {
                    return Collections.emptyList();
                }
                
                @Override
                public String getArgument(String name) {
                    if (name.equals(AddUserCommand.DB_URL_ARG)) {
                        return dbUrl;
                    }
                    return null;
                }
            });
            return ctx;
        }
    }
    
    private static class StorageStoppedListener implements ActionListener<ApplicationState> {

        private boolean storageStopPassed;
        private final CountDownLatch storageStoppedLatch;
        private StorageStoppedListener(CountDownLatch latch) {
            storageStoppedLatch = latch;
        }
        
        @Override
        public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
            if (actionEvent.getSource() instanceof AbstractStateNotifyingCommand) {
                AbstractStateNotifyingCommand storage = (AbstractStateNotifyingCommand) actionEvent.getSource();
                // remove ourselves so that we get called more than once.
                storage.getNotifier().removeActionListener(this);
                switch(actionEvent.getActionId()) {
                case STOP:
                    storageStopPassed = true;
                    storageStoppedLatch.countDown();
                    break;
                default:
                    storageStoppedLatch.countDown();
                    break;
                
                }
            }
        }
        
    }
}
