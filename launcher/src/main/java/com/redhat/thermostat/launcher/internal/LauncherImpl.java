/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

package com.redhat.thermostat.launcher.internal;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;

import org.apache.commons.cli.Options;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Launcher;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.CommandInfoNotFoundException;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.launcher.BundleManager;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.utils.keyring.Keyring;

public class LauncherImpl implements Launcher {

    private ClientPreferences prefs;

    private String[] args;

    private CommandContextFactory cmdCtxFactory;

    private int usageCount = 0;
    private final Semaphore argsBarrier = new Semaphore(0);

    private BundleContext context;
    private BundleManager registry;
    private final DbServiceFactory dbServiceFactory;
    
    public LauncherImpl(BundleContext context, CommandContextFactory cmdCtxFactory, BundleManager registry) {
        this(context, cmdCtxFactory, registry, new LoggingInitializer(), new DbServiceFactory());
    }

    LauncherImpl(BundleContext context, CommandContextFactory cmdCtxFactory, BundleManager registry,
            LoggingInitializer loggingInitializer, DbServiceFactory dbServiceFactory) {
        this.context = context;
        this.cmdCtxFactory = cmdCtxFactory;
        this.registry = registry;
        this.dbServiceFactory = dbServiceFactory;

        loggingInitializer.initialize();
    }

    @Override
    public synchronized void run() {
        run(null);
    }

    @Override
    public synchronized void run(Collection<ActionListener<ApplicationState>> listeners) {

        usageCount++;
        waitForArgs();

        try {
            if (hasNoArguments()) {
                runHelpCommand();
            } else if (isVersionQuery()) {
                // We want to print the version of core
                // thermostat, so we use the no-arg constructor of Version
                Version coreVersion = new Version();
                cmdCtxFactory.getConsole().getOutput().println(coreVersion.getVersionInfo());
            } else {
                runCommandFromArguments(listeners);
            }
        } finally {
            args = null;
            usageCount--;
            if (isLastLaunch()) {
                shutdown();
            }
        }
    }

    @Override
    public void setArgs(String[] args) {
        this.args = args;
        argsBarrier.release();
    }

