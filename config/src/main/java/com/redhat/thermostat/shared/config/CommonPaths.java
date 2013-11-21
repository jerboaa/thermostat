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

import com.redhat.thermostat.annotations.Service;

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
@Service
public interface CommonPaths {

    public File getSystemThermostatHome() throws InvalidConfigurationException;

    public File getUserThermostatHome() throws InvalidConfigurationException;

    public File getSystemPluginRoot() throws InvalidConfigurationException;

    public File getSystemLibRoot() throws InvalidConfigurationException;

    public File getSystemBinRoot() throws InvalidConfigurationException;

    public File getSystemNativeLibsRoot() throws InvalidConfigurationException;

    public File getSystemConfigurationDirectory() throws InvalidConfigurationException;

    public File getUserConfigurationDirectory() throws InvalidConfigurationException;

    /** A location that contains data that is persisted */
    public File getUserPersistentDataDirectory() throws InvalidConfigurationException;

    /** Contains data that is only useful for the duration that thermostat is running */
    public File getUserRuntimeDataDirectory() throws InvalidConfigurationException;

    public File getUserLogDirectory() throws InvalidConfigurationException;

    public File getUserCacheDirectory() throws InvalidConfigurationException;

    public File getUserPluginRoot() throws InvalidConfigurationException;

    public File getUserStorageDirectory() throws InvalidConfigurationException;

    public File getSystemStorageConfigurationFile() throws InvalidConfigurationException;

    public File getUserStorageConfigurationFile() throws InvalidConfigurationException;

    public File getUserStorageLogFile() throws InvalidConfigurationException;

    public File getUserStoragePidFile() throws InvalidConfigurationException;

    public File getSystemAgentConfigurationFile() throws InvalidConfigurationException;

    public File getUserAgentConfigurationFile() throws InvalidConfigurationException;

    public File getSystemAgentAuthConfigFile() throws InvalidConfigurationException;

    public File getUserAgentAuthConfigFile() throws InvalidConfigurationException;

    public File getUserClientConfigurationFile() throws InvalidConfigurationException;

    public File getUserHistoryFile() throws InvalidConfigurationException;

}