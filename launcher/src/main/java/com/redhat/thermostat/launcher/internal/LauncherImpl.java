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

package com.redhat.thermostat.launcher.internal;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.Options;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.ExitStatus;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.cli.AbstractStateNotifyingCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.CommandLineArgumentParseException;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.tools.ApplicationState;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.BundleManager;
import com.redhat.thermostat.launcher.InteractiveStorageCredentials;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.DbService;
import com.redhat.thermostat.storage.core.DbServiceFactory;
import com.redhat.thermostat.storage.core.Storage;
import com.redhat.thermostat.storage.core.StorageCredentials;
import com.redhat.thermostat.storage.core.StorageException;
import com.redhat.thermostat.utils.keyring.Keyring;

/**
 * This class is thread-safe.
 */
public class LauncherImpl implements Launcher {

    private static final String HELP_COMMAND_NAME = "help";

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final Logger logger = LoggingUtils.getLogger(LauncherImpl.class);

    private final AtomicInteger usageCount = new AtomicInteger(0);
    private final BundleContext context;
    private final BundleManager registry;
    private final CommandContextFactory cmdCtxFactory;
    private final DbServiceFactory dbServiceFactory;
    private final Version coreVersion;
    private final CommandSource commandSource;
    private final CommandInfoSource commandInfoSource;
    private final CurrentEnvironment currentEnvironment;
    private final CommonPaths paths;

    private final ClientPreferences prefs;
    private final Keyring keyring;

    public LauncherImpl(BundleContext context, CommandContextFactory cmdCtxFactory, BundleManager registry,
            CommandInfoSource infoSource, CurrentEnvironment env, ClientPreferences prefs, Keyring keyring, CommonPaths paths) {
        this(context, cmdCtxFactory, registry, infoSource, new CommandSource(context), env,
                new DbServiceFactory(), new Version(), prefs, keyring, paths);
    }

    LauncherImpl(BundleContext context, CommandContextFactory cmdCtxFactory, BundleManager registry,
            CommandInfoSource commandInfoSource, CommandSource commandSource,
            CurrentEnvironment currentEnvironment, DbServiceFactory dbServiceFactory, Version version,
            ClientPreferences prefs, Keyring keyring, CommonPaths paths) {
        this.context = context;
        this.cmdCtxFactory = cmdCtxFactory;
        this.registry = registry;
        this.dbServiceFactory = dbServiceFactory;
        this.coreVersion = version;
        this.commandSource = commandSource;
        this.commandInfoSource = commandInfoSource;
        this.currentEnvironment = currentEnvironment;
        this.prefs = prefs;
        this.keyring = keyring;
        this.paths = Objects.requireNonNull(paths);

        // We log this in the constructor so as to not log it multiple times when a command invokes
        // run() multiple times. This works since it is a singleton service.
        logger.log(Level.CONFIG, "THERMOSTAT_HOME=" + paths.getSystemThermostatHome().getAbsolutePath());
        logger.log(Level.CONFIG, "USER_THERMOSTAT_HOME=" + paths.getUserThermostatHome().getAbsolutePath());
    }

    @Override
    public void run(String[] args, boolean inShell) {
        run(args, null, inShell);
    }

    @Override
    public void run(String[] args, Collection<ActionListener<ApplicationState>> listeners, boolean inShell) {
        usageCount.incrementAndGet();

        Environment oldEnvironment = currentEnvironment.getCurrent();
        currentEnvironment.setCurrent(inShell ? Environment.SHELL : Environment.CLI);

        try {
            if (hasNoArguments(args)) {
                runHelpCommand();
            } else if (isVersionQuery(args, inShell)) {
                // We want to print the version of core
                // thermostat, so we use the no-arg constructor of Version
                cmdCtxFactory.getConsole().getOutput().println(coreVersion.getVersionInfo());
            } else {
                // With web-always-on we need to make sure that the setup ran.
                if (isThermostatConfigured()) {
                    logger.log(Level.FINE, "Running command without setup interception.");
                    runCommandFromArguments(args, listeners, inShell);
                } else {
                    logger.log(Level.FINE, "Running command through setup.");
                    runSetupThenInterceptedCommand(args);
                }
            }
        } catch (NoClassDefFoundError e) {
            // This could mean pom is missing <Private-Package> or <Export-Package> lines.
            // Should be resolved during development, but if we don't catch and print
            // something the error is swallowed and the cause is non-obvious.
            System.err.println("Caught NoClassDefFoundError! Check pom for the missing class: \""
                    + e.getMessage() + "\".  Its package may not be listed.");
            throw e;
        } catch (Throwable e) {
            // Sometimes get exceptions, which get seemingly swallowed, which
            // they really aren't, but the finally block make it seem so.
            e.printStackTrace(System.err);
            throw e;
        } finally {
            args = null;
            currentEnvironment.setCurrent(oldEnvironment);
            boolean isLastLaunch = (usageCount.decrementAndGet() == 0);
            if (isLastLaunch) {
                shutdown();
            }
        }
    }
    
