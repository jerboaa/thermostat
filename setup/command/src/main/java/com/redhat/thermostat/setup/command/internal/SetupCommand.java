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

package com.redhat.thermostat.setup.command.internal;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.cli.DependencyServices;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.internal.utils.laf.ThemeManager;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.service.process.UNIXProcessHandler;
import com.redhat.thermostat.setup.command.internal.cli.CLISetup;
import com.redhat.thermostat.setup.command.internal.model.ThermostatSetup;
import com.redhat.thermostat.setup.command.locale.LocaleResources;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.utils.keyring.Keyring;

public class SetupCommand extends AbstractCommand {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static final String ORIG_CMD_ARGUMENT_NAME = "origArgs";
    private static final String NON_GUI_OPTION_NAME = "nonGui";
    private static final Logger logger = LoggingUtils.getLogger(SetupCommand.class);
    private final DependencyServices dependentServices = new DependencyServices();
    private SetupWindow mainWindow;
    private CommonPaths paths;
    private Launcher launcher;
    private Keyring keyring;
    private String[] origArgsList;
    private UNIXProcessHandler processHandler;

    @Override
    public void run(CommandContext ctx) throws CommandException {
        Arguments args = ctx.getArguments();
        if (args.hasArgument(ORIG_CMD_ARGUMENT_NAME)) {
            String origArgs = args.getArgument(ORIG_CMD_ARGUMENT_NAME);
            origArgsList = origArgs.split("\\|\\|\\|");
            if (isSetupInvocation(origArgsList)) {
                String[] optionArgs = Arrays.copyOfRange(origArgsList, 1, origArgsList.length);
                args = mergeOriginalArgs(optionArgs, args);
            }
        }
        
        
        this.paths = dependentServices.getService(CommonPaths.class);
        requireNonNull(paths, t.localize(LocaleResources.SERVICE_UNAVAILABLE_MESSAGE, "CommonPaths"));
        this.launcher = dependentServices.getService(Launcher.class);
        requireNonNull(launcher, t.localize(LocaleResources.SERVICE_UNAVAILABLE_MESSAGE, "Launcher"));
        this.keyring = dependentServices.getService(Keyring.class);
        requireNonNull(keyring, t.localize(LocaleResources.SERVICE_UNAVAILABLE_MESSAGE, "Keyring"));
        this.processHandler = dependentServices.getService(UNIXProcessHandler.class);
        requireNonNull(processHandler, t.localize(LocaleResources.SERVICE_UNAVAILABLE_MESSAGE, "UnixProcessHandler"));
        ThermostatSetup setup = createSetup();
        if (args.hasArgument(NON_GUI_OPTION_NAME)) {
            runCLISetup(setup, ctx.getConsole());
        } else {
            runGUISetup(setup);
        }
        
        runOriginalCommand(origArgsList);
    }

    private Arguments mergeOriginalArgs(String[] origArgsList, Arguments args) {
        logger.fine("Intercepted setup invocation 'setup' " + Arrays.asList(origArgsList).toString());
        return new MergedSetupArguments(args, origArgsList);
    }

    private void runCLISetup(ThermostatSetup setup, Console console) throws CommandException {
        CLISetup cliSetup = new CLISetup(setup, console);
        cliSetup.run();
    }

    private void runGUISetup(ThermostatSetup setup) throws CommandException {
        try {
            setLookAndFeel();
            createMainWindowAndRun(setup);
        } catch (InterruptedException | InvocationTargetException e) {
            throw new CommandException(t.localize(LocaleResources.SETUP_FAILED), e);
        }
    }

    private void runOriginalCommand(String[] args) {
        if (args == null) {
            return;
        }
        if (args.length == 0) {
            throw new AssertionError("Original command args were empty!");
        }
        if (isSetupInvocation(args)) {
            // Do not run setup recursively
            return;
        }
        logger.log(Level.FINE, "Running intercepted command '" + args[0] + "' after setup.");
        launcher.run(args, false);
    }
    
