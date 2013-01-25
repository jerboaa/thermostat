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
import java.util.Properties;

import com.redhat.thermostat.common.config.Configuration;
import com.redhat.thermostat.common.config.InvalidConfigurationException;

public class AgentConfigsUtils {

    public static AgentStartupConfiguration createAgentConfigs() throws InvalidConfigurationException {
        
        AgentStartupConfiguration config = new AgentStartupConfiguration();
        
        File propertyFile = new Configuration().getAgentConfigurationFile();
        readAndSetProperties(propertyFile, config);
        
        return config;
    }
    
    private static void readAndSetProperties(File propertyFile, AgentStartupConfiguration configuration)
            throws InvalidConfigurationException
    {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(propertyFile));
            
        } catch (IOException e) {
            throw new InvalidConfigurationException(e);
        }
        
        if (properties.containsKey(AgentProperties.DB_URL.name())) {
            String db = properties.getProperty(AgentProperties.DB_URL.name());
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
    
}

