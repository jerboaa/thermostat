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

package com.redhat.thermostat.backend.internal;

import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.event.MonitorStatusChangeEvent;
import sun.jvmstat.monitor.event.VmEvent;
import sun.jvmstat.monitor.event.VmListener;

import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateException;
import com.redhat.thermostat.backend.VmUpdateListener;

public class VmListenerWrapper implements VmListener {
    
    private VmUpdateListener listener;
    private MonitoredVm vm;
    private VmUpdate update;

    public VmListenerWrapper(VmUpdateListener listener, MonitoredVm vm) {
        this.listener = listener;
        this.vm = vm;
        this.update = new VmUpdateImpl(this);
    }

    @Override
    public void monitorsUpdated(VmEvent event) {
        if (!vm.equals(event.getMonitoredVm())) {
            throw new AssertionError("Received change event for wrong VM");
        }
        listener.countersUpdated(update);
    }

    @Override
    public void monitorStatusChanged(MonitorStatusChangeEvent event) {
        // Nothing to do here
    }

    @Override
    public void disconnected(VmEvent event) {
        // Nothing to do here
    }
    
    public Monitor getMonitor(String name) throws VmUpdateException {
        Monitor result;
        try {
            result = vm.findByName(name);
        } catch (MonitorException e) {
            throw new VmUpdateException("Error communicating with monitored VM", e);
        }
        return result;
    }
    
    /*
     * For testing purposes only.
     */
    VmUpdateListener getVmUpdateListener() {
        return listener;
    }
    
}

