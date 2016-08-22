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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.BundleInformation;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;

public class BuiltInCommandInfo implements CommandInfo {

    private static final Logger logger = LoggingUtils.getLogger(BuiltInCommandInfo.class);
    private static final String PROPERTY_BUNDLES = "bundles";
    private static final String PROPERTY_SUMMARY = "summary";
    private static final String PROPERTY_DESC = "description";
    private static final String PROPERTY_USAGE = "usage";
    private static final String PROPERTY_OPTIONS = "options";
    private static final String PROPERTY_ENVIRONMENTS = "environments";

    private static final String PROP_SHORTOPT = ".short";
    private static final String PROP_LONGOPT = ".long";
    private static final String PROP_OPTHASARG = ".hasarg";
    private static final String PROP_OPTREQUIRED = ".required";
    private static final String PROP_OPTDESC = ".description";
    
    private String name, summary, description, usage;
    private Options options;
    private EnumSet<Environment> environment;
    private List<BundleInformation> dependencies;

    BuiltInCommandInfo(String commandName, Properties properties) {
        options = new Options();
        this.name = commandName;
        for (Entry<Object,Object> entry: properties.entrySet()) {
            String key = (String) entry.getKey();
            if (key.equals(PROPERTY_BUNDLES)) {
                learnDependencies((String) entry.getValue());
            } else if (key.equals(PROPERTY_SUMMARY)) {
                summary = properties.getProperty(key);
            } else if (key.equals(PROPERTY_DESC)) {
                description = properties.getProperty(key);
            } else if (key.equals(PROPERTY_USAGE)) {
                usage = properties.getProperty(key);
            } else if (key.equals(PROPERTY_OPTIONS)) {
                learnOptions((String) entry.getValue(), properties);
            } else if (key.equals(PROPERTY_ENVIRONMENTS)) {
                environment = parseEnvironment(properties.getProperty(key));
            }
        }
    }

    private void learnDependencies(String bundlesValue) {
        List<String> resourceNames = Arrays.asList(bundlesValue.split(","));
        dependencies = new ArrayList<>(resourceNames.size());
        for (String value : resourceNames) {
            String resource = value.trim();
            if (resource.length() == 0) {
                continue;
            }
            String[] parts = value.split("=");
            String name = parts[0].trim();
            String version = parts[1].trim();
            // FIXME hack to convert maven-style "-SNAPSHOT" versions to OSGi-style ".SNAPSHOT".
            // This is because we use ${project.version} for bundle version in our properties file
            // and that's the maven version, not the OSGi version.
            if (version != null && version.contains("-")) {
                version = version.replace("-", ".");
            }
            BundleInformation info = new BundleInformation(name, version);
            dependencies.add(info);
        }
    }

