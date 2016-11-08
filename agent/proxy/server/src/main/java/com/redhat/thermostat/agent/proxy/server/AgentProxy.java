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

package com.redhat.thermostat.agent.proxy.server;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.redhat.thermostat.agent.ipc.client.ClientIPCService;
import com.redhat.thermostat.agent.ipc.client.ClientIPCServiceFactory;
import com.redhat.thermostat.agent.ipc.client.IPCMessageChannel;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.sun.tools.attach.AttachNotSupportedException;

public class AgentProxy {
    
    private static final Logger logger = LoggingUtils.getLogger(AgentProxy.class);
    static final String CONFIG_FILE_PROP = "ipcConfigFile";
    static final String JSON_PID = "pid";
    static final String JSON_JMX_URL = "jmxUrl";
    
    private static ControlCreator creator = new ControlCreator();
    private static ClientIPCService ipcService = null;
    
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            usage();
        }
        
        // Get IPC configuration file location from system property
        String configFileStr = System.getProperty(CONFIG_FILE_PROP);
        if (configFileStr == null) {
            throw new IOException("Unknown IPC configuration file location");
        }
        File configFile = new File(configFileStr);
        if (ipcService == null) { // Only non-null for testing
            ipcService = ClientIPCServiceFactory.getIPCService(configFile);
        }
        
        int pid = -1;
        try {
            // First argument is pid of target VM
            pid = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            usage();
        }
        String ipcServerName = args[1];
        // Connect to IPC server
        IPCMessageChannel channel = ipcService.connectToServer(ipcServerName);
        
        // Start proxy agent
        AgentProxyControlImpl agent = creator.create(pid);
        
        try {
            attachToTarget(pid, agent);
            String connectorAddress = getJMXServiceURL(agent);
            sendConnectionInfo(channel, pid, connectorAddress);
        } finally {
            cleanup(agent, channel, pid);
        }
    }

    private static void attachToTarget(int pid, AgentProxyControlImpl agent) throws IOException {
        try {
            agent.attach();
        } catch (AttachNotSupportedException | IOException e) {
            throw new IOException("Failed to attach to VM (pid: " + pid + ")", e);
        }
    }
    
    private static String getJMXServiceURL(AgentProxyControlImpl agent) throws IOException {
        try {
            return agent.getConnectorAddress();
        } catch (IOException e) {
            throw new IOException("Failed to retrieve JMX connection URL", e);
        }
    }

    private static void sendConnectionInfo(IPCMessageChannel channel, int pid, String connectorAddress) throws IOException {
        try {
            // As JSON, write pid first, followed by JMX service URL
            GsonBuilder builder = new GsonBuilder();
            Gson gson = builder.create();
            JsonObject data = new JsonObject();
            data.addProperty(JSON_PID, pid);
            data.addProperty(JSON_JMX_URL, connectorAddress);
            
            String jsonData = gson.toJson(data);
            ByteBuffer buf = ByteBuffer.wrap(jsonData.getBytes("UTF-8"));
            channel.writeMessage(buf);
        } catch (IOException e) {
            throw new IOException("Failed to send JMX connection information to agent", e);
        }
    }

    private static void cleanup(AgentProxyControlImpl agent, IPCMessageChannel channel, int pid) {
        try {
            channel.close();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to close channel with agent for VM (pid: " + pid + ")", e);
        }

        if (agent.isAttached()) {
            try {
                agent.detach();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to detach from VM (pid: " + pid + ")", e);
            }
        }
    }

    private static void usage() {
        throw new RuntimeException("usage: java " + AgentProxy.class.getName() + " <pidOfTargetJvm> <userNameOfJvmOwner>");
    }
    
    static class ControlCreator {
        AgentProxyControlImpl create(int pid) {
            return new AgentProxyControlImpl(pid);
        }
    }
    
    /*
     * For testing purposes only.
     */
    static void setControlCreator(ControlCreator creator) {
        AgentProxy.creator = creator;
    }
    
    /*
     * For testing purposes only.
     */
    static void setIPCService(ClientIPCService ipcService) {
        AgentProxy.ipcService = ipcService;
    }
    
}

