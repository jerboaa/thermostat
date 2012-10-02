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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.CommandInfo;
import com.redhat.thermostat.common.cli.CommandInfoSource;
import com.redhat.thermostat.common.cli.SimpleCommand;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.launcher.CommonCommandOptions;

public class HelpCommand extends SimpleCommand {

    private static final int COMMANDS_COLUMNS_WIDTH = 14;
    private static final String NAME = "help";

    private static final CommandInfoComparator comparator = new CommandInfoComparator();

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
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference infosRef = context.getServiceReference(CommandInfoSource.class);
        CommandInfoSource infos = (CommandInfoSource) context.getService(infosRef);
        ctx.getConsole().getOutput().print("list of commands:\n\n");

        TableRenderer renderer = new TableRenderer(2, COMMANDS_COLUMNS_WIDTH);

        Collection<CommandInfo> commandInfos = infos.getCommandInfos();
        context.ungetService(infosRef);
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
        BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        ServiceReference infosRef = context.getServiceReference(CommandInfoSource.class);
        CommandInfoSource infos = (CommandInfoSource) context.getService(infosRef);
        CommandInfo info = infos.getCommandInfo(cmdName);
        context.ungetService(infosRef);
        if (info != null) {
            printHelp(ctx, info);
        } else {
            printCommandSummaries(ctx);
        }
    }

    private void printHelp(CommandContext ctx, CommandInfo info) {
        HelpFormatter helpFormatter = new HelpFormatter();
        PrintWriter pw = new PrintWriter(ctx.getConsole().getOutput());
        CommonCommandOptions commonOpts = new CommonCommandOptions();
        Options options = commonOpts.getOptionsFor(info);
        helpFormatter.printHelp(pw, 80, info.getName(), info.getUsage(), options, 2, 4, null, true);
        pw.flush();
    }

    @Override
    public String getName() {
        return NAME;
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
