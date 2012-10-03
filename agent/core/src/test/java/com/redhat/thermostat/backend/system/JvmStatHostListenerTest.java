/*
 * Copyright 2012 Red Hat, Inc.
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.mockito.Matchers;

import com.redhat.thermostat.common.dao.DAOFactory;
import com.redhat.thermostat.common.dao.VmClassStatDAO;
import com.redhat.thermostat.common.dao.VmInfoDAO;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.StringMonitor;
import sun.jvmstat.monitor.VmIdentifier;
import sun.jvmstat.monitor.event.VmStatusChangeEvent;

public class JvmStatHostListenerTest {

    @Test
    public void testVmStatusChangedAddsVmClassListener() throws Exception {
        VmStatusChangeEvent vmEvent = mock(VmStatusChangeEvent.class);
        Set<Integer> startedVms = new HashSet<Integer>();
        startedVms.add(123);
        when(vmEvent.getStarted()).thenReturn(startedVms);

        MonitoredVm vm = mock(MonitoredVm.class);
        StringMonitor monitor = mock(StringMonitor.class);
        when(monitor.stringValue()).thenReturn("test");
        when(monitor.getValue()).thenReturn("test");
        when(vm.findByName(anyString())).thenReturn(monitor);
        MonitoredHost host = mock(MonitoredHost.class);
        HostIdentifier hostId = mock(HostIdentifier.class);
        when(host.getHostIdentifier()).thenReturn(hostId);
        when(host.getMonitoredVm(any(VmIdentifier.class))).thenReturn(vm);
        when(vmEvent.getMonitoredHost()).thenReturn(host);

        VmClassStatDAO vmClassDAO = mock(VmClassStatDAO.class);
        VmInfoDAO vmInfoDAO = mock(VmInfoDAO.class);
        DAOFactory df = mock(DAOFactory.class);

        JvmStatHostListener l = new JvmStatHostListener(df, vmInfoDAO, null, vmClassDAO ,true);
        SystemBackend backend = mock(SystemBackend.class);
        when(backend.getObserveNewJvm()).thenReturn(true);

        l.vmStatusChanged(vmEvent);

        verify(vm).addVmListener(Matchers.isA(JvmStatVmClassListener.class));
    }

}