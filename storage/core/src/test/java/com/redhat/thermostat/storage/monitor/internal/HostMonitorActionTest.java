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
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.redhat.thermostat.storage.core.AgentId;
import org.junit.Before;
import org.junit.Test;

import com.redhat.thermostat.common.ActionNotifier;
import com.redhat.thermostat.storage.core.HostRef;
import com.redhat.thermostat.storage.core.VmRef;
import com.redhat.thermostat.storage.dao.VmInfoDAO;
import com.redhat.thermostat.storage.model.VmInfo;
import com.redhat.thermostat.storage.monitor.HostMonitor;
import com.redhat.thermostat.storage.monitor.HostMonitor.Action;

public class HostMonitorActionTest {

    private VmInfoDAO vmsDAO;
    
    private ActionNotifier<HostMonitor.Action> notifier;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        vmsDAO = mock(VmInfoDAO.class);
        notifier = mock(ActionNotifier.class);
    }
    
    @Test
    public void testAddRemoveVMS() {

        HostRef host = new HostRef("01", "01");
        
        List<VmInfo> currentVMs = new ArrayList<>();

        VmInfo info_a = mock(VmInfo.class);
        when(info_a.isAlive()).thenReturn(true);
        when(info_a.getVmId()).thenReturn("0");
        when(info_a.getVmPid()).thenReturn(0);
        when(info_a.getVmName()).thenReturn("0");
        
        VmInfo info_b = mock(VmInfo.class);
        when(info_b.isAlive()).thenReturn(true);
        when(info_b.getVmId()).thenReturn("1");
        when(info_b.getVmPid()).thenReturn(1);
        when(info_b.getVmName()).thenReturn("1");
        
        VmInfo info_c = mock(VmInfo.class);
        when(info_c.isAlive()).thenReturn(true);
        when(info_c.getVmId()).thenReturn("2");
        when(info_c.getVmPid()).thenReturn(2);
        when(info_c.getVmName()).thenReturn("2");
        
        VmInfo info_d = mock(VmInfo.class);
        when(info_d.isAlive()).thenReturn(true);
        when(info_d.getVmId()).thenReturn("3");
        when(info_d.getVmPid()).thenReturn(3);
        when(info_d.getVmName()).thenReturn("3");
        
        VmInfo info_e = mock(VmInfo.class);
        when(info_e.isAlive()).thenReturn(false);
        when(info_e.getVmId()).thenReturn("4");
        when(info_e.getVmPid()).thenReturn(4);
        when(info_e.getVmName()).thenReturn("4");
        
        currentVMs.add(info_a);
        currentVMs.add(info_b);
        currentVMs.add(info_c);
        currentVMs.add(info_d);
        currentVMs.add(info_e);

        when(vmsDAO.getAllVmInfosForAgent(any(AgentId.class))).thenReturn(currentVMs);
        

        // the first result is to be notified of all those vms
        HostMonitorAction action = new HostMonitorAction(notifier, vmsDAO, host);
        action.run();

        verify(notifier, times(4)).fireAction(eq(Action.VM_ADDED), any(VmRef.class));
        verify(notifier, times(0)).fireAction(eq(Action.VM_REMOVED), any(VmRef.class));

        // now remove one vm from each side
        currentVMs.remove(info_b);
        currentVMs.remove(info_c);

        action.run();

        verify(notifier, times(4)).fireAction(eq(Action.VM_ADDED), any(VmRef.class));
        verify(notifier, times(2)).fireAction(eq(Action.VM_REMOVED), any(VmRef.class));
        
        when(info_a.isAlive()).thenReturn(false);
        
        // not that a process can ever become alive again :)
        when(info_e.isAlive()).thenReturn(true);

        action.run();

        verify(notifier, times(5)).fireAction(eq(Action.VM_ADDED), any(VmRef.class));
        verify(notifier, times(3)).fireAction(eq(Action.VM_REMOVED), any(VmRef.class));
    }
}

