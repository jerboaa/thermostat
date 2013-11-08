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

package com.redhat.thermostat.client.filter.vm.swing;

import com.redhat.thermostat.client.filter.host.swing.IconUtils;
import com.redhat.thermostat.client.swing.components.FontAwesomeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.ui.Palette;
import com.redhat.thermostat.client.ui.PlatformIcon;
import com.redhat.thermostat.client.ui.ReferenceFieldIconDecorator;
import com.redhat.thermostat.storage.core.Ref;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;

public class DeadVMIconDecorator implements ReferenceFieldIconDecorator {

    public static final Icon DEAD = new FontAwesomeIcon('\uf00d', 12, Palette.THERMOSTAT_RED.getColor());
    public static final Icon DEAD_SELECTED = new FontAwesomeIcon('\uf00d', 12, Palette.THERMOSTAT_RED.getColor());

    private VmInfoDAO vmDao;
    private HostInfoDAO hostDao;
    
    public DeadVMIconDecorator(VmInfoDAO vmDao, HostInfoDAO hostDao) {
        this.vmDao = vmDao;
        this.hostDao = hostDao;
    }
    
    @Override
    public int getOrderValue() {
        return ORDER_FIRST + 100;
    }
    
    @Override
    public PlatformIcon getIcon(PlatformIcon originalIcon, Ref reference) {
        return doOverlay(originalIcon, reference, DEAD);
    }
    
    @Override
    public PlatformIcon getSelectedIcon(PlatformIcon originalIcon, Ref reference) {
        return doOverlay(originalIcon, reference, DEAD_SELECTED);
    }
    
    public PlatformIcon doOverlay(PlatformIcon originalIcon, Ref reference, Icon overlay) {
        
        if (!(reference instanceof VmRef)) {
            return originalIcon;
        }
        
        boolean match = true;
        
        VmRef vm = (VmRef) reference;
        if (hostDao.isAlive(vm.getHostRef())) {
            VmInfo vmInfo = vmDao.getVmInfo(vm);
            match = !vmInfo.isAlive();
            
        } else {
            match = true;
        }
     
        PlatformIcon result = originalIcon;
        if (match) {
            Icon canvas = (Icon) originalIcon;
            int y = canvas.getIconHeight() - overlay.getIconHeight();
            result = IconUtils.overlay(canvas, overlay, 0, y);            
        }
        
        return result;
    }
}
