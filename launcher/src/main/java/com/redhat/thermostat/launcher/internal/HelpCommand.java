/*
 * Copyright 2012-2017 Red Hat, Inc.
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.common.utils.StringUtils;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandGroupMetadata;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.cli.AbstractCommand;
import com.redhat.thermostat.common.cli.Arguments;
import com.redhat.thermostat.common.cli.CommandContext;
import com.redhat.thermostat.common.cli.TableRenderer;
import com.redhat.thermostat.launcher.FrameworkOptions;
import com.redhat.thermostat.shared.locale.Translate;

public class HelpCommand extends AbstractCommand  {

    private static final Translate<LocaleResources> translator = LocaleResources.createLocalizer();
    private static final Logger logger = LoggingUtils.getLogger(HelpCommand.class);
    static final String COMMAND_NAME = "help";

    private static final int COMMANDS_COLUMNS_WIDTH = 14;
    public static final int MAX_COLUMN_WIDTH = 80;
    private static final String APP_NAME = "thermostat";

    private static final CommandInfoComparator comparator = new CommandInfoComparator();
    private static final CommandGroupMetadata UNGROUPED_COMMANDS_METADATA = new CommandGroupMetadata(null, null, Integer.MAX_VALUE);

    private CommandInfoSource commandInfoSource;
    private CommandGroupMetadataSource commandGroupMetadataSource;

    private Environment currentEnvironment;

    private SortedMap<CommandGroupMetadata, SortedSet<CommandInfo>> commandGroupMap;
    private Map<String, CommandGroupMetadata> commandGroupMetadataMap;
    private Set<CommandInfo> contextualCommands = new HashSet<>();

    public void setCommandInfoSource(CommandInfoSource source) {
        this.commandInfoSource = source;
    }

    public void setCommandGroupMetadataSource(CommandGroupMetadataSource commandGroupMetadataSource) {
        this.commandGroupMetadataSource = commandGroupMetadataSource;
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

        if (commandGroupMetadataSource == null) {
            ctx.getConsole().getError().print(translator.localize(LocaleResources.CANNOT_GET_COMMAND_GROUP_METADATA).getContents());
            return;
        }

        for (CommandInfo info: commandInfoSource.getCommandInfos()) {
            if (info.getEnvironments().contains(currentEnvironment)) {
                contextualCommands.add(info);
            }
        }

        commandGroupMetadataMap = new HashMap<>(commandGroupMetadataSource.getCommandGroupMetadata());
        commandGroupMap = createCommandGroupMap();

        if (nonParsed.isEmpty()) {
            if (currentEnvironment == Environment.CLI) {
                //CLI only since the framework will already be
                //started for a command invoked via shell
                printOptionSummaries(ctx);
            }

            printCommandSummaries(ctx);
        } else {
            printCommandUsage(ctx, nonParsed.get(0));
        }
    }

    private SortedMap<CommandGroupMetadata, SortedSet<CommandInfo>> createCommandGroupMap() {
        Set<CommandInfo> seen = new HashSet<>();
        Map<String, SortedSet<CommandInfo>> groupNameMap = new HashMap<>();
        for (CommandInfo commandInfo : contextualCommands) {
            for (String commandGroup : commandInfo.getCommandGroups()) {
                seen.add(commandInfo);
                if (!groupNameMap.containsKey(commandGroup)) {
                    groupNameMap.put(commandGroup, new TreeSet<>(comparator));
                }
                groupNameMap.get(commandGroup).add(commandInfo);
            }
        }

        SortedMap<CommandGroupMetadata, SortedSet<CommandInfo>> result = new TreeMap<>(new CommandGroupMetadataComparator());
        for (Map.Entry<String, SortedSet<CommandInfo>> entry : groupNameMap.entrySet()) {
            String groupName = entry.getKey();
            CommandGroupMetadata metadata = commandGroupMetadataMap.get(groupName);
            if (metadata == null) {
                logger.warning("No metadata provided for command group \"" + groupName + "\"");
                metadata = new CommandGroupMetadata(groupName, groupName, Integer.MAX_VALUE);
                commandGroupMetadataMap.put(groupName, metadata);
            }
            result.put(metadata, entry.getValue());
        }

        SortedSet<CommandInfo> ungrouped = new TreeSet<>(comparator);
        ungrouped.addAll(contextualCommands);
        ungrouped.removeAll(seen);
        result.put(UNGROUPED_COMMANDS_METADATA, ungrouped);

        return result;
    }

    private void printCommandSummaries(CommandContext ctx) {
        ctx.getConsole().getOutput().print(translator.localize(LocaleResources.COMMAND_HELP_COMMAND_LIST_HEADER).getContents());

        TableRenderer renderer = new TableRenderer(2, COMMANDS_COLUMNS_WIDTH);

        for (Map.Entry<CommandGroupMetadata, SortedSet<CommandInfo>> group : commandGroupMap.entrySet()) {
            CommandGroupMetadata commandGroupMetadata = group.getKey();
            if (commandGroupMetadata.equals(UNGROUPED_COMMANDS_METADATA) || group.getValue().isEmpty()) {
                continue;
            }
            renderer.printLine(translator.localize(LocaleResources.COMMAND_GROUP_HEADER,
                    commandGroupMetadata.getDescription()).getContents(), "");
            for (CommandInfo info : group.getValue()) {
                printCommandSummary(renderer, info);
            }
            renderer.printLine("", "");
        }
        for (CommandInfo ungroupedCommand : commandGroupMap.get(UNGROUPED_COMMANDS_METADATA)) {
            printCommandSummary(renderer, ungroupedCommand);
        }

        renderer.render(ctx.getConsole().getOutput());
    }

    private void printOptionSummaries(CommandContext ctx) {
        ctx.getConsole().getOutput().print(translator.localize(LocaleResources.COMMAND_HELP_COMMAND_OPTION_HEADER).getContents());

        TableRenderer renderer = new TableRenderer(2, COMMANDS_COLUMNS_WIDTH);

        renderer.printLine(" " + Version.VERSION_OPTION, "display the version of the current thermostat installation");

        for (FrameworkOptions opt : FrameworkOptions.values()) {
            renderer.printLine(" " + opt.getOptString(), opt.getDescription());
        }

        renderer.render(ctx.getConsole().getOutput());

        ctx.getConsole().getOutput().println();
    }

    private void printCommandSummary(TableRenderer renderer, CommandInfo info) {
        renderer.printLine(" " + info.getName(), info.getSummary());
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
            header = getAvailabilityNote(info);
        }
        header = header + "\n" + APP_NAME + " " + info.getName();
        Option help = CommonOptions.getHelpOption();
        options.addOption(help);
        helpFormatter.printHelp(pw, MAX_COLUMN_WIDTH, usage, header, options, 2, 4, null);

        if (!info.getSubcommands().isEmpty()) {
            pw.println();
            helpFormatter.printWrapped(pw, MAX_COLUMN_WIDTH, translator.localize(LocaleResources.SUBCOMMANDS_SECTION_HEADER).getContents());
            pw.println();
            for (PluginConfiguration.Subcommand subcommand : info.getSubcommands()) {
                pw.println(translator.localize(LocaleResources.SUBCOMMAND_ENTRY_HEADER, subcommand.getName()).getContents());
                pw.println(subcommand.getDescription());
                Options o = subcommand.getOptions();
                helpFormatter.printOptions(pw, MAX_COLUMN_WIDTH, o, 2, 4);
                if (!o.getOptions().isEmpty()) {
                    pw.println();
                }
            }
        }

        SortedSet<CommandInfo> relatedCommands = new TreeSet<>(comparator);
        for (String commandGroup : info.getCommandGroups()) {
            relatedCommands.addAll(commandGroupMap.get(commandGroupMetadataMap.get(commandGroup)));
        }
        relatedCommands.remove(info);
        if (!relatedCommands.isEmpty()) {
            pw.println();
            pw.println(translator.localize(LocaleResources.SEE_ALSO_HEADER).getContents());
            pw.print(' ');
            RelatedCommandsFormatter relatedCommandsFormatter = new RelatedCommandsFormatter(relatedCommands);
            pw.println(relatedCommandsFormatter.format());
        }

        pw.flush();
    }

    private boolean isAvailabilityNoteNeeded(CommandInfo info) {
        return !info.getEnvironments().contains(currentEnvironment);
    }

    /** Describe where command is available */
    private String getAvailabilityNote(CommandInfo info) {
        // there are two mutually exclusive environments: if an availability
        // note is needed, it will just be about one
        if (info.getEnvironments().contains(Environment.SHELL)) {
            return translator.localize(LocaleResources.COMMAND_AVAILABLE_INSIDE_SHELL).getContents();
        } else if (info.getEnvironments().contains(Environment.CLI)) {
            return translator.localize(LocaleResources.COMMAND_AVAILABLE_OUTSIDE_SHELL).getContents();
        } else {
            throw new AssertionError("Need to handle a third environment");
        }
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

    private static class CommandGroupMetadataComparator implements Comparator<CommandGroupMetadata> {
        @Override
        public int compare(CommandGroupMetadata cgm1, CommandGroupMetadata cgm2) {
            int sortOrderComparison = Integer.compare(cgm1.getSortOrder(), cgm2.getSortOrder());
            if (sortOrderComparison != 0) {
                return sortOrderComparison;
            }
            return StringUtils.compare(cgm1.getName(), cgm2.getName());
        }
    }

    private static class RelatedCommandsFormatter {

        private final Set<CommandInfo> commandInfos;

        public RelatedCommandsFormatter(Set<CommandInfo> commandInfos) {
            this.commandInfos = commandInfos;
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            for (CommandInfo info : commandInfos) {
                String next = info.getName();
                if (sb.length() + (" " + next + ",").length() > MAX_COLUMN_WIDTH) {
                    sb.append("\n ");
                }
                sb.append(' ').append(next).append(",");
            }
            String result = sb.toString();
            if (result.endsWith(",")) {
                result = result.substring(0, result.length() - 1);
            }
            return result;
        }
    }

}

