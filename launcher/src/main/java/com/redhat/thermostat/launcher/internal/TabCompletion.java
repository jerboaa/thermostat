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

import com.redhat.thermostat.common.cli.CliCommandOption;
import com.redhat.thermostat.common.cli.CompleterService;
import com.redhat.thermostat.common.cli.TabCompleter;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import org.apache.commons.cli.Option;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.redhat.thermostat.launcher.internal.TreeCompleter.createStringNode;

public class TabCompletion {

    private static final String LONG_OPTION_PREFIX = "--";
    private static final String SHORT_OPTION_PREFIX = "-";

    /**
     * Not intended to be available to plugins, only built-ins such as vmId and agentId completion.
     * This is why reference equality is used when checking for this constant, otherwise it is too
     * easy for plugins to "hijack" this class and offer completions for commands from other plugins.
     */
    static final Set<String> ALL_COMMANDS_COMPLETER = Collections.singleton("ALL_COMMANDS_COMPLETER");

    private TreeCompleter treeCompleter;
    private Map<String, TreeCompleter.Node> commandMap;
    private Map<TreeCompleter.Node, Set<TreeCompleter.Node>> subcommandMap;

    public TabCompletion() {
        this(new TreeCompleter(), new HashMap<String, TreeCompleter.Node>(),
                new HashMap<TreeCompleter.Node, Set<TreeCompleter.Node>>());
    }

    /*
     * Testing only
     */
    TabCompletion(TreeCompleter treeCompleter, Map<String, TreeCompleter.Node> commandMap,
                  Map<TreeCompleter.Node, Set<TreeCompleter.Node>> subcommandMap) {
        this.treeCompleter = treeCompleter;
        this.commandMap = commandMap;
        this.subcommandMap = subcommandMap;

        treeCompleter.setAlphabeticalCompletions(true);
    }

    public void addCompleterService(CompleterService service) {
        for (String commandName : getCommandsForService(service)) {
            TreeCompleter.Node commandNode = getCommandByName(commandName);
            addCompleterServiceImpl(commandNode, service);
        }
    }

    private Set<String> getCommandsForService(CompleterService service) {
        if (ALL_COMMANDS_COMPLETER == service.getCommands()) {
            return commandMap.keySet();
        } else {
            return service.getCommands();
        }
    }

    private TreeCompleter.Node getCommandByName(String commandName) {
        if (!commandMap.containsKey(commandName)) {
            TreeCompleter.Node command = createStringNode(commandName);
            commandMap.put(commandName, command);
        }
        return commandMap.get(commandName);
    }

    private void addCompleterServiceImpl(TreeCompleter.Node commandNode, CompleterService service) {
        addTopLevelCommandOptionCompletions(commandNode, service);
        addSubCommandCompletionsIfRequired(commandNode, service);
    }

    private void addTopLevelCommandOptionCompletions(TreeCompleter.Node commandNode, CompleterService service) {
        for (Map.Entry<CliCommandOption, ? extends TabCompleter> entry : service.getOptionCompleters().entrySet()) {
            CliCommandOption cliCommandOption = entry.getKey();
            if (cliCommandOption == CliCommandOption.POSITIONAL_ARG_COMPLETION) {
                TreeCompleter.Node node = new TreeCompleter.Node(commandNode.getTag() + " completer", entry.getValue());
                node.setRestartNode(commandNode);
                commandNode.addBranch(node);
            }
            if (completerIsApplicable(cliCommandOption, commandNode)) {
                TreeCompleter.Node completionNode = new TreeCompleter.Node(commandNode.getTag() + " completer", entry.getValue());
                completionNode.setRestartNode(commandNode);
                addNodeByOption(commandNode, cliCommandOption, completionNode);
            }
        }
    }

    private static boolean completerIsApplicable(CliCommandOption option, TreeCompleter.Node node) {
        boolean matchesLongOpt = false;
        boolean matchesShortOpt = false;
        for (TreeCompleter.Node branch : node.getBranches()) {
            if (branch.getTag().equals("--" + option.getLongOpt())) {
                matchesLongOpt = true;
            } else if (branch.getTag().equals("-" + option.getOpt())) {
                matchesShortOpt = true;
            }
        }
        return matchesLongOpt && matchesShortOpt;
    }

