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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.shared.locale.Translate;

public class HelpCommand extends AbstractCommand  {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();

    private static final int COMMANDS_COLUMNS_WIDTH = 14;
    private static final String APP_NAME = "thermostat";

    private static final CommandInfoComparator comparator = new CommandInfoComparator();

    private CommandInfoSource commandInfoSource;

    private Environment currentEnvironment;

    public void setCommandInfoSource(CommandInfoSource source) {
        this.commandInfoSource = source;
    }

    public void setEnvironment(Environment env) {
        currentEnvironment = env;
    }

    @Override
    public void run(CommandContext ctx) {
        Arguments args = ctx.getArguments();
        List<String> nonParsed = args.getNonOptionArguments();

        if (commandInfoSource == null) {
            ctx.getConsole().getError().print(translator.localize(LocaleResources.CANNOT_GET_COMMAND_INFO).getContents());
            return;
        }

        if (nonParsed.isEmpty()) {
            printCommandSummaries(ctx);
        } else {
            printCommandUsage(ctx, nonParsed.get(0));
        }
    }

    private void printCommandSummaries(CommandContext ctx) {
        ctx.getConsole().getOutput().print(translator.localize(LocaleResources.COMMAND_HELP_COMMAND_LIST_HEADER).getContents());

        TableRenderer renderer = new TableRenderer(2, COMMANDS_COLUMNS_WIDTH);

        Collection<CommandInfo> commandInfos = new ArrayList<>();
        for (CommandInfo info: commandInfoSource.getCommandInfos()) {
            if (info.getEnvironments().contains(currentEnvironment)) {
                commandInfos.add(info);
            }
        }

        List<CommandInfo> sortedCommandInfos = new ArrayList<>(commandInfos);

        Collections.sort(sortedCommandInfos, comparator);
        for (CommandInfo info : sortedCommandInfos) {
            printCommandSummary(renderer, info);
        }
        renderer.render(ctx.getConsole().getOutput());
    }

    private void printCommandSummary(TableRenderer renderer, CommandInfo info) {
        renderer.printLine(" " + info.getName(), info.getDescription());
    }

    private void printCommandUsage(CommandContext ctx, String cmdName) {
        try {
            CommandInfo info = commandInfoSource.getCommandInfo(cmdName);
            printHelp(ctx, info);
        } catch (CommandInfoNotFoundException notFound) {
            ctx.getConsole().getOutput().print(translator.localize(LocaleResources.UNKNOWN_COMMAND, cmdName).getContents());
            printCommandSummaries(ctx);
        }
    }

    private void printHelp(CommandContext ctx, CommandInfo info) {
        HelpFormatter helpFormatter = new HelpFormatter();
        
        PrintWriter pw = new PrintWriter(ctx.getConsole().getOutput());
        Options options = info.getOptions();
        String usage = APP_NAME + " " + info.getUsage() + "\n" + info.getDescription();
        String header = "";
        if (isAvailabilityNoteNeeded(info)) {
            header = header + getAvailabilityNote(info);
        }
        header = header + "\n" + APP_NAME + " " + info.getName();
        helpFormatter.printHelp(pw, 80, usage, header, options, 2, 4, null);
        pw.flush();
    }

    private boolean isAvailabilityNoteNeeded(CommandInfo info) {
        return !info.getEnvironments().contains(currentEnvironment);
    }

    /** Describe where command is available */
    private String getAvailabilityNote(CommandInfo info) {

        String availabilityNote = "";

        // there are two mutually exclusive environments: if an availability
        // note is needed, it will just be about one
        if (info.getEnvironments().contains(Environment.SHELL)) {
            availabilityNote = translator.localize(LocaleResources.COMMAND_AVAILABLE_INSIDE_SHELL).getContents();
        } else if (info.getEnvironments().contains(Environment.CLI)) {
            availabilityNote = translator.localize(LocaleResources.COMMAND_AVAILABLE_OUTSIDE_SHELL).getContents();
        } else {
            throw new AssertionError("Need to handle a third environment");
        }

        return availabilityNote;
    }

    @Override
    public boolean isStorageRequired() {
        return false;
    }

    private static class CommandInfoComparator implements Comparator<CommandInfo> {

        @Override
        public int compare(CommandInfo o1, CommandInfo o2) {
            // this command ('help') is always listed first
            if (o1.getName().equals(o2.getName())) {
                return 0;
            }
            if (o1.getName().equals("help")) {
                return -1;
            }
            if (o2.getName().equals("help")) {
                return 1;
            }

            return o1.getName().compareTo(o2.getName());
        }

    }

}

