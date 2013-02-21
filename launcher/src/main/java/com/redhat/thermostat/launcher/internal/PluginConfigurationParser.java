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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.Options;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.CommandExtensions;
import com.redhat.thermostat.launcher.internal.PluginConfiguration.NewCommand;

/**
 * Parses the configuration of a plugin as specified in an {@code File} or an
 * {@code InputStream}. This configuration describes which new commands this
 * plugin provides as well as additional jars to load for existing commands.
 * <p>
 * A example configuration looks like the following:
 *
 * <pre>
 * &lt;?xml version="1.0"?&gt;
 * &lt;plugin&gt;
 *   &lt;commands&gt;
 *     &lt;command type="extends"&gt;
 *       &lt;name&gt;gui&lt;/name&gt;
 *       &lt;bundles&gt;
 *         &lt;bundle&gt;hello-world-plugin-0.1-SNAPSHOT.jar&lt;/bundle&gt;
 *       &lt;/bundles&gt;
 *       &lt;dependencies&gt;
 *         &lt;dependency&gt;thermostat-client-core-0.6.0-SNAPSHOT.jar&lt;/dependency&gt;
 *       &lt;/dependencies&gt;
 *     &lt;/command&gt;
 *     &lt;command type="provides"&gt;
 *       &lt;name&gt;hello&lt;/name&gt;
 *       &lt;description&gt;print hello&lt;/description&gt;
 *       &lt;usage&gt;hello&lt;/usage&gt;
 *       &lt;bundles&gt;
 *         &lt;bundle&gt;hello-world-plugin-0.1-SNAPSHOT.jar&lt;/bundle&gt;
 *       &lt;/bundles&gt;
 *       &lt;dependencies&gt;
 *         &lt;dependency&gt;thermostat-client-core-0.6.0-SNAPSHOT.jar&lt;/dependency&gt;
 *       &lt;/dependencies&gt;
 *     &lt;/command&gt;
 *   &lt;/commands&gt;
 * &lt;/plugin&gt;
 * </pre>
 * <p>
 * This class is thread-safe
 */
public class PluginConfigurationParser {

    private static final Logger logger = LoggingUtils.getLogger(PluginConfigurationParser.class);

    // thread safe because there is no state :)

    public PluginConfiguration parse(File configurationFile) throws FileNotFoundException {
        return parse(configurationFile.getParentFile().getName(), new FileInputStream(configurationFile));
    }

    public PluginConfiguration parse(String pluginName, InputStream configurationStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
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
        List<NewCommand> newCommands = Collections.emptyList();
        List<CommandExtensions> extensions = Collections.emptyList();

        Pair<List<NewCommand>, List<CommandExtensions>> commands = new Pair<>(newCommands, extensions);
        if (root.getNodeName().equals("plugin")) {
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equals("commands")) {
                    commands = parseCommands(pluginName, node);
                }
            }
        }

        return new PluginConfiguration(commands.getFirst(), commands.getSecond());
    }

    private Pair<List<NewCommand>, List<CommandExtensions>> parseCommands(String pluginName, Node commandsNode) {
        List<NewCommand> newCommands = new ArrayList<NewCommand>();
        List<CommandExtensions> extendedCommands = new ArrayList<CommandExtensions>();
        NodeList childNodes = commandsNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeName().equals("command")) {
                String type = node.getAttributes().getNamedItem("type").getNodeValue();
                if (type.equals("extends")) {
                    CommandExtensions additions = parseAdditionsToExistingCommand(pluginName, node);
                    if (additions != null) {
                        extendedCommands.add(additions);
                    }
                } else if (type.equals("provides")) {
                    NewCommand newCmd = parseNewCommand(pluginName, node);
                    if (newCmd != null) {
                        newCommands.add(newCmd);
                    }
                }
            }
        }
        return new Pair<>(newCommands, extendedCommands);
    }

    private CommandExtensions parseAdditionsToExistingCommand(String pluginName, Node commandNode) {
        String name = null;
        List<String> bundles = new ArrayList<>();
        List<String> dependencies = new ArrayList<>();

        NodeList nodes = commandNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("name")) {
                name = node.getTextContent();
            } else if (node.getNodeName().equals("bundles")) {
                bundles.addAll(parseBundles(node));
            } else if (node.getNodeName().equals("dependencies")) {
                dependencies.addAll(parseDependencies(node));
            }
        }
        if (name == null) {
            logger.warning("plugin " + pluginName + " provides extensions without specifying the command");
            return null;
        }
        return new CommandExtensions(name, bundles, dependencies);
    }

    private NewCommand parseNewCommand(String pluginName, Node commandNode) {
        String name = null;
        String usage = null;
        String description = null;
        Options options = new Options();
        List<String> bundles = new ArrayList<>();
        List<String> dependencies = new ArrayList<>();

        NodeList nodes = commandNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("name")) {
                name = node.getTextContent();
            } else if (node.getNodeName().equals("usage")) {
                usage = node.getTextContent();
            } else if (node.getNodeName().equals("description")) {
                description = node.getTextContent();
            } else if (node.getNodeName().equals("arguments")) {
                options = parseArguments(node);
            } else if (node.getNodeName().equals("bundles")) {
                bundles.addAll(parseBundles(node));
            } else if (node.getNodeName().equals("dependencies")) {
                dependencies.addAll(parseDependencies(node));
            }
        }

        if (name == null || usage == null || description == null) {
            logger.warning("plugin " + pluginName + " provides an incomplete new command: " +
                    "name='" + name + "', usage='" + usage + "', description='" + description + "', options='" + options + "'");
            return null;
        } else {
            return new NewCommand(name, usage, description, options, bundles, dependencies);
        }
    }

    private Collection<String> parseBundles(Node bundlesNode) {
        List<String> bundles = new ArrayList<>();
        NodeList nodes = bundlesNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("bundle")) {
                String bundleName = node.getTextContent();
                bundles.add(bundleName);
            }
        }
        return bundles;
    }

    private Collection<String> parseDependencies(Node dependenciesNode) {
        List<String> dependencies = new ArrayList<>();
        NodeList nodes = dependenciesNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("dependency")) {
                String bundleName = node.getTextContent();
                dependencies.add(bundleName);
            }
        }
        return dependencies;
    }

    private Options parseArguments(Node argumentsNode) {
        // TODO need to identify a way to express arguments
        return new Options();
    }

}
