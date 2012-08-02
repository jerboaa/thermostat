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

package com.redhat.thermostat.launcher.internal;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.redhat.thermostat.bundles.OSGiRegistryService;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.appctx.ApplicationContext;
import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandContextFactory;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.CommandRegistry;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.storage.ConnectionException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.launcher.CommonCommandOptions;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.utils.keyring.Keyring;

public class LauncherImpl implements Launcher {

    private static final String UNKNOWN_COMMAND_MESSAGE = "unknown command '%s'\n";

    private ClientPreferences prefs;

    private String[] args;

    private CommandContextFactory cmdCtxFactory;

    private int usageCount = 0;
    private final Semaphore argsBarrier = new Semaphore(0);

    private BundleContext context;
    private OSGiRegistryService registry;
    
    public LauncherImpl(BundleContext context, CommandContextFactory cmdCtxFactory,
            OSGiRegistryService registry) {
        this.context = context;
        this.cmdCtxFactory = cmdCtxFactory;
        this.registry = registry;
    }

    @Override
    public synchronized void run() {
        usageCount++;
        try {
            argsBarrier.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            // TODO move this logging init out where it's not part of every command launch
            initLogging();
            if (hasNoArguments()) {
                runHelpCommand();
            } else {
                runCommandFromArguments();
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

    private boolean isLastLaunch() {
        return usageCount == 0;
    }

    private void shutdown() throws InternalError {
        try {
            TimerFactory tf = ApplicationContext.getInstance().getTimerFactory();
            if (tf != null) {
                tf.shutdown();
            }
            context.getBundle(0).stop();
        } catch (BundleException e) {
            throw (InternalError) new InternalError().initCause(e);
        }
    }

    public void setPreferences(ClientPreferences prefs) {
        this.prefs = prefs;
    }

    private void initLogging() {
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

    private boolean hasNoArguments() {
        return args.length == 0;
    }

    private void runHelpCommand() {
        runCommand("help", new String[0]);
    }

    private void runCommandFromArguments() {
        runCommand(args[0], Arrays.copyOfRange(args, 1, args.length));
    }

    private void runCommand(String cmdName, String[] cmdArgs) {
        try {
            parseArgsAndRunCommand(cmdName, cmdArgs);
        } catch (CommandException e) {
            cmdCtxFactory.getConsole().getError().println(e.getMessage());
        }
    }

    private void parseArgsAndRunCommand(String cmdName, String[] cmdArgs) throws CommandException {

        PrintStream out = cmdCtxFactory.getConsole().getOutput();
        try {
            registry.addBundlesFor(cmdName);
        } catch (BundleException | IOException e) {
            // If this happens we definitely need to do something about it, and the
            // trace will be immeasurably helpful in figuring out what is wrong.
            out.println("Could not load necessary bundles for: " + cmdName);
            e.printStackTrace();
            out.print(e.getStackTrace());
            return;
        }

        Command cmd = getCommand(cmdName);
        if (cmd == null) {
            out.print(String.format(UNKNOWN_COMMAND_MESSAGE, cmdName));
            runHelpCommand();
            return;
        }
        CommonCommandOptions commonOpts = new CommonCommandOptions();
        Collection<ArgumentSpec> acceptedOptions = commonOpts.getAcceptedOptionsFor(cmd);
        Arguments args = parseCommandArguments(cmdArgs, acceptedOptions);
        setupLogLevel(args);
        CommandContext ctx = setupCommandContext(cmd, args);
        cmd.run(ctx);
    }

    private void setupLogLevel(Arguments args) {
        if (args.hasArgument(CommonCommandOptions.LOG_LEVEL_ARG)) {
            String levelOption = args.getArgument(CommonCommandOptions.LOG_LEVEL_ARG);
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

    private Arguments parseCommandArguments(String[] cmdArgs, Collection<ArgumentSpec> acceptedArguments)
            throws CommandLineArgumentParseException {

        CommandLineArgumentsParser cliArgsParser = new CommandLineArgumentsParser();
        cliArgsParser.addArguments(acceptedArguments);
        Arguments args = cliArgsParser.parse(cmdArgs);
        return args;
    }

    private CommandContext setupCommandContext(Command cmd, Arguments args) throws CommandException {

        if (prefs == null) {
            prefs = new ClientPreferences(OSGIUtils.getInstance().getService(Keyring.class));
        }
        
        CommandContext ctx = cmdCtxFactory.createContext(args);
        if (cmd.isStorageRequired()) {
            String dbUrl = ctx.getArguments().getArgument(CommonCommandOptions.DB_URL_ARG);
            if (dbUrl == null) {
                dbUrl = prefs.getConnectionUrl();
            }
            String username = ctx.getArguments().getArgument(CommonCommandOptions.USERNAME_ARG);
            String password = ctx.getArguments().getArgument(CommonCommandOptions.PASSWORD_ARG);
            try {
                ctx.getAppContextSetup().setupAppContext(dbUrl, username, password);
            } catch (ConnectionException ex) {
                throw new CommandException("Could not connect to: " + dbUrl, ex);
            }
        }
        return ctx;
    }

}
