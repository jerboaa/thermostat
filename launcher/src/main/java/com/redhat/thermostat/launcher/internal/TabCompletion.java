/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.redhat.thermostat.common.utils.LoggingUtils;
import jline.console.ConsoleReader;
import jline.console.completer.FileNameCompleter;
import org.apache.commons.cli.Option;

import static com.redhat.thermostat.launcher.internal.TreeCompleter.createStringNode;

public class TabCompletion {

    private static final String LONG_OPTION_PREFIX = "--";
    private static final String SHORT_OPTION_PREFIX = "-";

    public static void setupTabCompletion(ConsoleReader reader, CommandInfoSource commandInfoSource) {
        List<String> logLevels = new ArrayList<>();

        for (LoggingUtils.LogLevel level : LoggingUtils.LogLevel.values()) {
            logLevels.add(level.getLevel().getName());
        }

        TreeCompleter treeCompleter = new TreeCompleter();
        for (CommandInfo info : commandInfoSource.getCommandInfos()) {

            if (info.getEnvironments().contains(Environment.SHELL)) {
                String commandName = info.getName();
                TreeCompleter.Node command = createStringNode(commandName);

                for (Option option : (Collection<Option>) info.getOptions().getOptions()) {
                    if (option.getLongOpt().equals("logLevel")) {
                        setupLogLevelCompletion(logLevels, command, option);
                    } else {
                        setupDefaultCompletion(command, option);
                    }

                }

                if (info.needsFileTabCompletions()) {
                    TreeCompleter.Node files = new TreeCompleter.Node(new FileNameCompleter());
                    files.setRestartNode(command);
                    command.addBranch(files);
                }
                treeCompleter.addBranch(command);
            }
        }

        reader.addCompleter(treeCompleter);
    }

    private static void setupDefaultCompletion(final TreeCompleter.Node command, final Option option) {
        if (option.getLongOpt() != null) {
            String optionLongName = LONG_OPTION_PREFIX + option.getLongOpt();
            TreeCompleter.Node defaultNode = createStringNode(optionLongName);
            defaultNode.setRestartNode(command);
            command.addBranch(defaultNode);
        }
        if (option.getOpt() != null) {
            String optionShortName = SHORT_OPTION_PREFIX + option.getOpt();
            TreeCompleter.Node defaultNode = createStringNode(optionShortName);
            defaultNode.setRestartNode(command);
            command.addBranch(defaultNode);
        }
    }

    private static void setupLogLevelCompletion( final List<String> logLevels, final TreeCompleter.Node command, final Option option) {
        if (option.getLongOpt() != null) {
            String optionLongName = LONG_OPTION_PREFIX + option.getLongOpt();
            TreeCompleter.Node logLevelOption = createStringNode(optionLongName);
            TreeCompleter.Node logLevelChoices = createStringNode(logLevels);
            logLevelChoices.setRestartNode(command);
            logLevelOption.addBranch(logLevelChoices);
            command.addBranch(logLevelOption);
        }
        if (option.getOpt() != null) {
            String optionShortName = SHORT_OPTION_PREFIX + option.getOpt();
            TreeCompleter.Node logLevelOption = createStringNode(optionShortName);
            TreeCompleter.Node logLevelChoices = createStringNode(logLevels);
            logLevelChoices.setRestartNode(command);
            logLevelOption.addBranch(logLevelChoices);
            command.addBranch(logLevelOption);
        }
    }

}
