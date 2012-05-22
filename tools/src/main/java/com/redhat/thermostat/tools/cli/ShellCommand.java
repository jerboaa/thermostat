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

package com.redhat.thermostat.tools.cli;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.Collections;

import jline.Terminal;
import jline.TerminalFactory;
import jline.console.ConsoleReader;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.ArgumentSpec;
import com.redhat.thermostat.common.cli.Command;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandException;
import com.redhat.thermostat.common.cli.Launcher;

public class ShellCommand implements Command {

    private static final String NAME = "shell";

    private static final String DESCRIPTION = "launches the Thermostat interactive shell";

    private static final String USAGE = DESCRIPTION;

    private static final String PROMPT = "Thermostat > ";

    private CommandContext context;

    @Override
    public void run(CommandContext ctx) throws CommandException {
        context = ctx;
        Terminal term = TerminalFactory.create();
        try {
            shellMainLoop(ctx, term);
        } catch (IOException ex) {
            throw new CommandException(ex);
        } finally {
            closeTerminal(term);
        }
    }

    private void closeTerminal(Terminal term) throws CommandException {
        try {
            term.restore();
        } catch (Exception e) {
            throw new CommandException(e);
        }
    }

    private void shellMainLoop(CommandContext ctx, Terminal term) throws IOException, CommandException {
        ConsoleReader reader = new ConsoleReader(ctx.getConsole().getInput(), new OutputStreamWriter(ctx.getConsole().getOutput()), term);
        while (handleConsoleInput(reader));
    }

    private boolean handleConsoleInput(ConsoleReader reader) throws IOException, CommandException {
        String line = reader.readLine(PROMPT).trim();
        if (line.equals("")) {
            return true;
        } else if (line.equals("exit")) {
            return false;
        } else {
            launchCommand(line);
            return true;
        }
    }

    private void launchCommand(String line) throws CommandException {
        String[] parsed = line.split(" ");
        BundleContext bCtx = context.getCommandContextFactory().getBundleContext();
        ServiceReference launcherRef = bCtx.getServiceReference(Launcher.class.getName());
        if (launcherRef != null) {
            Launcher launcher = (Launcher) bCtx.getService(launcherRef);
            launcher.run(parsed);
        } else {
            throw new CommandException("Severe: Could not locate launcher");
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
        return Collections.emptyList();
    }

    @Override
    public boolean isStorageRequired() {
        // TODO Auto-generated method stub
        return false;
    }

}
