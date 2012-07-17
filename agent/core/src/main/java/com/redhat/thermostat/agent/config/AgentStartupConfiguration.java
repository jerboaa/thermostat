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

package com.redhat.thermostat.agent.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.redhat.thermostat.backend.BackendID;
import com.redhat.thermostat.backend.BackendsProperties;
import com.redhat.thermostat.common.cli.AuthenticationConfiguration;
import com.redhat.thermostat.common.config.ConfigUtils;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.config.StartupConfiguration;

public class AgentStartupConfiguration implements StartupConfiguration, AuthenticationConfiguration {

    private List<BackendID> backends;
    
    private boolean debugConsole;
    private boolean purge;
    
    private String url;
    private String username;
    private String password;

    private long startTime;
    
    AgentStartupConfiguration() {
        this.backends = new ArrayList<>();
    }
    
    @Override
    public String getDBConnectionString() {
        return url;
    }

    void parseBackends(String[] backendsList) throws InvalidConfigurationException {
        backends.clear();
        
        for (String simpleName : backendsList) {
            String backendName = simpleName.trim();
            
            // a file must exist, at least with the class name
            File backendSettings = ConfigUtils.getBackendPropertyFile(backendName);
            if (!backendSettings.exists())
                throw new InvalidConfigurationException("backends configuration " +
                                                        "directory doesn't exist: " +
                                                        backendSettings);
            Properties backendProps = new Properties();
            try {
                backendProps.load(new FileInputStream(backendSettings));
                
            } catch (IOException e) {
                throw new InvalidConfigurationException(e);
            }
            
            String backendClass = backendProps.getProperty(BackendsProperties.BACKEND_CLASS.name());
            if (backendClass == null) {
                throw new InvalidConfigurationException("Class name not found for backend: " +
                                                        backendName);
            }
            
            BackendID backend = new BackendID(backendName, backendClass);
            backends.add(backend);
        }
    }
    
    public List<BackendID> getBackends() {
        return backends;
    }

    void setDebugConsole(boolean debugConsole) {
        this.debugConsole = debugConsole;
    }
    
    public boolean isDebugConsole() {
        return debugConsole;
    }

    public void setDatabaseURL(String url) {
        this.url = url;
    }
    
    // TODO: that should be a friend, we only want the Service to set this value
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public long getStartTime() {
        return startTime;
    }

    void setPurge(boolean purge) {
        this.purge = purge;
    }
    
    public boolean purge() {
        return purge;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
