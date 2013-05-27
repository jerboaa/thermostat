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

package com.redhat.thermostat.common.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.Configuration;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;

public class SSLConfiguration {

    private static Properties clientProps = null;
    private static final String KEYSTORE_FILE_KEY = "KEYSTORE_FILE";
    private static final String KEYSTORE_FILE_PWD_KEY = "KEYSTORE_PASSWORD";
    private static final String CMD_CHANNEL_SSL_KEY = "COMMAND_CHANNEL_USE_SSL";
    private static final String BACKING_STORAGE_USE_SSL_KEY = "BACKING_STORAGE_CONNECTION_USE_SSL";
    private static final String DISABLE_HOSTNAME_VERIFICATION = "DISABLE_HOSTNAME_VERIFICATION";
    private static final Logger logger = LoggingUtils.getLogger(SSLConfiguration.class);

    /**
     * 
     * @return The keystore file as specified in $THERMOSTAT_HOME/etc/ssl.properties
     *         if any. null otherwise.
     */
    public static File getKeystoreFile() {
        try {
            loadClientProperties();
        } catch (InvalidConfigurationException e) {
            // Thermostat home not set? Should have failed earlier. Do something
            // reasonable.
            return null;
        }
        String path = clientProps.getProperty(KEYSTORE_FILE_KEY);
        if (path != null) {
            File file = new File(path);
            return file;
        }
        return null;
    }

    /**
     * 
     * @return The keystore file as specified in $THERMOSTAT_HOME/etc/ssl.properties
     *         if any, null otherwise.
     */
    public static String getKeyStorePassword() {
        try {
            loadClientProperties();
        } catch (InvalidConfigurationException e) {
            // Thermostat home not set? Do something reasonable
            return null;
        }
        String pwd = clientProps.getProperty(KEYSTORE_FILE_PWD_KEY);
        return pwd;
    }
    
    /**
     * 
     * @return true if and only if SSL should be enabled for command channel
     *         communication between agent and client. I.e. if
     *         $THERMOSTAT_HOME/etc/ssl.properties exists and proper config has
     *         been added. false otherwise.
     */
    public static boolean enableForCmdChannel() {
        return readBooleanProperty(CMD_CHANNEL_SSL_KEY);
    }

    /**
     * 
     * @return true if and only if SSL should be used for backing storage
     *         connections. I.e. if $THERMOSTAT_HOME/etc/ssl.properties exists
     *         and proper config has been added. false otherwise.
     */
    public static boolean enableForBackingStorage() {
        return readBooleanProperty(BACKING_STORAGE_USE_SSL_KEY);
    }
    
    /**
     * 
     * @return true if and only if host name verification should not be
     *         performed during SSL handshake. In other words if
     *         $THERMOSTAT_HOME/etc/ssl.properties exists and proper config has
     *         been added. false otherwise.
     */
    public static boolean disableHostnameVerification() {
        return readBooleanProperty(DISABLE_HOSTNAME_VERIFICATION);
    }

    // testing hook
    static void initClientProperties(File clientPropertiesFile) {
        clientProps = new Properties();
        try {
            clientProps.load(new FileInputStream(clientPropertiesFile));
        } catch (IOException | IllegalArgumentException e) {
            // Could not load ssl properties file. This is fine as it's
            // an optional config.
        }
    }

    private static boolean readBooleanProperty(final String property) {
        boolean result = false;
        try {
            loadClientProperties();
        } catch (InvalidConfigurationException e) {
            logger.log(Level.WARNING,
                    "THERMOSTAT_HOME not set and config file attempted to be " +
                    		"read from there! Returning false.");
            return result;
        }
        String token = clientProps.getProperty(property);
        if (token != null) {
            result = Boolean.parseBoolean(token);
        }
        return result;
    }

    private static void loadClientProperties()
            throws InvalidConfigurationException {
        if (clientProps == null) {
            File clientPropertiesFile = new File(new Configuration().getConfigurationDir(),
                    "ssl.properties");
            initClientProperties(clientPropertiesFile);
        }
    }
}

