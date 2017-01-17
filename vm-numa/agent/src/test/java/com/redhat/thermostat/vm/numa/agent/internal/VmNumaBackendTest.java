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

package com.redhat.thermostat.vm.numa.agent.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.storage.core.WriterID;
import com.redhat.thermostat.vm.numa.common.VmNumaDAO;
import com.redhat.thermostat.vm.numa.common.VmNumaStat;

public class VmNumaBackendTest {
    private VmNumaBackend backend;
    private VmNumaDAO vmNumaDAO;
    private ScheduledExecutorService executor;
    private VmStatusListenerRegistrar registrar;

    @Before
    public void setup() {
        executor = mock(ScheduledExecutorService.class);
        vmNumaDAO = mock(VmNumaDAO.class);

        Version version = mock(Version.class);
        when(version.getVersionNumber()).thenReturn("0.0.0");

        registrar = mock(VmStatusListenerRegistrar.class);

        WriterID id = mock(WriterID.class);
        when(id.getWriterID()).thenReturn("id");
        backend = new VmNumaBackend(executor, vmNumaDAO, version, registrar, id);
    }

    @Test
    public void testActivate() {
        backend.activate();

        verify(executor).scheduleAtFixedRate(isA(Runnable.class), eq(0l), eq(1000l), eq(TimeUnit.MILLISECONDS));
        verify(registrar).register(backend);
        assertTrue(backend.isActive());
    }

    @Test
    public void testActivateTwice() {
        assertTrue(backend.activate());
        assertTrue(backend.isActive());
        assertTrue(backend.activate());
        assertTrue(backend.isActive());

        assertTrue(backend.deactivate());
    }

    @Test
    public void testDeactivate() {
        backend.activate();
        backend.deactivate();

        verify(executor).shutdown();
        verify(registrar).unregister(backend);
        assertFalse(backend.isActive());
    }


    @Test
    public void testDeactivateTwice() {
        assertTrue(backend.activate());
        assertTrue(backend.isActive());

        assertTrue(backend.deactivate());
        assertFalse(backend.isActive());
        assertTrue(backend.deactivate());
        assertFalse(backend.isActive());
    }

    @Test
    public void testStart() throws ParseException {
        mockCollector(backend, 0);
        mockCollector(backend, 1);

        backend.activate();

        verify(registrar).register(backend);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleAtFixedRate(captor.capture(), any(Long.class), any(Long.class), any(TimeUnit.class));
        assertTrue(backend.isActive());

        backend.vmStatusChanged(VmStatusListener.Status.VM_ACTIVE, "vm1", 0);
        backend.vmStatusChanged(VmStatusListener.Status.VM_STARTED, "vm2", 1);

        Runnable runnable = captor.getValue();
        runnable.run();

        verify(vmNumaDAO, times(2)).putVmNumaStat(any(VmNumaStat.class));

        backend.vmStatusChanged(VmStatusListener.Status.VM_STOPPED, "vm1", 0);
        backend.vmStatusChanged(VmStatusListener.Status.VM_STOPPED, "vm2", 1);

        runnable.run();
        verifyNoMoreInteractions(vmNumaDAO);
    }

    private void mockCollector(VmNumaBackend backend, int pid) throws ParseException {
        VmNumaCollector collector = mock(VmNumaCollector.class);
        when(collector.collect()).thenReturn(mock(VmNumaStat.class));
        backend.setVmNumaBackendCollector(pid, collector);
    }

    @Test
    public void testOrderValue() {
        int orderValue = backend.getOrderValue();

        assertTrue(orderValue == Ordered.ORDER_MEMORY_GROUP);
    }
}
