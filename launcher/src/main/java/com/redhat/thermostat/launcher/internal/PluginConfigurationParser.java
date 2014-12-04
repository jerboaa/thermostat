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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.BundleInformation;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandExtensions;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.Configurations;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.NewCommand;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.PluginID;
import com.redhat.thermostat.plugin.validator.PluginConfigurationValidatorException;
import com.redhat.thermostat.plugin.validator.PluginValidator;

/**
 * Parses the configuration of a plugin as specified in an {@code File} or an
 * {@code InputStream}. This configuration describes which new commands this
 * plugin provides as well as additional jars to load for existing commands.
 * <p>
 * A example configuration looks like the following:
 *
 * <pre>
 *
&lt;?xml version="1.0" encoding="UTF-8"?&gt;
&lt;plugin xmlns="http://icedtea.classpath.org/thermostat/plugins/v1.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://icedtea.classpath.org/thermostat/plugins/v1.0"&gt;
  &lt;commands&gt;
    &lt;command&gt;
      &lt;name&gt;platform&lt;/name&gt;
      &lt;description&gt;launches a bare bone Platform Client&lt;/description&gt;
      &lt;arguments&gt;
        &lt;argument&gt;argument1&lt;/argument&gt;
      &lt;/arguments&gt;
      &lt;options&gt;
        &lt;group&gt;
          &lt;required&gt;true&lt;/required&gt;
          &lt;option&gt;
            &lt;long&gt;optA&lt;/long&gt;
            &lt;short&gt;a&lt;/short&gt;
            &lt;required&gt;true&lt;/required&gt;
          &lt;/option&gt;
          &lt;option&gt;
            &lt;long&gt;optB&lt;/long&gt;
            &lt;short&gt;b&lt;/short&gt;
            &lt;required&gt;true&lt;/required&gt;
          &lt;/option&gt;
          &lt;option&gt;
            &lt;long&gt;optC&lt;/long&gt;
            &lt;short&gt;b&lt;/short&gt;
            &lt;required&gt;false&lt;/required&gt;
          &lt;/option&gt;
          &lt;option common="true"&gt;
            &lt;long&gt;dbUrl&lt;/long&gt;
          &lt;/option&gt;
          &lt;option common="true"&gt;
            &lt;long&gt;username&lt;/long&gt;
          &lt;/option&gt;
          &lt;option common="true"&gt;
            &lt;long&gt;password&lt;/long&gt;
          &lt;/option&gt;
          &lt;option common="true"&gt;
            &lt;long&gt;logLevel&lt;/long&gt;
          &lt;/option&gt;
        &lt;/group&gt;
        &lt;option&gt;
          &lt;long&gt;heapId&lt;/long&gt;
          &lt;short&gt;h&lt;/short&gt;
          &lt;argument&gt;heapArgument&lt;/argument&gt;
          &lt;required&gt;true&lt;/required&gt;
          &lt;description&gt;the ID of the heapdump to analyze&lt;/description&gt;
        &lt;/option&gt;
        &lt;option&gt;
          &lt;long&gt;limit&lt;/long&gt;
          &lt;short&gt;L&lt;/short&gt;
          &lt;argument&gt;limitArgument&lt;/argument&gt;
          &lt;required&gt;false&lt;/required&gt;
          &lt;description&gt;limit search to top N results, defaults to 10&lt;/description&gt;
        &lt;/option&gt;
      &lt;/options&gt;
      &lt;environments&gt;
        &lt;environment&gt;cli&lt;/environment&gt;
        &lt;environment&gt;shell&lt;/environment&gt;
      &lt;/environments&gt;
      &lt;bundles&gt;
        &lt;bundle&gt;&lt;symbolic-name&gt;platform.core&lt;/symbolic-name&gt;&lt;version&gt;0.6.0&lt;/version&gt;&lt;/bundle&gt;
        &lt;bundle&gt;&lt;symbolic-name&gt;platform.swing&lt;/symbolic-name&gt;&lt;version&gt;0.6.0&lt;/version&gt;&lt;/bundle&gt;
      &lt;/bundles&gt;
      &lt;dependencies&gt;
        &lt;bundle&gt;&lt;symbolic-name&gt;client.core&lt;/symbolic-name&gt;&lt;version&gt;0.6.0&lt;/version&gt;&lt;/bundle&gt;
      &lt;/dependencies&gt;
    &lt;/command&gt;
    &lt;command&gt;
      &lt;name&gt;platform2&lt;/name&gt;
      &lt;description&gt;launches a bare bone Platform Client&lt;/description&gt;
      &lt;arguments&gt;
        &lt;argument&gt;argument2&lt;/argument&gt;
      &lt;/arguments&gt;
      &lt;options&gt;
        &lt;option&gt;
          &lt;long&gt;heapId2&lt;/long&gt;
          &lt;short&gt;h&lt;/short&gt;
          &lt;argument&gt;heapId2Argument&lt;/argument&gt;
          &lt;required&gt;true&lt;/required&gt;
          &lt;description&gt;the ID of the heapdump to analyze&lt;/description&gt;
        &lt;/option&gt;
        &lt;option&gt;
          &lt;long&gt;limit2&lt;/long&gt;
          &lt;short&gt;L&lt;/short&gt;
          &lt;argument&gt;limit2Argument&lt;/argument&gt;
          &lt;required&gt;false&lt;/required&gt;
          &lt;description&gt;limit search to top N results, defaults to 10&lt;/description&gt;
        &lt;/option&gt;
      &lt;/options&gt;
      &lt;environments&gt;
        &lt;environment&gt;shell&lt;/environment&gt;
      &lt;/environments&gt;
      &lt;bundles&gt;
        &lt;bundle&gt;&lt;symbolic-name&gt;platform.common&lt;/symbolic-name&gt;&lt;version&gt;0.6.0&lt;/version&gt;&lt;/bundle&gt;
        &lt;bundle&gt;&lt;symbolic-name&gt;platform.controllers&lt;/symbolic-name&gt;&lt;version&gt;0.6.0&lt;/version&gt;&lt;/bundle&gt;
      &lt;/bundles&gt;
    &lt;/command&gt;
  &lt;/commands&gt;
  &lt;extensions&gt;
    &lt;extension&gt;
      &lt;name&gt;platform3&lt;/name&gt;
      &lt;bundles&gt;
        &lt;bundle&gt;&lt;symbolic-name&gt;platform.common&lt;/symbolic-name&gt;&lt;version&gt;0.6.0&lt;/version&gt;&lt;/bundle&gt;
        &lt;bundle&gt;&lt;symbolic-name&gt;platform.controllers&lt;/symbolic-name&gt;&lt;version&gt;0.6.0&lt;/version&gt;&lt;/bundle&gt;
        &lt;bundle&gt;&lt;symbolic-name&gt;platform.command&lt;/symbolic-name&gt;&lt;version&gt;0.6.0&lt;/version&gt;&lt;/bundle&gt;
        &lt;bundle&gt;&lt;symbolic-name&gt;platform.common-export&lt;/symbolic-name&gt;&lt;version&gt;0.6.0&lt;/version&gt;&lt;/bundle&gt;
        &lt;bundle&gt;&lt;symbolic-name&gt;platform.swing&lt;/symbolic-name&gt;&lt;version&gt;0.6.0&lt;/version&gt;&lt;/bundle&gt;
      &lt;/bundles&gt;
    &lt;/extension&gt;
  &lt;/extensions&gt;
&lt;/plugin&gt;

 * </pre>
 * <p>
 * This class is thread-safe
 */
