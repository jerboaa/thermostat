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

package com.redhat.thermostat.itest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PluginTest extends IntegrationTest {
    protected static final String SYSTEM_PLUGIN_HOME = getSystemPluginHome();
    protected static final String SYSTEM_PLUGIN_INSTALL_LOCATION = SYSTEM_PLUGIN_HOME + File.separator + "new";

    protected abstract static class BasicPlugin {
        private final String command;
        private final String pluginHome;
        
        protected BasicPlugin(String command, String pluginHome) {
            this.command = command;
            this.pluginHome = pluginHome;
        }
        
        protected String getCommandName() {
            return command;
        }
        
        protected String getPluginHome() {
            return pluginHome;
        }
        
        protected void doInstall(String thermostatPluginXml) {
            File home = new File(getPluginHome());
            if (!home.isDirectory() && !home.mkdirs()) {
                throw new AssertionError("could not create directory: " + getPluginHome());
            }
            try (FileWriter writer = new FileWriter(getPluginHome() + File.separator + "thermostat-plugin.xml")) {
                writer.write(thermostatPluginXml);
            } catch (IOException e) {
                throw new AssertionError("unable to write plugin configuration", e);
            }
        }
        
        protected void uninstall() {
            if (!new File(getPluginHome()).exists()) {
                return;
            }
            if (!new File(getPluginHome(), "thermostat-plugin.xml").delete()) {
                throw new AssertionError("Could not delete plugin file");
            }
            if (!new File(getPluginHome()).delete()) {
                throw new AssertionError("Could not delete plugin directory");
            }
        }
        
        protected abstract void install();
    }

    /**
     * This plugin provides a new command
     */
    protected static class NewCommandPlugin extends BasicPlugin {

        private final String description;
        private final String summary;

        public NewCommandPlugin(String command, String description, String summary, String pluginLocation) {
            super(command, pluginLocation);
            this.description = description;
            this.summary = summary;
        }

        @Override
        protected void install() {
            String pluginContents = "" +
                    "<?xml version=\"1.0\"?>\n" +
                    "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0 thermost-plugin.xsd\">\n" +
                    "  <commands>" +
                    "    <command>" +
                    "      <name>" + getCommandName() + "</name>" +
                    "      <summary>" + summary + "</summary>" +
                    "      <description>" + description + "</description>" +
                    "      <options>" +
                    "        <option>" +
                    "         <long>aaaaa</long>" +
                    "         <short>a</short>" +
                    "        </option>" +
                    "      </options>" +
                    "      <environments>" +
                    "        <environment>shell</environment>" +
                    "        <environment>cli</environment>" +
                    "      </environments>" +
                    "      <bundles>" +
                    "        <bundle>" +
                    "          <symbolic-name>bar</symbolic-name>" +
                    "          <version>0.1.0</version>" +
                    "        </bundle>" +
                    "      </bundles>" +
                    "    </command>" +
                    "  </commands>" +
                    "</plugin>";
            super.doInstall(pluginContents);
        }

    }

    /**
     * This plugin extends an unknown command
     */
    protected static class UnknownExtendsPlugin extends BasicPlugin {

        public UnknownExtendsPlugin(String pluginLocation) {
            super("unknown-command", pluginLocation);
        }

        @Override
        protected void install() {
            String pluginContents = "" +
                    "<?xml version=\"1.0\"?>\n" +
                    "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0 thermost-plugin.xsd\">\n" +
                    "  <extensions>" +
                    "    <extension>" +
                    "      <name>" + getCommandName() + "</name>" +
                    "      <bundles>" +
                    "        <bundle>" +
                    "          <symbolic-name>bar</symbolic-name>" +
                    "          <version>0.1.0</version>" +
                    "        </bundle>" +
                    "      </bundles>" +
                    "    </extension>" +
                    "  </extensions>" +
                    "</plugin>";

            super.doInstall(pluginContents);
        }

    }

}

