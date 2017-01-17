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

import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Options;

import com.redhat.thermostat.launcher.BundleInformation;

public class BasicCommandInfo implements CommandInfo {

    private final String name;
    private final String summary;
    private final String description;
    private final String usage;
    private final List<PluginConfiguration.Subcommand> subcommands;
    private final Options options;
    private final Set<Environment> environments;
    private final List<BundleInformation> bundles;

    public BasicCommandInfo(String name, String summary, String description, String usage, Options options, List<PluginConfiguration.Subcommand> subcommands,
                            Set<Environment> environments, List<BundleInformation> bundles) {
        this.name = name;
        this.summary = summary;
        this.description = description;
        this.usage = usage;
        this.options = options;
        this.subcommands = subcommands;
        this.environments = environments;
        this.bundles = bundles;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getUsage() {
        return usage;
    }

    @Override
    public Options getOptions() {
        return options;
    }

    @Override
    public List<PluginConfiguration.Subcommand> getSubcommands() {
        return subcommands;
    }

    @Override
    public Set<Environment> getEnvironments() {
        return environments;
    }

    @Override
    public List<BundleInformation> getBundles() {
        return bundles;
    }

    @Override
    public String toString() {
        return String.format("%s (summary='%s', description='%s', dependencies='%s')", name, summary, description, bundles.toString());
    }
}

