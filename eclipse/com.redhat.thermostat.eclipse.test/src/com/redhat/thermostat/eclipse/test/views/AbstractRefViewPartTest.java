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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.junit.Before;

import com.redhat.thermostat.common.dao.Ref;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.eclipse.SWTComponent;
import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.internal.views.HostsVmsTreeViewPart;
import com.redhat.thermostat.eclipse.internal.views.RefViewPart;

public abstract class AbstractRefViewPartTest<T extends Ref> {

    protected Composite parent;
    protected RefViewPart<T> view;
    protected SWTComponent thermoView;
    protected HostsVmsTreeViewPart hostVMView;
    private ISelectionProvider provider;
    protected OSGIUtils osgi;

    public AbstractRefViewPartTest() {
        super();
    }

    @Before
    public void before() {
        parent = spy(new Shell(Display.getCurrent()));
        
        view = spy(createViewPart());
        hostVMView = mock(HostsVmsTreeViewPart.class);
        osgi = mock(OSGIUtils.class);
        OSGIUtils.setInstance(osgi);
    
        // Workbench mocks
        IWorkbenchWindow window = mock(IWorkbenchWindow.class);
        IWorkbenchPage page = mock(IWorkbenchPage.class);
        ISelectionService service = mock(ISelectionService.class);
        IWorkbenchPartSite site = mock(IWorkbenchPartSite.class);
        
        when(page.findView(ThermostatConstants.VIEW_ID_HOST_VM)).thenReturn(hostVMView);
        when(window.getSelectionService()).thenReturn(service);
        when(window.getActivePage()).thenReturn(page);
        when(view.getWorkbenchWindow()).thenReturn(window);
        when(site.getId()).thenReturn(getViewID());
        when(view.getSite()).thenReturn(site);
        
        // Controller mock
        mockController();
        
        // Selection mocks
        IWorkbenchPartSite hostVMSite = mock(IWorkbenchPartSite.class);
        provider = mock(ISelectionProvider.class);
 
        when(hostVMSite.getId()).thenReturn(ThermostatConstants.VIEW_ID_HOST_VM);
        when(hostVMSite.getSelectionProvider()).thenReturn(provider);
        when(hostVMView.getSite()).thenReturn(hostVMSite);
    }

    protected abstract void mockController();

    protected abstract RefViewPart<T> createViewPart();
    
    protected abstract String getViewID();

    protected IStructuredSelection mockSelection(Ref ref) {
        IStructuredSelection selection = mock(IStructuredSelection.class);;
        when(provider.getSelection()).thenReturn(selection);
        when(selection.getFirstElement()).thenReturn(ref);
        
        return selection;
    }

}
