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

package com.redhat.thermostat.eclipse.test.views;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.junit.Before;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.eclipse.SWTComponent;
import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.internal.views.HostsVmsTreeViewPart;
import com.redhat.thermostat.eclipse.internal.views.RefViewPart;
import com.redhat.thermostat.storage.core.Ref;

public abstract class AbstractRefViewPartTest<T extends Ref> {

    protected Composite parent;
    protected RefViewPart<T> view;
    protected SWTComponent thermoView;
    protected HostsVmsTreeViewPart hostVMView;
    protected BundleContext context;
    protected InformationServiceController<T> controller;
    private ISelectionProvider provider;

    public AbstractRefViewPartTest() {
        super();
    }

    @SuppressWarnings("unchecked")
    @Before
    public void before() throws InvalidSyntaxException, PartInitException {
        parent = spy(new Shell(Display.getCurrent()));
        
        context = mock(BundleContext.class);
        view = spy(createViewPart(context));
        hostVMView = mock(HostsVmsTreeViewPart.class);
        ServiceReference<InformationService<T>> ref = (ServiceReference<InformationService<T>>) mock(ServiceReference.class);
        InformationService<T> infoService = (InformationService<T>) mock(InformationService.class);
        controller = (InformationServiceController<T>) mock(InformationServiceController.class);
        when(infoService.getInformationServiceController((T) any(Ref.class))).thenReturn(controller);
        when(context.getService(ref)).thenReturn(infoService);
        when(context.getServiceReferences((String) isNull(), anyString())).thenReturn(new ServiceReference<?>[] { ref });
    
        // Workbench mocks
        IWorkbenchWindow window = mock(IWorkbenchWindow.class);
        IWorkbenchPage page = mock(IWorkbenchPage.class);
        ISelectionService service = mock(ISelectionService.class);
        IViewSite site = mock(IViewSite.class);
        
        when(page.findView(ThermostatConstants.VIEW_ID_HOST_VM)).thenReturn(hostVMView);
        when(window.getSelectionService()).thenReturn(service);
        when(window.getActivePage()).thenReturn(page);
        when(view.getWorkbenchWindow()).thenReturn(window);
        when(site.getId()).thenReturn(getViewID());
        when(view.getSite()).thenReturn(site);
        
        // ViewProvider mock
        mockViewProvider();
        
        // Selection mocks
        IWorkbenchPartSite hostVMSite = mock(IWorkbenchPartSite.class);
        provider = mock(ISelectionProvider.class);
 
        when(hostVMSite.getId()).thenReturn(ThermostatConstants.VIEW_ID_HOST_VM);
        when(hostVMSite.getSelectionProvider()).thenReturn(provider);
        when(hostVMView.getSite()).thenReturn(hostVMSite);
        
        view.init(site);
    }

    protected abstract void mockViewProvider();

    protected abstract RefViewPart<T> createViewPart(BundleContext context);
    
    protected abstract String getViewID();

    protected IStructuredSelection mockSelection(Ref ref) {
        IStructuredSelection selection = mock(IStructuredSelection.class);;
        when(provider.getSelection()).thenReturn(selection);
        when(selection.getFirstElement()).thenReturn(ref);
        
        return selection;
    }

}

