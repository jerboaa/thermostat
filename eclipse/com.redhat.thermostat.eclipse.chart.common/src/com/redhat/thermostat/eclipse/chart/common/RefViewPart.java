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

package com.redhat.thermostat.eclipse.chart.common;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.redhat.thermostat.common.dao.Ref;
import com.redhat.thermostat.eclipse.ThermostatConstants;

public abstract class RefViewPart<T extends Ref> extends ViewPart implements
        ISelectionListener {

    protected Composite top;

    private Composite parent;
    private IStructuredSelection currentSelection;

    public RefViewPart() {
        super();
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
                    createControllerView(ref);
                    selected = true;
                }
            }
        }
        if (!selected) {
            createNoSelectionLabel();
        }
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

    protected abstract void createControllerView(T ref);

    protected abstract T getRefFromSelection(Object selection);

    protected abstract String getNoSelectionMessage();

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
                        createControllerView(ref);
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

}
