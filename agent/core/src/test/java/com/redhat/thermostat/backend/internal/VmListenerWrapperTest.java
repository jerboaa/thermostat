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

package com.redhat.thermostat.backend.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.backend.VmUpdate;
import com.redhat.thermostat.backend.VmUpdateException;
import com.redhat.thermostat.backend.VmUpdateListener;
import com.redhat.thermostat.common.internal.test.Bug;

import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.event.VmEvent;

public class VmListenerWrapperTest {

    private VmListenerWrapper wrapper;
    private MonitoredVm monitoredVm;
    private VmUpdateListener listener;

    @Before
    public void setUp() throws Exception {
        listener = mock(VmUpdateListener.class);
        monitoredVm = mock(MonitoredVm.class);
        wrapper = new VmListenerWrapper(listener, monitoredVm);
    }
    
    /**
     * Verify that a bad listener which throws exceptions gets removed
     * from the JVM beyond a threshold.
     * @throws MonitorException 
     */
    @Bug(id = "3242",
         summary = "Adverse Backend breaks other Backends badly ",
         url = "http://icedtea.classpath.org/bugzilla/show_bug.cgi?id=3242")
    @Test
    public void testMonitorsUpdatedListenerExceptions() throws MonitorException {
        final int beyondThresholdLimit = 11;
        VmUpdateListener badListener = new VmUpdateListener() {
            @Override
            public void countersUpdated(VmUpdate update) {
                throw new RuntimeException("countersUpdated() testing!");
            }
        };
        VmListenerWrapper vmListenerWrapper = new VmListenerWrapper(badListener, monitoredVm);
        VmEvent event = mock(VmEvent.class);
        for (int i = 0; i < beyondThresholdLimit; i++) {
            when(event.getMonitoredVm()).thenReturn(monitoredVm);
            
            vmListenerWrapper.monitorsUpdated(event);
            
        }
        verify(monitoredVm, times(1)).removeVmListener(vmListenerWrapper);
    }

    @Test
    public void testMonitorsUpdated() {
        VmEvent event = mock(VmEvent.class);
        when(event.getMonitoredVm()).thenReturn(monitoredVm);
        
        wrapper.monitorsUpdated(event);
        
        ArgumentCaptor<VmUpdateImpl> captor = ArgumentCaptor.forClass(VmUpdateImpl.class);
        verify(listener).countersUpdated(captor.capture());
        VmUpdateImpl update = captor.getValue();
        assertEquals(wrapper, update.getWrapper());
    }
    
    @Test(expected=AssertionError.class)
    public void testMonitorsUpdatedWrongVm() {
        VmEvent event = mock(VmEvent.class);
        MonitoredVm badVm = mock(MonitoredVm.class);
        when(event.getMonitoredVm()).thenReturn(badVm);
        
        wrapper.monitorsUpdated(event);
    }

    @Test
    public void testGetCounter() throws MonitorException, VmUpdateException {
        final String counter = "myCounter";
        
        Monitor monitor = mock(Monitor.class);
        when(monitoredVm.findByName(counter)).thenReturn(monitor);
        
        Monitor result = wrapper.getMonitor(counter);
        assertEquals(monitor, result);
    }
    
    @Test
    public void testGetCounterNotFound() throws MonitorException, VmUpdateException {
        final String counter = "myCounter";
        
        when(monitoredVm.findByName(counter)).thenReturn(null);
        
        Monitor result = wrapper.getMonitor(counter);
        assertNull(result);
    }
    
    @Test(expected=VmUpdateException.class)
    public void testGetCounterError() throws MonitorException, VmUpdateException {
        final String counter = "myCounter";
        
        when(monitoredVm.findByName(counter)).thenThrow(new MonitorException());
        
        wrapper.getMonitor(counter);
    }

}

