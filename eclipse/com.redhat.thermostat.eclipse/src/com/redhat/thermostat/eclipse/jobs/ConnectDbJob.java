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

package com.redhat.thermostat.eclipse.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.common.storage.ConnectionException;
import com.redhat.thermostat.eclipse.Activator;
import com.redhat.thermostat.eclipse.ConnectionConfiguration;
import com.redhat.thermostat.eclipse.LoggerFacility;
import com.redhat.thermostat.launcher.DbService;
import com.redhat.thermostat.launcher.DbServiceFactory;

public class ConnectDbJob extends Job {

    private ConnectionConfiguration configuration;
    
    public ConnectDbJob(String name, ConnectionConfiguration configuration) {
        super(name);
        this.configuration = configuration;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask(
                "Connecting to " + configuration.getDBConnectionString(),
                IProgressMonitor.UNKNOWN);
        try {
            connectToBackEnd();
            return Status.OK_STATUS;
        } catch (ConnectionException e) {
            LoggerFacility.getInstance().log(IStatus.ERROR,
                    "Could not connect to DB", e);
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Could not connect to DB", e);
        }
    }
    
    /*
     * Establish a DB connection.
     */
    private void connectToBackEnd() throws ConnectionException {
        DbService dbService = DbServiceFactory.createDbService(configuration.getUsername(),
                configuration.getPassword(), configuration.getDBConnectionString());
        dbService.connect();
        // register service in order to indicate that we are connected
        BundleContext ctxt = Activator.getDefault().getBundle().getBundleContext();
        ctxt.registerService(DbService.class, dbService, null);
    }

}
