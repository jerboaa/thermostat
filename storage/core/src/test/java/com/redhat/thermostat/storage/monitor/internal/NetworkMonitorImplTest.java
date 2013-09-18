/*
 * Copyright 2012, 2013 Red Hat, Inc.
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.ActionListener;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.storage.dao.HostInfoDAO;
import com.redhat.thermostat.storage.monitor.NetworkMonitor;
import com.redhat.thermostat.storage.monitor.NetworkMonitor.Action;

public class NetworkMonitorImplTest {

    private HostInfoDAO hostDao;
    private TimerFactory timerFactory;
    
    @Before
    public void setup() {
        hostDao = mock(HostInfoDAO.class);
        timerFactory = mock(TimerFactory.class);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void test() {
        
        ActionListener<Action> listener1 = mock(ActionListener.class);
        ActionListener<Action> listener2 = mock(ActionListener.class);

        Timer timer = mock(Timer.class);
        when(timerFactory.createTimer()).thenReturn(timer);
        
        NetworkMonitor monitor = new NetworkMonitorImpl(timerFactory, hostDao);
        monitor.addNetworkChangeListener(listener1);
        
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
        verify(timer).setDelay(NetworkMonitorImpl.DELAY);
        verify(timer).setSchedulingType(Timer.SchedulingType.FIXED_RATE);
                
        verify(timer).start();
        verify(timer, times(0)).stop();

        monitor.addNetworkChangeListener(listener2);

        verify(timer, times(1)).start();
        verify(timer, times(0)).stop();

        monitor.removeNetworkChangeListener(listener1);
        verify(timer, times(0)).stop();
        verify(timer, times(1)).start();

        monitor.removeNetworkChangeListener(listener2);
        verify(timer).stop();
    }

}
