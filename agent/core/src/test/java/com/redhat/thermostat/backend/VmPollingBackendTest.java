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

package com.redhat.thermostat.backend;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.agent.VmStatusListener.Status;
import com.redhat.thermostat.agent.VmStatusListenerRegistrar;
import com.redhat.thermostat.common.Version;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VmPollingBackendTest {

    private VmPollingBackend backend;
    private ScheduledExecutorService mockExecutor;
    private VmStatusListenerRegistrar mockRegistrar;

    @Before
    public void setUp() {
        mockExecutor = mock(ScheduledExecutorService.class);
        Version mockVersion = mock(Version.class);
        when(mockVersion.getVersionNumber()).thenReturn("backend-version");
        mockRegistrar = mock(VmStatusListenerRegistrar.class);
        backend = new VmPollingBackend("backend-name", "backend-description",
                  "backend-vendor", mockVersion, mockExecutor, mockRegistrar) {
                    @Override
                    public int getOrderValue() {
                        return 0; // Doesn't matter, not being tested.
                    }
        };
        if (!backend.getObserveNewJvm()) {
            /* At time of writing, default is true.  This is
             * inherited from parent PollingBackend.  In case
             * default changes:
             */
            backend.setObserveNewJvm(true);
        }
    }

    @Test
    public void verifyCustomActivateRegistersListener() {
        backend.preActivate();
        verify(mockRegistrar).register(backend);
    }

    @Test
    public void verifyCustomDeactivateUnregistersListener() {
        backend.postDeactivate();
        verify(mockRegistrar).unregister(backend);
    }

    @Test
    public void verifyRegisteredActionPerformed() {
        String vmId = "test-vm-id";
        int pid = 123;
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId, pid);
        VmPollingAction action = mock(VmPollingAction.class);
        backend.registerAction(action);
        backend.doScheduledActions();

        verify(action).run(eq(vmId), eq(pid));
    }

    @Test
    public void verifyMultipleRegisteredActionsPerformed() {
        String vmId = "test-vm-id";
        int pid = 123;
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId, pid);
        VmPollingAction action1 = mock(VmPollingAction.class);
        VmPollingAction action2 = mock(VmPollingAction.class);
        backend.registerAction(action1);
        backend.registerAction(action2);
        backend.doScheduledActions();

        verify(action1).run(eq(vmId), eq(pid));
        verify(action2).run(eq(vmId), eq(pid));
    }

    @Test
    public void verifyActionsPerformedOnMultipleVms() {
        String vmId1 = "test-vm-id1", vmId2 = "test-vm-id2";
        int pid1 = 123, pid2 = 456;
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId1, pid1);
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId2, pid2);
        VmPollingAction action = mock(VmPollingAction.class);
        backend.registerAction(action);
        backend.doScheduledActions();

        verify(action).run(eq(vmId1), eq(pid1));
        verify(action).run(eq(vmId2), eq(pid2));
    }

    @Test
    public void verifyMultipleRegisteredActionsPerformedOnMultipleVms() {
        String vmId1 = "test-vm-id1", vmId2 = "test-vm-id2";
        int pid1 = 123, pid2 = 456;
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId1, pid1);
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId2, pid2);
        VmPollingAction action1 = mock(VmPollingAction.class);
        VmPollingAction action2 = mock(VmPollingAction.class);
        backend.registerAction(action1);
        backend.registerAction(action2);
        backend.doScheduledActions();

        verify(action1).run(eq(vmId1), eq(pid1));
        verify(action1).run(eq(vmId2), eq(pid2));
        verify(action2).run(eq(vmId1), eq(pid1));
        verify(action2).run(eq(vmId2), eq(pid2));
    }

    @Test
    public void verifyUnregisteredActionNotPerformed() {
        String vmId = "test-vm-id";
        int pid = 123;
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId, pid);
        VmPollingAction action1 = mock(VmPollingAction.class);
        VmPollingAction action2 = mock(VmPollingAction.class);
        backend.registerAction(action1);
        backend.registerAction(action2);
        backend.doScheduledActions(); // Triggers both
        backend.unregisterAction(action1);
        backend.doScheduledActions(); // Triggers only action2

        verify(action1, times(1)).run(eq(vmId), eq(pid));
        verify(action2, times(2)).run(eq(vmId), eq(pid));
    }

    @Test
    public void verifyVmStatusChangedStartedAndActiveResultInPolling() {
        String vmId1 = "test-vm-id1", vmId2 = "test-vm-id2";
        int pid1 = 123, pid2 = 456;
        backend.vmStatusChanged(Status.VM_STARTED, vmId1, pid1);
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId2, pid2);
        VmPollingAction action = mock(VmPollingAction.class);
        backend.registerAction(action);
        backend.doScheduledActions();

        verify(action).run(eq(vmId1), eq(pid1));
        verify(action).run(eq(vmId2), eq(pid2));
    }

    @Test
    public void verifyVmStatusChangedStopsResultsInNoMorePolling() {
        String vmId1 = "test-vm-id1", vmId2 = "test-vm-id2";
        int pid1 = 123, pid2 = 456;
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId1, pid1);
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId2, pid2);
        VmPollingAction action = mock(VmPollingAction.class);
        backend.registerAction(action);
        backend.doScheduledActions(); // Triggers for both vms
        backend.vmStatusChanged(Status.VM_STOPPED, vmId1, pid1);
        backend.doScheduledActions(); // Triggers only for vm2

        verify(action, times(1)).run(eq(vmId1), eq(pid1));
        verify(action, times(2)).run(eq(vmId2), eq(pid2));
    }

    @Test
    public void verifyGetSetObserveNewJvmWorksAsExpected() {
        String vmId1 = "test-vm-id1", vmId2 = "test-vm-id2";
        int pid1 = 123, pid2 = 456;
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId1, pid1);
        backend.setObserveNewJvm(false);
        backend.vmStatusChanged(Status.VM_ACTIVE, vmId2, pid2); // Should be ignored.
        VmPollingAction action = mock(VmPollingAction.class);
        backend.registerAction(action);
        backend.doScheduledActions();

        verify(action).run(eq(vmId1), eq(pid1));
        verify(action, never()).run(eq(vmId2), eq(pid2));
    }
}
