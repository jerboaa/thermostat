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

package com.redhat.thermostat.itest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import expectj.Spawn;

public class PluginTest extends IntegrationTest {

    private static final String PLUGIN_HOME = getThermostatHome() + File.separator + "plugins";

    private static NewCommandPlugin newPlugin = new NewCommandPlugin(PLUGIN_HOME + File.separator + "new");
    private static UnknownExtendsPlugin unknownExtension = new UnknownExtendsPlugin(PLUGIN_HOME + File.separator + "unknown");

    @BeforeClass
    public static void setUpOnce() {
        newPlugin.install();
        unknownExtension.install();
    }

    @AfterClass
    public static void tearDownOnce() {
        unknownExtension.uninstall();
        newPlugin.uninstall();
    }

    @Test
    public void testHelpIsOkay() throws Exception {
        Spawn shell = spawnThermostat("help");
        shell.expectClose();

        String stdOut = shell.getCurrentStandardOutContents();
        String stdErr = shell.getCurrentStandardErrContents();

        assertTrue(stdOut.contains("list of commands"));
        assertTrue(stdOut.contains("help"));
        assertTrue(stdOut.contains("agent"));
        assertTrue(stdOut.contains("gui"));
        assertTrue(stdOut.contains("ping"));
        assertTrue(stdOut.contains("shell"));

        assertTrue(stdOut.contains(newPlugin.command));
        assertTrue(stdOut.contains(newPlugin.description));

        assertFalse(stdOut.contains(unknownExtension.command));

        // TODO assertEquals("", stdErr);
    }

    /**
     * This plugin provides a new command
     */
    private static class NewCommandPlugin {

        private final String pluginHome;
        private final String command;
        private final String description;

        public NewCommandPlugin(String pluginLocation) {
            this.pluginHome = pluginLocation;

            this.command = "foo";
            this.description = "foo plugin to foo bar";
        }

        private void install() {
            File home = new File(pluginHome);
            if (!home.isDirectory() && !home.mkdir()) {
                throw new AssertionError("could not create directory: " + pluginHome);
            }

            String pluginContents = "" +
                    "<?xml version=\"1.0\"?>\n" +
                    "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0 thermost-plugin.xsd\">\n" +
                    "  <commands>" +
                    "    <command>" +
                    "      <name>" + command + "</name>" +
                    "      <description>" + description + "</description>" +
                    "      <environments>" +
                    "        <environment>shell</environment>" +
                    "        <environment>cli</environment>" +
                    "      </environments>" +
                    "      <options>" +
                    "        <option>" +
                    "         <long>aaaaa</long>" +
                    "         <short>a</short>" +
                    "        </option>" +
                    "      </options>" +
                    "      <bundles>" +
                    "        <bundle>bar</bundle>" +
                    "      </bundles>" +
                    "      <dependencies>" +
                    "        <dependency>foo</dependency>" +
                    "      </dependencies>" +
                    "    </command>" +
                    "  </commands>" +
                    "</plugin>";

            try (FileWriter writer = new FileWriter(pluginHome + File.separator + "thermostat-plugin.xml")) {
                writer.write(pluginContents);
            } catch (IOException e) {
                throw new AssertionError("unable to write plugin configuration", e);
            }

        }

        private void uninstall() {
            if (!new File(pluginHome).exists()) {
                return;
            }
            if (!new File(pluginHome, "thermostat-plugin.xml").delete()) {
                throw new AssertionError("Could not delete plugin file");
            }
            if (!new File(pluginHome).delete()) {
                throw new AssertionError("Could not delete plugin directory");
            }
        }
    }

    /**
     * This plugin extends an unknown command
     */
    private static class UnknownExtendsPlugin {

        private final String pluginHome;
        private final String command;

        public UnknownExtendsPlugin(String pluginLocation) {
            this.pluginHome = pluginLocation;

            this.command = "unknown-command";
        }

        private void install() {
            File home = new File(pluginHome);
            if (!home.isDirectory() && !home.mkdir()) {
                throw new AssertionError("could not create directory: " + pluginHome);
            }

            String pluginContents = "" +
                    "<?xml version=\"1.0\"?>\n" +
                    "<plugin xmlns=\"http://icedtea.classpath.org/thermostat/plugins/v1.0\"\n" +
                    " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    " xsi:schemaLocation=\"http://icedtea.classpath.org/thermostat/plugins/v1.0 thermost-plugin.xsd\">\n" +
                    "  <extensions>" +
                    "    <extension>" +
                    "      <name>" + command + "</name>" +
                    "      <bundles>" +
                    "        <bundle>bar</bundle>" +
                    "      </bundles>" +
                    "      <dependencies>" +
                    "        <dependency>foo</dependency>" +
                    "      </dependencies>" +
                    "    </extension>" +
                    "  </extensions>" +
                    "</plugin>";

            try (FileWriter writer = new FileWriter(pluginHome + File.separator + "thermostat-plugin.xml")) {
                writer.write(pluginContents);
            } catch (IOException e) {
                throw new AssertionError("unable to write plugin configuration", e);
            }

        }

        private void uninstall() {
            if (!new File(pluginHome).exists()) {
                return;
            }
            if (!new File(pluginHome, "thermostat-plugin.xml").delete()) {
                throw new AssertionError("Could not delete plugin file");
            }
            if (!new File(pluginHome).delete()) {
                throw new AssertionError("Could not delete plugin directory");
            }
        }
    }

}
