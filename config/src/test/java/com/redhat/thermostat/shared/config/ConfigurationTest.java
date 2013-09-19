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

package com.redhat.thermostat.shared.config;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationTest {

    @Before
    public void setUp() {
        System.clearProperty("THERMOSTAT_HOME");
        System.clearProperty("USER_THERMOSTAT_HOME");
        System.clearProperty("THERMOSTAT_SYSTEM_USER");
    }
    
    @After
    public void tearDown() {
        System.clearProperty("THERMOSTAT_HOME");
        System.clearProperty("USER_THERMOSTAT_HOME");
        System.clearProperty("THERMOSTAT_SYSTEM_USER");
    }

    @Test
    public void testSystemLocations() throws InvalidConfigurationException, IOException {
        String thermostatHome = "/tmp";
        System.setProperty("THERMOSTAT_HOME", thermostatHome);

        char s = File.separatorChar;

        Configuration config = new Configuration();

        Assert.assertEquals(thermostatHome, config.getSystemThermostatHome().getCanonicalPath());

        Assert.assertEquals(thermostatHome + s + "libs" + s + "native",
                config.getSystemNativeLibsRoot().getCanonicalPath());
        Assert.assertEquals(thermostatHome + s + "etc", config.getSystemConfigurationDirectory().getCanonicalPath());
        Assert.assertEquals(thermostatHome + s + "libs", config.getSystemLibRoot().getCanonicalPath());
        Assert.assertEquals(thermostatHome + s + "plugins", config.getSystemPluginRoot().getCanonicalPath());
    }

    @Test
    public void testUserLocations() throws InvalidConfigurationException, IOException {
        String thermostatHome = "/tmp";
        System.setProperty("THERMOSTAT_HOME", thermostatHome);
        char s = File.separatorChar;
        String userHome = System.getProperty("user.home") + s + ".thermostat";
        Configuration config = new Configuration();

        Assert.assertEquals(userHome + s + "etc" + s + "agent.properties",
                config.getUserAgentConfigurationFile().getCanonicalPath());
        Assert.assertEquals(userHome + s + "etc" + s + "agent.auth",
                config.getUserAgentAuthConfigFile().getCanonicalPath());
        Assert.assertEquals(userHome + s + "etc" + s + "db.properties",
                config.getUserStorageConfigurationFile().getCanonicalPath());

        Assert.assertEquals(userHome + s + "data" + s + "db",
                config.getUserStorageDirectory().getCanonicalPath());
        Assert.assertEquals(userHome + s + "run" + s + "db.pid",
                config.getUserStoragePidFile().getCanonicalPath());
        Assert.assertEquals(userHome + s + "logs" + s + "db.log",
                config.getUserStorageLogFile().getCanonicalPath());

        Assert.assertEquals(userHome + s + "data" + s + "plugins",
                config.getUserPluginRoot().getCanonicalPath());
    }

    @Test
    public void testPrivilegedUserLocations() throws InvalidConfigurationException, IOException {
        String thermostatHome = "/tmp";
        System.setProperty("THERMOSTAT_HOME", thermostatHome);
        System.setProperty("THERMOSTAT_SYSTEM_USER", "");
        Configuration config = new Configuration();

        // the paths are unix specific, but so are the paths in Configuration

        Assert.assertEquals("/etc/thermostat/agent.properties", config.getUserAgentConfigurationFile().getCanonicalPath());
        Assert.assertEquals("/etc/thermostat/agent.auth", config.getUserAgentAuthConfigFile().getCanonicalPath());
        Assert.assertEquals("/etc/thermostat/db.properties", config.getUserStorageConfigurationFile().getCanonicalPath());

        Assert.assertEquals("/var/lib/thermostat/db", config.getUserStorageDirectory().getCanonicalPath());
        Assert.assertEquals("/var/run/thermostat/db.pid", config.getUserStoragePidFile().getAbsolutePath());
        Assert.assertEquals("/var/log/thermostat/db.log", config.getUserStorageLogFile().getCanonicalPath());

        Assert.assertEquals("/var/lib/thermostat/plugins", config.getUserPluginRoot().getCanonicalPath());
    }

    @Test
    public void testPrivilegedUserLocationsWithPrefix() throws InvalidConfigurationException, IOException {
        String thermostatHome = "/tmp";
        String prefix = "/opt/custom/prefix";
        System.setProperty("THERMOSTAT_HOME", thermostatHome);
        System.setProperty("USER_THERMOSTAT_HOME", prefix);
        System.setProperty("THERMOSTAT_SYSTEM_USER", "");
        Configuration config = new Configuration();

        // the paths are unix specific, but so are the paths in Configuration

        Assert.assertEquals(prefix + "/etc/thermostat/agent.properties", config.getUserAgentConfigurationFile().getCanonicalPath());
        Assert.assertEquals(prefix + "/etc/thermostat/agent.auth", config.getUserAgentAuthConfigFile().getCanonicalPath());
        Assert.assertEquals(prefix + "/etc/thermostat/db.properties", config.getUserStorageConfigurationFile().getCanonicalPath());

        Assert.assertEquals(prefix + "/var/lib/thermostat/db", config.getUserStorageDirectory().getCanonicalPath());
        Assert.assertEquals(prefix + "/var/run/thermostat/db.pid", config.getUserStoragePidFile().getAbsolutePath());
        Assert.assertEquals(prefix + "/var/log/thermostat/db.log", config.getUserStorageLogFile().getCanonicalPath());

        Assert.assertEquals(prefix + "/var/lib/thermostat/plugins", config.getUserPluginRoot().getCanonicalPath());
    }

    @Test
    public void instantiationThrowsException() {
        try {
            new Configuration();
            // The web archive uses this. See WebStorageEndPoint#init();
            fail("Should have thrown InvalidConfigurationException");
        } catch (InvalidConfigurationException e) {
            // pass
        }
    }
}
