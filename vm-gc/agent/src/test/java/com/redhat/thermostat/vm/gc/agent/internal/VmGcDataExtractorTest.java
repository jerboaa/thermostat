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

package com.redhat.thermostat.vm.gc.agent.internal;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import sun.jvmstat.monitor.LongMonitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.StringMonitor;

public class VmGcDataExtractorTest {

    private MonitoredVm buildStringMonitoredVm(String monitorName, String monitorReturn) throws MonitorException {
        final StringMonitor monitor = mock(StringMonitor.class);
        when(monitor.stringValue()).thenReturn(monitorReturn);
        when(monitor.getValue()).thenReturn(monitorReturn);
        MonitoredVm vm = mock(MonitoredVm.class);
        when(vm.findByName(monitorName)).thenReturn(monitor);
        return vm;
    }

    private MonitoredVm buildLongMonitoredVm(String monitorName, Long monitorReturn) throws MonitorException {
        final LongMonitor monitor = mock(LongMonitor.class);
        when(monitor.longValue()).thenReturn(monitorReturn);
        when(monitor.getValue()).thenReturn(monitorReturn);
        MonitoredVm vm = mock(MonitoredVm.class);
        when(vm.findByName(monitorName)).thenReturn(monitor);
        return vm;
    }

    @Test
    public void testTotalCollectors() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.policy.collectors";
        final Long MONITOR_VALUE = 9l;
        MonitoredVm vm = buildLongMonitoredVm(MONITOR_NAME, MONITOR_VALUE);

        VmGcDataExtractor extractor = new VmGcDataExtractor(vm);
        Long returned = extractor.getTotalCollectors();

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(MONITOR_VALUE, returned);
    }

    @Test
    public void testCollectorName() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.collector.0.name";
        final String COLLECTOR_NAME = "SomeMemoryCollector";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, COLLECTOR_NAME);

        VmGcDataExtractor extractor = new VmGcDataExtractor(vm);
        String returned = extractor.getCollectorName(0);

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(COLLECTOR_NAME, returned);
    }

    @Test
    public void testCollectorTime() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.collector.0.time";
        final Long COLLECTOR_TIME = 99l;
        MonitoredVm vm = buildLongMonitoredVm(MONITOR_NAME, COLLECTOR_TIME);

        VmGcDataExtractor extractor = new VmGcDataExtractor(vm);
        Long returned = extractor.getCollectorTime(0);

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(COLLECTOR_TIME, returned);
    }

    @Test
    public void testCollectorInvocations() throws MonitorException {
        final String MONITOR_NAME = "sun.gc.collector.0.invocations";
        final Long COLLECTOR_INVOCATIONS = 99l;
        MonitoredVm vm = buildLongMonitoredVm(MONITOR_NAME, COLLECTOR_INVOCATIONS);

        VmGcDataExtractor extractor = new VmGcDataExtractor(vm);
        Long returned = extractor.getCollectorInvocations(0);

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(COLLECTOR_INVOCATIONS, returned);
    }

}

