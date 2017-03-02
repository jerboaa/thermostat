/*
 * Copyright 2012-2017 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.core.controllers.InformationServiceController;
import com.redhat.thermostat.client.core.internal.platform.DynamicHostPluginProvider;
import com.redhat.thermostat.client.core.internal.platform.UIPluginAction;
import com.redhat.thermostat.client.core.views.BasicView;
import com.redhat.thermostat.client.core.views.HostInformationView;
import com.redhat.thermostat.client.core.views.HostInformationViewProvider;
import com.redhat.thermostat.client.core.views.UIPluginInfo;
import com.redhat.thermostat.storage.core.HostRef;

public class HostInformationController implements ContentProvider {

    private final List<InformationService<HostRef>> hostInfoServices;
    private final HostRef ref;
    private final HostInformationView view;
    private List<DynamicHostPluginProvider> dynamicProviders;

    private static class PluginAction implements UIPluginAction {

        private List<UIPluginInfo> plugins;
        PluginAction(List<UIPluginInfo> plugins) {
            this.plugins = plugins;
        }

        @Override
        public void execute(UIPluginInfo info) {
            plugins.add(info);
        }
    }

    public HostInformationController(List<InformationService<HostRef>> hostInfoServices,
                                     HostRef ref,
                                     HostInformationViewProvider provider,
                                     List<DynamicHostPluginProvider> dynamicProviders)
    {
        this.hostInfoServices = hostInfoServices;
        this.ref = ref;
        this.dynamicProviders = dynamicProviders;
        view = provider.createView();
    }

    public void rebuild() {
        List<UIPluginInfo> plugins = new ArrayList<>();
        view.clear();

        for (InformationService<HostRef> hostInfoService : hostInfoServices) {
            if (hostInfoService.getFilter().matches(ref)) {
                InformationServiceController<HostRef> ctrl = hostInfoService.getInformationServiceController(ref);
                plugins.add(new PluginInfo(ctrl.getLocalizedName(), ctrl.getView(), hostInfoService.getOrderValue()));
            }
        }

        PluginAction action = new PluginAction(plugins);

        for (DynamicHostPluginProvider dynamicProvider : dynamicProviders) {
            dynamicProvider.forEach(ref, action);
        }

        Collections.sort(plugins, new PluginInfoComparator<UIPluginInfo>());

        view.addChildViews(plugins);
    }

    public BasicView getView() {
        rebuild();
        return view;
    }
}

