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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.cli.CommandInfo;
import com.redhat.thermostat.common.cli.CommandInfoNotFoundException;
import com.redhat.thermostat.common.cli.CommandInfoSource;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandExtensions;

public class PluginCommandInfoSource implements CommandInfoSource {

    private Logger logger = LoggingUtils.getLogger(PluginCommandInfoSource.class);

    private Map<String, List<String>> allInfo = new HashMap<>();

    public PluginCommandInfoSource(String internalJarRoot, String pluginRootDir) {
        this(new File(internalJarRoot), new File(pluginRootDir), new PluginConfigurationParser());
    }

    public PluginCommandInfoSource(File internalJarRoot, File pluginRootDir,
            PluginConfigurationParser parser) {
        File[] pluginDirs = pluginRootDir.listFiles();
        if (pluginDirs == null) {
            logger.log(Level.SEVERE, "plugin root dir " + pluginRootDir + " does not exist");
            return;
        }

        for (File pluginDir : pluginDirs) {
            try {
                File configurationFile = new File(pluginDir, "plugin.conf");
                PluginConfiguration pluginConfig = parser.parse(configurationFile);
                loadNewAndExtendedCommands(internalJarRoot, pluginDir, pluginConfig);
            } catch (PluginConfigurationParseException | FileNotFoundException exception) {
                logger.log(Level.WARNING, "unable to parse plugin configuration", exception);
            }
        }
    }

    private void loadNewAndExtendedCommands(File coreJarRoot, File pluginDir,
            PluginConfiguration pluginConfig) {

        List<CommandExtensions> allExtensions = pluginConfig.getExtendedCommands();

        for (CommandExtensions extension : allExtensions) {
            String commandName = extension.getCommandName();
            List<String> pluginBundles = extension.getAdditionalBundles();
            List<String> dependencyBundles = extension.getDepenedencyBundles();
            logger.config("plugin at " + pluginDir + " contributes " +
                    pluginBundles.size() + " bundles to comamnd '" + commandName + "'");

            List<String> bundlePaths = allInfo.get(commandName);
            if (bundlePaths == null) {
                bundlePaths = new LinkedList<>();
            }

            for (String bundle : pluginBundles) {
                bundlePaths.add(new File(pluginDir, bundle).toURI().toString());
            }
            for (String bundle : dependencyBundles) {
                bundlePaths.add(new File(coreJarRoot, bundle).toURI().toString());
            }

            allInfo.put(commandName, bundlePaths);
        }
    }

    @Override
    public CommandInfo getCommandInfo(String name) throws CommandInfoNotFoundException {
        List<String> bundles = allInfo.get(name);
        if (bundles == null) {
            return null;
        }
        return new BasicCommandInfo(name, null, null, null, bundles);
    }

    @Override
    public Collection<CommandInfo> getCommandInfos() {
        List<CommandInfo> result = new ArrayList<>();
        for (Entry<String, List<String>> entry : allInfo.entrySet()) {
            result.add(new BasicCommandInfo(entry.getKey(), null, null, null, entry.getValue()));
        }
        return result;
    }

}
