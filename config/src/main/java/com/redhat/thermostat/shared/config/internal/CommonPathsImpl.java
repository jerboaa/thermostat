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

package com.redhat.thermostat.shared.config.internal;

import java.io.File;

import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.locale.Translate;
import com.redhat.thermostat.shared.locale.internal.LocaleResources;

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
 * <p>
 * The behaviour of this class is affected by the following environment
 * variables:
 * <dl>
 *   <di>{@code THERMOSTAT_HOME}</di>
 *   <dd>Specifies the location thermostat uses for it's read-only data,
 *     such as jars.</dd>
 *   <di>{@code THERMOSTAT_SYSTEM_USER}</di>
 *   <dd>If set, indicates that thermostat is running as a system user.
 *     In this mode, it reads configuration from system paths and places
 *     data in system writeable locations.</dd>
 *   <di>{@code USER_THERMOSTAT_HOME}</di>
 *   <dd>The meaning of this varies depending on whether thermostat is
 *     running as a system user or not. In normal mode, this controls
 *     where thermostat places it's data and where configuration files
 *     are searched for. In system mode, this is treated as a prefix
 *     writing system data.</dd>
 * </dl>
 */
public class CommonPathsImpl implements CommonPaths {

    // Note: these paths are used by the integration tests too. Please update
    // them whenever you change this class.

    // environment variables (also system properties for convenience):
    private static final String THERMOSTAT_HOME = "THERMOSTAT_HOME";
    private static final String USER_THERMOSTAT_HOME = "USER_THERMOSTAT_HOME";
    private static final String THERMOSTAT_SYSTEM_USER = "THERMOSTAT_SYSTEM_USER";


    private static final String THERMOSTAT_USER_DIR = ".thermostat";

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();

    private final File systemHome;
    private final UserDirectories userDirectories;

    private static File defaultSystemUserPrefix;

    public CommonPathsImpl() throws InvalidConfigurationException {
        this(makeDir(null, "/"));
    }

    CommonPathsImpl(String altTestingPrefix) {
        this(makeDir(null, altTestingPrefix));
    }

    private CommonPathsImpl(File defaultPrefix) {
        CommonPathsImpl.defaultSystemUserPrefix = defaultPrefix;
        // allow this to be specified also as a property, especially for
        // tests, this overrides the env setting
        String home = System.getProperty(THERMOSTAT_HOME);
        if (home == null) {
            home = System.getenv(THERMOSTAT_HOME);
        }

        if (home == null) {
            throw new InvalidConfigurationException(t.localize(LocaleResources.SYSHOME_NO_HOME));
        }
        this.systemHome = new File(home);
        if (!systemHome.exists()) {
            systemHome.mkdirs();
        }
        if (!systemHome.isDirectory()) {
            throw new InvalidConfigurationException(t.localize(LocaleResources.SYSHOME_NOT_A_DIR, home));
        }

        String systemUser = System.getProperty(THERMOSTAT_SYSTEM_USER);
        if (systemUser == null) {
            systemUser = System.getenv(THERMOSTAT_SYSTEM_USER);
        }

        if (systemUser != null) {
            userDirectories = new SystemUserDirectories();
        } else {
            userDirectories = new UnprivilegedUserDirectories();
        }
    }

    /*
     * Overall hierarchy
     */

    @Override
    public File getSystemThermostatHome() throws InvalidConfigurationException {
        return systemHome;
    }

    @Override
    public File getUserThermostatHome() throws InvalidConfigurationException {
        return userDirectories.getSystemRoot();
    }

    @Override
    public File getSystemPluginRoot() throws InvalidConfigurationException {
        return makeDir(systemHome, "plugins");
    }

    @Override
    public File getSystemLibRoot() throws InvalidConfigurationException {
        return makeDir(systemHome, "libs");
    }

    @Override
    public File getSystemBinRoot() throws InvalidConfigurationException {
        return makeDir(systemHome, "bin");
    }

    @Override
    public File getSystemNativeLibsRoot() throws InvalidConfigurationException {
        return makeDir(getSystemLibRoot(), "native");
    }

    @Override
    public File getSystemConfigurationDirectory() throws InvalidConfigurationException {
        return makeDir(getSystemThermostatHome(), "etc");
    }

    @Override
    public File getUserConfigurationDirectory() throws InvalidConfigurationException {
        return userDirectories.getUserConfigurationDirectory();
    }

    /** A location that contains data that is persisted */
    @Override
    public File getUserPersistentDataDirectory() throws InvalidConfigurationException {
        return userDirectories.getUserPersistentDataDirectory();
    }

    /**
     * Contains data that is only useful for the duration that thermostat is
     * running
     */
    @Override
    public File getUserRuntimeDataDirectory() throws InvalidConfigurationException {
        return userDirectories.getUserRuntimeDataDirectory();
    }

    @Override
    public File getUserLogDirectory() throws InvalidConfigurationException {
        return userDirectories.getUserLogDirectory();

    }

    @Override
    public File getUserCacheDirectory() throws InvalidConfigurationException {
        return userDirectories.getUserCacheDirectory();
    }

    /* Specific files and directories. All these methods should use the directories defined above */

    @Override
    public File getUserPluginRoot() throws InvalidConfigurationException {
        return new File(getUserPersistentDataDirectory(), "plugins");
    }

    @Override
    public File getUserStorageDirectory() throws InvalidConfigurationException {
        return makeDir(getUserPersistentDataDirectory(), "db");
    }

    @Override
    public File getSystemStorageConfigurationFile() throws InvalidConfigurationException {
        return new File(getSystemConfigurationDirectory(), "db.properties");
    }

