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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.junit.Test;
import org.mockito.InOrder;

import com.redhat.thermostat.client.core.views.HostCpuViewProvider;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.client.ui.HostCpuController;
import com.redhat.thermostat.common.dao.CpuStatDAO;
import com.redhat.thermostat.common.dao.HostInfoDAO;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.eclipse.ThermostatConstants;
import com.redhat.thermostat.eclipse.chart.common.HostCpuViewPart;
import com.redhat.thermostat.eclipse.chart.common.SWTHostCpuView;
import com.redhat.thermostat.eclipse.chart.common.SWTHostCpuViewProvider;
import com.redhat.thermostat.eclipse.internal.views.RefViewPart;

public class HostCpuViewPartTest extends AbstractRefViewPartTest<HostRef> {
    
    private SWTHostCpuViewProvider viewProvider;
    private HostCpuController controller;

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
    protected RefViewPart<HostRef> createViewPart() {
        return new HostCpuViewPart();
    }

    @Override
    protected void mockController() {
        controller = mock(HostCpuController.class);
        thermoView = mock(SWTHostCpuView.class);

        HostInfoDAO hostInfoDao = mock(HostInfoDAO.class);
        CpuStatDAO cpuStatDao = mock(CpuStatDAO.class);
        viewProvider = mock(SWTHostCpuViewProvider.class);
        when(osgi.getService(HostInfoDAO.class)).thenReturn(hostInfoDao);
        when(osgi.getService(CpuStatDAO.class)).thenReturn(cpuStatDao);
        when(osgi.getService(HostCpuViewProvider.class)).thenReturn(
                viewProvider);

        doReturn(controller).when(((HostCpuViewPart) view)).createController(
                same(hostInfoDao), same(cpuStatDao), any(HostRef.class),
                same(viewProvider));
        when(controller.getView()).thenReturn((UIComponent) thermoView);
    }

    @Override
    protected String getViewID() {
        return ThermostatConstants.VIEW_ID_HOST_CPU;
    }

}