    private void learnOptions(String optionsValue, Properties props) {
        List<String> optionNamesWithWhitespace = Arrays.asList(optionsValue.split(","));
        List<String> optionNames = new ArrayList<>();
        for (String optionString : optionNamesWithWhitespace) {
            optionNames.add(optionString.trim());
        }

        /*
         * These intermediate Options objects are created, so that we can do the necessary
         * gymnastics to accomplish the following seemingly-contradictory goals:
         * 
         *  a) Preventing conflicts between options.
         *  b) Still allowing the "required" property of these DB and LOG
         *     options to be overridden.
         *
         * Change this code at your peril.
         *
         * Grep for "secret sauce" to find other relevant snippets in this class.
         */
        Options commonOptionsToAdd = new Options(); // For adding to this.options later, and checking for conflicts
        Options propertiesOptions = new Options(); // For checking for conflicts only, will contain OptionGroup members
        Options propertiesOptionsToAdd = new Options(); // For adding to this.options later, will not contain OptionGroup members

        if (optionNames.contains(CommonOptions.OPTIONS_COMMON_DB_OPTIONS)) {
            for (Option opt: CommonOptions.getDbOptions()) {
                commonOptionsToAdd.addOption(opt);
            }
            while (optionNames.contains(CommonOptions.OPTIONS_COMMON_DB_OPTIONS)) {
                optionNames.remove(CommonOptions.OPTIONS_COMMON_DB_OPTIONS);
            }
        }
        if (optionNames.contains(CommonOptions.OPTIONS_COMMON_LOG_OPTION)) {
            Option opt = CommonOptions.getLogOption();
            commonOptionsToAdd.addOption(opt);
            while (optionNames.contains(CommonOptions.OPTIONS_COMMON_LOG_OPTION)) {
                optionNames.remove(CommonOptions.OPTIONS_COMMON_LOG_OPTION);
            }
        }

        for (String optionString : optionNames) {
        	// A set of mutually exclusive options can be specified as strings separated by "|"
            List<String> optionsList = Arrays.asList(optionString.split("\\|"));
            if (optionsList.size() == 1) {
            	// Not a group
                String optionName = optionsList.get(0).trim();
                boolean forOverridingRequired = CommonOptions.ALL_COMMON_OPTIONS.contains(optionName);
                Option option = optionFromProperties(optionName, props, forOverridingRequired);
                if (optionConflicts(option, commonOptionsToAdd, propertiesOptions)) {
                    throwConflictingOption(option);
                }
                propertiesOptions.addOption(option);
                propertiesOptionsToAdd.addOption(option);
            } else {
            	// Is a group
                OptionGroup og = createOptionGroup(optionsList, props, commonOptionsToAdd, propertiesOptions);
                for (Option o : (Collection<Option>) og.getOptions()) {
                    propertiesOptions.addOption(o);
                }
                options.addOptionGroup(og);
            }
        }

        for (Option commonOpt : (Collection<Option>) commonOptionsToAdd.getOptions()) {
            options.addOption(commonOpt);
        }
        for (Option potentialOpt : (Collection<Option>) propertiesOptionsToAdd.getOptions()) {
            // Here is some of the secret sauce to allow overriding.
            Option existingOpt = options.getOption(potentialOpt.getArgName());
            if (existingOpt != null) {
                existingOpt.setRequired(potentialOpt.isRequired());
            } else {
                // Not secret sauce, this is what we'd otherwise just do.
                options.addOption(potentialOpt);
            }
        }
    }

    /**
     * @param name the name of the option
     * @param props the properties object that contains more information about that option
     * @param forOverridingRequiredOnly indicates that the option is for
     * overriding the required property of a common option
     * @return an Option object created by parsing the props
     */
    private Option optionFromProperties(String name, Properties props, boolean forOverridingRequiredOnly) {
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

        if (props.containsKey(optKey)) {
            opt = (String) props.getProperty(optKey);
        }
        if (props.containsKey(longKey)) {
            longOpt = (String) props.getProperty(longKey);
        }
        if (opt == null && longOpt == null) {
            longOpt = name;
            if (!forOverridingRequiredOnly) {
                logger.info("Command '" + this.name + "': Neither short nor long version of option " + name + " was set.  Assuming long option same as name.");
            }
        }
        if (props.containsKey(argKey)) {
            hasArg = Boolean.parseBoolean((String) props.getProperty(argKey));
        } else {
            if (!forOverridingRequiredOnly) {
                logger.info("Command '" + this.name + "': The 'hasarg' property for " + name + " was not set.  Assuming FALSE");
            }
        }
        if (props.containsKey(requiredKey)) {
            required = Boolean.parseBoolean((String) props.getProperty(requiredKey));
        } else {
            logger.info("Command '" + this.name + "': The 'required' property for " + name + " was not set.  Assuming FALSE");
        }
        if (props.containsKey(descKey)) {
            description = (String) props.getProperty(descKey);
        }

        Option option = new Option(opt, longOpt, hasArg, description);
        option.setArgName(name);
        option.setRequired(required);
        return option;
    }