public class PluginConfigurationParser {

    private static final Logger logger = LoggingUtils.getLogger(PluginConfigurationParser.class);
    // thread safe because there is no state :)

    public PluginConfiguration parse(File configurationFile) throws FileNotFoundException, PluginConfigurationValidatorException {
        PluginValidator validator = new PluginValidator();
        validator.validate(configurationFile);
        PluginConfiguration config = null;
        try (FileInputStream fis = new FileInputStream(configurationFile)) {
            config = parse(configurationFile.getParentFile().getName(), fis);

        } catch (IOException ioFisClosed) {
            // ignore if fis closing fails
        }
        return config;
    }

    PluginConfiguration parse(String pluginName, InputStream configurationStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new ConfigurationParserErrorHandler());
            Document xmlDoc = builder.parse(configurationStream);
            Node rootNode = xmlDoc.getFirstChild();
            if (rootNode == null) {
                throw new PluginConfigurationParseException("no configuration found");
            }
            return parseRootElement(pluginName, rootNode);
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new PluginConfigurationParseException("failed to parse plugin configuration", exception);
        }
    }

    private PluginConfiguration parseRootElement(String pluginName, Node root) {
        List<NewCommand> commands = Collections.emptyList();
        List<CommandExtensions> extensions = Collections.emptyList();
        Configurations configurations = null;
        String pluginID = null;

        if (root.getNodeName().equals("plugin")) {
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equals("commands")) {
                    commands = parseCommands(pluginName, node);
                } else if (node.getNodeName().equals("extensions")) {
                    extensions = parseExtensions(pluginName, node);
                } else if (node.getNodeName().equals("id")) {
                    pluginID = node.getTextContent().trim();
                } else if (node.getNodeName().equals("configurations") && (pluginID != null)) {
                    configurations = parseConfigurations(node);
                }
            }
        }

        if (commands.isEmpty() && extensions.isEmpty()) {
            logger.warning("plugin " + pluginName + " does not extend any command or provide any new commands");
        }

        if (configurations == null) {
            configurations = Configurations.emptyConfigurations();
        }
        if (pluginID == null) {
            pluginID = "";
        }

        return new PluginConfiguration(commands, extensions, new PluginID(pluginID), configurations);
    }

    private List<NewCommand> parseCommands(String pluginName, Node commandsNode) {
        List<NewCommand> newCommands = new ArrayList<NewCommand>();
        NodeList childNodes = commandsNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeName().equals("command")) {
                NewCommand newCmd = parseNewCommand(pluginName, node);
                if (newCmd != null) {
                    newCommands.add(newCmd);
                }
            }
        }
        return newCommands;
    }

    private List<CommandExtensions> parseExtensions(String pluginName, Node extensionsNode) {
        List<CommandExtensions> commandExtensions = new ArrayList<CommandExtensions>();
        NodeList childNodes = extensionsNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeName().equals("extension")) {
                CommandExtensions additions = parseAdditionsToExistingCommand(pluginName, node);
                if (additions != null) {
                    commandExtensions.add(additions);
                }
            }
        }
        return commandExtensions;
    }

    private Configurations parseConfigurations(Node configurationsNode) {
        NodeList childNodes = configurationsNode.getChildNodes();
        Map<String, String> configs = new HashMap<String, String>();


        String file = null;

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeName().equals("configuration")) {
                file = node.getTextContent().trim();
                File f = new File(file);
                configs.put(f.getName(), file);
                if (!f.exists() || !f.isFile()) {
                    logger.warning("Plugin file: " + file + " does not exist or is not a valid file.");
                }
            }
        }
        return new Configurations(configs);
    }

    private CommandExtensions parseAdditionsToExistingCommand(String pluginName, Node commandNode) {
        String name = null;
        List<BundleInformation> bundles = new ArrayList<>();

        NodeList nodes = commandNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("name")) {
                name = node.getTextContent().trim();
            } else if (node.getNodeName().equals("bundles")) {
                bundles.addAll(parseBundles(pluginName, name, node));
            }
        }

        if (bundles.isEmpty()) {
            logger.warning("plugin " + pluginName + " extends the command " + name + " but supplies no bundles");
        }

        if (name == null) {
            logger.warning("plugin " + pluginName + " provides extensions without specifying the command");
            return null;
        }
        return new CommandExtensions(name, bundles);
    }

    private NewCommand parseNewCommand(String pluginName, Node commandNode) {
        String name = null;
        String usage = null;
        String description = null;
        List<String> arguments = new ArrayList<>();
        Options options = new Options();
        Set<Environment> availableInEnvironments = EnumSet.noneOf(Environment.class);
        List<BundleInformation> bundles = new ArrayList<>();

        NodeList nodes = commandNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("name")) {
                name = node.getTextContent().trim();
            } else if (node.getNodeName().equals("usage")) {
                usage = node.getTextContent().trim();
            } else if (node.getNodeName().equals("description")) {
                description = parseDescription(node);
            } else if (node.getNodeName().equals("arguments")) {
                arguments = parseArguments(pluginName, name, node);
            } else if (node.getNodeName().equals("options")) {
                options = parseOptions(node);
            } else if (node.getNodeName().equals("environments")) {
                availableInEnvironments = parseEnvironment(pluginName, name, node);
            } else if (node.getNodeName().equals("bundles")) {
                bundles.addAll(parseBundles(pluginName, name, node));
            }
        }

        if (bundles.isEmpty()) {
            logger.warning("plugin " + pluginName  + " provides a new command " + name + " but supplies no bundles");
        }

        if (name == null || description == null || availableInEnvironments.isEmpty()) {
            logger.warning("plugin " + pluginName + " provides an incomplete new command: " +
                    "name='" + name + "', description='" + description + "', options='" + options + "'");
            return null;
        } else {
            return new NewCommand(name, usage, description, arguments, options, availableInEnvironments, bundles);
        }
    }

    private List<BundleInformation> parseBundles(String pluginName, String commandName, Node bundlesNode) {
        List<BundleInformation> result = new ArrayList<>();
        NodeList nodes = bundlesNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("bundle")) {
                BundleInformation bundleInfo = parseBundle(pluginName, commandName, node);
                if (bundleInfo != null) {
                    result.add(bundleInfo);
                }
            }
        }
        return result;
    }

    private BundleInformation parseBundle(String pluginName, String commandName, Node bundleNode) {
        String name = null;
        String version = null;
        NodeList nodes = bundleNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("symbolic-name")) {
                name = node.getTextContent().trim();
            } else if (node.getNodeName().equals("version")) {
                version = node.getTextContent().trim();
                // FIXME hack: convert maven-style qualifiers to osgi-style qualifiers.
                // thermostat uses ${project.version}, which is the maven version, in the
                // plugin.xml files. This converts that maven -SNAPSHOT into OSGi .SNAPSHOT
                if (version.endsWith("-SNAPSHOT")) {
                    version = version.replace("-SNAPSHOT", ".SNAPSHOT");
                }
            }
        }
        if (name == null || version == null) {
            logger.warning("plugin " + pluginName  + " has an empty bundles element for command " + name);
            return null;
        }

        return new BundleInformation(name, version);
    }

    private String parseDescription(Node descriptionNode) {
        String text = descriptionNode.getTextContent().trim();
        String[] lines = text.split("(" + System.lineSeparator() + ")+");
        StringBuilder result = new StringBuilder();
        for (String line : lines) {
            result.append(line.trim());
            result.append(" ");
        }
        return result.toString().trim();
    }

    private List<String> parseArguments(String pluginName, String commandName, Node argumentsNode) {
        return parseNodeAsList(pluginName, commandName, argumentsNode, "argument");
    }

    private Options parseOptions(Node optionsNode) {
        Options opts = new Options();
        NodeList nodes = optionsNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("group")) {
                OptionGroup group = parseOptionGroup(node);
                opts.addOptionGroup(group);
            } else if (node.getNodeName().equals("option")) {
                Option option = parseOption(node);
                if (option != null) {
                    opts.addOption(option);
                }
            }
        }

        return opts;
    }

    private OptionGroup parseOptionGroup(Node optionGroupNode) {
        OptionGroup group = new OptionGroup();

        NodeList nodes = optionGroupNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("option")) {
                Option option = parseOption(node);
                if (option != null) {
                    group.addOption(option);
                }
            } else if (node.getNodeName().equals("required")) {
                group.setRequired(Boolean.valueOf(node.getTextContent().trim()));
            }
        }

        return group;
    }

    private Option parseOption(Node optionNode) {
        Option option;
        Node type = optionNode.getAttributes().getNamedItem("common");
        if (type != null && Boolean.valueOf(type.getNodeValue())) {
            option = parseCommonOption(optionNode);
        } else {
            option = parseNormalOption(optionNode);
        }

        return option;
    }

    private Option parseCommonOption(Node optionNode) {
        String longName = null;
        String shortName = null;
        boolean required = false;
        Option option = null;

        NodeList nodes = optionNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("long")) {
                longName = node.getTextContent().trim();
            } else if (node.getNodeName().equals("short")) {
                shortName = node.getTextContent().trim();
            } else if (node.getNodeName().equals("required")) {
                required = Boolean.valueOf(node.getTextContent().trim());
            }
        }

        List<Option> allKnownOptions = new ArrayList<Option>();
        allKnownOptions.addAll(CommonOptions.getDbOptions());
        allKnownOptions.add(CommonOptions.getLogOption());

        for (Option knownOption : allKnownOptions) {
            if (knownOption.getOpt().equals(shortName) || knownOption.getLongOpt().equals(longName)) {
                option = new Option(knownOption.getOpt(), knownOption.getLongOpt(), knownOption.hasArg(), knownOption.getDescription());
                option.setRequired(required);
                option.setArgName(knownOption.getArgName());
                return option;
            }
        }

        logger.warning("The option " + longName != null ? longName : shortName + " claims to be a common option but it isn't");

        return null;
    }

    private Option parseNormalOption(Node optionNode) {
        String longName = null;
        String shortName = null;
        String argument = null;
        String description = null;
        boolean required = false;

        NodeList nodes = optionNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("long")) {
                longName = node.getTextContent().trim();
            } else if (node.getNodeName().equals("short")) {
                shortName = node.getTextContent().trim();
            } else if (node.getNodeName().equals("argument")) {
                argument = node.getTextContent().trim();
            } else if (node.getNodeName().equals("description")) {
                description = node.getTextContent().trim();
            } else if (node.getNodeName().equals("required")) {
                required = Boolean.valueOf(node.getTextContent().trim());
            }
        }

        Option opt = new Option(shortName, longName, (argument != null), description);
        if (argument != null) {
            opt.setArgName(argument);
        }
        opt.setRequired(required);
        return opt;
    }

    private Set<Environment> parseEnvironment(String pluginName, String commandName, Node environmentNode) {
        EnumSet<Environment> result = EnumSet.noneOf(Environment.class);
        List<String> environments = parseNodeAsList(pluginName, commandName, environmentNode, "environment");
        for (String environment : environments) {
            if (environment.equals("shell")) {
                result.add(Environment.SHELL);
            } else if (environment.equals("cli")) {
                result.add(Environment.CLI);
            }
        }
        return result;
    }

    private List<String> parseNodeAsList(String pluginName, String commandName, Node parentNode, String childElementName) {
        List<String> result = new ArrayList<>();
        NodeList nodes = parentNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals(childElementName)) {
                String data = node.getTextContent().trim();
                result.add(data);
            }
        }

        if (result.isEmpty()) {
            logger.warning("plugin " + pluginName + " has an empty " + parentNode.getNodeName()
                + " element for command " + commandName);
        }

        return result;
    }


    private static class ConfigurationParserErrorHandler implements ErrorHandler {

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            // no-op
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            throw exception;
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            throw exception;
        }
    }

}

