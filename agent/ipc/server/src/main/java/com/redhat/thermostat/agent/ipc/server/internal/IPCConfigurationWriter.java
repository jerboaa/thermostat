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

package com.redhat.thermostat.agent.ipc.server.internal;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import com.redhat.thermostat.agent.ipc.common.internal.IPCType;

class IPCConfigurationWriter {

    static final String PROP_IPC_TYPE = "type";
    static final String PROP_UNIX_SOCKET_DIR = "unixsocket.dir";
    private static final String COMMENTS = "Configuration for Inter-process Communication (IPC) used in the Thermostat agent.\n"
    + "The agent is configured to use Unix sockets for IPC by default.\n"
    + "The options below can be set to modify the defaults used by the agent:\n\n" 
    + "Directory where Unix sockets are created, which may be deleted if it already exists.\n"
    + PROP_UNIX_SOCKET_DIR + "=/path/to/unix/sockets\n";
    
    private final File configFile;
    private final PropertiesHelper helper;
    
    IPCConfigurationWriter(File configFile) {
        this(configFile, new PropertiesHelper());
    }
    
    IPCConfigurationWriter(File configFile, PropertiesHelper helper) {
        this.configFile = configFile;
        this.helper = helper;
    }
    
    void write() throws IOException {
        // Write defaults to config file
        configFile.createNewFile();
        
        Properties props = helper.createProperties();
        // Leave remainder of properties as defaults
        props.setProperty(PROP_IPC_TYPE, IPCType.UNIX_SOCKET.getConfigValue());
        
        try (FileOutputStream fos = helper.createStream(configFile)) {
            props.store(fos, COMMENTS);
        }
    }
    
    // For testing purposes
    static class PropertiesHelper {
        FileOutputStream createStream(File configFile) throws IOException {
            return new FileOutputStream(configFile);
        }
        Properties createProperties() {
            return new Properties();
        }
    }

}
