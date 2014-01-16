/*
 * Copyright 2012-2014 Red Hat, Inc.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.BundleInformation;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandExtensions;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.NewCommand;
import com.redhat.thermostat.plugin.validator.PluginConfigurationValidatorException;
import com.redhat.thermostat.plugin.validator.ValidationErrorsFormatter;

/**
 * Searches for plugins under <code>$THERMOSTAT_HOME/plugins/</code> and
 * provides information about all commands specified by them.
 * <p>
 * Each plugin is located under
 * <code>$THERMOSTAT_HOME/plugins/$PLUGIN_NAME/</code> and must have a
 * <code>plugin.xml</code> file in the main plugin directory.
 *
 * @see PluginConfigurationParser how the plugin.xml file is parsed
 */
public class PluginCommandInfoSource implements CommandInfoSource {

    private static final String PLUGIN_CONFIG_FILE = "thermostat-plugin.xml";

    private static final Logger logger = LoggingUtils.getLogger(PluginCommandInfoSource.class);

    private final UsageStringBuilder usageBuilder;

    private Map<String, BasicCommandInfo> allNewCommands = new HashMap<>();
    private Map<String, List<BundleInformation>> additionalBundlesForExistingCommands = new HashMap<>();

    public PluginCommandInfoSource(String internalJarRoot, String systemPluginRootDir, String userPluginRootDir) {
        this(new File(internalJarRoot), new File(systemPluginRootDir), new File(userPluginRootDir), new PluginConfigurationParser(), new UsageStringBuilder());
    }

    PluginCommandInfoSource(File internalJarRoot, File systemPluginRootDir, File userPluginRootDir, PluginConfigurationParser parser, UsageStringBuilder usageBuilder) {
        this.usageBuilder = usageBuilder;

        List<File> pluginDirectories = new ArrayList<>();

        addPluginDirectory(pluginDirectories, systemPluginRootDir);
        addPluginDirectory(pluginDirectories, userPluginRootDir);

        for (File pluginDir : pluginDirectories) {
            try {
                File configurationFile = new File(pluginDir, PLUGIN_CONFIG_FILE);
                PluginConfiguration pluginConfig = parser.parse(configurationFile);
                loadNewAndExtendedCommands(internalJarRoot, pluginDir, pluginConfig);
                
            } catch (PluginConfigurationParseException exception) {
                logger.log(Level.WARNING, "unable to parse plugin configuration", exception);
                
            } catch (PluginConfigurationValidatorException pcve) {
                ValidationErrorsFormatter formatter = new ValidationErrorsFormatter();
                logger.log(Level.INFO, formatter.format(pcve.getAllErrors()));
                logger.log(Level.INFO, "unable to validate " + pcve.getXmlFile().getAbsolutePath() + " file\n");
                
            } catch (FileNotFoundException exception) {
                logger.log(Level.INFO, "file not found", exception);
            }
        }
        combineCommands();
    }
    
    private void addPluginDirectory(List<File> allPluginDirectories, File aPluginRoot) {
        File[] pluginDirs = aPluginRoot.listFiles();

        if (pluginDirs != null) {
            allPluginDirectories.addAll(Arrays.asList(pluginDirs));
        }
    }
   
    private void loadNewAndExtendedCommands(File coreJarRoot, File pluginDir,
            PluginConfiguration pluginConfig) {

        for (CommandExtensions extension : pluginConfig.getExtendedCommands()) {
            String commandName = extension.getCommandName();
            List<BundleInformation> bundles = extension.getBundles();
            logger.config("plugin at " + pluginDir + " needs " +
                    bundles.size() + " bundles for comamnd '" + commandName + "'");

            List<BundleInformation> bundlePaths = additionalBundlesForExistingCommands.get(commandName);
            if (bundlePaths == null) {
                bundlePaths = new LinkedList<>();
            }

            bundlePaths.addAll(bundles);

            additionalBundlesForExistingCommands.put(commandName, bundlePaths);
        }

        for (NewCommand command : pluginConfig.getNewCommands()) {
            String commandName = command.getCommandName();
            logger.config("plugin at " + pluginDir + " contributes new command '" + commandName + "'");

            if (allNewCommands.containsKey(commandName)) {
                throw new IllegalStateException("multiple plugins are providing the command " + commandName);
            }

            String usage = command.getUsage();
            if (usage == null) {
                usage = usageBuilder.getUsage(commandName, command.getOptions(), command.getPositionalArguments().toArray(new String[0]));
            }
            BasicCommandInfo info = new BasicCommandInfo(commandName,
                    command.getDescription(),
                    usage,
                    command.getOptions(),
                    command.getEnvironments(),
                    command.getBundles());

            allNewCommands.put(commandName, info);
        }

    }

    private void combineCommands() {
        Iterator<Entry<String, List<BundleInformation>>> iter = additionalBundlesForExistingCommands.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<BundleInformation>> entry = iter.next();
            if (allNewCommands.containsKey(entry.getKey())) {
                BasicCommandInfo old = allNewCommands.get(entry.getKey());
                List<BundleInformation> updatedBundles = new ArrayList<>();
                updatedBundles.addAll(old.getBundles());
                updatedBundles.addAll(entry.getValue());
                BasicCommandInfo updated = new BasicCommandInfo(old.getName(),
                        old.getDescription(),
                        old.getUsage(),
                        old.getOptions(),
                        old.getEnvironments(),
                        updatedBundles);
                allNewCommands.put(entry.getKey(), updated);
                iter.remove();
            }
        }
    }

    @Override
    public CommandInfo getCommandInfo(String name) throws CommandInfoNotFoundException {
        if (allNewCommands.containsKey(name)) {
            return allNewCommands.get(name);
        }
        List<BundleInformation> bundles = additionalBundlesForExistingCommands.get(name);
        if (bundles != null) {
            return new BasicCommandInfo(name, null, null, null, null, bundles);
        }
        throw new CommandInfoNotFoundException(name);
    }

    @Override
    public Collection<CommandInfo> getCommandInfos() {
        List<CommandInfo> result = new ArrayList<>();
        result.addAll(allNewCommands.values());
        for (Entry<String, List<BundleInformation>> entry : additionalBundlesForExistingCommands.entrySet()) {
            result.add(new BasicCommandInfo(entry.getKey(), null, null, null, null, entry.getValue()));
        }
        return result;
    }
    

}

