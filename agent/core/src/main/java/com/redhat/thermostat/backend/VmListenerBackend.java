/*
 * Copyright 2012, 2013 Red Hat, Inc.
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

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;
import sun.jvmstat.monitor.event.VmListener;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.utils.LoggingUtils;

/**
 * This class is a convenient subclass of {@link Backend} for those that need to
 * attach {@link VmListener} in response to starting and stopping of JVMs on a
 * host.
 * 
 * @see VmStatusListener
 * @see Backend
 */
public abstract class VmListenerBackend extends Backend implements VmStatusListener {
    
    private static final Logger logger = LoggingUtils.getLogger(VmListenerBackend.class);

    private MonitoredHost host;
    private Map<Integer, Pair<MonitoredVm, ? extends VmListener>> pidToData = new HashMap<>();
    private final VmStatusListenerRegistrar registrar;
    private boolean started;

    public VmListenerBackend(BackendID id, VmStatusListenerRegistrar registrar) {
        super(id);
        this.registrar = registrar;
        
        try {
            HostIdentifier hostId = new HostIdentifier((String) null);
            host = MonitoredHost.getMonitoredHost(hostId);
        } catch (MonitorException me) {
            logger.log(Level.WARNING, "Problems with connecting jvmstat to local machine", me);
        } catch (URISyntaxException use) {
            logger.log(Level.WARNING, "Failed to create host identifier", use);
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>
     * Registers a VmListener to begin receiving VM lifecycle events.
     * Subclasses should call <code>super.activate()</code> when overriding this method.
     * </p>
     */
    @Override
    public boolean activate() {
        if (!started && host != null) {
            registrar.register(this);
            started = true;
        }
        return started;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Unregisters the VmListener to stop receiving VM lifecycle events.
     * Subclasses should call <code>super.deactivate()</code> when overriding this method.
     * </p>
     */
    @Override
    public boolean deactivate() {
        if (started) {
            registrar.unregister(this);
            removeVmListeners();
            started = false;
        }
        return !started;
    }
    
    @Override
    public boolean isActive() {
        return started;
    }

    public void vmStatusChanged(Status newStatus, int pid) {
        switch (newStatus) {
        case VM_STARTED:
            /* fall-through */
        case VM_ACTIVE:
            handleNewVm(pid);
            break;
        case VM_STOPPED:
            handleStoppedVm(pid);
            break;
        default:
            break;
        }
    }

    private void handleNewVm(int pid) {
        if (attachToNewProcessByDefault()) {
            try {
                MonitoredVm vm = host.getMonitoredVm(host.getHostIdentifier().resolve(new VmIdentifier(String.valueOf(pid))));
                VmListener listener = createVmListener(pid);
                vm.addVmListener(listener);
    
                pidToData.put(pid, new Pair<>(vm, listener));
                logger.finer("Attached VmListener for VM: " + pid);
            } catch (MonitorException | URISyntaxException e) {
                logger.log(Level.WARNING, "unable to attach to vm " + pid, e);
            }
        } else {
            logger.log(Level.FINE, "skipping new vm " + pid);
        }
    }

    private void handleStoppedVm(int pid) {
        Pair<MonitoredVm, ? extends VmListener> data = pidToData.remove(pid);
        // we were not monitoring pid at all, so nothing to do
        if (data == null) {
            return;
        }
    
        MonitoredVm vm = data.getFirst();
        VmListener listener = data.getSecond();
        try {
            vm.removeVmListener(listener);
        } catch (MonitorException e) {
            logger.log(Level.WARNING, "can't remove vm listener", e);
        }
        vm.detach();
    }

    private void removeVmListeners() {
        for (Pair<MonitoredVm, ? extends VmListener> data : pidToData.values()) {
            MonitoredVm vm = data.getFirst();
            VmListener listener = data.getSecond();
            try {
                vm.removeVmListener(listener);
            } catch (MonitorException e) {
                logger.log(Level.WARNING, "can't remove vm listener", e);
            }
        }
    }
    
    /**
     * Creates a new {@link VmListener} for the virtual machine
     * specified by the pid. This method is called when a new
     * JVM is started or for JVMs already active when this Backend
     * was activated.
     * @param pid the process ID of the JVM
     * @return a new VmListener for the VM specified by pid
     */
    protected abstract VmListener createVmListener(int pid);
    
    /*
     * For testing purposes only.
     */
    void setHost(MonitoredHost host) {
        this.host = host;
    }
    
    /*
     * For testing purposes only.
     */
    Map<Integer, Pair<MonitoredVm, ? extends VmListener>> getPidToDataMap() {
        return pidToData;
    }

}