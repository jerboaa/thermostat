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

package com.redhat.thermostat.vm.classstat.agent.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;

import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.agent.VmStatusListener.Status;
import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.vm.classstat.common.VmClassStatDAO;

public class VmClassStatBackendTest {
    
    private VmClassStatBackend backend;
    private MonitoredHost host;
    private VmStatusListenerRegistrar registrar;
    private HostIdentifier hostIdentifier;
    private MonitoredVm monitoredVm1;

    @Before
    public void setup() throws MonitorException, URISyntaxException {
        VmClassStatDAO vmClassStatDao = mock(VmClassStatDAO.class);
        
        Version version = mock(Version.class);
        when(version.getVersionNumber()).thenReturn("0.0.0");
        
        registrar = mock(VmStatusListenerRegistrar.class);

        hostIdentifier = mock(HostIdentifier.class);
        when(hostIdentifier.resolve(isA(VmIdentifier.class))).then(new Answer<VmIdentifier>() {
            @Override
            public VmIdentifier answer(InvocationOnMock invocation) throws Throwable {
                return (VmIdentifier) invocation.getArguments()[0];
            }
        });
        host = mock(MonitoredHost.class);
        when(host.getHostIdentifier()).thenReturn(hostIdentifier);

        monitoredVm1 = mock(MonitoredVm.class);

        backend = new VmClassStatBackend(vmClassStatDao, version, registrar);
        
        backend.setHost(host);
    }

    @Test
    public void testActivate() {
        backend.activate();
        assertTrue(backend.isActive());
        verify(registrar).register(backend);
    }

    @Test
    public void testActivateTwice() {
        assertTrue(backend.activate());
        assertTrue(backend.isActive());

        assertTrue(backend.activate());
        assertTrue(backend.isActive());
    }

    @Test
    public void testCanNotActivateWithoutMonitoredHost() {
        backend.setHost(null);

        assertFalse(backend.activate());
        assertFalse(backend.isActive());
    }
    
    @Test
    public void testDeactivate() {
        backend.activate();
        backend.deactivate();
        verify(registrar).unregister(backend);
        assertFalse(backend.isActive());
    }

    @Test
    public void testDeactivateTwice() {
        backend.activate();

        assertTrue(backend.deactivate());
        assertFalse(backend.isActive());
        assertTrue(backend.deactivate());
    }

    @Test
    public void testNewVM() throws MonitorException, URISyntaxException {
        int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm1);

        backend.vmStatusChanged(Status.VM_STARTED, 1);

        verify(monitoredVm1).addVmListener(isA(VmClassStatVmListener.class));
    }

    @Test
    public void testAlreadyRunningVM() throws MonitorException, URISyntaxException {
        int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm1);

        backend.vmStatusChanged(Status.VM_ACTIVE, 1);

        verify(monitoredVm1).addVmListener(isA(VmClassStatVmListener.class));
    }

    @Test
    public void testStatVMGetMonitoredVmFails() throws MonitorException {
        MonitorException monitorException = new MonitorException();
        when(host.getMonitoredVm(isA(VmIdentifier.class))).thenThrow(monitorException);

        backend.vmStatusChanged(Status.VM_STARTED, 1);

        assertFalse(backend.getPidToDataMap().containsKey(1));
    }

    @Test
    public void testStoppedVM() throws MonitorException, URISyntaxException {
        int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm1);

        backend.vmStatusChanged(Status.VM_STARTED, 1);
        backend.vmStatusChanged(Status.VM_STOPPED, 1);

        verify(monitoredVm1).removeVmListener(isA(VmClassStatVmListener.class));
    }

    @Test
    public void testUnknownVMStopped() throws URISyntaxException, MonitorException {
        int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm1);

        backend.vmStatusChanged(Status.VM_STOPPED, 1);

        verifyNoMoreInteractions(monitoredVm1);
    }

    @Test
    public void testErrorRemovingVmListener() throws URISyntaxException, MonitorException {
        int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm1);
        MonitorException monitorException = new MonitorException();
        doThrow(monitorException).when(monitoredVm1).removeVmListener(isA(VmClassStatVmListener.class));

        backend.vmStatusChanged(Status.VM_STARTED, 1);
        backend.vmStatusChanged(Status.VM_STOPPED, 1);

        verify(monitoredVm1).detach();
    }

    @Test
    public void testOrderValue() {
        int orderValue = backend.getOrderValue();
        assertTrue(orderValue > Ordered.ORDER_MEMORY_GROUP);
        assertTrue(orderValue < Ordered.ORDER_NETWORK_GROUP);
    }
}

