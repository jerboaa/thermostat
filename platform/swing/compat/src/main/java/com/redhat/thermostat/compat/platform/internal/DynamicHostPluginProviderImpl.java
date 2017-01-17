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

package com.redhat.thermostat.compat.platform.internal;

import com.redhat.thermostat.client.core.internal.platform.DynamicHostPluginProvider;
import com.redhat.thermostat.client.core.internal.platform.UIPluginAction;
import com.redhat.thermostat.client.core.views.UIPluginInfo;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.OrderedComparator;
import com.redhat.thermostat.compat.platform.DynamicHostPlugin;
import com.redhat.thermostat.platform.MDIService;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.HostRef;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;

import java.util.Collections;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 */
@Component
@Service(DynamicHostPluginProvider.class)
public class DynamicHostPluginProviderImpl implements DynamicHostPluginProvider {

    private class UIPluginInfoImpl implements UIPluginInfo {
        private DynamicHostPlugin plugin;
        private UIPluginInfoImpl(DynamicHostPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public UIComponent getView() {
            return plugin.getView();
        }

        @Override
        public LocalizedString getLocalizedName() {
            return plugin.getController().getName();
        }

        @Override
        public int getOrderValue() {
            return plugin.getOrderValue();
        }
    }
    
    private DynamicHostPluginRegistry registry;
    
    @Activate
    private void activate() {
        BundleContext context =
            FrameworkUtil.getBundle(MDIService.class).getBundleContext();
        registry = new DynamicHostPluginRegistry(context);
        registry.open();
    }

    @Deactivate
    private void deactivate() {
        registry.close();
    }

    @Override
    public void forEach(HostRef host, UIPluginAction action) {
        List<DynamicHostPlugin> plugins = registry.getPlugins();
        Collections.sort(plugins, new OrderedComparator<DynamicHostPlugin>());
        for (DynamicHostPlugin plugin : plugins) {
            if (plugin.getFilter().matches(host)) {
                UIPluginInfo info = new UIPluginInfoImpl(plugin);
                action.execute(info);
            }
        }
    }
}
