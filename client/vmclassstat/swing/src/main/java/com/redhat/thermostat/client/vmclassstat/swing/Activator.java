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

package com.redhat.thermostat.client.vmclassstat.swing;

import java.util.Map;
import java.util.Objects;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.redhat.thermostat.client.core.VmInformationService;
import com.redhat.thermostat.client.osgi.service.ApplicationService;
import com.redhat.thermostat.client.vmclassstat.core.VmClassStatService;
import com.redhat.thermostat.client.vmclassstat.core.VmClassStatViewProvider;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.Action;
import com.redhat.thermostat.common.dao.VmClassStatDAO;

public class Activator implements BundleActivator {

    private MultipleServiceTracker classStatTracker;
    private ServiceRegistration classStatRegistration;

    @Override
    public void start(final BundleContext context) throws Exception {
        VmClassStatViewProvider viewProvider = new SwingVmClassStatViewProvider();
        context.registerService(VmClassStatViewProvider.class.getName(), viewProvider, null);

        Class<?>[] deps = new Class<?>[] {
            ApplicationService.class,
            VmClassStatDAO.class,
        };

        classStatTracker = new MultipleServiceTracker(context, deps, new Action() {

            @Override
            public void dependenciesAvailable(Map<String, Object> services) {
                VmClassStatDAO dao = (VmClassStatDAO) services.get(VmClassStatDAO.class.getName());
                Objects.requireNonNull(dao);
                VmClassStatService service = new VmClassStatService(dao);
                classStatRegistration = context.registerService(VmInformationService.class.getName(), service, null);
            }

            @Override
            public void dependenciesUnavailable() {
                classStatRegistration.unregister();
            }

        });
        classStatTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        classStatTracker.close();
    }
}
