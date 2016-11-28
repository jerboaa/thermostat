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

package com.redhat.thermostat.backend;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.backend.internal.BackendException;
import com.redhat.thermostat.backend.internal.VmMonitor;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.storage.core.WriterID;

/**
 * This class is a convenient subclass of {@link Backend} (via {@link BaseBackend}) for those
 * that need to attach {@link VmUpdateListener} in response to starting and stopping of JVMs on a
 * host.
 * 
 * @see VmStatusListener
 * @see Backend
 * @see BaseBackend
 */
public abstract class VmListenerBackend extends BaseBackend implements VmStatusListener {
    
    private static final Logger logger = LoggingUtils.getLogger(VmListenerBackend.class);
    
    private final VmStatusListenerRegistrar registrar;
    private final WriterID writerId;
    private VmMonitor monitor;
    private boolean started;

    public VmListenerBackend(String backendName, String description,
            String vendor, String version, VmStatusListenerRegistrar registrar,
            WriterID writerId) {
        this(backendName, description, vendor, version, false, registrar, writerId);
    }
    public VmListenerBackend(String backendName, String description,
            String vendor, String version, boolean observeNewJvm,
            VmStatusListenerRegistrar registrar, WriterID writerId) {
        super(backendName, description, vendor, version, observeNewJvm);
        this.registrar = registrar;
        this.writerId = writerId;
        try {
            this.monitor = new VmMonitor();
        } catch (BackendException e) {
            logger.log(Level.SEVERE, "Unable to create backend", e);
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * Registers a VmUpdateListener to begin receiving VM lifecycle events.
     * Subclasses should call <code>super.activate()</code> when overriding this method.
     * </p>
     */
    @Override
    public boolean activate() {
        if (!started && monitor != null) {
            registrar.register(this);
            started = true;
        }
        return started;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Unregisters the VmUpdateListener to stop receiving VM lifecycle events.
     * Subclasses should call <code>super.deactivate()</code> when overriding this method.
     * </p>
     */
    @Override
    public boolean deactivate() {
        if (started && monitor != null) {
            registrar.unregister(this);
            monitor.removeVmListeners();
            started = false;
        }
        return !started;
    }
    
    @Override
    public boolean isActive() {
        return started;
    }

    public void vmStatusChanged(Status newStatus, String vmId, int pid) {
        switch (newStatus) {
        case VM_STARTED:
            /* fall-through */
        case VM_ACTIVE:
            if (getObserveNewJvm()) {
                String wId = writerId.getWriterID();
                VmUpdateListener listener = null;
                try {
                    listener = createVmListener(wId, vmId, pid);
                } catch (Throwable t) {
                    logger.log(Level.INFO, "Creating the VM listener for a VmListenerBackend threw an exception. Going to ignore the backend!", t);
                }
                if (listener != null) {
                    monitor.handleNewVm(listener, pid);
                }
            } else {
                logger.log(Level.FINE, "skipping new vm " + pid);
            }
            break;
        case VM_STOPPED:
            monitor.handleStoppedVm(pid);
            break;
        default:
            break;
        }
    }

    /**
     * Creates a new {@link VmUpdateListener} for the virtual machine
     * specified by the pid. This method is called when a new
     * JVM is started or for JVMs already active when this Backend
     * was activated.
     * @param vmId unique identifier of the JVM
     * @param pid the process ID of the JVM
     * @return a new listener for the VM specified by pid
     */
    protected abstract VmUpdateListener createVmListener(String writerId, String vmId, int pid);
    
    /*
     * For testing purposes only.
     */
    void setMonitor(VmMonitor monitor) {
        this.monitor = monitor;
    }
}

