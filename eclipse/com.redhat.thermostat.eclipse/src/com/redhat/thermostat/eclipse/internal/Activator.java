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

package com.redhat.thermostat.eclipse.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.utils.OSGIUtils;
import com.redhat.thermostat.eclipse.LoggerFacility;
import com.redhat.thermostat.eclipse.internal.views.SWTHostOverviewViewProvider;
import com.redhat.thermostat.host.overview.client.core.HostOverviewViewProvider;
import com.redhat.thermostat.storage.core.ConnectionException;
import com.redhat.thermostat.storage.core.DbService;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "com.redhat.thermostat.eclipse"; //$NON-NLS-1$

    // The shared instance
    private static Activator plugin;

    /**
     * The constructor
     */
    public Activator() {
    }

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

        // Register ViewProvider
        context.registerService(HostOverviewViewProvider.class,
                new SWTHostOverviewViewProvider(), null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    public void stop(BundleContext context) throws Exception {
        ServiceReference<DbService> dbServiceRef = context.getServiceReference(DbService.class);
        if (dbServiceRef != null) {
            DbService dbService = context.getService(dbServiceRef);
            if (dbService != null) {
                try {
                    dbService.disconnect();
                    context.ungetService(dbServiceRef);
                } catch (ConnectionException e) {
                    LoggerFacility.getInstance().log(IStatus.ERROR,
                            "Error disconnecting from database", e);
                }
            }
        }
        plugin = null;
        super.stop(context);
    }

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator getDefault() {
        return plugin;
    }

    /**
     * 
     * @return {@code true} when platform was started in debug mode (
     *         {@code -debug} switch) and
     *         {@code com.redhat.thermostat.eclipse/debug=true} is set in some
     *         .options file either in $HOME/.options or $(pwd)/.options.
     */
    public static boolean inDebugMode() {
        if (Platform.inDebugMode()) {
            String debugOption = Platform.getDebugOption(PLUGIN_ID + "/debug"); //$NON-NLS-1$
            if (debugOption != null && debugOption.equals("true")) { //$NON-NLS-1$
                return true;
            }
        }
        return false;
    }

    public IWorkbenchPage getActivePage() {
        return internalGetActivePage();
    }

    private IWorkbenchPage internalGetActivePage() {
        IWorkbenchWindow window = getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
            return null;
        return window.getActivePage();
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path
     * 
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    public boolean isDbConnected() {
        DbService dbService = OSGIUtils.getInstance().getServiceAllowNull(
                DbService.class);
        return dbService != null;
    }

}

