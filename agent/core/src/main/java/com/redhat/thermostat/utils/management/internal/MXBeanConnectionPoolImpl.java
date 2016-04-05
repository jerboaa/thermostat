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

package com.redhat.thermostat.utils.management.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.utils.ProcDataSource;
import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionException;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.agent.utils.username.UserNameUtil;
import com.redhat.thermostat.utils.management.internal.ProcessUserInfoBuilder.ProcessUserInfo;

public class MXBeanConnectionPoolImpl implements MXBeanConnectionPool, ThermostatIPCCallbacks {

    private static final Object CURRENT_ENTRY_LOCK = new Object();
    static final String IPC_SERVER_NAME = "agent-proxy";
    static final String JSON_PID = "pid";
    static final String JSON_JMX_URL = "jmxUrl";
    
    // pid -> (usageCount, actualObject)
    private final Map<Integer, MXBeanConnectionPoolEntry> pool;
    private final ConnectorCreator creator;
    private final File binPath;
    private final ProcessUserInfoBuilder userInfoBuilder;
    private final AgentIPCService ipcService;
    private final File ipcConfigFile;
    
    /**
     * Current {@link MXBeanConnectionPoolEntry} being created by {@link #acquire(int)} for use
     * by {@link #dataReceived(byte[])}. 
     * Since {@link #acquire(int)} is a synchronized method and blocks until
     * {@link #dataReceived(byte[])} is invoked, only one entry can be processed at a time.
     * Access/modification must be synchronized using {@link #CURRENT_ENTRY_LOCK}.
     */
    private MXBeanConnectionPoolEntry currentNewEntry;
    private boolean started;

    public MXBeanConnectionPoolImpl(File binPath, UserNameUtil userNameUtil, 
            AgentIPCService ipcService, File ipcConfigFile) {
        this(new ConnectorCreator(), binPath, new ProcessUserInfoBuilder(new ProcDataSource(), userNameUtil), 
                ipcService, ipcConfigFile);
    }

    MXBeanConnectionPoolImpl(ConnectorCreator connectorCreator, File binPath, ProcessUserInfoBuilder userInfoBuilder, 
            AgentIPCService ipcService, File ipcConfigFile) {
        this.pool = new HashMap<>();
        this.creator = connectorCreator;
        this.binPath = binPath;
        this.userInfoBuilder = userInfoBuilder;
        this.ipcService = ipcService;
        this.ipcConfigFile = ipcConfigFile;
        this.currentNewEntry = null;
        this.started = false;
    }

    @Override
    public void start() throws IOException {
        // Create IPC server for agent proxies
        startIPCServer();
        this.started = true;
    }
    
    @Override
    public boolean isStarted() {
        return started;
    }
    
    private void startIPCServer() throws IOException {
        // IPC server may have been left behind
        deleteServerIfExists();
        ipcService.createServer(IPC_SERVER_NAME, this);
    }
    
    @Override
    public void shutdown() throws IOException {
        deleteServerIfExists();
        this.started = false;
    }

    private void deleteServerIfExists() throws IOException {
        if (ipcService.serverExists(IPC_SERVER_NAME)) {
            ipcService.destroyServer(IPC_SERVER_NAME);
        }
    }
    
    @Override
    public byte[] dataReceived(byte[] data) {
        synchronized (CURRENT_ENTRY_LOCK) {
            MXBeanConnectionPoolEntry entry = currentNewEntry;
            Objects.requireNonNull(entry, "currentNewEntry was null, should never happen");
            
            String dataString = new String(data, Charset.forName("UTF-8"));
            try {
                // Deserialize JSON data
                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();
                JsonParser parser = new JsonParser();
                
                // Get root of JsonObject tree
                JsonElement parsed = parser.parse(dataString);
                requireNonNull(parsed, "Received empty JSON data");
                if (!parsed.isJsonObject()) {
                    throw new IOException("Malformed data from agent proxy");
                }
                JsonObject jsonObj = parsed.getAsJsonObject();
                
                int pid = getPidFromJson(gson, jsonObj);
                // Verify PID is correct
                if (entry.getPid() != pid) {
                    throw new IOException("Expected message for PID: " + currentNewEntry.getPid() 
                        + ", got message for PID: " + pid);
                }

                String jmxUrl = getJmxUrlFromJson(gson, jsonObj, pid);
                entry.setJmxUrl(jmxUrl);
            } catch (JsonParseException | IOException e) {
                entry.setException(e);
            }
            // No response needed
            return null;
        }
    }
    
