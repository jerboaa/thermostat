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
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.redhat.thermostat.common.Version;

public class PollingBackendTest {

    private PollingBackend backend;
    private ScheduledExecutorService mockExecutor;
    private CustomActivateTester mockActivate;

    @Before
    public void setUp() {
        mockExecutor = mock(ScheduledExecutorService.class);
        mockActivate = mock(CustomActivateTester.class);
        Version mockVersion = mock(Version.class);
        when(mockVersion.getVersionNumber()).thenReturn("backend-version");
        backend = new PollingBackend("backend-name", "backend-description",
                  "backend-vendor", mockVersion, mockExecutor) {

            @Override
            public int getOrderValue() {
                return 0; // Doesn't matter, not being tested.
            }

            @Override
            void doScheduledActions() {
                // Won't be called because mock executor.
            }

            @Override
            void preActivate() {
                mockActivate.activate();
            }

            @Override
            void postDeactivate() {
                mockActivate.deactivate();
            }};
    }

    @After
    public void tearDown() {
        backend = null;
    }

    @Test
    public void verifyActivate() {
        backend.activate();
        verify(mockExecutor).scheduleAtFixedRate(any(Runnable.class), eq( (long) 0),
                                                 eq( (long) 1000), eq(TimeUnit.MILLISECONDS));
        verify(mockActivate).activate();
    }

    @Test
    public void verifyNoopActivateWhenAlreadyActive() {
        backend.setActive(true);
        backend.activate();
        verifyNoMoreInteractions(mockExecutor);
        verifyNoMoreInteractions(mockActivate);
    }

    @Test
    public void verifyDeactivate() {
        backend.setActive(true);
        backend.deactivate();
        verify(mockActivate).deactivate();
        verify(mockExecutor).shutdown();
    }

    @Test
    public void verifyNoopDeactivateWhenNotActive() {
        backend.deactivate();
        verifyNoMoreInteractions(mockExecutor);
        verifyNoMoreInteractions(mockActivate);
    }

    @Test
    public void verifyScheduledRunnableActuallyRunsScheduledAction() {
        final ScheduledActionTester mockAction = mock(ScheduledActionTester.class);
        Version mockVersion = mock(Version.class);
        when(mockVersion.getVersionNumber()).thenReturn("backend-version");
        ScheduledExecutorService mockExecutor = mock(ScheduledExecutorService.class);
        backend = new PollingBackend("backend-name", "backend-description",
                  "backend-vendor", mockVersion, mockExecutor) {

            @Override
            public int getOrderValue() {
                return 0;
            }

            @Override
            void doScheduledActions() {
                mockAction.doAction();
            }};
        backend.activate();
        ArgumentCaptor<Runnable> scheduledRunnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockExecutor).scheduleAtFixedRate(scheduledRunnableCaptor.capture(), eq( (long) 0),
                eq( (long) 1000), eq(TimeUnit.MILLISECONDS));
        Runnable scheduledRunnable = scheduledRunnableCaptor.getValue();
        scheduledRunnable.run();
        verify(mockAction).doAction();
        backend.deactivate();
        verify(mockExecutor).shutdown();
        verifyNoMoreInteractions(mockAction);
    }

    private interface CustomActivateTester {
        public void activate();
        public void deactivate();
    }

    private interface ScheduledActionTester {
        public void doAction();
    }
}
