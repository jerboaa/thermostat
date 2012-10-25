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

package com.redhat.thermostat.eclipse.test.views;

import static org.mockito.Matchers.anyFloat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.views.ThermostatPerspectiveFactory;

public class ThermostatPerspectiveTest {
    
    private IFolderLayout leftFolder;
    private IFolderLayout rightFolder;
    private IPageLayout layout;

    @Before
    public void createLayout() {
        ThermostatPerspectiveFactory factory = new ThermostatPerspectiveFactory();
        layout = mock(IPageLayout.class);
        leftFolder = mock(IFolderLayout.class);
        rightFolder = mock(IFolderLayout.class);
        when(layout.createFolder(eq(ThermostatPerspectiveFactory.FOLDER_LEFT), anyInt(), anyFloat(), anyString())).thenReturn(leftFolder);
        when(layout.createFolder(eq(ThermostatPerspectiveFactory.FOLDER_RIGHT), anyInt(), anyFloat(), anyString())).thenReturn(rightFolder);
        factory.createInitialLayout(layout);
    }

    @Test
    public void testHostVMViewAdded() {
        verifyViewAdded(ThermostatConstants.VIEW_ID_HOST_VM, leftFolder);
    }
    
    @Test
    public void testHostVMShortcutAdded() {
        verifyShortcutAdded(ThermostatConstants.VIEW_ID_HOST_VM);
    }
    
    @Test
    public void testHostOverviewViewAdded() {
        verifyViewAdded(ThermostatConstants.VIEW_ID_HOST_OVERVIEW, rightFolder);
    }
    
    @Test
    public void testHostOverviewShortcutAdded() {
        verifyShortcutAdded(ThermostatConstants.VIEW_ID_HOST_OVERVIEW);
    }
    
    @Test
    public void testHostCPUViewAdded() {
        verifyViewAdded(ThermostatConstants.VIEW_ID_HOST_CPU, rightFolder);
    }
    
    @Test
    public void testHostCPUShortcutAdded() {
        verifyShortcutAdded(ThermostatConstants.VIEW_ID_HOST_CPU);
    }
    
    @Test
    public void testHostMemoryViewAdded() {
        verifyViewAdded(ThermostatConstants.VIEW_ID_HOST_MEMORY, rightFolder);
    }
    
    @Test
    public void testHostMemoryShortcutAdded() {
        verifyShortcutAdded(ThermostatConstants.VIEW_ID_HOST_MEMORY);
    }
    
    @Test
    public void testVMCPUViewAdded() {
        verifyViewAdded(ThermostatConstants.VIEW_ID_VM_CPU, rightFolder);
    }
    
    @Test
    public void testVMCPUShortcutAdded() {
        verifyShortcutAdded(ThermostatConstants.VIEW_ID_VM_CPU);
    }
    
    @Test
    public void testVMGCViewAdded() {
        verifyViewAdded(ThermostatConstants.VIEW_ID_VM_GC, rightFolder);
    }
    
    @Test
    public void testVMGCShortcutAdded() {
        verifyShortcutAdded(ThermostatConstants.VIEW_ID_VM_GC);
    }
    
    private void verifyViewAdded(String viewID, IFolderLayout folder) {
        verify(folder).addView(viewID);
    }
    
    private void verifyShortcutAdded(String viewID) {
        verify(layout).addShowViewShortcut(viewID);
    }

}
