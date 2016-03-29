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

package com.redhat.thermostat.launcher.internal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jline.Terminal;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.history.FileHistory;
import jline.console.history.History;
import jline.console.history.PersistentHistory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.ApplicationInfo;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Console;
import com.redhat.thermostat.common.config.ClientPreferences;
import com.redhat.thermostat.common.config.experimental.ConfigurationInfoSource;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.Launcher;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.storage.core.DbService;

public class ShellCommand extends AbstractCommand {

    private static final Logger logger = LoggingUtils.getLogger(ShellCommand.class);
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private static final String[] exitKeywords = { "exit", "quit", "q" };

    private HistoryProvider historyProvider;
    private Version version;

    private BundleContext bundleContext;

    private final ShellPrompt shellPrompt;
    private CommandInfoSource commandInfoSource;
    private final ClientPreferences prefs;
    private TabCompletion tabCompletion;

    static class HistoryProvider {

        private CommonPaths paths;

        HistoryProvider(CommonPaths paths) {
            this.paths = paths;
        }

        public PersistentHistory get() {
            PersistentHistory history = null;
            try {
                history = new FileHistory(paths.getUserHistoryFile());
            } catch (InvalidConfigurationException | IOException e) {
                /* no history available */
            }
            return history;
        }
    }

    public ShellCommand(BundleContext context, CommonPaths paths, ConfigurationInfoSource config) {
        this(context, new Version(), new HistoryProvider(paths), config, new ClientPreferences(paths));
    }

    ShellCommand(BundleContext context, Version version, HistoryProvider provider, ConfigurationInfoSource config, ClientPreferences prefs) {
        this.historyProvider = provider;
        this.bundleContext = context;
        this.version = version;

        this.prefs = prefs;
        this.shellPrompt = new ShellPrompt();

        try {
            Map<String, String> promptConfig = config.getConfiguration("shell-command", "shell-prompt.conf");
            this.shellPrompt.overridePromptConfig(promptConfig);
        } catch (IOException e) {
            //Do nothing
        }
    }

    @Override
    public void run(CommandContext ctx) throws CommandException {
        Terminal term = TerminalFactory.create();
        PersistentHistory history = historyProvider.get();

        try {
            ctx.getConsole().getOutput().println(version.getVersionInfo());
            String userGuideUrl = new ApplicationInfo().getUserGuide();
            String userGuideMessage = t.localize(LocaleResources.COMMAND_SHELL_USER_GUIDE, userGuideUrl).getContents();
            ctx.getConsole().getOutput().println(userGuideMessage);
            shellMainLoop(ctx, history, term);
        } catch (IOException ex) {
            throw new CommandException(t.localize(LocaleResources.COMMAND_SHELL_IO_EXCEPTION), ex);
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
            logger.log(Level.WARNING, "Error restoring terminal state.", e);
        }
    }

    private void shellMainLoop(CommandContext ctx, History history, Terminal term) throws IOException, CommandException {
        ConsoleReader reader = new ConsoleReader(ctx.getConsole().getInput(), ctx.getConsole().getOutput(), term);
        if (reader.getCompleters().isEmpty() && commandInfoSource != null && tabCompletion != null) {
            tabCompletion.setupTabCompletion(reader, commandInfoSource, bundleContext, prefs);
        }
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
        line = reader.readLine(shellPrompt.getPrompt());
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
        ShellArgsParser parser = new ShellArgsParser(line);
        String[] parsed = parser.parse();
        ShellArgsParser.Issues issues = parser.getParseIssues();
        if (!issues.getAllIssues().isEmpty()) {
            ShellArgsParser.IssuesFormatter issuesFormatter = new ShellArgsParser.IssuesFormatter();
            if (!issues.getWarnings().isEmpty()) {
                String formattedIssues = issuesFormatter.format(issues.getWarnings());
                logger.warning(t.localize(LocaleResources.PARSER_WARNING, formattedIssues).getContents());
            }
            if (!issues.getErrors().isEmpty()) {
                String formattedIssues = issuesFormatter.format(issues.getErrors());
                logger.warning(t.localize(LocaleResources.PARSER_ERROR, formattedIssues).getContents());
                return;
            }
        }
        ServiceReference launcherRef = bundleContext.getServiceReference(Launcher.class.getName());
        if (launcherRef != null) {
            Launcher launcher = (Launcher) bundleContext.getService(launcherRef);
            launcher.run(parsed, true);
            bundleContext.ungetService(launcherRef);
        } else {
            throw new CommandException(t.localize(LocaleResources.MISSING_LAUNCHER));
        }
    }

    void setTabCompletion(TabCompletion tabCompletion) {
        this.tabCompletion = tabCompletion;
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }

    public void dbServiceAvailable(DbService dbService) {
        this.shellPrompt.storageConnected(dbService);
    }

    public void dbServiceUnavailable() {
        this.shellPrompt.storageDisconnected();
    }

    public void setCommandInfoSource(final CommandInfoSource source) {
        this.commandInfoSource = source;
    }

}