    private boolean addNodeByOption(TreeCompleter.Node parent, CliCommandOption option, TreeCompleter.Node toAdd) {
        TreeCompleter.Node shortOptChild = findChildNode(parent, "-" + option.getOpt());
        TreeCompleter.Node longOptChild = findChildNode(parent, "--" + option.getLongOpt());
        if (shortOptChild == null || longOptChild == null) {
            return false;
        }
        shortOptChild.addBranch(toAdd);
        longOptChild.addBranch(toAdd);
        return true;
    }

    private TreeCompleter.Node findChildNode(TreeCompleter.Node parent, String childTag) {
        for (TreeCompleter.Node node : parent.getBranches()) {
            if (node.getTag().equals(childTag)) {
                return node;
            }
        }
        return null;
    }

    private void addSubCommandCompletionsIfRequired(TreeCompleter.Node commandNode, CompleterService service) {
        for (TreeCompleter.Node subcommand : getSubcommands(commandNode)) {
            addTopLevelCommandOptionCompletions(subcommand, service);
        }
        Map<String, Map<CliCommandOption, ? extends TabCompleter>> subcommandCompleters = service.getSubcommandCompleters();
        if (subcommandCompleters == null || subcommandCompleters.isEmpty()) {
            return;
        }
        for (String subcommand : subcommandCompleters.keySet()) {
            for (Map.Entry<CliCommandOption, ? extends TabCompleter> entry : subcommandCompleters.get(subcommand).entrySet()) {
                CliCommandOption cliCommandOption = entry.getKey();
                TabCompleter completer = entry.getValue();
                TreeCompleter.Node subcommandNode = findChildNode(commandNode, subcommand);
                if (subcommandNode == null) {
                    break;
                }
                TreeCompleter.Node completionNode = new TreeCompleter.Node(subcommand + " completer", completer);
                completionNode.setRestartNode(commandNode);
                addNodeByOption(subcommandNode, cliCommandOption, completionNode);
            }
        }
    }

    private Set<TreeCompleter.Node> getSubcommands(TreeCompleter.Node commandNode) {
        if (!subcommandMap.containsKey(commandNode)) {
            return Collections.emptySet();
        }
        return subcommandMap.get(commandNode);
    }

    public void removeCompleterService(CompleterService service) {
        for (String commandName : getCommandsForService(service)) {
            TreeCompleter.Node command = commandMap.get(commandName);
            if (command != null) {
                removeCompleterServiceImpl(command, service);
            }
        }
    }

    private void removeCompleterServiceImpl(TreeCompleter.Node commandNode, CompleterService service) {
        removeTopLevelCommandOptionCompletions(commandNode, service);
        removeSubCommandCompletionsIfRequired(commandNode, service);
    }

    private void removeTopLevelCommandOptionCompletions(TreeCompleter.Node commandNode, CompleterService service) {
        if (service.getOptionCompleters() == null) {
            return;
        }
        for (CliCommandOption cliCommandOption : service.getOptionCompleters().keySet()) {
            if (cliCommandOption == CliCommandOption.POSITIONAL_ARG_COMPLETION) {
                commandNode.removeByTag(commandNode.getTag() + " completer");
            } else {
                for (TreeCompleter.Node node : commandNode.getBranches()) {
                    if (node.getTag().equals("-" + cliCommandOption.getOpt())
                            || node.getTag().equals("--" + cliCommandOption.getLongOpt())) {
                        node.clear();
                    }
                }
            }
        }
    }

