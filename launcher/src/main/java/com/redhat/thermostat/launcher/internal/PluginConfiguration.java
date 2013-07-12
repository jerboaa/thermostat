/*
 * Copyright 2013 Red Hat, Inc.
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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Options;


public class PluginConfiguration {

    private final List<CommandExtensions> extensions;
    private final List<NewCommand> newCommands;

    public PluginConfiguration(List<NewCommand> newCommands, List<CommandExtensions> extensions) {
        this.newCommands = newCommands;
        this.extensions = extensions;
    }

    public List<CommandExtensions> getExtendedCommands() {
        return extensions;
    }

    public List<NewCommand> getNewCommands() {
        return newCommands;
    }

    public static class CommandExtensions {

        private final String commandName;
        private final List<String> additionalResources;
        private final List<String> coreDeps;

        public CommandExtensions(String name, List<String> additionalResources, List<String> coreDeps) {
            this.commandName = name;
            this.additionalResources = additionalResources;
            this.coreDeps = coreDeps;
        }

        public String getCommandName() {
            return commandName;
        }

        public List<String> getPluginBundles() {
            return Collections.unmodifiableList(additionalResources);
        }

        public List<String> getDepenedencyBundles() {
            return coreDeps;
        }
    }

    public static class NewCommand {

        private final String commandName;
        private final String usage;
        private final String description;
        private final List<String> positionalArguments;
        private final Options options;
        private final Set<Environment> environment;
        private final List<String> additionalResources;
        private final List<String> coreDeps;

        public NewCommand(String name, String usage, String description,
                List<String> positionalArguments, Options options,
                Set<Environment> environment,
                List<String> additionalResources, List<String> coreDeps) {
            this.commandName = name;
            this.usage = usage;
            this.description = description;
            this.positionalArguments = positionalArguments;
            this.options = options;
            this.environment = environment;
            this.additionalResources = additionalResources;
            this.coreDeps = coreDeps;
        }

        public String getCommandName() {
            return commandName;
        }

        /**
         * The usage string may be null if no usage string was explicitly
         * provided. In that case, usage should be "computed" using options and
         * arguments
         */
        public String getUsage() {
            return usage;
        }

        public String getDescription() {
            return description;
        }

        /** Returns a list of strings indicating positional arguments */
        public List<String> getPositionalArguments() {
            return positionalArguments;
        }

        /** Returns options (both optional and required) */
        public Options getOptions() {
            return options;
        }

        /** Returns the environments where this command is available to be used */
        public Set<Environment> getEnvironments() {
            return Collections.unmodifiableSet(environment);
        }

        public List<String> getPluginBundles() {
            return Collections.unmodifiableList(additionalResources);
        }

        public List<String> getDepenedencyBundles() {
            return Collections.unmodifiableList(coreDeps);
        }


    }

}
