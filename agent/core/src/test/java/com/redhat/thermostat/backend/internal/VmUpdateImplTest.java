/*
 * Copyright 2012-2015 Red Hat, Inc.
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

package com.redhat.thermostat.backend.internal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;

import sun.jvmstat.monitor.Monitor;

import com.redhat.thermostat.backend.VmUpdateException;

public class VmUpdateImplTest {

    private VmUpdateImpl update;
    private VmListenerWrapper wrapper;

    @Before
    public void setUp() throws Exception {
        wrapper = mock(VmListenerWrapper.class);
        update = new VmUpdateImpl(wrapper);
    }

    @Test
    public void testGetPerformanceCounterLong() throws VmUpdateException {
        final String counter = "myCounter";
        final Long value = 9001L;
        Monitor monitor = mock(Monitor.class);
        when(monitor.getValue()).thenReturn(value);
        when(wrapper.getMonitor(counter)).thenReturn(monitor);
        
        Long result = update.getPerformanceCounterLong(counter);
        assertEquals(value, result);
    }
    
    @Test(expected=ClassCastException.class)
    public void testGetPerformanceCounterLongBadType() throws VmUpdateException {
        final String counter = "myCounter";
        final String value = "myValue";
        Monitor monitor = mock(Monitor.class);
        when(monitor.getValue()).thenReturn(value);
        when(wrapper.getMonitor(counter)).thenReturn(monitor);
        
        update.getPerformanceCounterLong(counter);
    }
    
    @Test
    public void testGetPerformanceCounterLongNoCounter() throws VmUpdateException {
        final String counter = "myCounter";
        when(wrapper.getMonitor(counter)).thenReturn(null);
        
        Long result = update.getPerformanceCounterLong(counter);
        assertNull(result);
    }

    @Test
    public void testGetPerformanceCounterString() throws VmUpdateException {
        final String counter = "myCounter";
        final String value = "myValue";
        Monitor monitor = mock(Monitor.class);
        when(monitor.getValue()).thenReturn(value);
        when(wrapper.getMonitor(counter)).thenReturn(monitor);
        
        String result = update.getPerformanceCounterString(counter);
        assertEquals(value, result);
    }
    
    @Test(expected=ClassCastException.class)
    public void testGetPerformanceCounterStringBadType() throws VmUpdateException {
        final String counter = "myCounter";
        final Long value = 9001L;
        Monitor monitor = mock(Monitor.class);
        when(monitor.getValue()).thenReturn(value);
        when(wrapper.getMonitor(counter)).thenReturn(monitor);
        
        update.getPerformanceCounterString(counter);
    }
    
    @Test
    public void testGetPerformanceCounterStringNoCounter() throws VmUpdateException {
        final String counter = "myCounter";
        when(wrapper.getMonitor(counter)).thenReturn(null);
        
        String result = update.getPerformanceCounterString(counter);
        assertNull(result);
    }

}

