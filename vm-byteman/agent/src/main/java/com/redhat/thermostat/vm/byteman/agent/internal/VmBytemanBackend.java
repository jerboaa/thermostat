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
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.shared.config.CommonPaths;
import com.redhat.thermostat.storage.core.VmId;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.byteman.common.VmBytemanDAO;

@Component
@Service(value = Backend.class)
public class VmBytemanBackend implements VmStatusListener, Backend {

    private static final String NAME = "VM Byteman backend (attacher)";
    private static final String DESCRIPTION = "Attaches the byteman java agent to JVMs";
    private static final String VENDOR = "Red Hat Inc.";
    private static final Logger logger = LoggingUtils.getLogger(VmBytemanBackend.class);
    
    static final int BACKEND_ORDER_VALUE = ORDER_CODE_GROUP + 3;
    private final BytemanAgentAttachManager attachManager;
    private boolean started;
    private boolean observeNewJVMs;
    private IPCEndpointsManager ipcEndpointsManager;
    private VmStatusListenerRegistrar registrar;
    private Version version;
    
    public VmBytemanBackend() {
        this(new BytemanAgentAttachManager(), false);
        // Default public constructor for DS
        this.observeNewJVMs = true;
    }
    
    // Package private for testing
    VmBytemanBackend(BytemanAgentAttachManager attachManager, boolean started) {
        this.started = started;
        this.attachManager = attachManager;
    }

    // Services
    
    @Reference
    private VmBytemanDAO dao;
    
    @Reference
    private CommonPaths paths;
    
    @Reference
    private AgentIPCService ipcService;
    
    @Reference
    private WriterID writerId;
    
    ////////////////////////////////////////////////
    // methods used by DS
    ////////////////////////////////////////////////
    
    protected void bindPaths(CommonPaths paths) {
        this.paths = paths;
        BytemanAttacher attacher = new BytemanAttacher(paths);
        this.attachManager.setAttacher(attacher);
        this.attachManager.setPaths(paths);
    }
    
    protected void unBindPaths(CommonPaths paths) {
        this.paths = null;
        this.attachManager.setPaths(null);
    }
    
    protected void bindWriterId(WriterID writerId) {
        this.writerId = writerId;
        this.attachManager.setWriterId(writerId);
    }
    
    protected void bindDao(VmBytemanDAO dao) {
        this.dao = dao;
        this.attachManager.setVmBytemanDao(dao);
    }
    
    protected void bindIpcService(AgentIPCService service) {
        this.ipcService = service;
        this.ipcEndpointsManager = new IPCEndpointsManager(service);
        attachManager.setIpcManager(ipcEndpointsManager);
    }
    
    protected void activate(ComponentContext context) {
        BundleContext ctx = context.getBundleContext();
        Bundle thisBundle = ctx.getBundle();
        version = new Version(thisBundle);
        registrar = new VmStatusListenerRegistrar(ctx);
    }
    
    protected void deactivate(ComponentContext context) {
        // nothing
    }
    
    ////////////////////////////////////////////////
    // end methods used by DS
    ////////////////////////////////////////////////

    @Override
    public int getOrderValue() {
        return ORDER_CODE_GROUP + 3;
    }

    @Override
    public boolean activate() {
        if (!started) {
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
            ipcEndpointsManager.stopIPCEndpoint(new VmSocketIdentifier(vmId, pid, writerId.getWriterID()));
            break;
        }
        
    }

    // Package-private for testing
    synchronized void attachBytemanToVm(String vmId, int pid) {
        if (!started) {
            logger.fine(getName() +" not active. Thus not attaching Byteman agent to VM '" + pid + "'");
            return;
        }
        attachManager.attachBytemanToVm(new VmId(vmId), pid);
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
