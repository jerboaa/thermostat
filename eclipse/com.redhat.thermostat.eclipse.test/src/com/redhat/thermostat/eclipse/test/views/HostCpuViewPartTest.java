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

package com.redhat.thermostat.eclipse.test.views;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.junit.Test;
import org.mockito.InOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.chart.common.HostCpuViewPart;
import com.redhat.thermostat.eclipse.chart.common.SWTHostCpuViewProvider;
import com.redhat.thermostat.eclipse.internal.views.RefViewPart;
import com.redhat.thermostat.host.cpu.client.core.HostCpuViewProvider;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;

public class HostCpuViewPartTest extends AbstractRefViewPartTest<HostRef> {
    
    private SWTHostCpuViewProvider viewProvider;

    @Test
    public void testSetFocus() throws Exception {
        view.createPartControl(parent);
        view.setFocus();

        verify(parent).setFocus();
    }

    @Test
    public void testNoSelection() throws Exception {
        view.createPartControl(parent);
        verify(view).createNoSelectionLabel();
    }

    @Test
    public void testBadSelection() throws Exception {
        mockSelection(null);
        view.createPartControl(parent);
        verify(view).createNoSelectionLabel();
    }

    @Test
    public void testSelectionBefore() throws Exception {
        HostRef hostRef = new HostRef("TEST", "Test");
        mockSelection(hostRef);
        view.createPartControl(parent);

        verify(view, never()).createNoSelectionLabel();

        verifyViewProvider();
    }

    private void verifyViewProvider() {
        InOrder order = inOrder(viewProvider, controller);
        order.verify(viewProvider).setParent(any(Composite.class));
        order.verify(controller).getView();
    }

    @Test
    public void testSelectionAfter() throws Exception {
        view.createPartControl(parent);

        HostRef hostRef = new HostRef("TEST", "Test");
        IStructuredSelection selection = mockSelection(hostRef);
        view.selectionChanged(hostVMView, selection);

        verifyViewProvider();
    }

    @Test
    public void testSelectionVmRef() throws Exception {
        view.createPartControl(parent);

        HostRef hostRef = new HostRef("TEST", "Test");
        VmRef vmRef = new VmRef(hostRef, 0, "Test");
        IStructuredSelection selection = mockSelection(vmRef);
        view.selectionChanged(hostVMView, selection);

        verifyViewProvider();
    }

    @Override
    protected RefViewPart<HostRef> createViewPart(BundleContext context) {
        return new HostCpuViewPart(context);
    }

    @Override
    protected void mockViewProvider() {
        viewProvider = mock(SWTHostCpuViewProvider.class);
        @SuppressWarnings("unchecked")
        ServiceReference<HostCpuViewProvider> ref = (ServiceReference<HostCpuViewProvider>) mock(ServiceReference.class);
        when(context.getService(ref)).thenReturn(viewProvider);
        when(context.getServiceReference(HostCpuViewProvider.class)).thenReturn(
                ref);
    }

    @Override
    protected String getViewID() {
        return ThermostatConstants.VIEW_ID_HOST_CPU;
    }

}