    private boolean isSetupInvocation(String[] args) {
        return args[0].equals("setup");
    }

    public void setPaths(CommonPaths paths) {
        dependentServices.addService(CommonPaths.class, paths);
    }
    
    public void setLauncher(Launcher launcher) {
        dependentServices.addService(Launcher.class, launcher);
    }
    
    public void setKeyring(Keyring keyring) {
        dependentServices.addService(Keyring.class, keyring);
    }

    public void setProcessHandler(UNIXProcessHandler processHandler) {
        dependentServices.addService(UNIXProcessHandler.class, processHandler);
    }
    
    public void setServicesUnavailable() {
        dependentServices.removeService(Launcher.class);
        dependentServices.removeService(CommonPaths.class);
        dependentServices.removeService(Keyring.class);
        dependentServices.removeService(UNIXProcessHandler.class);
    }

    public boolean isStorageRequired() {
        return false;
    }

    // package-private for testing
    void createMainWindowAndRun(ThermostatSetup setup) throws CommandException {
        mainWindow = new SetupWindow(setup);
        mainWindow.run();
    }

    // package-private for testing
    void setLookAndFeel() throws InvocationTargetException, InterruptedException {
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ThemeManager themeManager = ThemeManager.getInstance();
                themeManager.setLAF();
            }
        });
    }
    
    // package-private for testing
    ThermostatSetup createSetup() {
        return ThermostatSetup.create(launcher, paths, processHandler, keyring);
    }
    
    static class MergedSetupArguments implements Arguments {

        private static final String SINGLE_DASH = "-";
        private static final String DOUBLE_DASH = "--";
        private static final String NON_GUI_SHORT_OPT = "c";
        
        private final Arguments argsDelegate;
        private final String[] origArgs;
        private final Map<String, String> additionalOptions;
        
        MergedSetupArguments(Arguments args, String[] origArgs) {
            this.origArgs = origArgs;
            this.argsDelegate = Objects.requireNonNull(args);
            this.additionalOptions = buildAdditionalOptions(origArgs);
        }
        
        private Map<String, String> buildAdditionalOptions(String[] origArgs) {
            Map<String, String> options = new HashMap<>();
            for (int i = 0; i < origArgs.length; i++) {
                String opt = origArgs[i];
                String value = "NONE";
                if (opt.startsWith(DOUBLE_DASH)) {
                    if (i + 1 < origArgs.length && isOptionArg(origArgs, i)) {
                        value = origArgs[i + 1];
                        i++; // skip argument
                    }
                    options.put(opt.substring(2), value);
                    continue;
                } else if (opt.startsWith(SINGLE_DASH)) {
                    // This is a poor-man's version of short-arg parsing for
                    // non-gui setup option.
                    String cleanedOp = opt.substring(1);
                    if (cleanedOp.equals(NON_GUI_SHORT_OPT)) {
                        options.put(NON_GUI_OPTION_NAME, Boolean.TRUE.toString());
                    }
                    continue;
                } else {
                    throw new AssertionError("Invalid option for setup: " + opt);
                }
            }
            return options;
        }
        
        private boolean isOptionArg(String[] origArgs, int i) {
            return !(origArgs[i + 1].startsWith(SINGLE_DASH) || origArgs[i + 1].startsWith(DOUBLE_DASH));
        }

        @Override
        public List<String> getNonOptionArguments() {
            return argsDelegate.getNonOptionArguments();
        }

        @Override
        public boolean hasArgument(String name) {
            boolean delegateHasArgument = argsDelegate.hasArgument(name);
            if (delegateHasArgument) {
                return true;
            }
            return additionalOptions.keySet().contains(name);
        }

        @Override
        public String getArgument(String name) {
            String arg = argsDelegate.getArgument(name);
            if (arg != null) {
                return arg;
            }
            return additionalOptions.get(name);
        }
        
        @Override
        public String toString() {
            return "setup [delegate=" + argsDelegate.toString() + ",origArgs=" + Arrays.asList(origArgs).toString() + "]";
        }
        
    }
}

