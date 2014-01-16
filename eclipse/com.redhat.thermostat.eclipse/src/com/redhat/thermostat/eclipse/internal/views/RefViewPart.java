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

package com.redhat.thermostat.eclipse.internal.views;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.View;
import com.redhat.thermostat.client.core.views.ViewProvider;
import com.redhat.thermostat.eclipse.SWTComponent;
import com.redhat.thermostat.eclipse.SWTViewProvider;
import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.storage.core.Ref;

public abstract class RefViewPart<T extends Ref> extends ViewPart implements
        ISelectionListener {

    protected Composite top;

    private Composite parent;
    private IStructuredSelection currentSelection;

    private ServiceTracker<InformationService<T>, InformationService<T>> tracker;
    private InformationService<T> infoService;
    private BundleContext context;

    public RefViewPart(BundleContext context) {
        super();
        this.context = context;
    }
    
    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);
        
        try {
            tracker = new ServiceTracker<InformationService<T>, InformationService<T>>(context, createFilter(), null) {
                @Override
                public InformationService<T> addingService(
                        ServiceReference<InformationService<T>> reference) {
                    infoService = super.addingService(reference);
                    return infoService;
                }
                @Override
                public void removedService(
                        ServiceReference<InformationService<T>> reference,
                        InformationService<T> service) {
                    infoService = null;
                    super.removedService(reference, service);
                }
            };
        } catch (InvalidSyntaxException e) {
            throw new PartInitException("Bad OSGi filter", e);
        }
        tracker.open();
    }

    @Override
    public void createPartControl(Composite parent) {
        this.parent = parent;

        createComposite();
        
        getWorkbenchWindow().getSelectionService().addSelectionListener(this);

        // Check for an existing selection
        boolean selected = false;
        IViewPart part = getWorkbenchWindow().getActivePage().findView(
                ThermostatConstants.VIEW_ID_HOST_VM);
        if (part != null) {
            ISelection selection = part.getSite().getSelectionProvider()
                    .getSelection();
            if (selection instanceof IStructuredSelection
                    && !selection.isEmpty()) {
                currentSelection = (IStructuredSelection) selection;
                T ref = handleSelection(selection);
                if (ref != null) {
                    createView(ref);
                    selected = true;
                }
            }
        }
        if (!selected) {
            createNoSelectionLabel();
        }
    }
    
    @Override
    public void dispose() {
        tracker.close();
        super.dispose();
    }

    public void createNoSelectionLabel() {
        Label noHost = new Label(top, SWT.NONE);
        noHost.setText(getNoSelectionMessage());
    }

    public IWorkbenchWindow getWorkbenchWindow() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    }

    @Override
    public void setFocus() {
        parent.setFocus();
    }
    
    /**
     * Returns the {@link ViewProvider} class for the root {@link View}
     * whose controls will be placed in this ViewPart.
     * @return the {@link ViewProvider} class
     */
    protected abstract Class<? extends ViewProvider> getViewProviderClass();

    /**
     * Parses the given selection and returns a {@link Ref} suitable as input for
     * this view, if one can be found.
     * @param selection a selected element from the Hosts/VMs view
     * @return a {@link Ref} parsed from selection
     */
    protected abstract T getRefFromSelection(Object selection);

    /**
     * Returns a helpful message when the current selection in the Hosts/VMs
     * view does not contain a {@link Ref} that this view can provide
     * information for. This message should inform the user what they should
     * select to receive data in this view.
     * @return a message prompting the user to make a selection in the 
     * Hosts/VMs view
     */
    protected abstract String getNoSelectionMessage();

    /**
     * Returns the identifier for the {@link InformationService} that this
     * ViewPart corresponds to. This identifier must have been set as a
     * property with key {@link InformationService#KEY_SERVICE_ID} when the
     * service was registered.
     * @return the unique service ID for a specific implementation of
     * {@link InformationService}
     */
    protected abstract String getInformationServiceID();

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        // We must have received createPartControl
        if (parent != null && !parent.isDisposed()) {
            // Check if a HostRef has been selected
            if (part.getSite().getId()
                    .equals(ThermostatConstants.VIEW_ID_HOST_VM)) {
                if (selection instanceof IStructuredSelection
                        && !selection.isEmpty()
                        && !selection.equals(currentSelection)) {
                    currentSelection = (IStructuredSelection) selection;
                    T ref = handleSelection(selection);
                    // Replace the existing view
                    top.dispose();
                    createComposite();

                    if (ref != null) {
                        createView(ref);
                    } else {
                        // Prompt the user to select a valid ref
                        createNoSelectionLabel();
                    }
                    parent.layout();
                }
            }
        }
    }

    private T handleSelection(ISelection selection) {
        Object selectedElement = ((IStructuredSelection) selection)
                .getFirstElement();
        return getRefFromSelection(selectedElement);
    }

    private void createComposite() {
        top = new Composite(parent, SWT.NONE);
        top.setLayout(new GridLayout());
        top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    private void createView(T ref) {
        if (infoService != null) {
            // Get the ViewProvider and set its parent Composite
            Class<? extends ViewProvider> viewProviderClazz = getViewProviderClass();
            ServiceReference<? extends ViewProvider> serviceRef = context.getServiceReference(viewProviderClazz);
            SWTViewProvider viewProvider = (SWTViewProvider) context.getService(serviceRef);
            viewProvider.setParent(top);
            
            // Instantiate the service's controller and view
            InformationServiceController<T> controller = infoService.getInformationServiceController(ref);
            SWTComponent view = (SWTComponent) controller.getView();
            
            // Register for view visibility updates
            ViewVisibilityWatcher watcher = new ViewVisibilityWatcher(view);
            watcher.watch(top, getSite().getId());
            
            context.ungetService(serviceRef);
        }
    }
    
    private Filter createFilter() throws InvalidSyntaxException {
        String filter = "(&(" + Constants.OBJECTCLASS + "="
                + InformationService.class.getName() + ")("
                + InformationService.KEY_SERVICE_ID + "=" + getInformationServiceID() + "))";
        return FrameworkUtil.createFilter(filter);
    }

}

