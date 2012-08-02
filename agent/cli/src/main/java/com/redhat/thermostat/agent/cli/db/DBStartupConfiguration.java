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

package com.redhat.thermostat.agent.cli.db;

import java.io.File;

import com.redhat.thermostat.common.config.ConfigUtils;
import com.redhat.thermostat.common.config.InvalidConfigurationException;
import com.redhat.thermostat.common.config.StartupConfiguration;

public class DBStartupConfiguration implements StartupConfiguration {
    
    private File dbPath;
    private File logFile;
    private File pidFile;
        
    private long localPort;
    private long clusterPort;
    
    private String url;
    
    private String dbConnectionString;
    
    private String ip;
    
    private boolean local;
    
    public DBStartupConfiguration() throws InvalidConfigurationException {
        dbPath = ConfigUtils.getStorageDirectory();
        logFile = ConfigUtils.getStorageLogFile();
        pidFile = ConfigUtils.getStoragePidFile();
    }
    
    public File getDBPath() {
        return dbPath;
    }
    
    public File getLogFile() {
        return logFile;
    }
    
    public File getPidFile() {
        return pidFile;
    }
   
    public void setLocalPort(long localPort) {
        this.localPort = localPort;
    }
    
    public long getLocalPort() {
        return localPort;
    }
    
    public long getClusterPort() {
        return clusterPort;
    }
    
    public void setClusterPort(long clusterPort) {
        this.clusterPort = clusterPort;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getUrl() {
        return url;
    }
    
    void setDBConnectionString(String dbConnectionString) {
        this.dbConnectionString = dbConnectionString;
    }
    
    @Override
    public String getDBConnectionString() {
        return dbConnectionString;
    }

    public void setBindIP(String ip) {
        this.ip = ip;
    }
    
    public String getBindIP() {
        return ip;
    }
    
    void setLocal(boolean local) {
        this.local = local;
    }
    
    public boolean isLocal() {
        return local;
    }
}