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

package com.redhat.thermostat.numa.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.redhat.thermostat.common.ApplicationService;
import com.redhat.thermostat.common.Ordered;
import com.redhat.thermostat.common.Timer;
import com.redhat.thermostat.common.TimerFactory;
import com.redhat.thermostat.common.Version;
import com.redhat.thermostat.numa.common.NumaDAO;
import com.redhat.thermostat.numa.common.NumaHostInfo;
import com.redhat.thermostat.numa.common.NumaNodeStat;
import com.redhat.thermostat.numa.common.NumaStat;
import com.redhat.thermostat.storage.core.WriterID;

public class NumaBackendTest {
    
    private NumaBackend backend;
    private ApplicationService appService;
    private NumaDAO numaDAO;
    private NumaCollector collector;
    private Timer timer;

    @Before
    public void setup() {
        appService = mock(ApplicationService.class);
        TimerFactory timerFactory = mock(TimerFactory.class);
        timer = mock(Timer.class);        
        when(timerFactory.createTimer()).thenReturn(timer);
        when(appService.getTimerFactory()).thenReturn(timerFactory);
        collector = mock(NumaCollector.class);
        Version version = mock(Version.class);
        when(version.getVersionNumber()).thenReturn("0.0.0");
        numaDAO = mock(NumaDAO.class);
        WriterID id = mock(WriterID.class);
        backend = new NumaBackend(appService, numaDAO, collector, version, id);
    }

    @After
    public void tearDown() {
        backend = null;
        collector = null;
        timer = null;
        appService = null;
        numaDAO = null;
    }

    @Test
    public void testActivate() throws IOException, InterruptedException {

        ArgumentCaptor<Runnable> actionCaptor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(timer).setAction(actionCaptor.capture());
        NumaNodeStat stat1 = mock(NumaNodeStat.class);
        NumaNodeStat stat2 = mock(NumaNodeStat.class);
        NumaNodeStat[] stats = new NumaNodeStat[] { stat1, stat2 };
        when(collector.collectData()).thenReturn(stats);
        when(collector.getNumberOfNumaNodes()).thenReturn(42);
        ArgumentCaptor<NumaStat> statCaptor = ArgumentCaptor.forClass(NumaStat.class);
        doNothing().when(numaDAO).putNumaStat(statCaptor.capture());
        
        boolean activated = backend.activate();
        assertTrue(activated);
        assertTrue(backend.isActive());
        verify(timer).setDelay(1);
        verify(timer).setInitialDelay(0);
        verify(timer).setTimeUnit(TimeUnit.SECONDS);
        verify(timer).setSchedulingType(Timer.SchedulingType.FIXED_RATE);
        verify(timer).start();
        verify(timer).setAction(any(Runnable.class));
        verifyNoMoreInteractions(timer);

        Runnable action = actionCaptor.getValue();
        NumaHostInfo info = new NumaHostInfo(null);
        info.setNumNumaNodes(42);
        verify(numaDAO).putNumberOfNumaNodes(info);
        verifyNoMoreInteractions(numaDAO);
        verify(collector).getNumberOfNumaNodes();
        verifyNoMoreInteractions(collector);

        action.run();
        verify(collector).collectData();
        verify(numaDAO).putNumaStat(any(NumaStat.class));
        NumaStat stat = statCaptor.getValue();
        assertSame(stat1, stat.getNodeStats()[0]);
        assertSame(stat2, stat.getNodeStats()[1]);
        long time1 = stat.getTimeStamp();
        verifyNoMoreInteractions(numaDAO);
        verifyNoMoreInteractions(collector);

        Thread.sleep(10);

        action.run();
        verify(collector, times(2)).collectData();
        verify(numaDAO, times(2)).putNumaStat(any(NumaStat.class));
        stat = statCaptor.getValue();
        assertSame(stat1, stat.getNodeStats()[0]);
        assertSame(stat2, stat.getNodeStats()[1]);
        long time2 = stat.getTimeStamp();
        assertTrue(time2 > time1);
        verifyNoMoreInteractions(numaDAO);
        verifyNoMoreInteractions(collector);

        when(collector.collectData()).thenThrow(new IOException());
        action.run();
        verify(collector, times(3)).collectData();
        verifyNoMoreInteractions(collector);
        verifyNoMoreInteractions(numaDAO);

        boolean deactivated = backend.deactivate();
        assertTrue(deactivated);
        verify(timer).stop();
    }

    @Test
    public void testOrderValue() {
        assertEquals(Ordered.ORDER_MEMORY_GROUP + 80, backend.getOrderValue());
    }
}