    private void waitForArgs() {
        try {
            argsBarrier.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean isLastLaunch() {
        return usageCount == 0;
    }

    private void shutdown() throws InternalError {
        try {
            ApplicationService appSvc = OSGIUtils.getInstance().getService(ApplicationService.class);
            appSvc.getApplicationExecutor().shutdown();
            appSvc.getTimerFactory().shutdown();

            Bundle bundle = FrameworkUtil.getBundle(LauncherImpl.class);
            if (bundle != null) {
                BundleContext ctx = bundle.getBundleContext();
                if (ctx != null) {
                    ServiceReference storageRef = ctx.getServiceReference(Storage.class);
                    if (storageRef != null) {
                        Storage storage = (Storage) ctx.getService(storageRef);
                        if (storage != null) {
                            storage.shutdown();
                        }
                    }
                }
            }

            context.getBundle(0).stop();
        } catch (BundleException e) {
            throw (InternalError) new InternalError().initCause(e);
        }
    }

    public void setPreferences(ClientPreferences prefs) {
        this.prefs = prefs;
    }

    private boolean hasNoArguments() {
        return args.length == 0;
    }

    private void runHelpCommand() {
        runCommand("help", new String[0], null);
    }

    private void runHelpCommandFor(String cmdName) {
        runCommand("help", new String[] { "--", cmdName }, null);
    }

    private void runCommandFromArguments(Collection<ActionListener<ApplicationState>> listeners) {
        runCommand(args[0], Arrays.copyOfRange(args, 1, args.length), listeners);
    }

    private void runCommand(String cmdName, String[] cmdArgs, Collection<ActionListener<ApplicationState>> listeners) {
        try {
            parseArgsAndRunCommand(cmdName, cmdArgs, listeners);
        } catch (CommandException e) {
            cmdCtxFactory.getConsole().getError().println(e.getMessage());
        }
    }

    private void parseArgsAndRunCommand(String cmdName, String[] cmdArgs, Collection<ActionListener<ApplicationState>> listeners) throws CommandException {

        PrintStream out = cmdCtxFactory.getConsole().getOutput();
        try {
            registry.addBundlesFor(cmdName);
        } catch (BundleException | IOException e) {
            // If this happens we definitely need to do something about it, and the
            // trace will be immeasurably helpful in figuring out what is wrong.
            out.println("Could not load necessary bundles for: " + cmdName);
            e.printStackTrace(out);
            return;
        } catch (CommandInfoNotFoundException commandNotFound) {
            runHelpCommandFor(cmdName);
            return;
        }

        Command cmd = getCommand(cmdName);
        if (cmd == null) {
            runHelpCommandFor(cmdName);
            return;
        }
        if (listeners != null && cmd instanceof AbstractStateNotifyingCommand) {
            AbstractStateNotifyingCommand basicCmd = (AbstractStateNotifyingCommand) cmd;
            ActionNotifier<ApplicationState> notifier = basicCmd.getNotifier();
            for (ActionListener<ApplicationState> listener : listeners) {
                notifier.addActionListener(listener);
            }
        }
        Options options = cmd.getOptions();
        Arguments args = parseCommandArguments(cmdArgs, options);
        setupLogLevel(args);
        CommandContext ctx = setupCommandContext(cmd, args);
        cmd.run(ctx);
    }

    private void setupLogLevel(Arguments args) {
        if (args.hasArgument(CommonOptions.LOG_LEVEL_ARG)) {
            String levelOption = args.getArgument(CommonOptions.LOG_LEVEL_ARG);
            setLogLevel(levelOption);
        }
    }

    private void setLogLevel(String levelOption) {
        try {
            Level level = Level.parse(levelOption);
            LoggingUtils.setGlobalLogLevel(level);
        } catch (IllegalArgumentException ex) {
            // Ignore this, use default loglevel.
        }
    }

    private Command getCommand(String cmdName) {

        CommandRegistry registry = cmdCtxFactory.getCommandRegistry();
        Command cmd = registry.getCommand(cmdName);
        return cmd;
    }

    private Arguments parseCommandArguments(String[] cmdArgs, Options options)
            throws CommandLineArgumentParseException {

        CommandLineArgumentsParser cliArgsParser = new CommandLineArgumentsParser();
        cliArgsParser.addOptions(options);
        Arguments args = cliArgsParser.parse(cmdArgs);
        return args;
    }

    @SuppressWarnings("rawtypes")
    private CommandContext setupCommandContext(Command cmd, Arguments args) throws CommandException {

        CommandContext ctx = cmdCtxFactory.createContext(args);
        
        if (prefs == null) {
            ServiceReference keyringReference = context.getServiceReference(Keyring.class);
            @SuppressWarnings("unchecked")
            Keyring keyring = (Keyring) context.getService(keyringReference);
            prefs = new ClientPreferences(keyring);
        }
        
        if (cmd.isStorageRequired()) {
            ServiceReference dbServiceReference = context.getServiceReference(DbService.class);
            if (dbServiceReference == null) {
                String dbUrl = ctx.getArguments().getArgument(CommonOptions.DB_URL_ARG);
                if (dbUrl == null) {
                    dbUrl = prefs.getConnectionUrl();
                }
                String username = ctx.getArguments().getArgument(CommonOptions.USERNAME_ARG);
                String password = ctx.getArguments().getArgument(CommonOptions.PASSWORD_ARG);
                try {
                    // this may throw storage exception
                    DbService service = dbServiceFactory.createDbService(username, password, dbUrl);
                    // This registers the DbService if all goes well
                    service.connect();
                } catch (StorageException ex) {
                    throw new CommandException("Unsupported storage URL: " + dbUrl);
                } catch (ConnectionException ex) {
                    String error = ex.getMessage();
                    String message = ( error == null ? "" : " Error: " + error );
                    throw new CommandException("Could not connect to: " + dbUrl + message, ex);
                }
            }
        }
        return ctx;
    }

    private boolean isVersionQuery() {
        return args[0].equals(Version.VERSION_OPTION);
    }

    static class LoggingInitializer {
        public void initialize() {
            try {
                LoggingUtils.loadGlobalLoggingConfig();
            } catch (InvalidConfigurationException e) {
                System.err.println("WARNING: Could not read global Thermostat logging configuration.");
            }
            try {
                LoggingUtils.loadUserLoggingConfig();
            } catch (InvalidConfigurationException e) {
                // We intentionally ignore this.
            }
        }
    }
}

