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

package com.redhat.thermostat.web.client.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import com.redhat.thermostat.common.config.ConfigUtils;
import com.redhat.thermostat.common.config.InvalidConfigurationException;

public class SSLKeystoreConfiguration {

    private static Properties clientProps = null;
    private static final String KEYSTORE_FILE_KEY = "KEYSTORE_FILE";
    private static final String KEYSTORE_FILE_PWD_KEY = "KEYSTORE_PASSWORD";

    /**
     * 
     * @return The keystore file as specified in
     *         $THERMOSTAT_HOME/client.properties if any. null otherwise.
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
     * @return The keystore file as specified in
     *         $THERMOSTAT_HOME/client.properties if any. The empty string
     *         otherwise.
     * @throws InvalidConfigurationException
     */
    public static String getKeyStorePassword() {
        try {
            loadClientProperties();
        } catch (InvalidConfigurationException e) {
            // Thermostat home not set? Do something reasonable
            return "";
        }
        String pwd = clientProps.getProperty(KEYSTORE_FILE_PWD_KEY);
        if (pwd == null) {
            return "";
        } else {
            return pwd;
        }
    }

    // testing hook
    static void initClientProperties(File clientPropertiesFile) {
        clientProps = new Properties();
        try {
            clientProps.load(new FileInputStream(clientPropertiesFile));
        } catch (IOException | IllegalArgumentException e) {
            // Could not load client properties file. This is fine as it's
            // an optional config after all.
        }
    }

    private static void loadClientProperties()
            throws InvalidConfigurationException {
        if (clientProps == null) {
            File thermostatEtcDir = new File(ConfigUtils.getThermostatHome(),
                    "etc");
            File clientPropertiesFile = new File(thermostatEtcDir,
                    "client.properties");
            initClientProperties(clientPropertiesFile);
        }
    }
}
