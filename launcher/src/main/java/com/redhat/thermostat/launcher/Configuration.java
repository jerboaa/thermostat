/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.launcher;

import java.io.File;

/*
 * This is a duplicate of com.redhat.thermostat.common.config.ConfigUtils
 * TODO: expose this as an OSGI service and remove the copy in config
 *  _or_
 * remove this; somehow tell the launcher to figure out where it can find files
 * and bundles and expose the real ConfigUtils as an OSGI service.
 */
public class Configuration {

    private static final String THERMOSTAT_USER_DIR = ".thermostat";

    private final String home;

    public Configuration() {
        // allow this to be specified also as a property, especially for
        // tests, this overrides the env setting
        String home = System.getProperty("THERMOSTAT_HOME");
        if (home == null) {
            home = System.getenv("THERMOSTAT_HOME");
        }
        this.home = home;
    }

    public String getThermostatHome() throws ConfigurationException {
        if (home == null) {
            throw new ConfigurationException("THERMOSTAT_HOME not defined...");
        }
        return home;
    }

    public String getThermostatUserHome() {
        String home = System.getProperty("user.home");
        return home + File.separator + THERMOSTAT_USER_DIR;
    }

    public File getBackendsBaseDirectory() throws ConfigurationException {
        String loc = getThermostatHome() + File.separatorChar + "backends";
        File file = new File(loc);
        return file;
    }

    public File getStorageBaseDirectory() throws ConfigurationException {
        String loc = getThermostatHome() + File.separatorChar + "storage";
        File file = new File(loc);
        return file;
    }

    public File getStorageDirectory() throws ConfigurationException {
        return new File(getStorageBaseDirectory(), "db");
    }

    public File getStorageConfigurationFile() throws ConfigurationException {
        return new File(getStorageBaseDirectory(), "db.properties");
    }

    public File getStorageLogFile() throws ConfigurationException {
        File logDir = new File(getStorageBaseDirectory(), "logs");
        File logFile = new File(logDir, "db.log");

        return logFile;
    }

    public File getStoragePidFile() throws ConfigurationException {
        File logDir = new File(getStorageBaseDirectory(), "run");
        File logFile = new File(logDir, "db.pid");

        return logFile;
    }

    public File getBackendPropertyFile(String backendName) throws ConfigurationException {
        File backendsConfigDir = getBackendsBaseDirectory();
        File backendConfig = new File(backendsConfigDir, backendName);
        backendConfig = new File(backendConfig, "backend.properties");
        return backendConfig;
    }

    public File getAgentConfigurationFile() throws ConfigurationException {
        File agent = new File(getThermostatHome(), "agent");
        return new File(agent, "agent.properties");
    }
}
