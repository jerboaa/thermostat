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

package com.redhat.thermostat.compat.platform.internal;

import com.redhat.thermostat.client.core.internal.platform.DynamicVMPluginProvider;
import com.redhat.thermostat.client.core.internal.platform.UIPluginAction;
import com.redhat.thermostat.client.core.views.UIPluginInfo;
import com.redhat.thermostat.client.core.views.UIComponent;
import com.redhat.thermostat.common.OrderedComparator;
import com.redhat.thermostat.compat.platform.DynamicVMPlugin;
import com.redhat.thermostat.platform.MDIService;
import com.redhat.thermostat.shared.locale.LocalizedString;
import com.redhat.thermostat.storage.core.VmRef;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import java.util.Collections;
import java.util.List;

/**
 */
@Component
@Service(DynamicVMPluginProvider.class)
public class DynamicVMPluginProviderImpl implements DynamicVMPluginProvider {

    private class UIPluginInfoImpl implements UIPluginInfo {
        private DynamicVMPlugin plugin;
        private UIPluginInfoImpl(DynamicVMPlugin plugin) {
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

    }

    private DynamicVMPluginRegistry registry;

    @Activate
    private void activate() {
        BundleContext context =
                FrameworkUtil.getBundle(MDIService.class).getBundleContext();
        registry = new DynamicVMPluginRegistry(context);
        registry.open();
    }

    @Deactivate
    private void deactivate() {
        registry.close();
    }

    @Override
    public void forEach(VmRef vm, UIPluginAction action) {
        List<DynamicVMPlugin> plugins = registry.getPlugins();
        Collections.sort(plugins, new OrderedComparator<DynamicVMPlugin>());
        for (DynamicVMPlugin plugin : plugins) {
            if (plugin.getFilter().matches(vm)) {
                UIPluginInfo info = new UIPluginInfoImpl(plugin);
                action.execute(info);
            }
        }
    }
}
