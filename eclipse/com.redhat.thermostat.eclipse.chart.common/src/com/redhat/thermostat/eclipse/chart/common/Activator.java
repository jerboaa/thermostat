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

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.client.core.views.HostMemoryViewProvider;
import com.redhat.thermostat.client.core.views.VmCpuViewProvider;
import com.redhat.thermostat.client.core.views.VmGcViewProvider;
import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.host.cpu.client.core.HostCpuViewProvider;

public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.redhat.thermostat.eclipse.chart.common"; //$NON-NLS-1$

    private static Activator plugin;
  
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
     * )
     */
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        
        // Register our ViewProviders
        OSGIUtils.getInstance().registerService(HostCpuViewProvider.class, new SWTHostCpuViewProvider());
        OSGIUtils.getInstance().registerService(HostMemoryViewProvider.class, new SWTHostMemoryViewProvider());
        OSGIUtils.getInstance().registerService(VmCpuViewProvider.class, new SWTVmCpuViewProvider());
        OSGIUtils.getInstance().registerService(VmGcViewProvider.class, new SWTVmGcViewProvider());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    public void stop(BundleContext context) throws Exception {
        plugin = null;
        super.stop(context);
    }
    
    public static Activator getDefault() {
        return plugin;
    }

}
