/*
 * Copyright 2012-2016 Red Hat, Inc.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.config.SSLConfiguration;

public class SSLConfigurationImpl implements SSLConfiguration {

    private CommonPaths paths;
    private Properties configProps = null;
    private static final String SSL_PROPS_FILENAME = "ssl.properties";
    private static final String KEYSTORE_FILE_KEY = "KEYSTORE_FILE";
    private static final String KEYSTORE_FILE_PWD_KEY = "KEYSTORE_PASSWORD";
    private static final String CMD_CHANNEL_SSL_KEY = "COMMAND_CHANNEL_USE_SSL";
    private static final String BACKING_STORAGE_USE_SSL_KEY = "BACKING_STORAGE_CONNECTION_USE_SSL";
    private static final String DISABLE_HOSTNAME_VERIFICATION = "DISABLE_HOSTNAME_VERIFICATION";
    private static final Logger logger = Logger.getLogger(SSLConfigurationImpl.class.getName());

    public SSLConfigurationImpl(CommonPaths paths) {
        this.paths = paths;
    }

    @Override
    public File getKeystoreFile() {
        try {
            loadProperties();
        } catch (InvalidConfigurationException e) {
            // Thermostat home not set? Should have failed earlier. Do something
            // reasonable.
            return null;
        }
        String path = configProps.getProperty(KEYSTORE_FILE_KEY);
        if (path != null) {
            File file = new File(path);
            return file;
        }
        return null;
    }

    @Override
    public String getKeyStorePassword() {
        try {
            loadProperties();
        } catch (InvalidConfigurationException e) {
            // Thermostat home not set? Do something reasonable
            return null;
        }
        String pwd = configProps.getProperty(KEYSTORE_FILE_PWD_KEY);
        return pwd;
    }
    
    @Override
    public boolean enableForCmdChannel() {
        return readBooleanProperty(CMD_CHANNEL_SSL_KEY);
    }

    @Override
    public boolean enableForBackingStorage() {
        return readBooleanProperty(BACKING_STORAGE_USE_SSL_KEY);
    }
    
    @Override
    public boolean disableHostnameVerification() {
        return readBooleanProperty(DISABLE_HOSTNAME_VERIFICATION);
    }

    // testing hook
    void initProperties(File clientPropertiesFile) {
        configProps = new Properties();
        try {
            configProps.load(new FileInputStream(clientPropertiesFile));
        } catch (IOException | IllegalArgumentException e) {
            // Could not load ssl properties file. This is fine as it's
            // an optional config.
        }
    }

    private boolean readBooleanProperty(final String property) {
        boolean result = false;
        try {
            loadProperties();
        } catch (InvalidConfigurationException e) {
            logger.log(Level.WARNING,
                    "THERMOSTAT_HOME not set and config file attempted to be " +
                    		"read from there! Returning false.");
            return result;
        }
        String token = configProps.getProperty(property);
        if (token != null) {
            result = Boolean.parseBoolean(token);
        }
        return result;
    }

    // package-private for testing.
    void loadProperties()
            throws InvalidConfigurationException {
        if (configProps == null) {
            File userPropertiesFile = new File(paths.getUserConfigurationDirectory(),
                    SSL_PROPS_FILENAME);
            File systemPropertiesFile = new File(paths.getSystemConfigurationDirectory(),
                    SSL_PROPS_FILENAME);
            if (userPropertiesFile.exists()) {
                // user props overrides system file
                initProperties(userPropertiesFile);
            } else {
                // user props does not exist, use system properties file
                // (if any)
                initProperties(systemPropertiesFile);
            }
        }
    }
}

