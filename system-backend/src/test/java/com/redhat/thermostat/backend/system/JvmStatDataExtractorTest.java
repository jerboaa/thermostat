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

package com.redhat.thermostat.backend.system;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.StringMonitor;

public class JvmStatDataExtractorTest {

    private MonitoredVm buildStringMonitoredVm(String monitorName, String monitorReturn) throws MonitorException {
        final StringMonitor monitor = mock(StringMonitor.class);
        when(monitor.stringValue()).thenReturn(monitorReturn);
        when(monitor.getValue()).thenReturn(monitorReturn);
        MonitoredVm vm = mock(MonitoredVm.class);
        when(vm.findByName(monitorName)).thenReturn(monitor);
        return vm;
    }

    @Test
    public void testCommandLine() throws MonitorException {
        final String MONITOR_NAME = "sun.rt.javaCommand";
        final String MONITOR_VALUE = "command line java";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, MONITOR_VALUE);

        JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
        String returned = extractor.getCommandLine();

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(MONITOR_VALUE, returned);
    }

    @Test
    public void testMainClass() throws MonitorException {
        final String MONITOR_NAME = "sun.rt.javaCommand";
        final String MONITOR_VALUE = "some.package.Main";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, MONITOR_VALUE);

        JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
        String returned = extractor.getMainClass();

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(MONITOR_VALUE, returned);
    }

    @Test
    public void testJavaVersion() throws MonitorException {
        final String MONITOR_NAME = "java.property.java.version";
        final String MONITOR_VALUE = "some java version";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, MONITOR_VALUE);

        JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
        String returned = extractor.getJavaVersion();

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(MONITOR_VALUE, returned);
    }

    @Test
    public void testJavaHome() throws MonitorException {
        final String MONITOR_NAME = "java.property.java.home";
        final String MONITOR_VALUE = "${java.home}";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, MONITOR_VALUE);

        JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
        String returned = extractor.getJavaHome();

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(MONITOR_VALUE, returned);
    }

    @Test
    public void testVmName() throws MonitorException {
        final String MONITOR_NAME = "java.property.java.vm.name";
        final String MONITOR_VALUE = "${vm.name}";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, MONITOR_VALUE);

        JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
        String returned = extractor.getVmName();

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(MONITOR_VALUE, returned);
    }

    @Test
    public void testVmInfo() throws MonitorException {
        final String MONITOR_NAME = "java.property.java.vm.info";
        final String MONITOR_VALUE = "${vm.info}";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, MONITOR_VALUE);

        JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
        String returned = extractor.getVmInfo();

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(MONITOR_VALUE, returned);
    }

    @Test
    public void testVmVersion() throws MonitorException {
        final String MONITOR_NAME = "java.property.java.vm.version";
        final String MONITOR_VALUE = "${vm.version}";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, MONITOR_VALUE);

        JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
        String returned = extractor.getVmVersion();

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(MONITOR_VALUE, returned);
    }

    @Test
    public void testVmArguments() throws MonitorException {
        final String MONITOR_NAME = "java.rt.vmArgs";
        final String MONITOR_VALUE = "${vm.arguments}";
        MonitoredVm vm = buildStringMonitoredVm(MONITOR_NAME, MONITOR_VALUE);

        JvmStatDataExtractor extractor = new JvmStatDataExtractor(vm);
        String returned = extractor.getVmArguments();

        verify(vm).findByName(eq(MONITOR_NAME));
        assertEquals(MONITOR_VALUE, returned);
    }

}

