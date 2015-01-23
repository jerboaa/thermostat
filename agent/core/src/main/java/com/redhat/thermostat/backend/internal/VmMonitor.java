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

package com.redhat.thermostat.backend.internal;

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

import com.redhat.thermostat.backend.VmUpdateListener;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.utils.LoggingUtils;

public class VmMonitor {
    
    private final Logger logger = LoggingUtils.getLogger(VmMonitor.class);
    private MonitoredHost host;
    private Map<Integer, Pair<MonitoredVm, VmListenerWrapper>> pidToData = new HashMap<>();
    
    public VmMonitor() throws BackendException {
        try {
            HostIdentifier hostId = new HostIdentifier((String) null);
            host = MonitoredHost.getMonitoredHost(hostId);
        } catch (MonitorException me) {
            throw new BackendException("Problems with connecting jvmstat to local machine", me);
        } catch (URISyntaxException use) {
            throw new BackendException("Failed to create host identifier", use);
        }
    }
    
    public void handleNewVm(VmUpdateListener listener, int pid) {
        try {
            MonitoredVm vm = host.getMonitoredVm(host.getHostIdentifier().resolve(new VmIdentifier(String.valueOf(pid))));
            VmListenerWrapper wrapper = new VmListenerWrapper(listener, vm);
            vm.addVmListener(wrapper);

            pidToData.put(pid, new Pair<>(vm, wrapper));
            logger.finer("Attached " + listener.getClass().getName() + " for VM: " + pid);
        } catch (MonitorException | URISyntaxException e) {
            logger.log(Level.WARNING, "unable to attach to vm " + pid, e);
        }
    }

    public void handleStoppedVm(int pid) {
        Pair<MonitoredVm, VmListenerWrapper> data = pidToData.remove(pid);
        // we were not monitoring pid at all, so nothing to do
        if (data == null) {
            return;
        }
    
        MonitoredVm vm = data.getFirst();
        VmListenerWrapper listener = data.getSecond();
        try {
            vm.removeVmListener(listener);
        } catch (MonitorException e) {
            logger.log(Level.WARNING, "can't remove vm listener", e);
        }
        vm.detach();
    }

    public void removeVmListeners() {
        for (Pair<MonitoredVm, VmListenerWrapper> data : pidToData.values()) {
            MonitoredVm vm = data.getFirst();
            VmListenerWrapper listener = data.getSecond();
            try {
                vm.removeVmListener(listener);
            } catch (MonitorException e) {
                logger.log(Level.WARNING, "can't remove vm listener", e);
            }
        }
        pidToData.clear();
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
    Map<Integer, Pair<MonitoredVm, VmListenerWrapper>> getPidToDataMap() {
        return pidToData;
    }

}

