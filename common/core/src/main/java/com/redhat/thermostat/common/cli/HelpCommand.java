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

package com.redhat.thermostat.common.cli;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HelpCommand implements Command {

    private static final int COMMANDS_COLUMNS_WIDTH = 15;
    private static final String NAME = "help";
    private static final String DESCRIPTION = "show help for a given command or help overview";
    private static final String USAGE = DESCRIPTION;

    private static final CommandComparator comparator = new CommandComparator();

    @Override
    public void run(CommandContext ctx) {
        Arguments args = ctx.getArguments();
        List<String> nonParsed = args.getNonOptionArguments();
        if (nonParsed.isEmpty()) {
            printCommandSummaries(ctx);
        } else {
            printCommandUsage(ctx, nonParsed.get(0));
        }
    }

    private void printCommandSummaries(CommandContext ctx) {
        CommandRegistry cmdRegistry = ctx.getCommandRegistry();

        StringBuilder out = new StringBuilder();
        out.append("list of commands:\n\n");

        Collection<Command> commands = cmdRegistry.getRegisteredCommands();
        List<Command> sortedCommands = new ArrayList<>(commands);

        Collections.sort(sortedCommands, comparator);
        for (Command cmd : sortedCommands) {
            printCommandSummary(out, cmd);
        }
        ctx.getConsole().getOutput().print(out);
    }

    private void printCommandSummary(StringBuilder out, Command cmd) {
        out.append(" ");
        out.append(cmd.getName());
        for (int i = 0; i < COMMANDS_COLUMNS_WIDTH - cmd.getName().length() - 1; i++) {
            out.append(" ");
        }
        out.append(cmd.getDescription());
        out.append("\n");
    }

    private void printCommandUsage(CommandContext ctx, String cmdName) {
        Command cmd = ctx.getCommandRegistry().getCommand(cmdName);
        if (cmd != null) {
            CommandLineArgumentsParser cliParser = new CommandLineArgumentsParser();
            cliParser.printHelp(ctx, cmd);
        } else {
            printCommandSummaries(ctx);
        }
    }

    @Override
    public void disable() { /* NO-OP */ }

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
        return false;
    }

    private static class CommandComparator implements Comparator<Command> {

        @Override
        public int compare(Command o1, Command o2) {
            // this command ('help') is always listed first
            if (o1.getName().equals(o2.getName())) {
                return 0;
            }
            if (o1.getName().equals(NAME)) {
                return -1;
            }
            if (o2.getName().equals(NAME)) {
                return 1;
            }

            return o1.getName().compareTo(o2.getName());
        }

    }

}
