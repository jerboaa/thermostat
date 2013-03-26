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

package com.redhat.thermostat.thread.harvester;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.agent.VmStatusListener.Status;
import com.redhat.thermostat.agent.command.ReceiverRegistry;
import com.redhat.thermostat.common.Version;

public class ThreadBackendTest {

    private Version version;
    private VmStatusListenerRegistrar vmListenerRegistrar;
    private ReceiverRegistry receiverRegistry;
    private ThreadHarvester threadHarvester;

    private ThreadBackend backend;

    @Before
    public void setUp() {
        version = mock(Version.class);
        when(version.getVersionNumber()).thenReturn("foobar");
        vmListenerRegistrar = mock(VmStatusListenerRegistrar.class);
        receiverRegistry = mock(ReceiverRegistry.class);
        threadHarvester = mock(ThreadHarvester.class);

        backend = new ThreadBackend(version, vmListenerRegistrar, receiverRegistry, threadHarvester);
    }

    @Test
    public void testActivate() {
        assertTrue(backend.activate());

        verify(vmListenerRegistrar).register(backend);
        verify(receiverRegistry).registerReceiver(threadHarvester);
    }

    @Test
    public void testDeactivate() {
        assertTrue(backend.activate());
        assertTrue(backend.isActive());
        assertTrue(backend.deactivate());
        assertFalse(backend.isActive());

        verify(vmListenerRegistrar).unregister(backend);
        verify(receiverRegistry).unregisterReceivers();
    }

    @Test
    public void testVmStarts() {
        backend.vmStatusChanged(Status.VM_STARTED, 10);

        verify(threadHarvester).saveVmCaps("10");
        verify(threadHarvester).addThreadHarvestingStatus("10");
    }

    @Test
    public void testVmStops() {
        backend.vmStatusChanged(Status.VM_STOPPED, 10);

        verify(threadHarvester).stopHarvester("10");
        verify(threadHarvester).addThreadHarvestingStatus("10");
    }
}
