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

package com.redhat.thermostat.agent.ipc.unixsocket.common.internal;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.redhat.thermostat.agent.ipc.common.internal.IPCProperties;
import com.redhat.thermostat.agent.ipc.common.internal.IPCType;

public class UnixSocketIPCProperties extends IPCProperties {
    
    // Filename prefix for socket file
    static final String SOCKET_PREFIX = "sock-";
    static final String PROP_UNIX_SOCKET_DIR = "unixsocket.dir";
    private static final String SOCKET_DIR_NAME = "thermostat-socks";
    
    private final File sockDir;
    private final PathUtils pathUtils;
    
    UnixSocketIPCProperties(Properties props, File propFile) throws IOException {
        this(props, propFile, new PathUtils());
    }

    UnixSocketIPCProperties(Properties props, File propFile, PathUtils pathUtils) throws IOException {
        super(IPCType.UNIX_SOCKET, propFile);
        this.pathUtils = pathUtils;
        
        // If not specified, use default socket directory
        String sockDirPath = props.getProperty(PROP_UNIX_SOCKET_DIR);
        if (sockDirPath != null) {
            this.sockDir = new File(sockDirPath);
        } else {
            this.sockDir = getDefaultSocketDir();
        }
    }
    
    public File getSocketDirectory() {
        return sockDir;
    }
    
    public File getSocketFile(String serverName, String ownerName) {
        File ownerDir = new File(sockDir, ownerName);
        String socketFilename = SOCKET_PREFIX.concat(serverName);
        return new File(ownerDir, socketFilename);
    }
    
    /*
     * Default socket directory is calculated using the first available from the following:
     * 1. Environment variable "$XDG_RUNTIME_DIR" (e.g. /run/user/1000/)
     * 2. System property "java.io.tmpdir", with a subdirectory named from
     *    the value of system property "user.name" (e.g. /tmp/alice/)
     */
    private File getDefaultSocketDir() throws IOException {
        File result;
        // First check XDG_RUNTIME_DIR
        String path = pathUtils.getEnvironmentVariable("XDG_RUNTIME_DIR");
        if (path != null) {
            result = new File(path);
        } else {
            // Fall back to java.io.tmpdir
            path = pathUtils.getSystemProperty("java.io.tmpdir");
            if (path == null) {
                throw new IOException("Failed to build default socket directory");
            }
            String username = pathUtils.getSystemProperty("user.name");
            if (username == null) {
                throw new IOException("Unable to build socket directory path without username");
            }
            result = new File(path, username);
        }
        
        // Append our socket directory name
        result = new File(result, SOCKET_DIR_NAME);
        return result;
    }

    // Helper class for testing purposes
    static class PathUtils {
        String getSystemProperty(String name) {
            return System.getProperty(name);
        }
        String getEnvironmentVariable(String name) {
            return System.getenv(name);
        }
    }
}
