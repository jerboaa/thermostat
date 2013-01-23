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

package com.redhat.thermostat.vm.classstat.agent.internal;

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
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendID;
import com.redhat.thermostat.backend.BackendsProperties;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;

public class VmClassStatBackend extends Backend implements VmStatusListener {

    private static final Logger LOGGER = LoggingUtils.getLogger(VmClassStatBackend.class);

    private final VmClassStatDAO vmClassStats;
    private final VmStatusListenerRegistrar registrar;

    private final Map<Integer, Pair<MonitoredVm, ? extends VmListener>> pidToVmAndListener = new HashMap<>();

    private MonitoredHost host;

    private boolean started;

    public VmClassStatBackend(VmClassStatDAO vmClassStatDAO, Version version, VmStatusListenerRegistrar registrar) {
        super(new BackendID("VM Classes Backend", VmClassStatBackend.class.getName()));
        this.vmClassStats = vmClassStatDAO;
        this.registrar = registrar;
        
        setConfigurationValue(BackendsProperties.VENDOR.name(), "Red Hat, Inc.");
        setConfigurationValue(BackendsProperties.DESCRIPTION.name(), "Gathers class loading statistics about a JVM");
        setConfigurationValue(BackendsProperties.VERSION.name(), version.getVersionNumber());
        
        try {
            HostIdentifier hostId = new HostIdentifier((String) null);
            host = MonitoredHost.getMonitoredHost(hostId);
        } catch (MonitorException me) {
            LOGGER.log(Level.WARNING, "Problems with connecting jvmstat to local machine", me);
        } catch (URISyntaxException use) {
            LOGGER.log(Level.WARNING, "Failed to create host identifier", use);
        }
    }

    /*
     * Methods from Backend
     */

    @Override
    public boolean activate() {
        if (!started && host != null) {
            registrar.register(this);
            started = true;
        }
        return started;
    }

    @Override
    public boolean deactivate() {
        if (started) {
            registrar.unregister(this);
            started = false;
        }
        return !started;
    }
    
    @Override
    public boolean isActive() {
        return started;
    }
    
    @Override
    public String getConfigurationValue(String key) {
        return null;
    }

    @Override
    public boolean attachToNewProcessByDefault() {
        return true;
    }

    @Override
    public int getOrderValue() {
        return ORDER_MEMORY_GROUP + 30;
    }

    /*
     *  Methods from VmStatusListener
     */

    @Override
    public void vmStatusChanged(Status newStatus, int pid) {
        switch (newStatus) {
        case VM_STARTED:
            /* fall-through */
        case VM_ACTIVE:
            vmStarted(pid);
            break;
        case VM_STOPPED:
            vmStopped(pid);
            break;
        }
    }

    private void vmStarted(int pid) {
        if (attachToNewProcessByDefault()) {
            try {
                MonitoredVm vm = host.getMonitoredVm(host.getHostIdentifier().resolve(new VmIdentifier(String.valueOf(pid))));
                VmClassStatVmListener listener = new VmClassStatVmListener(vmClassStats, pid);
                vm.addVmListener(listener);

                pidToVmAndListener.put(pid, new Pair<>(vm, listener));
                LOGGER.finer("Attached VmListener for VM: " + pid);
            } catch (MonitorException | URISyntaxException e) {
                LOGGER.log(Level.WARNING, "Could not attach to new vm " + pid, e);
            }
        } else {
            LOGGER.log(Level.FINE, "skipping new vm " + pid);
        }
    }

    private void vmStopped(Integer pid) {
        Pair<MonitoredVm, ? extends VmListener> data = pidToVmAndListener.remove(pid);
        // if there is no data, we must never have attached to the vm. Nothing to do.
        if (data == null) {
            return;
        }

        MonitoredVm vm = data.getFirst();
        VmListener listener = data.getSecond();
        try {
            vm.removeVmListener(listener);
        } catch (MonitorException e) {
            LOGGER.log(Level.WARNING, "can't remove vm listener", e);
        }
        vm.detach();
    }

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
        return pidToVmAndListener;
    }

}

