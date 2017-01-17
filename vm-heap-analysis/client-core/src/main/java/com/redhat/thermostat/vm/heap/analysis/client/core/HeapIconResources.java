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

package com.redhat.thermostat.vm.heap.analysis.client.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.redhat.thermostat.client.ui.IconDescriptor;

public class HeapIconResources {

    private static final String PACKAGE_PATH =
            HeapIconResources.class.getPackage().getName().replace(".", "/");
    public static final String PIN_MASK = PACKAGE_PATH + "/pin_mask.png";
    public static final String TRIGGER_HEAP_DUMP = PACKAGE_PATH + "/take_dump.png";

    private static Map<String, IconDescriptor> icons = new HashMap<>();
    
    public synchronized static IconDescriptor getIcon(String path) {
        if (!icons.containsKey(path)) {
            try {
                IconDescriptor icon = IconDescriptor.loadIcon(HeapIconResources.class.getClassLoader(), path);
                icons.put(path, icon);
                
            } catch (IOException ex) {
                Logger.getLogger(HeapIconResources.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return icons.get(path);
    }
}

