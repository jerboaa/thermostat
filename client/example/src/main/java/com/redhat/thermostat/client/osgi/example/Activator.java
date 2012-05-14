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

package com.redhat.thermostat.client.osgi.example;

import java.util.Collection;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.HostRef;

public class Activator implements BundleActivator {

    @Override
    public void start(final BundleContext context) throws Exception {
        Thread thread = new Thread() {
            public void run() {
                ServiceReference ref = null;
                while (ref == null) {
                    ref = context.getServiceReference(ApplicationService.class.getName());
                }
                ApplicationService appService = (ApplicationService) context.getService(ref);
                final DAOFactory daoFactory = appService.getDAOFactory();
                // TODO: Instead of actively waiting for the DAOFActory to establish a connection,
                // we should have a DAOService that will only be activate when the DB connection
                // comes up, and have this bundle wait for it by means of a ServiceTracker.
                while (true) {
                    try {
                        Collection<HostRef> hosts = daoFactory.getHostInfoDAO().getHosts();
                        for (HostRef host : hosts) {
                            System.out.println("host: " + host);
                        }
                        break;
                    } catch (Exception ex) {
                        try {
			    Thread.sleep(100);
			} catch (InterruptedException ie) {
			    break;
			}
			    
                    }
                }
	    }
	};
        thread.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        // Nothing to do here.
    }

}
