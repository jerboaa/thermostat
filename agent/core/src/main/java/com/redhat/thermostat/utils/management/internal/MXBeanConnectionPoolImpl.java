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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.server.IPCMessage;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.agent.utils.ProcDataSource;
import com.redhat.thermostat.agent.utils.management.MXBeanConnection;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionException;
import com.redhat.thermostat.agent.utils.management.MXBeanConnectionPool;
import com.redhat.thermostat.agent.utils.username.UserNameUtil;
import com.redhat.thermostat.utils.management.internal.ProcessUserInfoBuilder.ProcessUserInfo;

public class MXBeanConnectionPoolImpl implements MXBeanConnectionPoolControl, ThermostatIPCCallbacks {

    private static final Object CURRENT_ENTRY_LOCK = new Object();
    private static final String IPC_SERVER_PREFIX = "agent-proxy";
    static final String JSON_PID = "pid";
    static final String JSON_JMX_URL = "jmxUrl";
    
    // pid -> (usageCount, actualObject)
    private final Map<Integer, MXBeanConnectionPoolEntry> pool;
    private final ConnectorCreator creator;
    private final File binPath;
    private final ProcessUserInfoBuilder userInfoBuilder;
    private final AgentIPCService ipcService;
    private final File ipcConfigFile;
    private final FileSystemUtils fsUtils;
    // Keep track of IPC servers we created
    private final Set<String> ipcServerNames;
    
    /**
     * Current {@link MXBeanConnectionPoolEntry} being created by {@link #acquire(int)} for use
     * by {@link #messageReceived(IPCMessage)}. 
     * Since {@link #acquire(int)} is a synchronized method and blocks until
     * {@link #messageReceived(IPCMessage)} is invoked, only one entry can be processed at a time.
     * Access/modification must be synchronized using {@link #CURRENT_ENTRY_LOCK}.
     */
    private MXBeanConnectionPoolEntry currentNewEntry;
    private boolean started;

    public MXBeanConnectionPoolImpl(File binPath, UserNameUtil userNameUtil, 
            AgentIPCService ipcService, File ipcConfigFile) {
        this(new ConnectorCreator(), binPath, new ProcessUserInfoBuilder(new ProcDataSource(), userNameUtil), 
                ipcService, ipcConfigFile, new FileSystemUtils());
    }

    MXBeanConnectionPoolImpl(ConnectorCreator connectorCreator, File binPath, ProcessUserInfoBuilder userInfoBuilder, 
            AgentIPCService ipcService, File ipcConfigFile, FileSystemUtils fsUtils) {
        this.pool = new HashMap<>();
        this.creator = connectorCreator;
        this.binPath = binPath;
        this.userInfoBuilder = userInfoBuilder;
        this.ipcService = ipcService;
        this.ipcConfigFile = ipcConfigFile;
        this.fsUtils = fsUtils;
        this.currentNewEntry = null;
        this.started = false;
        this.ipcServerNames = new HashSet<>();
    }

    @Override
    public synchronized void start() throws IOException {
        this.started = true;
    }
    
    @Override
    public synchronized boolean isStarted() {
        return started;
    }
    
    @Override
    public synchronized void shutdown() throws IOException {
        this.started = false;
        
        // Delete all IPC servers created by this class
        Set<String> serverNames = new HashSet<>(ipcServerNames);
        for (String serverName : serverNames) {
            deleteServerIfExists(serverName);
            ipcServerNames.remove(serverName);
        }
    }

    private void deleteServerIfExists(String serverName) throws IOException {
        if (ipcService.serverExists(serverName)) {
            ipcService.destroyServer(serverName);
        }
    }
    
    @Override
    public void messageReceived(IPCMessage message) {
        synchronized (CURRENT_ENTRY_LOCK) {
            MXBeanConnectionPoolEntry entry = currentNewEntry;
            Objects.requireNonNull(entry, "currentNewEntry was null, should never happen");
            
            ByteBuffer buf = message.get();
            CharBuffer charBuf = Charset.forName("UTF-8").decode(buf);
            String dataString = charBuf.toString();
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
        checkRunning();
        MXBeanConnectionPoolEntry data = pool.get(pid);
        if (data == null) {
            MXBeanConnector connector = null;
            ProcessUserInfo info = userInfoBuilder.build(pid);
            String username = info.getUsername();
            if (username == null) {
                throw new MXBeanConnectionException("Unable to determine owner of " + pid);
            }
            // Create an Agent Proxy IPC server for this user if it does not already exist
            String serverName = IPC_SERVER_PREFIX + "-" + String.valueOf(info.getUid());
            try {
                // Check if we created an IPC server for this user already
                if (!ipcServerNames.contains(serverName)) {
                    createIPCServer(username, serverName);
                }
                
                data = new MXBeanConnectionPoolEntry(pid);
                // Synchronized to ensure any previous callback has completely finished 
                // before changing currentNewEntry
                synchronized (CURRENT_ENTRY_LOCK) {
                    this.currentNewEntry = data;
                }
                // Add this to the map early, so our callback can find it
                pool.put(pid, data);
                
                // Start agent proxy which will send the JMX service URL to the IPC server we created
                AgentProxyClient proxy = creator.createAgentProxy(pid, username, binPath, ipcConfigFile, serverName);
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

    private void createIPCServer(String username, String serverName) throws IOException {
        // Lookup UserPrincipal using username
        UserPrincipalLookupService lookup = fsUtils.getUserPrincipalLookupService();
        UserPrincipal principal = lookup.lookupPrincipalByName(username);
        deleteServerIfExists(serverName); // Chance of old server left behind
        ipcService.createServer(serverName, this, principal);
        ipcServerNames.add(serverName);
    }

    private void checkRunning() throws MXBeanConnectionException {
        if (!started) {
            throw new MXBeanConnectionException(MXBeanConnectionPool.class.getSimpleName() + " service is not running");
        }
    }

    @Override
    public synchronized void release(int pid, MXBeanConnection toRelease) throws MXBeanConnectionException {
        checkRunning();
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
        AgentProxyClient createAgentProxy(int pid, String user, File binPath, File ipcConfigFile, String serverName) {
            return new AgentProxyClient(pid, user, binPath, ipcConfigFile, serverName);
        }
        
        MXBeanConnector createConnector(String jmxUrl) throws IOException {
            MXBeanConnector connector = new MXBeanConnector(jmxUrl);
            return connector;
        }
    }
    
    static class FileSystemUtils {
        UserPrincipalLookupService getUserPrincipalLookupService() {
            return FileSystems.getDefault().getUserPrincipalLookupService();
        }
    }
    
    // For testing purposes
    MXBeanConnectionPoolEntry getPoolEntry(int pid) {
        return pool.get(pid);
    }
    
    // For testing purposes
    synchronized Set<String> getIPCServerNames() {
        return ipcServerNames;
    }
    
}

