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

package com.redhat.thermostat.shared.config;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

public class DirectoryStructureCreatorTest {

    private File systemThermostatHome, systemLibRoot, systemNativeLibsRoot, systemBinRoot,
                systemPluginRoot, systemConfigurationDirectory, systemPluginConfigurationDirectory, 
                userThermostatHome, userConfigurationDirectory, userRuntimeDataDirectory,
                userPluginConfigurationDirectory, userLogDirectory, userCacheDirectory,
                userPersistentDataDirectory, userPluginRoot, userStorageDirectory;


    private class TestCommonPaths implements CommonPaths {

        private TestCommonPaths(File tempRootDir) {
            systemThermostatHome = new File(tempRootDir, "systemThermostatHome");
            systemThermostatHome.deleteOnExit();
            systemLibRoot = new File(tempRootDir, "systemLibRoot");
            systemLibRoot.deleteOnExit();
            systemNativeLibsRoot = new File(tempRootDir, "systemNativeLibsRoot");
            systemNativeLibsRoot.deleteOnExit();
            systemBinRoot = new File(tempRootDir, "systemBinRoot");
            systemBinRoot.deleteOnExit();
            systemPluginRoot = new File(tempRootDir, "systemPluginRoot");
            systemPluginRoot.deleteOnExit();
            systemConfigurationDirectory = new File(tempRootDir, "systemConfigurationDir");
            systemConfigurationDirectory.deleteOnExit();
            systemPluginConfigurationDirectory = new File(tempRootDir, "systemPluginConfigurationDir");
            systemPluginConfigurationDirectory.deleteOnExit();
            userThermostatHome = new File(tempRootDir, "userThermostatHome");
            userThermostatHome.deleteOnExit();
            userConfigurationDirectory = new File(tempRootDir, "userConfigurationDirectory");
            userConfigurationDirectory.deleteOnExit();
            userPluginConfigurationDirectory = new File(tempRootDir, "userPluginConfigurationDir");
            userPluginConfigurationDirectory.deleteOnExit();
            userRuntimeDataDirectory = new File(tempRootDir, "userRuntimeDataDirectory");
            userRuntimeDataDirectory.deleteOnExit();
            userLogDirectory = new File(tempRootDir, "userLogDirectory");
            userLogDirectory.deleteOnExit();
            userCacheDirectory = new File(tempRootDir, "userCacheDirectory");
            userCacheDirectory.deleteOnExit();
            userPersistentDataDirectory = new File(tempRootDir, "userPersistentDataDirectory");
            userPersistentDataDirectory.deleteOnExit();
            userPluginRoot = new File(tempRootDir, "userPluginRoot");
            userPluginRoot.deleteOnExit();
            userStorageDirectory = new File(tempRootDir, "userStorageDirectory");
            userStorageDirectory.deleteOnExit();
        }

        @Override
        public File getSystemThermostatHome()
                throws InvalidConfigurationException {
            return systemThermostatHome;
        }

        @Override
        public File getUserThermostatHome()
                throws InvalidConfigurationException {
            return userThermostatHome;
        }

        @Override
        public File getSystemPluginRoot() throws InvalidConfigurationException {
            return systemPluginRoot;
        }

        @Override
        public File getSystemLibRoot() throws InvalidConfigurationException {
            return systemLibRoot;
        }

        @Override
        public File getSystemNativeLibsRoot()
                throws InvalidConfigurationException {
            return systemNativeLibsRoot;
        }

        @Override
        public File getSystemBinRoot()
                throws InvalidConfigurationException {
            return systemBinRoot;
        }

        @Override
        public File getSystemConfigurationDirectory()
                throws InvalidConfigurationException {
            return systemConfigurationDirectory;
        }

        @Override
        public File getSystemPluginConfigurationDirectory() throws InvalidConfigurationException {
            return systemPluginConfigurationDirectory;
        }

        @Override
        public File getUserPluginConfigurationDirectory() throws InvalidConfigurationException {
            return userPluginConfigurationDirectory;
        }

        @Override
        public File getUserConfigurationDirectory()
                throws InvalidConfigurationException {
            return userConfigurationDirectory;
        }

        @Override
        public File getUserPersistentDataDirectory()
                throws InvalidConfigurationException {
            return userPersistentDataDirectory;
        }

        @Override
        public File getUserRuntimeDataDirectory()
                throws InvalidConfigurationException {
            return userRuntimeDataDirectory;
        }

        @Override
        public File getUserLogDirectory() throws InvalidConfigurationException {
            return userLogDirectory;
        }

        @Override
        public File getUserCacheDirectory()
                throws InvalidConfigurationException {
            return userCacheDirectory;
        }

        @Override
        public File getUserPluginRoot() throws InvalidConfigurationException {
            return userPluginRoot;
        }

        @Override
        public File getUserStorageDirectory()
                throws InvalidConfigurationException {
            return userStorageDirectory;
        }

        @Override
        public File getSystemStorageConfigurationFile()
                throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getUserStorageConfigurationFile()
                throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getUserStorageLogFile()
                throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getUserStoragePidFile()
                throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getSystemAgentConfigurationFile()
                throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getUserAgentConfigurationFile()
                throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getSystemAgentAuthConfigFile()
                throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getUserAgentAuthConfigFile()
                throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getUserClientConfigurationFile()
                throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getUserSharedPreferencesFile()
                throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getUserHistoryFile() throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getUserSetupCompleteStampFile()
                throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }

        @Override
        public File getUserSplashScreenStampFile() throws InvalidConfigurationException{
            return null; // Only directories need to be created
        }
        
        @Override
        public File getUserIPCConfigurationFile() throws InvalidConfigurationException {
            return null; // Only directories need to be created
        }
        
    }

    @Test
    public void createsAllDirectories() throws IOException {
        File testRoot = Files.createTempDirectory("dirStructTest").toFile();
        testRoot.deleteOnExit();
        CommonPaths paths = new TestCommonPaths(testRoot);
        DirectoryStructureCreator creator = new DirectoryStructureCreator(paths);
        creator.createPaths();

        ensureDirCreated(systemThermostatHome);
        ensureDirCreated(systemLibRoot);
        ensureDirCreated(systemNativeLibsRoot);
        ensureDirCreated(systemBinRoot);
        ensureDirCreated(systemPluginRoot);
        ensureDirCreated(systemConfigurationDirectory);
        ensureDirCreated(userThermostatHome);
        ensureDirCreated(userConfigurationDirectory);
        ensureDirCreated(userRuntimeDataDirectory);
        ensureDirCreated(userLogDirectory);
        ensureDirCreated(userCacheDirectory);
        ensureDirCreated(userPersistentDataDirectory);
        ensureDirCreated(userPluginRoot);
        ensureDirCreated(userStorageDirectory);
    }

    private void ensureDirCreated(File dir) {
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
    }
}

