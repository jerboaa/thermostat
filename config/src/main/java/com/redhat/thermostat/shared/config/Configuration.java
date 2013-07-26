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

/**
 * Contains locations to various files and directories used by thermostat
 * components.
 * <p>
 * Some configuration files or directories are system-wide while
 * others are per-user. System-wide file or directories are indicated
 * by the word System in the name. These should contain non-mutable
 * data that is meant to be used by all instances of thermostat
 * running on one machine. Per-user directories will normally contain
 * configuration and data specific for a different instance of
 * thermostat. Per-user files and directories indicated by the word
 * "User" in the method name.
 * <p>
 * The directories are split according to functionality, along the lines of
 * Filesystem Hierarchy Standard (FHS).
 */
public class Configuration {

    // Note: these paths are used by the integration tests too. Please update
    // them whenever you change this class.

    private static final String THERMOSTAT_HOME = "THERMOSTAT_HOME";
    private static final String USER_THERMOSTAT_HOME = "USER_THERMOSTAT_HOME";
    private static final String THERMOSTAT_USER_DIR = ".thermostat";
    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private final String home;
    private final File userHome;

    private boolean printOsgiInfo = false;

    public Configuration() throws InvalidConfigurationException {
        // allow this to be specified also as a property, especially for
        // tests, this overrides the env setting
        String home = System.getProperty(THERMOSTAT_HOME);
        if (home == null) {
            home = System.getenv(THERMOSTAT_HOME);
        }
        
        if (home == null) {
            throw new InvalidConfigurationException(THERMOSTAT_HOME + " not defined...");
        }
        this.home = home;

        // allow this to be specified also as a special property, meant for tests
        String userHome = System.getProperty(USER_THERMOSTAT_HOME);
        if (userHome == null) {
            userHome = System.getenv(USER_THERMOSTAT_HOME);
        }
        if (userHome == null) {
            userHome = System.getProperty("user.home") + File.separatorChar + THERMOSTAT_USER_DIR;
        }
        this.userHome = new File(userHome);
    }

    /*
     * Overall hierarchy
     *
     * TODO: these might have to be different for 'root' or 'thermostat' users
     */

    public File getSystemThermostatHome() throws InvalidConfigurationException {
        return new File(home);
    }

    public File getUserThermostatHome() throws InvalidConfigurationException {
        return userHome;
    }

    public File getSystemPluginRoot() throws InvalidConfigurationException {
        return new File(home, "plugins");
    }

    public File getSystemLibRoot() throws InvalidConfigurationException {
        return new File(home, "libs");
    }
    
    public File getSystemNativeLibsRoot() throws InvalidConfigurationException {
        return new File(getSystemLibRoot(), "native");
    }

    public File getSystemConfigurationDirectory() throws InvalidConfigurationException {
        return new File(getSystemThermostatHome(), "etc");
    }

    public File getUserConfigurationDirectory() throws InvalidConfigurationException {
        return new File(getUserThermostatHome(), "etc");
    }

    /** A location that contains data that is persisted */
    public File getUserPersistentDataDirectory() throws InvalidConfigurationException {
        File dataDir = new File(getUserThermostatHome(), "data");
        return dataDir;
    }

    /** Contains data that is only useful for the duration that thermostat is running */
    public File getUserRuntimeDataDirectory() throws InvalidConfigurationException {
        File runDir = new File(getUserThermostatHome(), "run");
        return runDir;
    }

    public File getUserLogDirectory() throws InvalidConfigurationException {
        File logDir = new File(getUserThermostatHome(), "logs");
        return logDir;
    }

    public File getUserCacheDirectory() throws InvalidConfigurationException {
        File cacheDir = new File(getUserThermostatHome(), "cache");
        return cacheDir;
    }

    /* Specific files and directories */

    public File getUserStorageDirectory() throws InvalidConfigurationException {
        return new File(getUserPersistentDataDirectory(), "db");
    }

    public File getSystemStorageConfigurationFile() throws InvalidConfigurationException {
        return new File(getSystemConfigurationDirectory(), "db.properties");
    }

    public File getUserStorageConfigurationFile() throws InvalidConfigurationException {
        return new File(getUserConfigurationDirectory(), "db.properties");
    }

    public File getUserStorageLogFile() throws InvalidConfigurationException {
        File logFile = new File(getUserLogDirectory(), "db.log");
        return logFile;
    }

    public File getUserStoragePidFile() throws InvalidConfigurationException {
        File logFile = new File(getUserRuntimeDataDirectory(), "db.pid");
        return logFile;
    }

    public File getSystemAgentConfigurationFile() throws InvalidConfigurationException {
        return new File(getSystemConfigurationDirectory(), "agent.properties");
    }

    public File getUserAgentConfigurationFile() throws InvalidConfigurationException {
        return new File(getUserConfigurationDirectory(), "agent.properties");
    }

    public File getSystemAgentAuthConfigFile() throws InvalidConfigurationException {
        return new File(getSystemConfigurationDirectory(), "agent.auth");
    }

    public File getUserAgentAuthConfigFile() throws InvalidConfigurationException {
        return new File(getUserConfigurationDirectory(), "agent.auth");
    }

    public File getUserClientConfigurationFile() throws InvalidConfigurationException {
        File client = new File(getUserConfigurationDirectory(), "client.properties");
        return client;
    }

    public File getUserHistoryFile() throws InvalidConfigurationException {
        File history = new File(getUserPersistentDataDirectory(), "cli-history");
        return history;
    }

    // TODO add logging files here (see LoggingUtils)
    // TODO add ssl.properties file here (see SSLConfiguration)
    
    public boolean getPrintOSGiInfo() {
        return printOsgiInfo;
    }

    public void setPrintOSGiInfo(boolean newValue) {
        printOsgiInfo = newValue;
    }

}

