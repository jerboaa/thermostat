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

package com.redhat.thermostat.vm.memory.agent.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;
import sun.jvmstat.monitor.event.VmListener;

import com.redhat.thermostat.agent.VmStatusListener.Status;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.vm.memory.common.VmMemoryStatDAO;

public class VmMemoryBackendTest {
    
    private VmMemoryBackend backend;
    private MonitoredHost host;
    private VmStatusListenerRegistrar registrar;
    private VmMemoryStatDAO vmMemoryStatDao;

    @Before
    public void setup() throws MonitorException, URISyntaxException {
        vmMemoryStatDao = mock(VmMemoryStatDAO.class);
        
        Version version = mock(Version.class);
        when(version.getVersionNumber()).thenReturn("0.0.0");

        registrar = mock(VmStatusListenerRegistrar.class);

        backend = new VmMemoryBackend(vmMemoryStatDao, version, registrar);
        
        HostIdentifier hostIdentifier = mock(HostIdentifier.class);
        when(hostIdentifier.resolve(isA(VmIdentifier.class))).then(new Answer<VmIdentifier>() {
            @Override
            public VmIdentifier answer(InvocationOnMock invocation) throws Throwable {
                return (VmIdentifier) invocation.getArguments()[0];
            }
        });
        host = mock(MonitoredHost.class);
        when(host.getHostIdentifier()).thenReturn(hostIdentifier);

        backend.setHost(host);
    }

    @Test
    public void testActivate() {
        assertTrue(backend.activate());

        verify(registrar).register(backend);
        assertTrue(backend.isActive());
    }

    @Test
    public void testActivateTwice() {
        assertTrue(backend.activate());
        assertTrue(backend.isActive());

        assertTrue(backend.activate());
        assertTrue(backend.isActive());

        verify(registrar).register(backend);
    }

    @Test
    public void testActivateFailsIfHostIsNull() {
        backend.setHost(null);

        assertFalse(backend.activate());
    }

    @Test
    public void testDeactivate() {
        backend.activate();
        backend.deactivate();

        verify(registrar).unregister(backend);
        assertFalse(backend.isActive());
    }

    @Test
    public void testDeactiveTwice() {
        assertTrue(backend.activate());

        assertTrue(backend.deactivate());
        assertFalse(backend.isActive());

        assertTrue(backend.deactivate());
        assertFalse(backend.isActive());

        verify(registrar).unregister(backend);
    }

    @Test
    public void testNewVmStarted() throws URISyntaxException, MonitorException {
        int VM_PID = 10;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        MonitoredVm monitoredVm = mock(MonitoredVm.class);

        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);

        backend.vmStatusChanged(Status.VM_STARTED, VM_PID);

        verify(monitoredVm).addVmListener(isA(VmMemoryVmListener.class));
    }

    @Test
    public void testErrorInAttachingToNewVm() throws MonitorException, URISyntaxException {
        int VM_PID = 10;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));

        when(host.getMonitoredVm(VM_ID)).thenThrow(new MonitorException());

        backend.vmStatusChanged(Status.VM_STARTED, VM_PID);
    }

    @Test
    public void testVmStopped() throws URISyntaxException, MonitorException {
        int VM_PID = 10;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        MonitoredVm monitoredVm = mock(MonitoredVm.class);

        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);

        backend.vmStatusChanged(Status.VM_STARTED, VM_PID);

        ArgumentCaptor<VmListener> listenerCaptor = ArgumentCaptor.forClass(VmListener.class);
        verify(monitoredVm).addVmListener(listenerCaptor.capture());

        backend.vmStatusChanged(Status.VM_STOPPED, VM_PID);

        verify(monitoredVm).removeVmListener(listenerCaptor.getValue());
        verify(monitoredVm).detach();
    }

    @Test
    public void testUnknownVmStoppedIsIgnored() {
        int VM_PID = 10;

        backend.vmStatusChanged(Status.VM_STOPPED, VM_PID);

        verifyNoMoreInteractions(host, vmMemoryStatDao);
    }

    @Test
    public void testStoppedVmIsDetachedEvenInPresenceOfErrors() throws URISyntaxException, MonitorException {
        int VM_PID = 10;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        MonitoredVm monitoredVm = mock(MonitoredVm.class);

        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);

        backend.vmStatusChanged(Status.VM_STARTED, VM_PID);

        ArgumentCaptor<VmListener> listenerCaptor = ArgumentCaptor.forClass(VmListener.class);
        verify(monitoredVm).addVmListener(listenerCaptor.capture());

        VmListener vmListener = listenerCaptor.getValue();
        doThrow(new MonitorException("test")).when(monitoredVm).removeVmListener(vmListener);

        backend.vmStatusChanged(Status.VM_STOPPED, VM_PID);

        verify(monitoredVm).detach();
    }

    @Test
    public void testOrderValue() {
        int orderValue = backend.getOrderValue();

        assertTrue(orderValue > Ordered.ORDER_MEMORY_GROUP);
        assertTrue(orderValue < Ordered.ORDER_NETWORK_GROUP);
    }
}

