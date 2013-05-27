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

import java.io.File;

import com.redhat.thermostat.shared.locale.LocaleResources;
import com.redhat.thermostat.shared.locale.Translate;

public class Configuration {

    private static final String THERMOSTAT_USER_DIR = ".thermostat";
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private String home;
    private boolean printOsgiInfo = false;

    public Configuration() throws InvalidConfigurationException {
        // allow this to be specified also as a property, especially for
        // tests, this overrides the env setting
        String home = System.getProperty("THERMOSTAT_HOME");
        if (home == null) {
            home = System.getenv("THERMOSTAT_HOME");
        }
        
        if (home == null) {
            throw new InvalidConfigurationException(t.localize(LocaleResources.ENV_NO_HOME));
        }
        this.home = home;
    }

    public String getThermostatHome() throws InvalidConfigurationException {
        return home;
    }

    public String getThermostatUserHome() {
        String home = System.getProperty("user.home");
        return home + File.separator + THERMOSTAT_USER_DIR;
    }

    public String getPluginRoot() throws InvalidConfigurationException {
        return home + File.separator + "plugins";
    }

    public File getBackendsBaseDirectory() throws InvalidConfigurationException {
        String loc = getThermostatHome() + File.separatorChar + "backends";
        File file = new File(loc);
        return file;
    }

    public String getLibRoot() throws InvalidConfigurationException {
        return home + File.separator + "libs";
    }
    
    public String getNativeLibsRoot() throws InvalidConfigurationException {
        return getLibRoot() + File.separator + "native";
    }

    public String getConfigurationDir() throws InvalidConfigurationException {
        return home + File.separator + "etc";
    }

    public File getStorageBaseDirectory() throws InvalidConfigurationException {
        String loc = getThermostatHome() + File.separatorChar + "storage";
        File file = new File(loc);
        return file;
    }
    
    public File getStorageDirectory() throws InvalidConfigurationException {
        return new File(getStorageBaseDirectory(), "db");
    }
    
    public File getStorageConfigurationFile() throws InvalidConfigurationException {
        return new File(getStorageBaseDirectory(), "db.properties");
    }

    public File getStorageLogFile() throws InvalidConfigurationException {
        File logDir = new File(getStorageBaseDirectory(), "logs");
        File logFile = new File(logDir, "db.log");
        
        return logFile;
    }

    public File getStoragePidFile() throws InvalidConfigurationException {
        File logDir = new File(getStorageBaseDirectory(), "run");
        File logFile = new File(logDir, "db.pid");
        
        return logFile;
    }

    public File getAgentConfigurationFile() throws InvalidConfigurationException {
        File agent = new File(getThermostatHome(), "agent");
        return new File(agent, "agent.properties");
    }

    public File getAgentAuthConfigFile() throws InvalidConfigurationException {
        File agent = new File(getThermostatHome(), "agent");
        return new File(agent, "agent.auth");
    }

    public File getClientConfigurationDirectory() throws InvalidConfigurationException {
        File client = new File(getThermostatHome(), "client");
        return client;
    }

    public File getHistoryFile() throws InvalidConfigurationException {
        File history = new File(getClientConfigurationDirectory(), "cli-history");
        return history;
    }

    public boolean getPrintOSGiInfo() {
        return printOsgiInfo;
    }

    public void setPrintOSGiInfo(boolean newValue) {
        printOsgiInfo = newValue;
    }

}

