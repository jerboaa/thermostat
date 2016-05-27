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
import com.redhat.thermostat.common.config.ClientPreferences;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import org.apache.commons.cli.Option;
import org.osgi.framework.BundleContext;

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
    private Set<CompleterService> globalCompleterServices;
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

        this.globalCompleterServices = new HashSet<>();

        treeCompleter.setAlphabeticalCompletions(true);
    }

    public void addCompleterService(CompleterService service) {
        if (ALL_COMMANDS_COMPLETER == service.getCommands()) {
            globalCompleterServices.add(service);
        }
        for (String commandName : service.getCommands()) {
            TreeCompleter.Node command = getCommandByName(commandName);
            addCompleterServiceImpl(command, service);
        }
    }

    private void addCompleterServiceImpl(TreeCompleter.Node command, CompleterService service) {
        for (Map.Entry<CliCommandOption, ? extends TabCompleter> entry : service.getOptionCompleters().entrySet()) {
            if (entry.getKey() == CliCommandOption.POSITIONAL_ARG_COMPLETION) {
                TreeCompleter.Node node = new TreeCompleter.Node(command.getTag() + " completer", entry.getValue());
                node.setRestartNode(command);
                command.addBranch(node);
            }
            for (TreeCompleter.Node branch : command.getBranches()) {
                Set<String> completerOptions = getCompleterOptions(entry.getKey());
                if (completerOptions.contains(branch.getTag())) {
                    TreeCompleter.Node node = new TreeCompleter.Node(command.getTag() + " completer", entry.getValue());
                    node.setRestartNode(command);
                    branch.addBranch(node);
                }
            }
        }
    }

    public void removeCompleterService(CompleterService service) {
        if (ALL_COMMANDS_COMPLETER == service.getCommands()) {
            globalCompleterServices.remove(service);
        }
        for (String commandName : service.getCommands()) {
            TreeCompleter.Node command = commandMap.get(commandName);
            if (command != null) {
                removeCompleterServiceImpl(command, service);
            }
        }
    }

    private void removeCompleterServiceImpl(TreeCompleter.Node command, CompleterService service) {
        for (Map.Entry<CliCommandOption, ? extends TabCompleter> entry : service.getOptionCompleters().entrySet()) {
            if (entry.getKey() == CliCommandOption.POSITIONAL_ARG_COMPLETION) {
                command.removeByTag(command.getTag() + " completer");
            }
            for (TreeCompleter.Node branch : command.getBranches()) {
                Set<String> completerOptions = getCompleterOptions(entry.getKey());
                if (completerOptions.contains(branch.getTag())) {
                    command.removeByTag(branch.getTag());
                }
            }
        }
    }

    private static Set<String> getCompleterOptions(CliCommandOption option) {
        Set<String> options = new HashSet<>();
        options.add(LONG_OPTION_PREFIX + option.getLongOpt());
        options.add(SHORT_OPTION_PREFIX + option.getOpt());
        return options;
    }

    public void setupTabCompletion(ConsoleReader reader, CommandInfoSource commandInfoSource, BundleContext context, ClientPreferences prefs) {
        for (CommandInfo info : commandInfoSource.getCommandInfos()) {

            if (info.getEnvironments().contains(Environment.SHELL)) {
                TreeCompleter.Node command = getCommandByName(info.getName());

                for (Option option : (Collection<Option>) info.getOptions().getOptions()) {
                    setupDefaultCompletion(command, option);
                }

                treeCompleter.addBranch(command);

                for (CompleterService service : globalCompleterServices) {
                    addCompleterServiceImpl(command, service);
                }
            }
        }

        reader.addCompleter(new JLineCompleterAdapter(treeCompleter));
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