    private int getPidFromJson(Gson gson, JsonObject json) throws IOException {
        JsonElement jsonPid = json.get(JSON_PID);
        requireNonNull(jsonPid, "No PID received from agent proxy");
        return gson.fromJson(jsonPid, Integer.class);
    }
    
    private String getJmxUrlFromJson(Gson gson, JsonObject json, int pid) throws IOException {
        JsonElement jsonJmxUrl = json.get(JSON_JMX_URL);
        requireNonNull(jsonJmxUrl, "No JMX service URL received from agent proxy for PID: " + pid);
        return gson.fromJson(jsonJmxUrl, String.class);
    }
    
    private void requireNonNull(JsonElement element, String errorMessage) throws IOException {
        if (element == null || element.isJsonNull()) {
            throw new IOException(errorMessage);
        }
    }
    
    @Override
    public synchronized MXBeanConnection acquire(int pid) throws MXBeanConnectionException {
        MXBeanConnectionPoolEntry data = pool.get(pid);
        if (data == null) {
            MXBeanConnector connector = null;
            ProcessUserInfo info = userInfoBuilder.build(pid);
            String username = info.getUsername();
            if (username == null) {
                throw new MXBeanConnectionException("Unable to determine owner of " + pid);
            }
            try {
                data = new MXBeanConnectionPoolEntry(pid);
                // Synchronized to ensure any previous callback has completely finished 
                // before changing currentNewEntry
                synchronized (CURRENT_ENTRY_LOCK) {
                    this.currentNewEntry = data;
                }
                // Add this to the map early, so our callback can find it
                pool.put(pid, data);
                
                // Start agent proxy which will send the JMX service URL to the IPC server we created
                AgentProxyClient proxy = creator.createAgentProxy(pid, username, binPath, ipcConfigFile);
                proxy.runProcess(); // Process completed when this returns
                
                // Block until we get a JMX service URL, or Exception
                String jmxUrl = data.getJmxUrlOrBlock();
                connector = creator.createConnector(jmxUrl);
                MXBeanConnectionImpl connection = connector.connect();
                data.setConnection(connection);
            } catch (IOException e) {
                pool.remove(pid);
                throw new MXBeanConnectionException(e);
            } catch (InterruptedException e) {
                pool.remove(pid);
                Thread.currentThread().interrupt();
                throw new MXBeanConnectionException(e);
            } finally {
                // Reset currentNewEntry
                synchronized (CURRENT_ENTRY_LOCK) {
                    this.currentNewEntry = null;
                }
            }
        } else {
            data.incrementUsageCount();
        }
        return data.getConnection();
    }

    @Override
    public synchronized void release(int pid, MXBeanConnection toRelease) throws MXBeanConnectionException {
        MXBeanConnectionPoolEntry data = pool.get(pid);
        if (data == null) {
            throw new MXBeanConnectionException("Unknown pid: " + pid);
        }
        MXBeanConnectionImpl connection = data.getConnection();
        if (connection == null) {
            throw new MXBeanConnectionException("No known open connection for pid: " + pid);
        } else if (connection != toRelease) {
            throw new MXBeanConnectionException("Connection mismatch for pid: " + pid);
        }
        
        data.decrementUsageCount();
        int usageCount = data.getUsageCount();
        if (usageCount == 0) {
            try {
                connection.close();
            } catch (IOException e) {
                throw new MXBeanConnectionException(e);
            }
            pool.remove(pid);
        }
    }
    
    static class ConnectorCreator {
        AgentProxyClient createAgentProxy(int pid, String user, File binPath, File ipcConfigFile) {
            return new AgentProxyClient(pid, user, binPath, ipcConfigFile);
        }
        
        MXBeanConnector createConnector(String jmxUrl) throws IOException {
            MXBeanConnector connector = new MXBeanConnector(jmxUrl);
            return connector;
        }
    }
    
    // For testing purposes
    MXBeanConnectionPoolEntry getPoolEntry(int pid) {
        return pool.get(pid);
    }
    
}

