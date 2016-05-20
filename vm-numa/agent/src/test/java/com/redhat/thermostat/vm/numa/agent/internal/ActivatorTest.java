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

package com.redhat.thermostat.vm.numa.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.backend.Backend;
import com.redhat.thermostat.backend.BackendService;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.testutils.StubBundleContext;
import com.redhat.thermostat.vm.numa.common.VmNumaDAO;

public class ActivatorTest {
    @Test
    public void verifyActivatorDoesNotRegisterServiceOnMissingDeps() throws Exception {
        StubBundleContext context = new StubBundleContext();

        Activator activator = new Activator();

        activator.start(context);

        assertEquals(0, context.getAllServices().size());
        assertEquals(3, context.getServiceListeners().size());

        activator.stop(context);
    }

    @Test
    public void verifyActivatorRegistersServices() throws Exception {
        StubBundleContext context = new StubBundleContext() {
            @Override
            public Bundle getBundle() {
                Bundle result = mock(Bundle.class);
                when(result.getVersion()).thenReturn(Version.emptyVersion);
                return result;
            }
        };
        BackendService service = mock(BackendService.class);
        VmNumaDAO vmNumaDAO = mock(VmNumaDAO.class);
        WriterID idService = mock(WriterID.class);

        context.registerService(BackendService.class.getName(), service, null);
        context.registerService(VmNumaDAO.class, vmNumaDAO, null);
        context.registerService(WriterID.class, idService, null);

        final VmNumaBackend[] mock = new VmNumaBackend[1];

        Activator activator = new Activator() {
            @Override
            VmNumaBackend constructBackend(ScheduledExecutorService executor, VmNumaDAO vmNumaDAO, com.redhat.thermostat.common.Version version, VmStatusListenerRegistrar registrar, WriterID writerID) {
                mock[0] = new VmNumaBackend(executor, vmNumaDAO, version, registrar, writerID)
                {
                    @Override
                    public boolean canRegister() {
                        return true;
                    }
                };
                return mock[0];
            }
        };

        activator.start(context);

        assertTrue(context.isServiceRegistered(Backend.class.getName(), mock[0].getClass()));
        VmNumaBackend backend = activator.getBackend();
        assertNotNull(backend);

        // core thermostat will activate the backend once it's registered
        backend.activate();

        activator.stop(context);

        assertFalse(backend.isActive());

        assertEquals(0, context.getServiceListeners().size());
        assertEquals(3, context.getAllServices().size());
    }
}
