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

package com.redhat.thermostat.utils.management.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.redhat.thermostat.common.LaunchException;
import com.redhat.thermostat.utils.management.internal.MXBeanConnectionPoolTracker.LatchCreator;

public class MXBeanConnectionPoolTrackerTest {
    
    private MXBeanConnectionPoolTracker tracker;
    private BundleContext context;
    private CountDownLatch latch;
    private LatchCreator latchCreator;
    private MXBeanConnectionPoolControl pool;
    private ServiceReference ref;
    
    @Before
    public void setUp() {
        context = mock(BundleContext.class);
        latchCreator = mock(LatchCreator.class);
        latch = mock(CountDownLatch.class);
        when(latchCreator.create()).thenReturn(latch);
        tracker = new MXBeanConnectionPoolTracker(context, latchCreator);
        pool = mock(MXBeanConnectionPoolControl.class);
        ref = mock(ServiceReference.class);
    }

    @Test
    public void testAddingService() {
        Object result = addService();
        
        assertEquals(pool, result);
        assertEquals(pool, tracker.getPool());
        verify(latch).countDown();
    }

    private Object addService() {
        when(context.getService(ref)).thenReturn(pool);
        Object result = tracker.addingService(ref);
        return result;
    }

    @Test
    public void testRemovedService() {
        addService();
        tracker.removedService(ref, pool);
        assertNull(tracker.getPool());
        verify(context).ungetService(ref); // Ensures super called
        verify(latchCreator, times(2)).create();
    }

    @Test
    public void testGetPoolWithTimeout() throws Exception {
        addService();
        MXBeanConnectionPoolControl result = tracker.getPoolWithTimeout();
        assertEquals(pool, result);
        verify(latch).await(anyLong(), any(TimeUnit.class));
    }
    
    @Test
    public void testGetPoolWithTimeoutNoPool() throws Exception {
        try {
            tracker.getPoolWithTimeout();
            fail("Expected LaunchException");
        } catch (LaunchException ignored) {
            verify(latch).await(anyLong(), any(TimeUnit.class));
        }
    }
    
}
