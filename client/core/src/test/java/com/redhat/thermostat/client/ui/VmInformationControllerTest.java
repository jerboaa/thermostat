/*
 * Copyright 2012-2016 Red Hat, Inc.
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

package com.redhat.thermostat.client.ui;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import com.redhat.thermostat.client.core.internal.platform.DynamicVMPluginProvider;
import com.redhat.thermostat.client.core.views.UIPluginInfo;
import com.redhat.thermostat.shared.locale.LocalizedString;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.client.core.NameMatchingRefFilter;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.views.VmInformationView;
import com.redhat.thermostat.client.core.views.VmInformationViewProvider;
import com.redhat.thermostat.storage.core.VmRef;

public class VmInformationControllerTest {

    private static final Filter<VmRef> FILTER = new NameMatchingRefFilter<>();

    private VmRef ref;
    private VmInformationViewProvider provider;
    private VmInformationView view;
    private List<DynamicVMPluginProvider> dynamicProviders;

    @Before
    public void setup() {
        ref = mock(VmRef.class);
        provider = mock(VmInformationViewProvider.class);
        view = mock(VmInformationView.class);
        when(provider.createView()).thenReturn(view);

        dynamicProviders = new ArrayList<>();
    }

    @Test
    public void testServiceOrder() {
        int[] orderValues = { 45, 20, 0, 90, 53 };

        // Mock services
        List<InformationService<VmRef>> services = mockServices(orderValues);

        VmInformationController controller =
                new VmInformationController(new ArrayList<>(services), ref, provider,
                                    dynamicProviders);

        controller.rebuild();

        ArgumentCaptor captor = ArgumentCaptor.forClass(List.class);
        verify(view).addChildViews((List<UIPluginInfo>) captor.capture());

        List<UIPluginInfo> plugins = (List<UIPluginInfo>) captor.getValue();
        assertEquals(5, plugins.size());
        assertEquals("2", plugins.get(0).getLocalizedName().getContents());
        assertEquals("1", plugins.get(1).getLocalizedName().getContents());
        assertEquals("0", plugins.get(2).getLocalizedName().getContents());
        assertEquals("4", plugins.get(3).getLocalizedName().getContents());
        assertEquals("3", plugins.get(4).getLocalizedName().getContents());
    }

    private List<InformationService<VmRef>> mockServices(int[] orderValues) {
        List<InformationService<VmRef>> services = new ArrayList<>();
        int id = 0;
        for (int order : orderValues) {
            InformationService<VmRef> service = mock(InformationService.class);
            InformationServiceController<VmRef> controller = mock(InformationServiceController.class);
            when(controller.getLocalizedName()).thenReturn(new LocalizedString("" + id));
            when(service.getFilter()).thenReturn(FILTER);
            when(service.getInformationServiceController(ref)).thenReturn(
                    controller);
            when(service.getOrderValue()).thenReturn(order);
            services.add(service);
            id++;
        }
        return services;
    }
}

