/*
 * Copyright 2012-2017 Red Hat, Inc.
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

package com.redhat.thermostat.client.filter.vm.core.internal;

import com.redhat.thermostat.client.ui.ReferenceFilter;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

public class LivingVMFilter extends ReferenceFilter {

    volatile boolean filterActive = true;
    
    private VmInfoDAO vmDao;
    private HostInfoDAO hostDao;
    
    public LivingVMFilter(VmInfoDAO vmDao, HostInfoDAO hostDao) {
        this.hostDao = hostDao;
        this.vmDao = vmDao;
    }
    
    @Override
    public boolean applies(Ref reference) {
        return (reference instanceof VmRef);
    }
    
    @Override
    public boolean matches(Ref ref) {
        if (!filterActive) {
            return true;
        }
        
        if (!applies(ref)) {
            return false;
        }
        
        // if the parent host if not alive, we don't want to hide this
        boolean match = true;
        
        if (hostDao.isAlive(((VmRef) ref).getHostRef())) {
            VmInfo vmInfo = vmDao.getVmInfo((VmRef) ref);
            match = vmInfo.isAlive();
        }

        return match;
    }

    public void setActive(boolean active) {
        boolean oldActive = this.filterActive;
        this.filterActive = active;
        if (oldActive != filterActive) {
            notify(FilterEvent.FILTER_CHANGED);
        }
    }

    public boolean isActive() {
        return filterActive;
    }
}

