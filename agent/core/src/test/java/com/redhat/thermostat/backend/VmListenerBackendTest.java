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

package com.redhat.thermostat.backend;

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

public class VmListenerBackendTest {
    
    private VmListenerBackend backend;
    private HostIdentifier hostIdentifier;
    private MonitoredHost host;
    private MonitoredVm monitoredVm;
    private VmListener listener;
    private VmStatusListenerRegistrar registrar;

    @Before
    public void setup() throws URISyntaxException, MonitorException {
        BackendID id = mock(BackendID.class);
        registrar = mock(VmStatusListenerRegistrar.class);
        listener = mock(VmListener.class);
        
        hostIdentifier = mock(HostIdentifier.class);
        when(hostIdentifier.resolve(isA(VmIdentifier.class))).then(new Answer<VmIdentifier>() {
            @Override
            public VmIdentifier answer(InvocationOnMock invocation) throws Throwable {
                return (VmIdentifier) invocation.getArguments()[0];
            }
        });
        host = mock(MonitoredHost.class);
        when(host.getHostIdentifier()).thenReturn(hostIdentifier);
        
        monitoredVm = mock(MonitoredVm.class);
        
        backend = new TestBackend(id, registrar);
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
        final int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);

        backend.vmStatusChanged(Status.VM_STARTED, 1);

        verify(monitoredVm).addVmListener(listener);
    }

    @Test
    public void testAlreadyRunningVM() throws MonitorException, URISyntaxException {
        final int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);

        backend.vmStatusChanged(Status.VM_ACTIVE, 1);

        verify(monitoredVm).addVmListener(listener);
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
        final int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);

        backend.vmStatusChanged(Status.VM_STARTED, 1);
        backend.vmStatusChanged(Status.VM_STOPPED, 1);

        verify(monitoredVm).removeVmListener(listener);
    }

    @Test
    public void testUnknownVMStopped() throws URISyntaxException, MonitorException {
        final int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);

        backend.vmStatusChanged(Status.VM_STOPPED, 1);

        verifyNoMoreInteractions(monitoredVm);
    }

    @Test
    public void testErrorRemovingVmListener() throws URISyntaxException, MonitorException {
        final int VM_PID = 1;
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);
        MonitorException monitorException = new MonitorException();
        doThrow(monitorException).when(monitoredVm).removeVmListener(listener);

        backend.vmStatusChanged(Status.VM_STARTED, 1);
        backend.vmStatusChanged(Status.VM_STOPPED, 1);

        verify(monitoredVm).detach();
    }
    
    @Test
    public void testDeactivateUnregistersListener() throws URISyntaxException, MonitorException {
        final int VM_PID = 1;
        backend.activate();
        
        VmIdentifier VM_ID = new VmIdentifier(String.valueOf(VM_PID));
        when(host.getMonitoredVm(VM_ID)).thenReturn(monitoredVm);

        backend.vmStatusChanged(Status.VM_STARTED, 1);
        backend.deactivate();
        verify(monitoredVm).removeVmListener(listener);
    }
    
    private class TestBackend extends VmListenerBackend {

        public TestBackend(BackendID id, VmStatusListenerRegistrar registrar) {
            super(id, registrar);
        }

        @Override
        public int getOrderValue() {
            return 0;
        }

        @Override
        protected VmListener createVmListener(int pid) {
            return listener;
        }

        @Override
        public boolean attachToNewProcessByDefault() {
            return true;
        }
        
    }

}
