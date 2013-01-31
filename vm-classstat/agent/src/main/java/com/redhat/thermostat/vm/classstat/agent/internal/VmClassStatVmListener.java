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

import java.util.logging.Level;
import java.util.logging.Logger;

import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.event.MonitorStatusChangeEvent;
import sun.jvmstat.monitor.event.VmEvent;
import sun.jvmstat.monitor.event.VmListener;

import com.redhat.thermostat.common.utils.LoggingUtils;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;
import com.redhat.thermostat.vm.classstat.common.model.VmClassStat;

class VmClassStatVmListener implements VmListener {

    private static final Logger logger = LoggingUtils.getLogger(VmClassStatVmListener.class);

    private VmClassStatDAO dao;
    private int vmId;

    VmClassStatVmListener(VmClassStatDAO dao, int vmId) {
        this.dao = dao;
        this.vmId = vmId;
    }

    @Override
    public void disconnected(VmEvent vmEvent) {
        /* nothing to do here */
    }

    @Override
    public void monitorStatusChanged(MonitorStatusChangeEvent vmEvent) {
        /* nothing to do here */
    }

    @Override
    public void monitorsUpdated(VmEvent vmEvent) {
        MonitoredVm vm = vmEvent.getMonitoredVm();
        try {
            VmClassStatDataExtractor extractor = new VmClassStatDataExtractor(vm);
            long loadedClasses = extractor.getLoadedClasses();
            long timestamp = System.currentTimeMillis();
            VmClassStat stat = new VmClassStat(vmId, timestamp, loadedClasses);
            dao.putVmClassStat(stat);
        } catch (MonitorException e) {
            logger.log(Level.WARNING, "error gathering class info for vm " + vmId, e);
        }


    }

}

