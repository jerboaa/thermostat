/*
 * Copyright 2012 Red Hat, Inc.
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

package com.redhat.thermostat.eclipse.internal.views;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

import com.redhat.thermostat.eclipse.ThermostatConstants;

public class ThermostatPerspectiveFactory implements IPerspectiveFactory {
    
    public static final String FOLDER_LEFT = "left";
    public static final String FOLDER_RIGHT = "right";

    @Override
    public void createInitialLayout(IPageLayout layout) {
        defineActions(layout);
        defineLayout(layout);
    }

    private void defineLayout(IPageLayout layout) {
        IFolderLayout left = layout.createFolder(FOLDER_LEFT, IPageLayout.LEFT, 0.25f, layout.getEditorArea());
        left.addView(ThermostatConstants.VIEW_ID_HOST_VM);
        
        IFolderLayout right = layout.createFolder(FOLDER_RIGHT, IPageLayout.RIGHT, 0.5f, layout.getEditorArea());
        right.addView(ThermostatConstants.VIEW_ID_HOST_OVERVIEW);
        right.addView(ThermostatConstants.VIEW_ID_HOST_CPU);
        right.addView(ThermostatConstants.VIEW_ID_HOST_MEMORY);
        right.addView(ThermostatConstants.VIEW_ID_VM_CPU);
        right.addView(ThermostatConstants.VIEW_ID_VM_GC);
        
        layout.setEditorAreaVisible(false);
    }

    private void defineActions(IPageLayout layout) {
        layout.addShowViewShortcut(ThermostatConstants.VIEW_ID_HOST_VM);
        layout.addShowViewShortcut(ThermostatConstants.VIEW_ID_HOST_OVERVIEW);
        layout.addShowViewShortcut(ThermostatConstants.VIEW_ID_HOST_CPU);
        layout.addShowViewShortcut(ThermostatConstants.VIEW_ID_HOST_MEMORY);
        layout.addShowViewShortcut(ThermostatConstants.VIEW_ID_VM_CPU);
        layout.addShowViewShortcut(ThermostatConstants.VIEW_ID_VM_GC);
    }

}
