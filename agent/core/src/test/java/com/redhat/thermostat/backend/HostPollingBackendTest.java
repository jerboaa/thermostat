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

import com.redhat.thermostat.common.NotImplementedException;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.common.internal.test.Bug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HostPollingBackendTest {

    private HostPollingBackend backend;
    private ScheduledExecutorService mockExecutor;

    @Before
    public void setUp() {
        mockExecutor = mock(ScheduledExecutorService.class);
        Version mockVersion = mock(Version.class);
        when(mockVersion.getVersionNumber()).thenReturn("backend-version");
        backend = new HostPollingBackend("backend-name", "backend-description",
                  "backend-vendor", mockVersion, mockExecutor) {
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
    
    /**
     * If an action throws exceptions repeatedly, that action shall get
     * disabled/unregistered.
     */
    @Bug(id = "3242",
         summary = "Adverse Backend breaks other Backends badly ",
         url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=3242")
    @Test
    public void testDoScheduledActionsWithExceptions() {
        final int beyondExceptionThreshold = 13; // Anything beyond 10 will do
        BadHostPollingAction badAction = new BadHostPollingAction();
        backend.registerAction(badAction);
        for (int i = 0; i < beyondExceptionThreshold; i++) {
            backend.doScheduledActions();
        }
        assertEquals(10, badAction.callCount);
    }

    @Test
    public void verifySetObserveNewJvmThrowsException() {
        try {
            backend.setObserveNewJvm(true);
        } catch (NotImplementedException e) {
            return; // pass
        }
        fail("Should have thrown NotImplementedException");
    }

    @Test
    public void verifyRegisteredActionPerformed() {
        HostPollingAction action = mock(HostPollingAction.class);
        backend.registerAction(action);
        backend.doScheduledActions();
        verify(action).run();
    }

    @Test
    public void verifyMultipleRegisteredActionsPerformed() {
        HostPollingAction action1 = mock(HostPollingAction.class);
        HostPollingAction action2 = mock(HostPollingAction.class);
        backend.registerAction(action1);
        backend.registerAction(action2);
        backend.doScheduledActions();
        verify(action1).run();
        verify(action2).run();
    }

    @Test
    public void verifyUnregisteredActionNotPerformed() {
        HostPollingAction action = mock(HostPollingAction.class);
        backend.registerAction(action);
        backend.unregisterAction(action);
        backend.doScheduledActions();
        verify(action, never()).run();
    }
    
    private static class BadHostPollingAction implements HostPollingAction {
        
        private int callCount;

        @Override
        public void run() {
            callCount++;
            throw new RuntimeException("HostPollingBackend.doScheduledActions() testing!");
        }
        
    }

}
