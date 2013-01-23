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

package com.redhat.thermostat.client.cli.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.Terminal;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.console.history.PersistentHistory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.cli.SimpleCommand;
import com.redhat.thermostat.common.config.ConfigUtils;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.Launcher;

public class ShellCommand extends SimpleCommand {

    private static final Logger logger = LoggingUtils.getLogger(ShellCommand.class);

    private static final String[] exitKeywords = { "exit", "quit", "q" };

    private static final String NAME = "shell";

    private static final String PROMPT = "Thermostat > ";

    private HistoryProvider historyProvider;

    private BundleContext bundleContext;
    
    static class HistoryProvider {
        public PersistentHistory get() {
            PersistentHistory history = null;
            try {
                history = new FileHistory(ConfigUtils.getHistoryFile());
            } catch (InvalidConfigurationException | IOException e) {
                /* no history available */
            }
            return history;
        }
    }

    public ShellCommand() {
        this(FrameworkUtil.getBundle(ShellCommand.class).getBundleContext(), new HistoryProvider());
    }

    ShellCommand(BundleContext context, HistoryProvider provider) {
        this.historyProvider = provider;
        this.bundleContext = context;
    }
    
    @Override
    public void run(CommandContext ctx) throws CommandException {
        Terminal term = TerminalFactory.create();
        PersistentHistory history = historyProvider.get();

        try {
            shellMainLoop(ctx, history, term);
        } catch (IOException ex) {
            throw new CommandException(ex);
        } finally {
            closeTerminal(term);
            if (history != null) {
                try {
                    history.flush();
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Unable to save history", e);
                }
            }
        }
    }

    private void closeTerminal(Terminal term) throws CommandException {
        try {
            term.restore();
        } catch (Exception e) {
            throw new CommandException(e);
        }
    }

    private void shellMainLoop(CommandContext ctx, History history, Terminal term) throws IOException, CommandException {
        ConsoleReader reader = new ConsoleReader(ctx.getConsole().getInput(), ctx.getConsole().getOutput(), term);
        if (history != null) {
            reader.setHistory(history);
        }
        while (handleConsoleInput(reader, ctx.getConsole())) { /* no-op; the loop conditional performs the action */ }
    }

    /**
     * @return true if the shell should continue accepting more input or false if the shell should quit
     */
    private boolean handleConsoleInput(ConsoleReader reader, Console console) throws IOException, CommandException {
        String line;
        try {
            line = reader.readLine(PROMPT);
        } catch (IllegalArgumentException iae) {
            if (iae.getMessage().endsWith(": event not found")) {
                console.getError().println(iae.getMessage());
                return true;
            }
            throw iae;
        }
        if (line == null) {
            return false;
        }
        line = line.trim();
        if (line.equals("")) {
            return true;
        } else if (Arrays.asList(exitKeywords).contains(line)) {
            return false;
        } else {
            launchCommand(line);
            return true;
        }
    }

    private void launchCommand(String line) throws CommandException {
        String[] parsed = line.split(" ");
        ServiceReference launcherRef = bundleContext.getServiceReference(Launcher.class.getName());
        if (launcherRef != null) {
            Launcher launcher = (Launcher) bundleContext.getService(launcherRef);
            launcher.setArgs(parsed);
            launcher.run();
        } else {
            throw new CommandException("Severe: Could not locate launcher");
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }

}

