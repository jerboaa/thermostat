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

package com.redhat.thermostat.backend.system.internal;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ConcurrentModificationException;

import org.junit.Test;
import org.osgi.framework.BundleContext;

import com.redhat.thermostat.agent.VmStatusListener;
import com.redhat.thermostat.agent.VmStatusListener.Status;
import com.redhat.thermostat.testutils.StubBundleContext;

public class VmStatusChangeNotifierTest {

    @Test
    public void verifyWorksWithoutAnyListeners() {
        final String VM_ID = "vmId";
        final int VM_PID = 2;
        StubBundleContext bundleContext = new StubBundleContext();

        VmStatusChangeNotifier notifier = new VmStatusChangeNotifier(bundleContext);
        notifier.start();
        notifier.notifyVmStatusChange(Status.VM_STARTED, VM_ID, VM_PID);

        notifier.notifyVmStatusChange(Status.VM_STOPPED, VM_ID, VM_PID);
    }

    @Test
    public void verifyAllListenersAreNotified() {
        final String VM_ID = "vmId";
        final int VM_PID = 2;
        StubBundleContext bundleContext = new StubBundleContext();

        VmStatusListener listener = mock(VmStatusListener.class);
        bundleContext.registerService(VmStatusListener.class, listener, null);

        VmStatusChangeNotifier notifier = new VmStatusChangeNotifier(bundleContext);
        notifier.start();
        notifier.notifyVmStatusChange(Status.VM_STARTED, VM_ID, VM_PID);

        verify(listener).vmStatusChanged(Status.VM_STARTED, VM_ID, VM_PID);

        notifier.notifyVmStatusChange(Status.VM_STOPPED, VM_ID, VM_PID);

        verify(listener).vmStatusChanged(Status.VM_STOPPED, VM_ID, VM_PID);
    }

    @Test
    public void verifyListenersAddedAfterVmStartRecieveVmActiveEvent() {
        final String VM_ID = "vmId";
        final int VM_PID = 2;
        StubBundleContext bundleContext = new StubBundleContext();

        VmStatusChangeNotifier notifier = new VmStatusChangeNotifier(bundleContext);
        notifier.start();
        notifier.notifyVmStatusChange(Status.VM_STARTED, VM_ID, VM_PID);

        VmStatusListener listener = mock(VmStatusListener.class);
        bundleContext.registerService(VmStatusListener.class, listener, null);

        verify(listener).vmStatusChanged(Status.VM_ACTIVE, VM_ID, VM_PID);

    }
    
    /*
     * Some backends might activate on activation of another backend. If both of
     * them are also VmStatusListener's, a concurrent modification exception
     * might be thrown. This tests verifies it's OK to do so (concurrent modification).
     */
    @Test
    public void canAddListenersWhileFiringEvent() throws InterruptedException {
        StubBundleContext bundleContext = new StubBundleContext();
        VmStatusChangeNotifier notifier = new VmStatusChangeNotifier(bundleContext);
        
        // Add > 2 listeners. One of them registers another listener in vmStatusChanged()
        // Thus provoking ConcurrentModificationException.
        bundleContext.registerService(VmStatusListener.class, new TestVmStatusListener(bundleContext), null);
        bundleContext.registerService(VmStatusListener.class, new VmStatusListener() {

            @Override
            public void vmStatusChanged(Status newStatus, String vmId,
                    int pid) {
                Debug.println("Second registered listener fired");
            }
            
            @Override
            public int hashCode() {
                return 2; // second listener to be fired
            }
            
        }, null);
        bundleContext.registerService(VmStatusListener.class, new VmStatusListener() {

            @Override
            public void vmStatusChanged(Status newStatus, String vmId,
                    int pid) {
                Debug.println("Third registered listener fired");
            }
            
            @Override
            public int hashCode() {
                return 3;
            }
            
        }, null);
        notifier.start();
        
        try {
            notifier.notifyVmStatusChange(Status.VM_STARTED, "foo-vmid", 333);
            // this will trigger the newly added listener being invoked and counting
            // down the latch.
            notifier.notifyVmStatusChange(Status.VM_STARTED, "foo-other", 9999);
            // pass
        } catch (ConcurrentModificationException e) {
            fail("Unexpected conncurrent modification exception!");
        }
    }
    
    static class TestVmStatusListener implements VmStatusListener {

        private final BundleContext context;
        
        private TestVmStatusListener(BundleContext context) {
            this.context = context;
        }
        
        @Override
        public void vmStatusChanged(Status newStatus, String vmId, int pid) {
            Debug.println("First registered listener fired");
            context.registerService(VmStatusListener.class, new VmStatusListener() {

                @Override
                public void vmStatusChanged(Status newStatus, String vmId,
                        int pid) {
                    Debug.println("Listener registered in listener (between first and second) fired");
                }
                
                @Override
                public int hashCode() {
                    // Pick a large hash code since that tends to result in
                    // the listener being fired later on.
                    return 101;
                }
                
            }, null);
        }
        
        @Override
        public int hashCode() {
            // Heuristic for HashMap it tends to be picked first when iterating over
            return 1;
        }
        
    }
    
    static class Debug {
        
        static final boolean debugOn = false; // set to true for debug output
        
        static void println(String msg) {
            if (debugOn) {
                System.err.println(msg);
            }
        }
    }
}

