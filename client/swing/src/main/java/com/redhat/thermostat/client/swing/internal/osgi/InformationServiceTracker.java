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

package com.redhat.thermostat.client.swing.internal.osgi;

import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.client.ui.UiFacadeFactory;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.dao.HostRef;
import com.redhat.thermostat.common.dao.VmRef;
import com.redhat.thermostat.common.utils.LoggingUtils;

@SuppressWarnings("rawtypes")
public class InformationServiceTracker extends ServiceTracker {

    private UiFacadeFactory uiFacadeFactory;
    private static final Logger logger = LoggingUtils.getLogger(InformationServiceTracker.class);

    @SuppressWarnings("unchecked")
    public InformationServiceTracker(BundleContext context, UiFacadeFactory uiFacadeFactory) {
        super(context, InformationService.class.getName(), null);
        this.uiFacadeFactory = uiFacadeFactory;
    }

    @Override
    public Object addingService(ServiceReference reference) {
        Object service = super.addingService(reference);
        String genericType = (String) reference.getProperty(Constants.GENERIC_SERVICE_CLASSNAME);
        if (genericType.equals(HostRef.class.getName())) {
            uiFacadeFactory.addHostInformationService((InformationService<HostRef>) service);
        } else if (genericType.equals(VmRef.class.getName())) {
            uiFacadeFactory.addVmInformationService((InformationService<VmRef>) service);
        } else {
            logUnknownGenericServiceType(genericType);
        }
        return service;
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        String genericType = (String) reference.getProperty(Constants.GENERIC_SERVICE_CLASSNAME);
        if (genericType.equals(HostRef.class.getName())) {
            uiFacadeFactory.removeHostInformationService((InformationService<HostRef>) service);
        } else if (genericType.equals(VmRef.class.getName())) {
            uiFacadeFactory.removeVmInformationService((InformationService<VmRef>) service);
        } else {
            logUnknownGenericServiceType(genericType);
        }
        super.removedService(reference, service);
    }

    private void logUnknownGenericServiceType(String genericType) {
        logger.warning("InformationServiceTracker encountered an unknown generic type: " + genericType);
    }
}

