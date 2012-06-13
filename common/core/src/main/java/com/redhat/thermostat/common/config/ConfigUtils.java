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

package com.redhat.thermostat.common.config;

import java.io.File;

public class ConfigUtils {

    private static final String THERMOSTAT_USER_DIR = ".thermostat";

    public static String getThermostatHome() throws InvalidConfigurationException {
        // allow this to be specified also as a property, especially for
        // tests, this overrides the env setting
        String home = System.getProperty("THERMOSTAT_HOME");
        if (home == null) {
            home = System.getenv("THERMOSTAT_HOME");
        }
        
        if (home == null) {
            throw new InvalidConfigurationException("THERMOSTAT_HOME not defined...");
        }
        return home;
    }

    public static String getThermostatUserHome() {
        String home = System.getProperty("user.home");
        return home + File.separator + THERMOSTAT_USER_DIR;
    }

    public static File getBackendsBaseDirectory() throws InvalidConfigurationException {
        String loc = getThermostatHome() + File.separatorChar + "backends";
        File file = new File(loc);
        return file;
    }
    
    public static File getStorageBaseDirectory() throws InvalidConfigurationException {
        String loc = getThermostatHome() + File.separatorChar + "storage";
        File file = new File(loc);
        return file;
    }
    
    public static File getStorageDirectory() throws InvalidConfigurationException {
        return new File(getStorageBaseDirectory(), "db");
    }
    
    public static File getStorageConfigurationFile() throws InvalidConfigurationException {
        return new File(getStorageBaseDirectory(), "db.properties");
    }

    public static File getStorageLogFile() throws InvalidConfigurationException {
        File logDir = new File(getStorageBaseDirectory(), "logs");
        File logFile = new File(logDir, "db.log");
        
        return logFile;
    }

    public static File getStoragePidFile() throws InvalidConfigurationException {
        File logDir = new File(getStorageBaseDirectory(), "run");
        File logFile = new File(logDir, "db.pid");
        
        return logFile;
    }

    public static File getBackendPropertyFile(String backendName)
            throws InvalidConfigurationException
    {
        File backendsConfigDir = ConfigUtils.getBackendsBaseDirectory();
        File backendConfig = new File(backendsConfigDir, backendName);
        backendConfig = new File(backendConfig, "backend.properties");
        return backendConfig;
    }

    public static File getAgentConfigurationFile() throws InvalidConfigurationException {

        File agent = new File(getThermostatHome(), "agent");
        return new File(agent, "agent.properties");
    }

    public static File getClientConfigurationDirectory() throws InvalidConfigurationException {
        File client = new File(getThermostatHome(), "client");
        return client;
    }

    public static File getHistoryFile() throws InvalidConfigurationException {
        File history = new File(getClientConfigurationDirectory(), "cli-history");
        return history;
    }
}