    /* TODO currently this assumes that any set of mutually exclusive options will be
     * required.  Needs some sort of enhancement in properties file to allow them to
     * be optional.  For the time being this is good enough, since in practice all such
     * sets *are* required.
     * The members of an option group should be separated by a "|" character.
     */
    private OptionGroup createOptionGroup(List<String> optionsList, Properties props, Options commonOpts, Options propOpts) {
        OptionGroup og = new OptionGroup();
        og.setRequired(true);
        for (String optionName : optionsList) {
            boolean forOverridingRequired = CommonOptions.ALL_COMMON_OPTIONS.contains(optionName);
            Option option = optionFromProperties(optionName.trim(), props, forOverridingRequired);
            if (optionConflictsWithGroup(option, og) |
                    optionConflicts(option, commonOpts, propOpts)) {
                throwConflictingOption(option);
            }
            og.addOption(option);
        }
        return og;
    }

    private boolean optionConflicts(Option option, Options commonOpts, Options propOpts) {
        if (optionConflictsWithOptions(option, propOpts) ||
                optionConflictsWithCommonOptions(option, commonOpts)) {
            return true;
        }
        return false;
    }

    // All conflicts detected.
    private boolean optionConflictsWithOptions(Option option, Options options) {
        for (Option o : (Collection<Option>) options.getOptions()) {
            if (optionConflictsWithOption(option, o)) {
                return true;
            }
        }
        return false;
    }

    // Conflicts not interpreted as override detected.
    private boolean optionConflictsWithCommonOptions(Option option, Options options) {
        for (Option o : (Collection<Option>) options.getOptions()) {
            if (optionConflictsWithOption(option, o)) {
                /* As a special case, if these things are not defined we do not consider
                 * this to be a conflict.  This will instead be treated as an override,
                 * specifically for the "required" property of the common option.
                 * Grep for "secret sauce" to find other relevant snippets in this class.
                 */
                if (option.getOpt() != null || option.getDescription() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    // All conflicts detected.
    private boolean optionConflictsWithGroup(Option option, OptionGroup group) {
        for (Option o : (Collection<Option>) group.getOptions()) {
            if(optionConflictsWithOption(o, option)) {
                return true;
            }
        }
        return false;
    }

    // Compares the two to detect conflicts.  Not to be confused with conflictExistsWithCommonOption
    private boolean optionConflictsWithOption(Option o1, Option o2) {
        String name = o1.getArgName();
        String opt = o1.getOpt();
        String longOpt = o1.getLongOpt();
        if (name != null && name.equals(o2.getArgName())) {
            return true;
        }
        if (opt != null && opt.equals(o2.getOpt())) {
            return true;
        }
        if (longOpt != null && longOpt.equals(o2.getLongOpt())) {
            return true;
        }
        return false;
    }

    private void throwConflictingOption(Option option) {
        // Throwing this to catch issues during development.  Not intended as a
        // robust solution to this happening in production.
        throw new RuntimeException("The " + name +
                " command contains a conflicting option: " +
                option.getArgName());
    }

    private EnumSet<Environment> parseEnvironment(String value) {
        EnumSet<Environment> result = EnumSet.noneOf(Environment.class);
        String[] terms = value.split(",");
        for (String term : terms) {
            term = term.trim();
            if (term.equals("shell")) {
                result.add(Environment.SHELL);
            } else if (term.equals("cli")) {
                result.add(Environment.CLI);
            } else {
                logger.info("Command " + this.name + " is available in unknown context: " + term);
            }
        }

        if (result.isEmpty()) {
            throw new InvalidConfigurationException("no value for environments");
        }

        return result;
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
        return Collections.emptyList();
    }

    @Override
    public Set<Environment> getEnvironments() {
        return environment;
    }

    @Override
    public List<BundleInformation> getBundles() {
        return dependencies;
    }

}

