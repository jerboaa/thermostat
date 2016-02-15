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

package com.redhat.thermostat.storage.monitor.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.common.AllPassFilter;
import com.redhat.thermostat.common.Filter;
import com.redhat.thermostat.common.Pair;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.monitor.HostMonitor;

public class HostMonitorImplTest {

    private VmInfoDAO vmDao;
    private TimerFactory timerFactory;
    private Timer timer1;
    
    @Before
    public void setup() {
        vmDao = mock(VmInfoDAO.class);
        timerFactory = mock(TimerFactory.class);
        timer1 = mock(Timer.class);
        when(timerFactory.createTimer()).thenReturn(timer1);
    }
    
    @Test
    public void testGetVirtualMachines() {
        List<VmRef> testData = new ArrayList<>(); 
        List<VmRef> testData2 = new ArrayList<>(); 

        HostRef host0 = new HostRef("0", "0");
        HostRef host1 = new HostRef("1", "1");
        
        VmRef vm0 = new VmRef(host0, "0", 0, "0");
        VmRef vm1 = new VmRef(host0, "1", 1, "2");
        VmRef vm2 = new VmRef(host0, "2", 2, "3");
        VmRef vm3 = new VmRef(host0, "3", 3, "3");
        VmRef vm4 = new VmRef(host0, "4", 4, "4");
        VmRef vm5 = new VmRef(host0, "5", 5, "5");
    
        testData.add(vm0);
        testData.add(vm1);
        testData.add(vm2);
        testData.add(vm3);
        testData.add(vm4);
        testData.add(vm5);
        
        when(vmDao.getVMs(host0)).thenReturn(testData);
        when(vmDao.getVMs(host1)).thenReturn(testData2);

        HostMonitor monitor = new HostMonitorImpl(timerFactory, vmDao);
        List<VmRef> vms = monitor.getVirtualMachines(host0, new AllPassFilter<VmRef>());
        assertEquals(testData.size(), vms.size());
        for (VmRef ref : testData) {
            assertTrue(vms.contains(ref));
        }
        
        vms = monitor.getVirtualMachines(host1, new AllPassFilter<VmRef>());
        assertEquals(0, vms.size());

        Filter<VmRef> bandFilter = new Filter<VmRef>() {
            @Override
            public boolean matches(VmRef toMatch) {
                return toMatch.getName().equals("1") ||
                       toMatch.getName().equals("2") ||
                       toMatch.getName().equals("3");
            }
        };
        
        vms = monitor.getVirtualMachines(host0, bandFilter);
        assertEquals(3, vms.size());
        
        assertTrue(vms.contains(vm1));
        assertTrue(vms.contains(vm2));
        assertTrue(vms.contains(vm3));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        ActionListener<HostMonitor.Action> listener1 = mock(ActionListener.class);
        ActionListener<HostMonitor.Action> listener2 = mock(ActionListener.class);

        HostRef host1 = new HostRef("0", "0");

        HostMonitor monitor = new HostMonitorImpl(timerFactory, vmDao);
        Map<HostRef, Pair<Timer, ActionNotifier<HostMonitor.Action>>> listeners =
                ((HostMonitorImpl) monitor).getListeners();
        assertTrue(listeners.isEmpty());
        
        monitor.addHostChangeListener(host1, listener1);
        
        assertEquals(1, listeners.size());

        verify(timer1, times(1)).setTimeUnit(TimeUnit.MILLISECONDS);
        verify(timer1, times(1)).setDelay(HostMonitorImpl.DELAY);
        verify(timer1, times(1)).setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        verify(timer1, times(1)).start();

        verify(timer1).setAction(any(HostMonitorAction.class));
        
        monitor.addHostChangeListener(host1, listener2);
        
        assertEquals(1, listeners.size());

        verify(timer1, times(1)).setTimeUnit(TimeUnit.MILLISECONDS);
        verify(timer1, times(1)).setDelay(HostMonitorImpl.DELAY);
        verify(timer1, times(1)).start();
        verify(timer1, times(1)).setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        
        verify(timer1).setAction(any(HostMonitorAction.class));
        
        monitor.removeHostChangeListener(host1, listener1);
        verify(timer1, times(0)).stop();
        
        assertEquals(1, listeners.size());
        monitor.removeHostChangeListener(host1, listener2);

        assertTrue(listeners.isEmpty());
        verify(timer1, times(1)).stop();
        
        HostRef host2 = new HostRef("1", "1");

        monitor.addHostChangeListener(host1, listener1);
        monitor.addHostChangeListener(host2, listener2);
    }

}

