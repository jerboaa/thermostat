/*
 * Copyright 2012, 2013 Red Hat, Inc.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

import com.redhat.thermostat.common.cli.CommandInfo;
import com.redhat.thermostat.common.utils.LoggingUtils;


public class CommandInfoImpl implements CommandInfo {

    private static final Logger logger = LoggingUtils.getLogger(CommandInfoSourceImpl.class);
    private static final String PROPERTY_BUNDLES = "bundles";
    private static final String PROPERTY_DESC = "description";
    private static final String PROPERTY_USAGE = "usage";
    private static final String PROPERTY_OPTIONS = "options";

    private static final String PROP_SHORTOPT = ".short";
    private static final String PROP_LONGOPT = ".long";
    private static final String PROP_OPTHASARG = ".hasarg";
    private static final String PROP_OPTREQUIRED = ".required";
    private static final String PROP_OPTDESC = ".description";
    
    private String name, description, usage;
    private Options options;
    private List<String> dependencies;

    CommandInfoImpl(String name, Properties properties, String thermostatHome) {
        options = new Options();
        this.name = name;
        for (Entry<Object,Object> entry: properties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.equals(PROPERTY_BUNDLES)) {
                learnDependencies((String) entry.getValue(), thermostatHome);
            } else if (key.equals(PROPERTY_DESC)) {
                description = properties.getProperty(key);
            } else if (key.equals(PROPERTY_USAGE)) {
                usage = properties.getProperty(key);
            } else if (key.equals(PROPERTY_OPTIONS)) {
                learnOptions((String) entry.getValue(), properties);
            }
        }
    }

    private void learnDependencies(String bundlesValue, String thermostatHome) {
        String libRoot = thermostatHome + File.separator + "libs";
        List<String> resourceNames = Arrays.asList(bundlesValue.split(","));
        dependencies = new ArrayList<>(resourceNames.size());
        for (String value : resourceNames) {
            String resource = value.trim();
            if (resource.length() == 0) {
                continue;
            }
            File file = new File(libRoot, value.trim());
            String path = file.toURI().toString();
            if (!file.exists()) {
                logger.severe("Bundle " + path + " required by " + getName() +
                        " command does not exist in the filesystem.  This will cause" +
                        " osgi wiring issue when attempting to run this command.");
                // Allow to proceed because this command may never be called.
            } else {
                dependencies.add(path);
            }
        }
    }

    private void learnOptions(String optionsValue, Properties props) {
        List<String> optionNames = Arrays.asList(optionsValue.split(","));
        for (String optionString : optionNames) {
            List<String> optionsList = Arrays.asList(optionString.trim().split("\\|"));
            if (optionsList.size() == 1) {
                learnOption(optionsList.get(0).trim(), props);
            } else {
                learnOptionGroup(optionsList, props);
            }
        }
    }

    private void learnOption(String name, Properties props) {
        if (name.equals(CommonOptions.OPTIONS_COMMON_DB_OPTIONS)) {
            addDbOptions();
        } else if (name.equals(CommonOptions.OPTIONS_COMMON_LOG_OPTION)) {
            options.addOption(CommonOptions.getLogOption());
        } else {
            Option option = optionFromProperties(name, props);
            options.addOption(option);
        }
    }

    private void addDbOptions() {
        for (Option opt: CommonOptions.getDbOptions()) {
            options.addOption(opt);
        }
    }

    /* TODO currently this assumes that any set of mutually exclusive options will be
     * required.  Needs some sort of enhancement in properties file to allow them to
     * be optional.  For the time being this is good enough, since in practice all such
     * sets *are* required.
     */
    private void learnOptionGroup(List<String> optionsList, Properties props) {
        OptionGroup og = new OptionGroup();
        og.setRequired(true);
        for (String optionName : optionsList) {
            Option option = optionFromProperties(optionName.trim(), props);
            og.addOption(option);
        }
        options.addOptionGroup(og);
    }

    private Option optionFromProperties(String name, Properties props) {
        String opt = null;
        String longOpt = null;
        boolean hasArg = false;
        boolean required = false;
        String description = null;

        String optKey = name + PROP_SHORTOPT;
        String longKey = name + PROP_LONGOPT;
        String argKey = name + PROP_OPTHASARG;
        String requiredKey = name + PROP_OPTREQUIRED;
        String descKey = name + PROP_OPTDESC;
        
        // required property of common options are allowed to be overridden by
        // command.properties files
        if (CommonOptions.ALL_COMMON_OPTIONS.contains(name) && options.hasOption(name)) {
            if (props.containsKey(requiredKey)) {
                Option optionToChange = options.getOption(name);
                required = Boolean.parseBoolean((String) props.getProperty(requiredKey));
                optionToChange.setRequired(required);
                return optionToChange;
            }
        }

        if (props.containsKey(optKey)) {
            opt = (String) props.getProperty(optKey);
        }
        if (props.containsKey(longKey)) {
            longOpt = (String) props.getProperty(longKey);
        }
        if (opt == null && longOpt == null) {
            logger.severe("Neither short nor long version of option " + name + " was set.  Check properties file.");
        }
        if (props.containsKey(argKey)) {
            hasArg = Boolean.parseBoolean((String) props.getProperty(argKey));
        } else {
            logger.warning("The 'hasarg' property for " + name + " was not set.  Assuming FALSE");
        }
        if (props.containsKey(requiredKey)) {
            required = Boolean.parseBoolean((String) props.getProperty(requiredKey));
        } else {
            logger.warning("The 'required' property for " + name + " was not set.  Assuming FALSE");
        }
        if (props.containsKey(descKey)) {
            description = (String) props.getProperty(descKey);
        }

        Option option = new Option(opt, longOpt, hasArg, description);
        option.setArgName(name);
        option.setRequired(required);
        return option;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public Options getOptions() {
        return options;
    }

    public List<String> getDependencyResourceNames() {
        return dependencies;
    }
}

