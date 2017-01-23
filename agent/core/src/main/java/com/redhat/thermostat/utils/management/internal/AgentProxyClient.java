/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.utils.management.internal;

import java.io.File;
import java.io.IOException;

import com.redhat.thermostat.common.ExitStatus;
import com.redhat.thermostat.shared.config.OS;

class AgentProxyClient {
    
    private static final String SERVER_NAME = "thermostat-agent-proxy";
    
    private final int pid;
    private final ProcessCreator procCreator;
    private final File binPath;
    private final String username;
    private final File ipcConfigFile;
    private final String serverName;
    
    AgentProxyClient(int pid, String user, File binPath, File ipcConfigFile, String serverName) {
        this(pid, user, binPath, ipcConfigFile, serverName, new ProcessCreator());
    }
    
    AgentProxyClient(int pid, String user, File binPath, File ipcConfigFile, String serverName, ProcessCreator procCreator) {
        this.pid = pid;
        this.binPath = binPath;
        this.procCreator = procCreator;
        this.username = user;
        this.ipcConfigFile = ipcConfigFile;
        this.serverName = serverName;
    }

    void runProcess() throws IOException, InterruptedException {
        // Start the agent proxy
        String serverPath = binPath + File.separator + SERVER_NAME;
        String[] args = OS.IS_UNIX
                ? new String[] { serverPath, String.valueOf(pid), username, ipcConfigFile.getAbsolutePath(), serverName }
                : new String[] { "cmd", "/C", serverPath+".cmd", String.valueOf(pid), username, ipcConfigFile.getAbsolutePath(), serverName };
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.inheritIO();
        Process proxy = procCreator.startProcess(builder);
        
        try {
            // Wait for process to terminate
            proxy.waitFor();
            if (proxy.exitValue() != ExitStatus.EXIT_SUCCESS) {
                throw new IOException("Agent proxy for " + pid + " exited with non-zero exit code");
            }
        } finally {
            proxy.destroy();
        }
    }
    
    static class ProcessCreator {
        Process startProcess(ProcessBuilder builder) throws IOException {
            return builder.start();
        }
    }

}

