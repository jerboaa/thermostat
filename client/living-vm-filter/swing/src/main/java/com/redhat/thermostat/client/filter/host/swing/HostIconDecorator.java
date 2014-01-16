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

package com.redhat.thermostat.client.filter.host.swing;

import java.awt.Paint;

import com.redhat.thermostat.client.swing.IconResource;
import com.redhat.thermostat.client.swing.UIDefaults;
import com.redhat.thermostat.client.swing.components.CompositeIcon;
import com.redhat.thermostat.client.swing.components.Icon;
import com.redhat.thermostat.client.ui.PlatformIcon;
import com.redhat.thermostat.client.ui.ReferenceFieldIconDecorator;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.Ref;

public class HostIconDecorator implements ReferenceFieldIconDecorator {
    
    private static Icon ICON;
    private static Icon SELECTED;

    private static HostIconDecorator theInstance;
    public static synchronized HostIconDecorator getInstance(UIDefaults uiDefaults) {
        
        if (theInstance == null) {            
            int size = uiDefaults.getReferenceFieldDefaultIconSize();
            Paint fg = uiDefaults.getReferenceFieldIconColor();
            Paint selected = uiDefaults.getReferenceFieldIconSelectedColor();
            
            Icon hostIcon = IconUtils.resizeIcon(IconResource.HOST_24.getIcon(), size);
            
            ICON = CompositeIcon.createDefaultComposite(hostIcon, fg);
            SELECTED = CompositeIcon.createDefaultComposite(hostIcon, selected);
            
            theInstance = new HostIconDecorator();
        }
        
        return theInstance;
    }
    
    private HostIconDecorator() {}
    
    @Override
    public int getOrderValue() {
        return ORDER_FIRST;
    }
    
    @Override
    public PlatformIcon getIcon(PlatformIcon originalIcon, Ref reference) {
        if (reference instanceof HostRef) {
            return ICON;
        } else {
            return originalIcon;
        }
    }
    
    @Override
    public PlatformIcon getSelectedIcon(PlatformIcon originalIcon, Ref reference) {
        if (reference instanceof HostRef) {
            return SELECTED;
        } else {
            return originalIcon;
        }
    }
}

