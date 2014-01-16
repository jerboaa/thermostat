/*
 * Copyright 2012-2014 Red Hat, Inc.
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

package com.redhat.thermostat.storage.monitor.internal;

import java.util.ArrayList;
import java.util.Collection;

import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.storage.monitor.HostMonitor;
import com.redhat.thermostat.storage.monitor.HostMonitor.Action;

class HostMonitorAction extends MonitorAction<VmRef, HostMonitor.Action> {

    private VmInfoDAO vmsDao;
    private HostRef host;
        
    public HostMonitorAction(ActionNotifier<Action> notifier, VmInfoDAO vmsDao,
                             HostRef host)
    {
        super(notifier);
        this.host = host;
        this.vmsDao = vmsDao;
    }

    @Override
    protected Action getAddAction() {
        return HostMonitor.Action.VM_ADDED;
    }

    @Override
    protected Action getRemoveAction() {
        return HostMonitor.Action.VM_REMOVED;
    }

    @Override
    protected Collection<VmRef> getNewReferences() {
        Collection<VmRef> vms = vmsDao.getVMs(host);
        Collection<VmRef> livingVMS = new ArrayList<>();
        for (VmRef vm : vms) {
            VmInfo vmInfo = vmsDao.getVmInfo(vm);
            if (vmInfo.isAlive()) {
                livingVMS.add(vm);
            }
        }
        return livingVMS;
    }
}

