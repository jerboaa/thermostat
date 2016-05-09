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

package com.redhat.thermostat.vm.byteman.agent.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.agent.ipc.server.AgentIPCService;
import com.redhat.thermostat.agent.ipc.server.ThermostatIPCCallbacks;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;
import com.redhat.thermostat.vm.byteman.common.VmBytemanStatus;

@Component
@Service(value = Backend.class)
public class VmBytemanBackend implements VmStatusListener, Backend {

    private static final String NAME = "VM Byteman backend (attacher)";
    private static final String DESCRIPTION = "Attaches the byteman java agent to JVMs";
    private static final String VENDOR = "Red Hat Inc.";
    private static final String BYTEMAN_PLUGIN_DIR = System.getProperty("thermostat.plugin", "vm-byteman");
    private static final String BYTEMAN_INSTALL_HOME = BYTEMAN_PLUGIN_DIR + File.separator + "plugin-libs" + File.separator + "byteman-install";
    private static final String BYTEMAN_HOME_PROPERTY = "org.jboss.byteman.home";
    private static final Logger logger = LoggingUtils.getLogger(VmBytemanBackend.class);
    static final int BACKEND_ORDER_VALUE = ORDER_CODE_GROUP + 3;
    private final Set<String> sockets = Collections.synchronizedSet(new HashSet<String>());
    private final Map<String, BytemanAgentInfo> agentInfos = new ConcurrentHashMap<>();
    private boolean started;
    private boolean observeNewJVMs;
    private BytemanAttacher attacher;
    private VmStatusListenerRegistrar registrar;
    private Version version;

    // Services
    
    @Reference
    private VmBytemanDAO dao;
    
    @Reference
    private CommonPaths paths;
    
    @Reference
    private AgentIPCService ipcService;
    
    @Reference
    private WriterID writerId;
    
    public VmBytemanBackend() {
        // Default public constructor for DS
        this.observeNewJVMs = true;
    }
    
    protected void bindPaths(CommonPaths paths) {
        this.paths = paths;
        this.attacher = new BytemanAttacher(paths);
    }
    
    protected void unBindPaths(CommonPaths paths) {
        this.paths = null;
        this.attacher = null;
    }
    
    protected void activate(ComponentContext context) {
        BundleContext ctx = context.getBundleContext();
        Bundle thisBundle = ctx.getBundle();
        version = new Version(thisBundle);
        registrar = new VmStatusListenerRegistrar(ctx);
    }
    
    protected void deactivate(ComponentContext context) {
        // DS wants to use this method
    }

    @Override
    public int getOrderValue() {
        return ORDER_CODE_GROUP + 3;
    }

    @Override
    public boolean activate() {
        if (!started) {
            String bytemanHome = paths.getSystemPluginRoot().getAbsolutePath() + File.separator + BYTEMAN_INSTALL_HOME;
            // This will depend on BYTEMAN-303 being fixed and incorporated in a release
            logger.fine("Setting system property " + BYTEMAN_HOME_PROPERTY + "=" + bytemanHome);
            System.setProperty(BYTEMAN_HOME_PROPERTY, bytemanHome);
            registrar.register(this);
            started = true;
        }
        return started;
    }

    @Override
    public boolean deactivate() {
        started = false;
        return true;
    }

    @Override
    public boolean isActive() {
        return started;
    }

    @Override
    public void vmStatusChanged(Status newStatus, String vmId, int pid) {
        switch(newStatus) {
        case VM_ACTIVE:
            // fall-through
        case VM_STARTED:
            attachBytemanToVm(vmId, pid);
            break;
        case VM_STOPPED:
            // cannot unload byteman agent, thus cannot un-attach
            stopIPCEndPoint(new VmSocketIdentifier(vmId, pid, writerId.getWriterID()));
            break;
        }
        
    }

    private void stopIPCEndPoint(VmSocketIdentifier vmSocketIdentifier) {
        String socketId = vmSocketIdentifier.getName();
        synchronized(sockets) {
            if (sockets.contains(socketId)) {
                logger.fine("Destroying socket for id: " + socketId);
                try {
                    ipcService.destroyServer(socketId);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to destroy socket id: " + socketId, e);
                }
                sockets.remove(socketId);
            }
        }
    }

    private synchronized void attachBytemanToVm(String vmId, int pid) {
        if (!started) {
            logger.fine(getName() +" not active. Thus not attaching Byteman agent to VM '" + pid + "'");
            return;
        }
        logger.fine("Attaching byteman agent to VM '" + pid + "'");
        BytemanAgentInfo info = attacher.attach(vmId, pid, writerId.getWriterID());
        agentInfos.put(vmId, info);
        if (info == null) {
            logger.warning("Failed to attach byteman agent for VM '" + pid + "'. Skipping rule updater and IPC channel.");
            return;
        }
        logger.fine("Attached byteman agent to VM '" + pid + "' at port: '" + info.getAgentListenPort());
        logger.fine("Starting IPC socket for byteman helper");
        VmSocketIdentifier socketId = new VmSocketIdentifier(vmId, pid, writerId.getWriterID());
        final ThermostatIPCCallbacks callback = new BytemanMetricsReceiver(dao, socketId);
        startIPCEndpoint(socketId, callback);
        // Add a status record to storage
        VmBytemanStatus status = new VmBytemanStatus(writerId.getWriterID());
        status.setListenPort(info.getAgentListenPort());
        status.setTimeStamp(System.currentTimeMillis());
        status.setVmId(vmId);
        dao.addOrReplaceBytemanStatus(status);
    }

    private void startIPCEndpoint(VmSocketIdentifier identifier, ThermostatIPCCallbacks callback) {
        String socketId = identifier.getName();
        synchronized(sockets) {
            if (!sockets.contains(socketId)) {
                try {
                    if (ipcService.serverExists(socketId)) {
                        // We create the sockets in a way that's unique per agent/vmId/pid. If we have
                        // two such sockets there is a problem somewhere.
                        logger.warning("Socket with id: " + socketId + " already exists. Bug?");
                        return;
                    }
                    ipcService.createServer(socketId, callback);
                    sockets.add(socketId);
                    logger.fine("Created IPC endpoint for id: " + socketId);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Failed to start IPC entpoint for id: " + socketId);
                }
            }
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getVendor() {
        return VENDOR;
    }

    @Override
    public String getVersion() {
        return version.getVersionInfo();
    }

    @Override
    public boolean getObserveNewJvm() {
        return observeNewJVMs;
    }

    @Override
    public void setObserveNewJvm(boolean newValue) {
        observeNewJVMs = newValue;
    }
    
    @Override
    public String toString() {
        return "Backend [name=" + getName() + ", version=" + getVersion() + ", vendor=" + getVendor()
                + ", description=" + getDescription() + "]";
    }
}
