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

package com.redhat.thermostat.backend;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import sun.jvmstat.monitor.MonitorException;

import com.redhat.thermostat.agent.VmStatusListener.Status;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.backend.internal.VmMonitor;
import com.redhat.thermostat.common.internal.test.Bug;
import com.redhat.thermostat.storage.core.WriterID;

public class VmListenerBackendTest {
    private static final String VM_ID = "vmId";
    private static final int VM_PID = 1;
    
    private VmListenerBackend backend;
    private VmStatusListenerRegistrar registrar;
    private VmMonitor monitor;
    private VmUpdateListener listener;

    @Before
    public void setup() {
        registrar = mock(VmStatusListenerRegistrar.class);
        WriterID id = mock(WriterID.class);
        backend = new TestBackend("Test Backend", "Backend for test", "Test Co.",
                "0.0.0", registrar, id);
        monitor = mock(VmMonitor.class);
        listener = mock(VmUpdateListener.class);
        backend.setMonitor(monitor);
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
    public void testCanNotActivateWithoutMonitor() {
        backend.setMonitor(null);

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
    public void testNewVM() {
        // Should be no response if not observing new jvm.
        backend.setObserveNewJvm(false);
        backend.vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);
        verify(monitor, times(0)).handleNewVm(same(listener), same(VM_PID));

        backend.setObserveNewJvm(true);
        backend.vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);
        verify(monitor).handleNewVm(listener, VM_PID);
    }
    
    /**
     * createVmListener() might be plugin-supplied code. When creating the
     * listener fails due to exceptions, other listeners should still continue
     * to work. That is, the exception of creating one listener must not be
     * propagated.
     */
    @Bug(id = "3242",
         summary = "Adverse Backend breaks other Backends badly ",
         url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=3242")
    @Test
    public void testNewVMCreateListenerWithExceptions() {
        VmStatusListenerRegistrar customRegistrar = mock(VmStatusListenerRegistrar.class);
        WriterID wid = mock(WriterID.class);
        VmListenerBackend testBackend = new ExceptionThrowingCreateVmListenerBackend(
                "Test Backend", "Backend for test", "Test Co.",
                "0.0.0", customRegistrar, wid);
        testBackend.setObserveNewJvm(true);
        VmMonitor testMonitor = mock(VmMonitor.class);
        testBackend.setMonitor(testMonitor);
        testBackend.vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);
        verify(testMonitor, times(0)).handleNewVm(any(VmUpdateListener.class), any(int.class));
    }

    @Test
    public void testAlreadyRunningVM() {
        backend.setObserveNewJvm(true);
        backend.vmStatusChanged(Status.VM_ACTIVE, VM_ID, VM_PID);

        verify(monitor).handleNewVm(listener, VM_PID);
    }

    @Test
    public void testStoppedVM() throws MonitorException, URISyntaxException {
        backend.setObserveNewJvm(true);
        backend.vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);
        backend.vmStatusChanged(Status.VM_STOPPED, VM_ID, VM_PID);

        verify(monitor).handleStoppedVm(VM_PID);
    }

    @Test
    public void testDeactivateUnregistersListener() throws URISyntaxException, MonitorException {
        backend.activate();
        
        backend.setObserveNewJvm(true);
        backend.vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);
        backend.deactivate();
        verify(monitor).removeVmListeners();
    }
    
    private class TestBackend extends VmListenerBackend {

        public TestBackend(String name, String description, String vendor,
                String version, VmStatusListenerRegistrar registrar, WriterID writerId) {
            super(name, description, vendor, version, registrar, writerId);
        }

        @Override
        public int getOrderValue() {
            return 0;
        }

        @Override
        protected VmUpdateListener createVmListener(String writerId, String vmId, int pid) {
            return listener;
        }
        
    }
    
    private class ExceptionThrowingCreateVmListenerBackend extends TestBackend {
        
        public ExceptionThrowingCreateVmListenerBackend(String name, String description, String vendor,
                String version, VmStatusListenerRegistrar registrar, WriterID writerId) {
            super(name, description, vendor, version, registrar, writerId);
        }
        
        @Override
        protected VmUpdateListener createVmListener(String writerId, String vmId, int pid) {
            throw new RuntimeException("createVmListener() testing!");
        }
    }

}

