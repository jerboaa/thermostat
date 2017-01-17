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

package com.redhat.thermostat.notes.client.core.internal;

import com.redhat.thermostat.client.core.InformationService;
import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Constants;
import com.redhat.thermostat.common.MultipleServiceTracker;
import com.redhat.thermostat.common.MultipleServiceTracker.DependencyProvider;
import com.redhat.thermostat.common.SystemClock;
import com.redhat.thermostat.notes.client.core.HostNotesControllerProvider;
import com.redhat.thermostat.notes.client.core.NotesViewProvider;
import com.redhat.thermostat.notes.client.core.VmNotesControllerProvider;
import com.redhat.thermostat.notes.common.HostNoteDAO;
import com.redhat.thermostat.notes.common.VmNoteDAO;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Hashtable;
import java.util.Objects;

public class Activator implements BundleActivator {

    @SuppressWarnings("rawtypes")
    private ServiceRegistration<InformationService> vmNotesRegistration;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration<InformationService> hostNotesRegistration;

    private MultipleServiceTracker hostNotesDaoTracker;
    private MultipleServiceTracker vmNotesDaoTracker;

    @Override
    public void start(final BundleContext context) {
        Class<?>[] hostDeps = new Class<?>[] {
                HostNoteDAO.class,
                ApplicationService.class,
                NotesViewProvider.class
        };
        hostNotesDaoTracker = new MultipleServiceTracker(context, hostDeps, new MultipleServiceTracker.Action() {
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                ApplicationService appSvc = services.get(ApplicationService.class);
                HostNoteDAO hostNoteDao = services.get(HostNoteDAO.class);
                NotesViewProvider viewProvider = services.get(NotesViewProvider.class);
                HostNotesControllerProvider hostNotesService = new HostNotesControllerProvider(new SystemClock(), appSvc, hostNoteDao, viewProvider);
                Hashtable<String, String> properties = new Hashtable<>();
                properties.put(Constants.GENERIC_SERVICE_CLASSNAME, HostRef.class.getName());
                properties.put(InformationService.KEY_SERVICE_ID, hostNotesService.getClass().getName());
                hostNotesRegistration = context.registerService(InformationService.class, hostNotesService, properties);
            }
            @Override
            public void dependenciesUnavailable() {
                hostNotesRegistration.unregister();
                hostNotesRegistration = null;
            }
        });
        hostNotesDaoTracker.open();

        Class<?>[] vmDeps = new Class<?>[]{
                VmNoteDAO.class,
                ApplicationService.class,
                NotesViewProvider.class
        };
        vmNotesDaoTracker = new MultipleServiceTracker(context, vmDeps, new MultipleServiceTracker.Action() {
            @Override
            public void dependenciesAvailable(DependencyProvider services) {
                ApplicationService appSvc = services.get(ApplicationService.class);
                VmNoteDAO vmNoteDao = services.get(VmNoteDAO.class);
                NotesViewProvider viewProvider = services.get(NotesViewProvider.class);
                VmNotesControllerProvider notesService = new VmNotesControllerProvider(new SystemClock(), appSvc, vmNoteDao, viewProvider);
                Hashtable<String, String> properties = new Hashtable<>();
                properties.put(Constants.GENERIC_SERVICE_CLASSNAME, VmRef.class.getName());
                properties.put(InformationService.KEY_SERVICE_ID, notesService.getClass().getName());
                vmNotesRegistration = context.registerService(InformationService.class, notesService, properties);
            }

            @Override
            public void dependenciesUnavailable() {
                vmNotesRegistration.unregister();
                vmNotesRegistration = null;
            }
        });
        vmNotesDaoTracker.open();
    }

    @Override
    public void stop(BundleContext context) {
        hostNotesDaoTracker.close();
        vmNotesDaoTracker.close();
    }

}