    @Override
    public File getUserStorageConfigurationFile() throws InvalidConfigurationException {
        return new File(getUserConfigurationDirectory(), "db.properties");
    }

    @Override
    public File getUserStorageLogFile() throws InvalidConfigurationException {
        File logFile = new File(getUserLogDirectory(), "db.log");
        return logFile;
    }

    @Override
    public File getUserStoragePidFile() throws InvalidConfigurationException {
        File logFile = new File(getUserRuntimeDataDirectory(), "db.pid");
        return logFile;
    }

    @Override
    public File getSystemAgentConfigurationFile() throws InvalidConfigurationException {
        return new File(getSystemConfigurationDirectory(), "agent.properties");
    }

    @Override
    public File getUserAgentConfigurationFile() throws InvalidConfigurationException {
        return new File(getUserConfigurationDirectory(), "agent.properties");
    }

    @Override
    public File getSystemAgentAuthConfigFile() throws InvalidConfigurationException {
        return new File(getSystemConfigurationDirectory(), "agent.auth");
    }

    @Override
    public File getUserAgentAuthConfigFile() throws InvalidConfigurationException {
        return new File(getUserConfigurationDirectory(), "agent.auth");
    }

    @Override
    public File getUserClientConfigurationFile() throws InvalidConfigurationException {
        File client = new File(getUserConfigurationDirectory(), "client.properties");
        return client;
    }

    @Override
    public File getUserHistoryFile() throws InvalidConfigurationException {
        File history = new File(getUserPersistentDataDirectory(), "cli-history");
        return history;
    }

    // TODO add logging files here (see LoggingUtils)
    // TODO add ssl.properties file here (see SSLConfiguration)

    private interface UserDirectories {

        public File getSystemRoot();

        public File getUserConfigurationDirectory();

        public File getUserPersistentDataDirectory();

        public File getUserRuntimeDataDirectory();

        public File getUserLogDirectory();

        public File getUserCacheDirectory();

    }

    private static File makeDir(File parent, String name) {
        File dir = new File(parent, name);
        boolean exists = dir.exists();
        if (!exists) {
            exists = dir.mkdirs();
        }
        if (!exists) {
            throw new InvalidConfigurationException("Directory could not be created: " + dir.getAbsolutePath());
        }
        if (!dir.isDirectory()) {
            throw new InvalidConfigurationException(t.localize(LocaleResources.GENERAL_NOT_A_DIR, dir.getAbsolutePath()));
        }
        return dir;
    }

    /*
     * We need two different implementations because the paths are different. We
     * can't get clean paths by simply changing the prefix.
     *
     * user path:   $USER/.thermostat/{etc,log,...}
     * system path: /{etc,var/log,var/lib}/thermostat
     *
     * Notice how 'thermostat' comes first in one set of paths and later in the second set.
     */

    private static class UnprivilegedUserDirectories implements UserDirectories {

        private File userHome;

        public UnprivilegedUserDirectories() {
            // allow this to be specified also as a special property, meant for tests
            String userHome = System.getProperty(USER_THERMOSTAT_HOME);
            if (userHome == null) {
                userHome = System.getenv(USER_THERMOSTAT_HOME);
            }
            if (userHome == null) {
                userHome = System.getProperty("user.home") + File.separatorChar + THERMOSTAT_USER_DIR;
            }
            this.userHome = makeDir(null, userHome);
        }

        public File getSystemRoot() throws InvalidConfigurationException {
            return userHome;
        }


        public File getUserConfigurationDirectory() throws InvalidConfigurationException {
            return makeDir(getSystemRoot(), "etc");
        }

        public File getUserPersistentDataDirectory() throws InvalidConfigurationException {
            return makeDir(getSystemRoot(), "data");
        }

        public File getUserRuntimeDataDirectory() throws InvalidConfigurationException {
            return makeDir(getSystemRoot(), "run");
        }

        public File getUserLogDirectory() throws InvalidConfigurationException {
            return makeDir(getSystemRoot(), "logs");
        }

        public File getUserCacheDirectory() throws InvalidConfigurationException {
            return makeDir(getSystemRoot(), "cache");
        }
    }

    private static class SystemUserDirectories implements UserDirectories {

        private File prefix;

        public SystemUserDirectories() {
            // allow this to be specified also as a special property, meant for tests
            String userHome = System.getProperty(USER_THERMOSTAT_HOME);
            if (userHome == null) {
                userHome = System.getenv(USER_THERMOSTAT_HOME);
            }
            if (userHome == null) {
                this.prefix = defaultSystemUserPrefix;
            } else {
                this.prefix = makeDir(null, userHome);
            }
        }

        public File getSystemRoot() throws InvalidConfigurationException {
            return prefix;
        }


        public File getUserConfigurationDirectory() throws InvalidConfigurationException {
            return makeDir(getSystemRoot(), "etc/thermostat");
        }

        public File getUserPersistentDataDirectory() throws InvalidConfigurationException {
            return makeDir(getSystemRoot(), "var/lib/thermostat");
        }

        public File getUserRuntimeDataDirectory() throws InvalidConfigurationException {
            return makeDir(getSystemRoot(), "var/run/thermostat");
        }

        public File getUserLogDirectory() throws InvalidConfigurationException {
            return makeDir(getSystemRoot(), "var/log/thermostat");
        }

        public File getUserCacheDirectory() throws InvalidConfigurationException {
            return makeDir(getSystemRoot(), "var/cache/thermostat");
        }
    }

}
