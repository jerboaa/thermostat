/*
 * Copyright 2013 Red Hat, Inc.
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

package com.redhat.thermostat.backend.system;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListener.Status;
import com.redhat.thermostat.test.StubBundleContext;

public class VmStatusChangeNotifierTest {

    @Test
    public void verifyWorksWithoutAnyListeners() {
        final int VM_ID = 2;
        StubBundleContext bundleContext = new StubBundleContext();

        VmStatusChangeNotifier notifier = new VmStatusChangeNotifier(bundleContext);
        notifier.start();
        notifier.notifyVmStatusChange(Status.VM_STARTED, VM_ID);

        notifier.notifyVmStatusChange(Status.VM_STOPPED, VM_ID);
    }

    @Test
    public void verifyAllListenersAreNotified() {
        final int VM_ID = 2;
        StubBundleContext bundleContext = new StubBundleContext();

        VmStatusListener listener = mock(VmStatusListener.class);
        bundleContext.registerService(VmStatusListener.class, listener, null);

        VmStatusChangeNotifier notifier = new VmStatusChangeNotifier(bundleContext);
        notifier.start();
        notifier.notifyVmStatusChange(Status.VM_STARTED, VM_ID);

        verify(listener).vmStatusChanged(Status.VM_STARTED, VM_ID);

        notifier.notifyVmStatusChange(Status.VM_STOPPED, VM_ID);

        verify(listener).vmStatusChanged(Status.VM_STOPPED, VM_ID);
    }

    @Test
    public void verifyListenersAddedAfterVmStartRecieveVmActiveEvent() {
        final int VM_ID = 2;
        StubBundleContext bundleContext = new StubBundleContext();

        VmStatusChangeNotifier notifier = new VmStatusChangeNotifier(bundleContext);
        notifier.start();
        notifier.notifyVmStatusChange(Status.VM_STARTED, VM_ID);

        VmStatusListener listener = mock(VmStatusListener.class);
        bundleContext.registerService(VmStatusListener.class, listener, null);

        verify(listener).vmStatusChanged(Status.VM_ACTIVE, VM_ID);

    }
}
