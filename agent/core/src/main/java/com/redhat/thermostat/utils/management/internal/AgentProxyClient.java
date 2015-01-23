/*
 * Copyright 2012-2015 Red Hat, Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.common.tools.ApplicationException;
import com.redhat.thermostat.common.utils.LoggingUtils;

class AgentProxyClient {
    
    private static final String SERVER_NAME = "thermostat-agent-proxy";
    private static final Logger logger = LoggingUtils.getLogger(AgentProxyClient.class);
    
    private final int pid;
    private final ProcessCreator procCreator;
    private final File binPath;
    private final String username;
    
    AgentProxyClient(int pid, String user, File binPath) {
        this(pid, user, binPath, new ProcessCreator());
    }

    AgentProxyClient(int pid, String user, File binPath, ProcessCreator procCreator) {
        this.pid = pid;
        this.binPath = binPath;
        this.procCreator = procCreator;
        this.username = user;
    }

    String getJMXServiceURL() throws IOException, ApplicationException {
        // Start the agent proxy
        Process proxy = null;
        Thread errReaderThread = null;
        try {
            proxy = startProcess();

            final InputStream errStream = proxy.getErrorStream();

            // Log stderr in a separate thread
            errReaderThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    BufferedReader errReader = new BufferedReader(new InputStreamReader(errStream));
                    String line;
                    try {
                        while ((line = errReader.readLine()) != null 
                                && !Thread.currentThread().isInterrupted()) {
                            logger.info(line);
                        }
                        errReader.close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Failed to read error stream", e);
                    }
                }
            });
            errReaderThread.start();

            // Get JMX service URL from stdout
            BufferedReader outReader = new BufferedReader(new InputStreamReader(proxy.getInputStream()));
            String url = outReader.readLine();

            // Wait for process to terminate
            try {
                proxy.waitFor();
            } catch (InterruptedException e) {
                errReaderThread.interrupt();
                Thread.currentThread().interrupt();
            }
            outReader.close();
            if (url == null) {
                throw new IOException("Failed to determine JMX service URL from proxy process");
            }

            return url;
        } finally {
            if (proxy != null) {
                proxy.destroy();
            }
            if (errReaderThread != null) {
                try {
                    errReaderThread.join();
                } catch (InterruptedException e) {
                    errReaderThread.interrupt();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private Process startProcess() throws IOException, ApplicationException {
        String serverPath = binPath + File.separator + SERVER_NAME;
        return procCreator.createAndRunProcess(new String[] { serverPath, String.valueOf(pid), username });
    }
    
    static class ProcessCreator {
        Process createAndRunProcess(String[] args) throws IOException, ApplicationException {
            ProcessBuilder process = new ProcessBuilder(args);
            return process.start();
        }
    }
    
}

