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
import static org.mockito.Mockito.when;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.junit.Test;
import org.mockito.InOrder;

import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.chart.common.SWTVmGcView;
import com.redhat.thermostat.eclipse.chart.common.SWTVmGcViewProvider;
import com.redhat.thermostat.eclipse.chart.common.VmGcViewPart;
import com.redhat.thermostat.eclipse.internal.views.RefViewPart;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.vm.gc.client.core.VmGcService;
import com.redhat.thermostat.vm.gc.client.core.VmGcViewProvider;

public class VmGcViewPartTest extends AbstractRefViewPartTest<VmRef> {

    private SWTVmGcViewProvider viewProvider;
    private InformationServiceController<VmRef> controller;

    @Test
    public void testSelectionAfter() throws Exception {
        view.createPartControl(parent);

        HostRef hostRef = new HostRef("TEST", "Test");
        VmRef vmRef = new VmRef(hostRef, 0, "Test");
        IStructuredSelection selection = mockSelection(vmRef);
        view.selectionChanged(hostVMView, selection);

        verifyViewProvider();
    }

    private void verifyViewProvider() {
        InOrder order = inOrder(viewProvider, controller);
        order.verify(viewProvider).setParent(any(Composite.class));
        order.verify(controller).getView();
    }

    @Override
    protected void mockController() {
        VmGcService service = mock(VmGcService.class);
        controller = mock(InformationServiceController.class);
        thermoView = mock(SWTVmGcView.class);

        when(osgi.getService(VmGcService.class)).thenReturn(service);
        when(service.getInformationServiceController(any(VmRef.class))).thenReturn(controller);
        when(controller.getView()).thenReturn(thermoView);

        viewProvider = mock(SWTVmGcViewProvider.class);
        when(osgi.getService(VmGcViewProvider.class)).thenReturn(viewProvider);
    }

    @Override
    protected RefViewPart<VmRef> createViewPart() {
        return new VmGcViewPart();
    }

    @Override
    protected String getViewID() {
        return ThermostatConstants.VIEW_ID_VM_GC;
    }

}

