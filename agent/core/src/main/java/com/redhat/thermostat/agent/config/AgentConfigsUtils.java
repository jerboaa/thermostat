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

package com.redhat.thermostat.agent.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;

import com.redhat.thermostat.agent.locale.LocaleResources;
import com.redhat.thermostat.shared.config.InvalidConfigurationException;
import com.redhat.thermostat.shared.locale.Translate;

public class AgentConfigsUtils {

    private static final Translate<LocaleResources> t = LocaleResources.createLocalizer();
    private static File systemConfiguration, userConfiguration, agentAuthFile;

    private static char[] pw = {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};
    private static char[] user = {'u', 's', 'e', 'r', 'n', 'a', 'm', 'e'};
    private static String newLine = System.lineSeparator();
    private static char comment = '#';

    public static void setConfigFiles(File systemConfigFile, File userConfigFile, File agentAuthFile) {
        AgentConfigsUtils.systemConfiguration = systemConfigFile;
        AgentConfigsUtils.userConfiguration = userConfigFile;
        AgentConfigsUtils.agentAuthFile = agentAuthFile;
    }

    public static AgentStartupConfiguration createAgentConfigs() throws InvalidConfigurationException {
        
        AgentStartupConfiguration config = new AgentStartupConfiguration();
        readAndSetProperties(systemConfiguration, userConfiguration, config);
        setAuthConfigFromFile(agentAuthFile, config);
        return config;
    }
    
    private static void readAndSetProperties(File systemConfigFile, File userConfigFile, AgentStartupConfiguration configuration)
            throws InvalidConfigurationException
    {
        Properties systemConfig = new Properties();
        if (systemConfigFile != null) {
            try {
                systemConfig.load(new FileInputStream(systemConfigFile));
            } catch (IOException e) {
                throw new InvalidConfigurationException(e);
            }
        }

        Properties properties = new Properties(systemConfig);
        if (userConfigFile != null) {
            try {
                properties.load(new FileInputStream(userConfigFile));
            } catch (IOException e) {
                // that's okay. just use system config
            }
        }

        String db = properties.getProperty(AgentProperties.DB_URL.name());
        if (db != null) {
            configuration.setDatabaseURL(db);
        }
        
        configuration.setPurge(true);
        if (properties.containsKey(AgentProperties.SAVE_ON_EXIT.name())) {
            String purge = (String) properties.get(AgentProperties.SAVE_ON_EXIT.name());
            configuration.setPurge(!Boolean.parseBoolean(purge));
        }

        // TODO: we could avoid this, which means the agent doesn't want to
        // accept any connection
        configuration.setConfigListenAddress("127.0.0.1:12000");
        if (properties.containsKey(AgentProperties.CONFIG_LISTEN_ADDRESS.name())) {
            String address = properties.getProperty(AgentProperties.CONFIG_LISTEN_ADDRESS.name());
            configuration.setConfigListenAddress(address);
        }
    }

    static void setAuthConfigFromFile(File authFile,
            AgentStartupConfiguration config) {
        // Default values will be enough if storage configured with not auth necessary.
        config.setUsername("");
        config.setPassword(new char[]{});
        if (authFile != null && authFile.canRead() && authFile.isFile()) {
            long length = authFile.length();
            char[] authData = null;
            try (Reader reader = new InputStreamReader(new FileInputStream(authFile), StandardCharsets.US_ASCII)) {
                if (length > Integer.MAX_VALUE || length < 0L) {
                    throw new InvalidConfigurationException(t.localize(LocaleResources.FILE_NOT_VALID, authFile.getCanonicalPath()));
                }
                int len = (int) length; // A reasonable file will be within int range.
                // file size in bytes >= # of chars so this size should be sufficient.
                authData = new char[len];
                // This is probably the most sensitive time for password-in-heap exposure.
                // The reader here may contain buffers containing the password.  It will,
                // of course, be garbage collected in due time.
                int chars = reader.read(authData, 0, len);
                parseAuthConfigFromData(authData, chars, config);
            } catch (IOException e) {
                throw new InvalidConfigurationException(e);
            } finally {
                if (authData != null) {
                    Arrays.fill(authData, '\0');
                }
            }
        }
    }

    static void parseAuthConfigFromData(char[] data, int dataLen, AgentStartupConfiguration config) {
        int position = 0;
        while (position < dataLen) {
            if ((position + 1 == dataLen) || data[position + 1] == newLine.charAt(0)) {
                // Empty line
                position = nextLine(data, position);
                continue;
            }
            if (data[position] == comment) {
                // Comment
                position = nextLine(data, position);
                continue;
            }
            char[] value = getPassword(data, position);
            if (value != null) {
                // Password
                config.setPassword(value);
                position = nextLine(data, position);
                value = null;
                continue;
            }
            value = getUserName(data, position);
            if (value != null) {
                // Username
                config.setUsername(new String(value));
                position = nextLine(data, position);
                value = null;
                continue;
            }
            throw new InvalidConfigurationException(t.localize(LocaleResources.BAD_AGENT_AUTH_CONTENTS));
        }
    }

    private static int nextLine(char[] data, int current) {
        int next = current + 1;
        while (next < data.length) {
            if (data[next] == newLine.charAt(0)) {
                break;
            }
            next += newLine.length();
        }
        next++;
        return next;
    }

    private static char[] getPassword(char[] data, int start) {
        return getValue(data, start, pw);
    }

    private static char[] getUserName(char[] data, int start) {
        return getValue(data, start, user);
    }

    private static char[] getValue(char[] data, int start, char[] key) {
        if (data[start + key.length] != '=') {
            return null;
        }
        for (int i = 0; i < key.length; i++) {
            if (key[i] != data[start + i]) {
                return null;
            }
        }
        int end = positionOf(newLine.charAt(0), data, start, data.length);
        char[] value = Arrays.copyOfRange(data, start + key.length + 1, end);
        return value;
    }

    private static int positionOf(char character, char[] data, int start, int end) {
        int position = -1;
        for (int possible = start; possible < data.length && possible <= end; possible++) {
            if (data[possible] == character) {
                position = possible;
                break;
            }
        }
        return position;
    }
}