    private void runSetupThenInterceptedCommand(String[] originalCmdArgs) {
        String origCmdArgs = convertOriginalArgsToString(originalCmdArgs);
        String[] setupArgs = { "setup",
                               "--origArgs",
                               origCmdArgs
                             };
        runCommandFromArguments(setupArgs, null, false);
    }

    private String convertOriginalArgsToString(String[] origArgs) {
        if (origArgs.length == 0) {
            throw new AssertionError("Running setup with no argument?");
        }
        final String separator = "|||";
        // single argument
        if (origArgs.length == 1) {
            return origArgs[0];
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < origArgs.length - 1; i++) {
            buffer.append(origArgs[i]);
            buffer.append(separator);
        }
        buffer.append(origArgs[origArgs.length - 1]);
        return buffer.toString();
    }

    private boolean isThermostatConfigured() throws InvalidConfigurationException {
        File setupCompleteFile = paths.getUserSetupCompleteStampFile();
        return setupCompleteFile.exists();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void shutdown() throws InternalError {
        try {
            ServiceReference appServiceRef = context.getServiceReference(ApplicationService.class);
            if (appServiceRef != null) {
                ApplicationService appSvc = (ApplicationService) context.getService(appServiceRef);
                appSvc.getApplicationExecutor().shutdown();
                appSvc.getTimerFactory().shutdown();
                appSvc = null;
                context.ungetService(appServiceRef);
            }

            // default to success for exit status
            int exitStatus = ExitStatus.EXIT_SUCCESS;
            if (context != null) {
                ServiceReference storageRef = context.getServiceReference(Storage.class);
                if (storageRef != null) {
                    Storage storage = (Storage) context.getService(storageRef);
                    if (storage != null) {
                        storage.shutdown();
                    }
                }
                ServiceReference exitStatusRef = context.getServiceReference(ExitStatus.class);
                if (exitStatusRef != null) {
                    ExitStatus exitStatusService = (ExitStatus) context.getService(exitStatusRef);
                    exitStatus = exitStatusService.getExitStatus();
                }
            }
            context.getBundle(0).stop();
            System.exit(exitStatus);
        } catch (BundleException e) {
            throw (InternalError) new InternalError().initCause(e);
        }
    }

    private boolean hasNoArguments(String[] args) {
        return args == null || args.length == 0;
    }

    private void runHelpCommand() {
        runCommand(HELP_COMMAND_NAME, new String[0], null, false);
    }

    private void runHelpCommandFor(String cmdName) {
        runCommand(HELP_COMMAND_NAME, new String[] { "--", cmdName }, null, false);
    }

    // package-private for testing
    void runCommandFromArguments(String[] args, Collection<ActionListener<ApplicationState>> listeners, boolean inShell) {
        runCommand(args[0], Arrays.copyOfRange(args, 1, args.length), listeners, inShell);
    }

    private void runCommand(String cmdName, String[] cmdArgs, Collection<ActionListener<ApplicationState>> listeners, boolean inShell) {
        // treat 'foo --help' as 'help foo'
        if (!cmdName.equals(HELP_COMMAND_NAME) && Arrays.asList(cmdArgs).contains("--help")) {
            runCommand(HELP_COMMAND_NAME, new String[] { cmdName } , listeners, inShell);
            return;
        }

        try {
            parseArgsAndRunCommand(cmdName, cmdArgs, listeners, inShell);
        } catch (CommandException e) {
            cmdCtxFactory.getConsole().getError().println(e.getMessage());
        }
    }

    private void parseArgsAndRunCommand(String cmdName, String[] cmdArgs,
    		Collection<ActionListener<ApplicationState>> listeners, boolean inShell) throws CommandException {

        PrintStream out = cmdCtxFactory.getConsole().getOutput();
        PrintStream err = cmdCtxFactory.getConsole().getError();

        CommandInfo cmdInfo;
        try {
            cmdInfo = commandInfoSource.getCommandInfo(cmdName);
        } catch (CommandInfoNotFoundException commandNotFound) {
            runHelpCommandFor(cmdName);
            return;
        }

        try {
            registry.loadBundlesByName(cmdInfo.getBundles());
        } catch (BundleException | IOException e) {
            // If this happens we definitely need to do something about it, and the
            // trace will be immeasurably helpful in figuring out what is wrong.
            out.println(t.localize(LocaleResources.COMMAND_COULD_NOT_LOAD_BUNDLES, cmdName).getContents());
            e.printStackTrace(out);
            return;
        }

        Command cmd = commandSource.getCommand(cmdName);

        if (cmd == null) {
            err.println(t.localize(LocaleResources.COMMAND_DESCRIBED_BUT_NOT_AVAILALBE, cmdName).getContents());
            return;
        }

        if ((inShell && !cmdInfo.getEnvironments().contains(Environment.SHELL)) || (!inShell && !cmdInfo.getEnvironments().contains(Environment.CLI))) {
        	outputBadShellContext(inShell, out, cmdName);
        	return;
        }
        if (listeners != null && cmd instanceof AbstractStateNotifyingCommand) {
            AbstractStateNotifyingCommand basicCmd = (AbstractStateNotifyingCommand) cmd;
            ActionNotifier<ApplicationState> notifier = basicCmd.getNotifier();
            for (ActionListener<ApplicationState> listener : listeners) {
                notifier.addActionListener(listener);
            }
        }
        Options options = cmdInfo.getOptions();
        Arguments args = null;
        try {
            args = parseCommandArguments(cmdArgs, options);
            setupLogLevel(args);
            CommandContext ctx = setupCommandContext(cmd, args);
            cmd.run(ctx);
        } catch (CommandLineArgumentParseException e) {
            out.println(e.getMessage());
            runHelpCommandFor(cmdName);
            return;
        }
    }

    private void outputBadShellContext(boolean inShell, PrintStream out, String cmd) {
    	LocalizedString message = null;
    	if (inShell) {
            message = t.localize(LocaleResources.COMMAND_AVAILABLE_OUTSIDE_SHELL_ONLY, cmd);
    	} else {
            message = t.localize(LocaleResources.COMMAND_AVAILABLE_INSIDE_SHELL_ONLY, cmd);
    	}
    	out.println(message.getContents());
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

        if (cmd.isStorageRequired()) {

            ServiceReference dbServiceReference = context.getServiceReference(DbService.class);
            if (dbServiceReference == null) {
                String dbUrl = ctx.getArguments().getArgument(CommonOptions.DB_URL_ARG);
                if (dbUrl == null) {
                    dbUrl = prefs.getConnectionUrl();
                }
                StorageCredentials creds = new InteractiveStorageCredentials(prefs, keyring, dbUrl, ctx.getConsole());
                ServiceRegistration credsReg = context.registerService(StorageCredentials.class, creds, null);
                try {
                    // this may throw storage exception
                    DbService service = dbServiceFactory.createDbService(dbUrl);
                    // This registers the DbService if all goes well
                    service.connect();
                } catch (StorageException ex) {
                    throw new CommandException(t.localize(LocaleResources.LAUNCHER_MALFORMED_URL, dbUrl));
                } catch (ConnectionException ex) {
                    String error = ex.getMessage();
                    String message = ( error == null ? "" : " Error: " + error );
                    logger.log(Level.SEVERE, "Could not connect to: " + dbUrl + message, ex);
                    throw new CommandException(t.localize(LocaleResources.LAUNCHER_CONNECTION_ERROR, dbUrl), ex);
                } finally {
                    credsReg.unregister();
                }
            }
        }
        return ctx;
    }

    private boolean isVersionQuery(String[] args, boolean inShell) {
        // don't allow --version in the shell
        if (inShell) {
            return false;
        } else {
            return args[0].equals(Version.VERSION_OPTION);
        }
    }

}