    private void removeSubCommandCompletionsIfRequired(TreeCompleter.Node commandNode, CompleterService service) {
        for (TreeCompleter.Node subcommand : getSubcommands(commandNode)) {
            removeTopLevelCommandOptionCompletions(subcommand, service);
        }
        Map<String, Map<CliCommandOption, ? extends TabCompleter>> subcommandCompleters = service.getSubcommandCompleters();
        if (subcommandCompleters == null || subcommandCompleters.isEmpty()) {
            return;
        }
        for (String subcommand : subcommandCompleters.keySet()) {
            TreeCompleter.Node subcommandNode = findChildNode(commandNode, subcommand);
            if (subcommandNode == null) {
                continue;
            }
            for (CliCommandOption cliCommandOption : subcommandCompleters.get(subcommand).keySet()) {
                if (cliCommandOption == CliCommandOption.POSITIONAL_ARG_COMPLETION) {
                    subcommandNode.removeByTag(subcommandNode.getTag() + " completer");
                } else {
                    for (TreeCompleter.Node node : subcommandNode.getBranches()) {
                        if (node.getTag().equals("-" + cliCommandOption.getOpt())
                                || node.getTag().equals("--" + cliCommandOption.getLongOpt())) {
                            node.clear();
                        }
                    }
                }
            }
            removeTopLevelCommandOptionCompletions(subcommandNode, service);
        }
    }

    public void setupTabCompletion(CommandInfoSource commandInfoSource) {
        for (CommandInfo info : commandInfoSource.getCommandInfos()) {
            if (info.getEnvironments().contains(Environment.SHELL)) {
                String commandName = info.getName();
                TreeCompleter.Node command = getCommandByName(commandName);

                setupSubcommandCompletion(command, info);

                for (Option option : (Collection<Option>) info.getOptions().getOptions()) {
                    setupDefaultCompletion(command, option);
                }

                addHelpOptionIfRequired(command);

                treeCompleter.addBranch(command);
            }
        }
    }

    private void setupSubcommandCompletion(TreeCompleter.Node commandNode, CommandInfo commandInfo) {
        List<PluginConfiguration.Subcommand> subcommands = commandInfo.getSubcommands();
        for (PluginConfiguration.Subcommand subcommand : subcommands) {
            TreeCompleter.Node subcommandNode = createStringNode(subcommand.getName());
            subcommandNode.setRestartNode(commandNode);
            for (Option option : (Collection<Option>) commandInfo.getOptions().getOptions()) {
                setupDefaultCompletion(subcommandNode, option);
            }
            if (subcommand.getOptions() != null) {
                for (Option option : (Collection<Option>) subcommand.getOptions().getOptions()) {
                    setupDefaultCompletion(subcommandNode, option);
                }
            }
            commandNode.addBranch(subcommandNode);
            registerSubcommand(commandNode, subcommandNode);
        }
    }

    private void setupDefaultCompletion(final TreeCompleter.Node command, final Option option) {
        setupDefaultCompletion(command, option.getLongOpt(), LONG_OPTION_PREFIX);
        setupDefaultCompletion(command, option.getOpt(), SHORT_OPTION_PREFIX);
    }

    private void setupDefaultCompletion(final TreeCompleter.Node command, final String option, final String prefix) {
        if (option != null) {
            String optionShortName = prefix + option;
            TreeCompleter.Node defaultNode = createStringNode(optionShortName);
            defaultNode.setRestartNode(command);
            command.addBranch(defaultNode);
        }
    }

    private void registerSubcommand(TreeCompleter.Node commandNode, TreeCompleter.Node subcommandNode) {
        if (!subcommandMap.containsKey(commandNode)) {
            subcommandMap.put(commandNode, new HashSet<TreeCompleter.Node>());
        }
        subcommandMap.get(commandNode).add(subcommandNode);
    }

    private void addHelpOptionIfRequired(TreeCompleter.Node command) {
        if (HelpCommand.COMMAND_NAME.equals(command.getTag())) {
            return;
        }
        TreeCompleter.Node helpNode = TreeCompleter.createStringNode("--help");
        helpNode.setRestartNode(command);
        command.addBranch(helpNode);
    }

    public void attachToReader(ConsoleReader reader) {
        if (reader.getCompleters().isEmpty()) {
            reader.addCompleter(new JLineCompleterAdapter(treeCompleter));
        }
    }

    /* Testing only */
    Set<String> getKnownCommands() {
        return new HashSet<>(commandMap.keySet());
    }

    private static class JLineCompleterAdapter implements Completer {

        private TabCompleter tabCompleter;

        public JLineCompleterAdapter(TabCompleter tabCompleter) {
            this.tabCompleter = tabCompleter;
        }

        @Override
        public int complete(String s, int i, List<CharSequence> list) {
            return tabCompleter.complete(s, i, list);
        }
    }

}
