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

package com.redhat.thermostat.dev.ipc.test.server.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.server.IPCMessage;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.shared.config.CommonPaths;

class UnixSocketTestServer {
    
    // Filename prefix for socket file
    private static final String SOCKET_NAME = "test";
    // Executable to spawn client processes
    private static final String SERVER_BIN = "client.sh";
    // Number of server threads to create
    private static final int NUM_SERVERS = 10;
    // Number of client processes to create
    private static final int NUM_CLIENTS = 20;
    
    private final AgentIPCService ipcService;
    private final CommonPaths paths;
    // Global list of running client processes by all server threads
    private final List<Process> processes;
    
    UnixSocketTestServer(AgentIPCService ipcService, CommonPaths paths) {
        this.ipcService = ipcService;
        this.paths = paths;
        this.processes = Collections.synchronizedList(new ArrayList<Process>());
    }
    
    void start() throws IOException {
        try {
            // Create servers
            for (int j = 0; j < NUM_SERVERS; j++) {
                String serverName = SOCKET_NAME + String.valueOf(j);
                createServer(serverName);
            }

            // Create clients
            for (int j = 0; j < NUM_CLIENTS; j++) {
                runClient();
            }

            // Wait on all clients to finish
            waitForClients();
        } finally {
            cleanup();
        }
    }

    private void runClient() throws IOException {
        List<String> cmdLine = new ArrayList<>();
        cmdLine.add("/bin/bash");
        cmdLine.add(SERVER_BIN);
        // Pass NUM_SERVERS as command line argument
        cmdLine.add(String.valueOf(NUM_SERVERS));
        cmdLine.add(paths.getUserIPCConfigurationFile().getAbsolutePath());
        
        ProcessBuilder builder = new ProcessBuilder(cmdLine);
        builder.inheritIO();
        
        Process process = builder.start();
        processes.add(process);
    }

    private void waitForClients() throws IOException {
        // Wait for all clients
        try {
            synchronized (processes) {
                List<Process> processesCopy = new ArrayList<>(processes);
                for (Process process : processesCopy) {
                    process.waitFor();
                    processes.remove(process);
                    int rc = process.exitValue();
                    if (rc != 0) {
                        throw new IOException("Client exited with non-zero exit code (" + rc + ")");
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void createServer(String serverName) throws IOException {
        final ThermostatIPCCallbacks callbacks = new ThermostatIPCCallbacks() {
            
            @Override
            public void messageReceived(IPCMessage message) {
                try {
                    ByteBuffer data = message.get();
                    CharBuffer charBuf = Charset.forName("UTF-8").decode(data);
                    String question = charBuf.toString();
                    System.out.print(question + " -> ");
                    DummyReceiver receiver = new DummyReceiver();
                    String answer = receiver.answer(question);
                    System.out.println(answer);
                    
                    ByteBuffer response = ByteBuffer.wrap(answer.getBytes(Charset.forName("UTF-8")));
                    message.reply(response);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        
        // Create server socket
        ipcService.createServer(serverName, callbacks);
    }
    
    void cleanup() throws IOException {
        synchronized (processes) {
            for (Process process : processes) {
                System.err.println("Destroying process");
                process.destroy();
            }
        }
        try {
            destroyServers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void destroyServers() throws IOException {
        for (int i = 0; i < NUM_SERVERS; i++) {
            String serverName = SOCKET_NAME + String.valueOf(i);
            if (ipcService.serverExists(serverName)) {
                ipcService.destroyServer(serverName);
            }
        }
    }
    
}
