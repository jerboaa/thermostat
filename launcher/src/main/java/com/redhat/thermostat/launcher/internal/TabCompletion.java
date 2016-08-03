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

    public TabCompletion() {
        this(new TreeCompleter(), new HashMap<String, TreeCompleter.Node>());
    }

    /*
     * Testing only
     */
    TabCompletion(TreeCompleter treeCompleter, Map<String, TreeCompleter.Node> commandMap) {
        this.treeCompleter = treeCompleter;
        this.commandMap = commandMap;

        treeCompleter.setAlphabeticalCompletions(true);
    }

    public void addCompleterService(CompleterService service) {
        for (String commandName : getCommandsForService(service)) {
            TreeCompleter.Node command = getCommandByName(commandName);
            addCompleterServiceImpl(command, service);
        }
    }

    private Set<String> getCommandsForService(CompleterService service) {
        if (ALL_COMMANDS_COMPLETER == service.getCommands()) {
            return commandMap.keySet();
        } else {
            return service.getCommands();
        }
    }

    private void addCompleterServiceImpl(TreeCompleter.Node command, CompleterService service) {
        for (Map.Entry<CliCommandOption, ? extends TabCompleter> entry : service.getOptionCompleters().entrySet()) {
            CliCommandOption cliCommandOption = entry.getKey();
            if (cliCommandOption == CliCommandOption.POSITIONAL_ARG_COMPLETION) {
                TreeCompleter.Node node = new TreeCompleter.Node(command.getTag() + " completer", entry.getValue());
                node.setRestartNode(command);
                command.addBranch(node);
            }
            if (completerIsApplicable(cliCommandOption, command)) {
                for (TreeCompleter.Node branch : command.getBranches()) {
                    if (branch.getTag().equals("-" + cliCommandOption.getOpt())
                            || branch.getTag().equals("--" + cliCommandOption.getLongOpt())) {
                        TreeCompleter.Node node = new TreeCompleter.Node(command.getTag() + " completer", entry.getValue());
                        node.setRestartNode(command);
                        branch.addBranch(node);
                    }
                }
            }
        }
    }

    public void removeCompleterService(CompleterService service) {
        for (String commandName : getCommandsForService(service)) {
            TreeCompleter.Node command = commandMap.get(commandName);
            if (command != null) {
                removeCompleterServiceImpl(command, service);
            }
        }
    }

    private void removeCompleterServiceImpl(TreeCompleter.Node command, CompleterService service) {
        for (Map.Entry<CliCommandOption, ? extends TabCompleter> entry : service.getOptionCompleters().entrySet()) {
            CliCommandOption cliCommandOption = entry.getKey();
            if (cliCommandOption == CliCommandOption.POSITIONAL_ARG_COMPLETION) {
                command.removeByTag(command.getTag() + " completer");
            }
            Set<String> toRemove = new HashSet<>();
            for (TreeCompleter.Node branch : command.getBranches()) {
                if (branch.getTag().equals("-" + cliCommandOption.getOpt())
                        || branch.getTag().equals("--" + cliCommandOption.getLongOpt())) {
                    toRemove.add(branch.getTag());
                }
            }
            for (String tag : toRemove) {
                command.removeByTag(tag);
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

    public void setupTabCompletion(CommandInfoSource commandInfoSource) {
        for (CommandInfo info : commandInfoSource.getCommandInfos()) {
            if (info.getEnvironments().contains(Environment.SHELL)) {
                String commandName = info.getName();
                TreeCompleter.Node command = getCommandByName(commandName);

                for (Option option : (Collection<Option>) info.getOptions().getOptions()) {
                    setupDefaultCompletion(command, option);
                }

                treeCompleter.addBranch(command);
            }
        }
    }

    /* Testing only */
    Set<String> getKnownCommands() {
        return new HashSet<>(commandMap.keySet());
    }

    public void attachToReader(ConsoleReader reader) {
        if (reader.getCompleters().isEmpty()) {
            reader.addCompleter(new JLineCompleterAdapter(treeCompleter));
        }
    }

    private TreeCompleter.Node getCommandByName(String commandName) {
        if (!commandMap.containsKey(commandName)) {
            TreeCompleter.Node command = createStringNode(commandName);
            commandMap.put(commandName, command);
        }
        return commandMap.get(commandName);
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

    private void setupCompletion(final TreeCompleter.Node command, final Option option, TabCompleter completer) {
        setupCompletion(command, completer, option.getLongOpt(), LONG_OPTION_PREFIX);
        setupCompletion(command, completer, option.getOpt(), SHORT_OPTION_PREFIX);
    }

    private void setupCompletion(final TreeCompleter.Node command, final TabCompleter completer, final String option, final String prefix) {
        if (option != null) {
            final String optionName = prefix + option;
            TreeCompleter.Node nodeOption = setupCompletionNode(command, optionName, completer);
            command.addBranch(nodeOption);
        }
    }

    private TreeCompleter.Node setupCompletionNode(final TreeCompleter.Node command, final String optionName, TabCompleter completer) {
        TreeCompleter.Node option = createStringNode(optionName);
        TreeCompleter.Node choices = new TreeCompleter.Node(optionName, completer);
        option.addBranch(choices);
        option.setRestartNode(command);
        choices.setRestartNode(command);
        return option;
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
