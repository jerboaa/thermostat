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

package com.redhat.thermostat.agent.cli;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.agent.cli.db.StorageAlreadyRunningException;
import com.redhat.thermostat.agent.cli.impl.locale.LocaleResources;
import com.redhat.thermostat.agent.cli.impl.locale.Translate;
import com.redhat.thermostat.common.ActionEvent;
import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandContextImpl;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.OSGiContext;
import com.redhat.thermostat.common.cli.SimpleArgumentSpec;
import com.redhat.thermostat.common.cli.SimpleArguments;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.config.StartupConfiguration;
import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.common.tools.BasicCommand;
import com.redhat.thermostat.launcher.Launcher;

/**
 * Simple service that allows starting Agent and DB Backend
 * in a single step.
 */
public class ServiceCommand extends BasicCommand implements ActionListener<ApplicationState>, OSGiContext {
    
    private static final String NAME = "service";

    private static final String DESCRIPTION = Translate.localize(LocaleResources.COMMAND_SERVICE_DESCRIPTION);

    private static final String USAGE = DESCRIPTION;

    private BasicCommand database;
    private AgentApplication agent;
    
    private ActionNotifier<ApplicationState> notifier;

    private CommandContext context;
    private BundleContext bundleContext;
    
    private List<Runnable> tasksOnStop = new CopyOnWriteArrayList<>();

    public ServiceCommand() {
        database = new StorageCommand();
        agent = new AgentApplication();
        notifier = new ActionNotifier<>(this);
    }
    
    @Override
    public void setBundleContext(BundleContext context) {
        this.bundleContext = context;
    }
    
    private void addListeners() throws InvalidConfigurationException {
        // Currently, all the arguments are for the db. We only configure the
        // agent accordingly to the database settings.
        // so nothing else is done here at this stage
        database.getNotifier().addActionListener(this);
        agent.getNotifier().addActionListener(this);
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        context = ctx;
        try {
            addListeners();
            // just run the database, if the database is successful, let the
            // listeners start the agent for us.
            database.run(ctx);
        } catch (InvalidConfigurationException e) {
            throw new CommandException(e);
        }
    }

    @Override
    public ActionNotifier<ApplicationState> getNotifier() {
        return notifier;
    }

    @Override
    public StartupConfiguration getConfiguration() {
        throw new NotImplementedException("NYI");
    }

    @Override
    public void actionPerformed(ActionEvent<ApplicationState> actionEvent) {
        if (actionEvent.getSource().equals(database)) {
            switch (actionEvent.getActionId()) {
            // we are only interested in starting the agent if
            // we started the database ourselves
            case START:
                
                // set a bundle-stop hook if the db was started by us
                tasksOnStop.add(new Runnable() {
                    @Override
                    public void run() {
                        String[] args = new String[] { "storage", "--stop" };
                        
                        ServiceReference launcherRef = bundleContext.getServiceReference(Launcher.class.getName());
                        if (launcherRef != null) {
                            Launcher launcher = (Launcher) bundleContext.getService(launcherRef);
                            launcher.setArgs(args);
                            launcher.run();
                        } else {
                            try {
                                SimpleArguments arguments = new SimpleArguments();
                                arguments.addArgument("stop", args[1]);
                                database.run(new CommandContextImpl(arguments,
                                        ServiceCommand.this.context.getCommandContextFactory()));
                                
                            } catch (CommandException e) {
                                Logger.getLogger(getClass().getSimpleName()).log(Level.WARNING, "Can't stop database", e);
                            }
                        }
                        
                    }
                });
                
                String dbUrl = database.getConfiguration().getDBConnectionString();
                SimpleArguments args = new SimpleArguments();
                args.addArgument("dbUrl", dbUrl);
                try {
                    System.err.println(Translate.localize(LocaleResources.STARTING_AGENT));
                    agent.run(new CommandContextImpl(args, context.getCommandContextFactory()));
                } catch (CommandException e) {
                    notifier.fireAction(ApplicationState.FAIL);
                }
                break;

            case FAIL:
                System.err.println(Translate.localize(LocaleResources.ERROR_STARTING_DB));
                Object payload = actionEvent.getPayload();
                if (payload instanceof ApplicationException) {
                    ApplicationException exception = (ApplicationException) payload;
                    if (exception instanceof StorageAlreadyRunningException) {
                        System.err.println(Translate.localize(LocaleResources.STORAGE_ALREADY_RUNNING));
                    }
                }

                notifier.fireAction(ApplicationState.FAIL);
                break;
            }
        } else if (actionEvent.getSource().equals(agent)) {
            // agent is running
            switch (actionEvent.getActionId()) {
            case START:
                notifier.fireAction(ApplicationState.SUCCESS);
                break;
            }
        }
    }

    @Override
    public void disable() {
        for (Runnable task: tasksOnStop) {
            task.run();
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getUsage() {
        return USAGE;
    }

    @Override
    public Collection<ArgumentSpec> getAcceptedArguments() {
        ArgumentSpec start = new SimpleArgumentSpec("start", Translate.localize(LocaleResources.COMMAND_SERVICE_ARGUMENT_START_DESCRIPTION));
        ArgumentSpec stop = new SimpleArgumentSpec("stop", Translate.localize(LocaleResources.COMMAND_SERVICE_ARGUMENT_STOP_DESCRIPTION));
        return Arrays.asList(start, stop);
    }


}
